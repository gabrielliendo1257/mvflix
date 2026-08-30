package com.guille.media.reproductor.uploader.storage.managedstorage.application;

import com.guille.media.reproductor.uploader.storage.managedstorage.application.command.request.DeleteStoredObjectCommand;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception.StorageObjectMismatchException;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception.StorageObjectNotAvailable;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageObject;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.StorageRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

/**
 * Borrado M2M de un objeto MANAGED (Movies → Storage, scope
 * {@code storage.objects.delete}). El guard explícito (owner + objectKey) del
 * cuerpo protege contra borrar el objeto equivocado cuando la asociación del
 * catálogo está corrupta.
 *
 * <p>La transición física vive en {@link DeleteStoredObject}; aquí solo se
 * verifica la asociación y se delega.
 */
@Component
@RequiredArgsConstructor
public class RequestManagedObjectDeletion {

    private final StorageRepository storageRepository;
    private final DeleteStoredObject deleteStoredObject;

    public Mono<Void> execute(Long storageId, String expectedOwner, String expectedObjectKey) {
        return this.storageRepository
                .findById(storageId)
                .switchIfEmpty(Mono.error(new StorageObjectNotAvailable(
                        "Storage object not available: " + storageId)))
                .flatMap(object -> this.guardAndDelete(storageId, object, expectedOwner, expectedObjectKey));
    }

    private Mono<Void> guardAndDelete(
            Long storageId, StorageObject object, String expectedOwner, String expectedObjectKey) {
        if (!object.getOwnerUsername().equals(expectedOwner)) {
            return Mono.error(new StorageObjectMismatchException("owner", storageId));
        }
        if (!object.getStorageKey().key().equals(expectedObjectKey)) {
            return Mono.error(new StorageObjectMismatchException("objectKey", storageId));
        }
        return this.deleteStoredObject.execute(new DeleteStoredObjectCommand(storageId));
    }
}
