package com.guille.media.reproductor.uploader.storage.managedstorage.application;

import com.guille.media.reproductor.uploader.storage.managedstorage.application.command.request.DeleteStoredObjectCommand;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception.IllegalStateTransitionException;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception.StorageObjectNotAvailable;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception.UserStorageNotFoundException;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.BucketName;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageLocation;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StoreObject;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StoreObject.StorageSessionStatus;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.UserStorage;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.ObjectStorageService;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.StorageRepository;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.UserStorageRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

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
        return this.storageRepository
                .findById(command.storageId())
                .switchIfEmpty(Mono.error(new StorageObjectNotAvailable(
                        "Storage object not available: " + command.storageId())))
                .flatMap(this::deleteObject)
                .doOnSuccess(unused -> log.info("Objeto eliminado: storageId={}", command.storageId()));
    }

    private Mono<Void> deleteObject(StoreObject object) {
        return this.userStorageRepository
                .findByOwnerUsername(object.getOwnerUsername())
                .switchIfEmpty(Mono.error(new UserStorageNotFoundException(
                        "No storage registered for user: " + object.getOwnerUsername())))
                .flatMap(userStorage -> this.deleteOwnedObject(object, userStorage));
    }

    private Mono<Void> deleteOwnedObject(StoreObject object, UserStorage userStorage) {
        if (!object.markDeleted()) {
            log.info("delete: objeto {} ya estaba DELETED (idempotente)", object.getStorageId());
            return Mono.empty();
        }
        // defer: la transición solo se construye si el DELETE del blob tuvo
        // éxito; si MinIO está caído no se toca la DB ni la cuota.
        return this.deleteBlob(object, userStorage.getBucketName())
                .then(Mono.defer(() -> this.transitionToDeleted(object)));
    }

    private Mono<Void> deleteBlob(StoreObject object, BucketName bucket) {
        return Mono.<Void>fromRunnable(() -> this.objectStoragePort.delete(
                new StorageLocation(bucket, object.getStorageKey())));
    }

    private Mono<Void> transitionToDeleted(StoreObject object) {
        return this.terminalTransition
                .transitionAndRelease(object, StorageSessionStatus.COMPLETED)
                .then()
                .onErrorResume(IllegalStateTransitionException.class,
                        race -> this.storageRepository
                                .findById(object.getStorageId())
                                .filter(current ->
                                        current.getStorageObjectStatus() == StorageSessionStatus.DELETED)
                                .switchIfEmpty(Mono.error(race))
                                .then());
    }
}
