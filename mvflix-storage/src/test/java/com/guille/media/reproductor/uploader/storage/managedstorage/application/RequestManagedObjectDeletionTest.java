package com.guille.media.reproductor.uploader.storage.managedstorage.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guille.media.reproductor.uploader.storage.managedstorage.application.command.request.DeleteStoredObjectCommand;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception.StorageObjectMismatchException;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception.StorageObjectNotAvailable;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageKey;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageMetadata;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StoreObject;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StoreObject.StorageSessionStatus;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.StorageRepository;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

/**
 * El guard M2M protege contra borrar el objeto equivocado: el owner y el
 * objectKey del cuerpo deben coincidir con el objeto real antes de delegar.
 */
class RequestManagedObjectDeletionTest {

  private final StorageRepository storageRepository = mock(StorageRepository.class);
  private final DeleteStoredObject deleteStoredObject = mock(DeleteStoredObject.class);

  private final RequestManagedObjectDeletion useCase =
      new RequestManagedObjectDeletion(storageRepository, deleteStoredObject);

  private static final String OWNER = "Admin";
  private static final String KEY = "Admin/videos/abc.mp4";

  private static StoreObject object(long storageId, String owner, String key) {
    return new StoreObject(
        owner,
        new StorageKey(key),
        new StorageMetadata("video/mp4", 1024, null, Instant.now()),
        Instant.now(),
        storageId,
        StorageSessionStatus.COMPLETED);
  }

  @Test
  void correctOwnerAndKeyDelegatesToDeletion() {
    StoreObject object = object(7L, OWNER, KEY);
    when(this.storageRepository.findById(7L)).thenReturn(Mono.just(object));
    when(this.deleteStoredObject.execute(any(DeleteStoredObjectCommand.class)))
        .thenReturn(Mono.empty());

    StepVerifier.create(this.useCase.execute(7L, OWNER, KEY)).verifyComplete();

    verify(this.deleteStoredObject).execute(new DeleteStoredObjectCommand(7L));
  }

  @Test
  void wrongOwnerIsRejected() {
    StoreObject object = object(7L, "Otro", KEY);
    when(this.storageRepository.findById(7L)).thenReturn(Mono.just(object));

    StepVerifier.create(this.useCase.execute(7L, OWNER, KEY))
        .expectError(StorageObjectMismatchException.class)
        .verify();

    verify(this.deleteStoredObject, never()).execute(any(DeleteStoredObjectCommand.class));
  }

  @Test
  void wrongObjectKeyIsRejected() {
    StoreObject object = object(7L, OWNER, "otro/key.mp4");
    when(this.storageRepository.findById(7L)).thenReturn(Mono.just(object));

    StepVerifier.create(this.useCase.execute(7L, OWNER, KEY))
        .expectError(StorageObjectMismatchException.class)
        .verify();

    verify(this.deleteStoredObject, never()).execute(any(DeleteStoredObjectCommand.class));
  }

  @Test
  void notFoundIsRejected() {
    when(this.storageRepository.findById(99L)).thenReturn(Mono.empty());

    StepVerifier.create(this.useCase.execute(99L, OWNER, KEY))
        .expectError(StorageObjectNotAvailable.class)
        .verify();
  }

  @Test
  void repeatOnAlreadyDeletedObjectStillPassesGuardAndIsIdempotent() {
    // El objeto ya está DELETED pero la asociación sigue siendo válida: el
    // guard pasa y la idempotencia real (no-op) vive en DeleteStoredObject.
    StoreObject object = object(7L, OWNER, KEY);
    object.markDeleted();
    when(this.storageRepository.findById(7L)).thenReturn(Mono.just(object));
    when(this.deleteStoredObject.execute(any(DeleteStoredObjectCommand.class)))
        .thenReturn(Mono.empty());

    StepVerifier.create(this.useCase.execute(7L, OWNER, KEY)).verifyComplete();

    verify(this.deleteStoredObject).execute(new DeleteStoredObjectCommand(7L));
  }
}
