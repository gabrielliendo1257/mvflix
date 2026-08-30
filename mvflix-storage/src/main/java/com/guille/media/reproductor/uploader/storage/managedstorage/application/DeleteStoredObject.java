package com.guille.media.reproductor.uploader.storage.managedstorage.application;

import com.guille.media.reproductor.uploader.storage.managedstorage.application.command.request.DeleteStoredObjectCommand;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception.IllegalStateTransitionException;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception.StorageObjectNotAvailable;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception.StorageObjectMismatchException;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception.UserStorageNotFoundException;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.BucketName;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageLocation;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageObject;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageObject.StorageSessionStatus;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.UserStorage;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.ObjectStorageService;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.StorageRepository;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.UserStorageRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * Caso de uso: elimina un objeto almacenado. Es el dueño de la transición de
 * Storage y NO conoce Movies ni valida ownership (eso lo hace el llamante).
 *
 * <p>Secuencia, idempotente y retry-safe:
 * <ol>
 *   <li>DELETE en el object store (idempotente; si está caído, falla sin tocar
 *       PostgreSQL).</li>
 *   <li>CAS COMPLETED → DELETED + liberación de cuota, atómicos en una
 *       transacción local.</li>
 * </ol>
 *
 * <p>Si el objeto ya estaba DELETED, responde éxito sin efecto (no-op). Un
 * fallo de DB deja la fila COMPLETED con su cuota y el reintento repite el
 * DELETE (no-op) y reintenta la transacción.
 */
@Slf4j
@Component
public class DeleteStoredObject {

    private final ObjectStorageService objectStoragePort;
    private final StorageRepository storageRepository;
    private final UserStorageRepository userStorageRepository;
    private final TerminalUploadTransition terminalTransition;

    public DeleteStoredObject(
            ObjectStorageService objectStoragePort,
            StorageRepository storageRepository,
            UserStorageRepository userStorageRepository,
            TerminalUploadTransition terminalTransition) {
        this.objectStoragePort = objectStoragePort;
        this.storageRepository = storageRepository;
        this.userStorageRepository = userStorageRepository;
        this.terminalTransition = terminalTransition;
    }

    public Mono<Void> execute(DeleteStoredObjectCommand command) {
        return this.executeAndReturnResult(command, null, null, null).then();
    }

    public Mono<DeletionResult> execute(
            DeleteStoredObjectCommand command,
            String expectedOwner,
            String expectedObjectKey,
            Function<DeletionResult, Mono<Void>> afterDeletion) {
        return this.executeAndReturnResult(command, expectedOwner, expectedObjectKey, afterDeletion);
    }

    public Mono<DeletionResult> execute(
            DeleteStoredObjectCommand command, String expectedOwner, String expectedObjectKey) {
        return this.executeAndReturnResult(command, expectedOwner, expectedObjectKey, null);
    }

    private Mono<DeletionResult> executeAndReturnResult(
            DeleteStoredObjectCommand command,
            String expectedOwner,
            String expectedObjectKey,
            Function<DeletionResult, Mono<Void>> afterDeletion) {
        return this.storageRepository
                .findById(command.storageId())
                .switchIfEmpty(Mono.error(new StorageObjectNotAvailable(
                        "Storage object not available: " + command.storageId())))
                .flatMap(object -> this.deleteObject(object, expectedOwner, expectedObjectKey, afterDeletion))
                .doOnSuccess(unused -> log.info("Objeto eliminado: storageId={}", command.storageId()));
    }

    private Mono<DeletionResult> deleteObject(
            StorageObject object,
            String expectedOwner,
            String expectedObjectKey,
            Function<DeletionResult, Mono<Void>> afterDeletion) {
        if (expectedOwner != null && !expectedOwner.equals(object.getOwnerUsername())) {
            return Mono.error(new StorageObjectMismatchException("ownerUsername", object.getStorageId()));
        }
        if (expectedObjectKey != null && !expectedObjectKey.equals(object.getStorageKey().key())) {
            return Mono.error(new StorageObjectMismatchException("objectKey", object.getStorageId()));
        }
        return this.userStorageRepository
                .findByOwnerUsername(object.getOwnerUsername())
                .switchIfEmpty(Mono.error(new UserStorageNotFoundException(
                        "No storage registered for user: " + object.getOwnerUsername())))
                .flatMap(userStorage -> this.deleteOwnedObject(object, userStorage, afterDeletion));
    }

    private Mono<DeletionResult> deleteOwnedObject(
            StorageObject object,
            UserStorage userStorage,
            Function<DeletionResult, Mono<Void>> afterDeletion) {
        if (!object.markDeleted()) {
            log.info("delete: objeto {} ya estaba DELETED (idempotente)", object.getStorageId());
            DeletionResult result = new DeletionResult(0, "ALREADY_ABSENT");
            return afterDeletion == null
                    ? Mono.just(result)
                    : afterDeletion.apply(result).thenReturn(result);
        }
        // defer: la transición solo se construye si el DELETE del blob tuvo
        // éxito; si MinIO está caído no se toca la DB ni la cuota.
        return this.deleteBlob(object, userStorage.getBucketName())
                .then(Mono.defer(() -> this.transitionToDeleted(
                         object, new DeletionResult(object.sizeInBytes(), "DELETED"), afterDeletion)))
                .thenReturn(new DeletionResult(object.sizeInBytes(), "DELETED"));
    }

    private Mono<Void> deleteBlob(StorageObject object, BucketName bucket) {
        return Mono.<Void>fromRunnable(() -> this.objectStoragePort.delete(
                new StorageLocation(bucket, object.getStorageKey())));
    }

    private Mono<Void> transitionToDeleted(
            StorageObject object,
            DeletionResult result,
            Function<DeletionResult, Mono<Void>> afterDeletion) {
        Mono<StorageObject> transition = afterDeletion == null
                ? this.terminalTransition.transitionAndRelease(object, StorageSessionStatus.COMPLETED)
                : this.terminalTransition.transitionAndRelease(object, StorageSessionStatus.COMPLETED,
                        ignored -> afterDeletion.apply(result));
        return transition
                .then()
                .onErrorResume(IllegalStateTransitionException.class,
                        race -> this.storageRepository
                                .findById(object.getStorageId())
                                .filter(current ->
                                        current.getStorageObjectStatus() == StorageSessionStatus.DELETED)
                                .switchIfEmpty(Mono.error(race))
                                .then());
    }

    public record DeletionResult(long releasedBytes, String deletionStatus) {}
}
