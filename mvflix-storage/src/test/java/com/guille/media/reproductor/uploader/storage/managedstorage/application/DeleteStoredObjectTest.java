package com.guille.media.reproductor.uploader.storage.managedstorage.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.guille.media.reproductor.uploader.storage.managedstorage.application.command.request.DeleteStoredObjectCommand;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception.StorageException;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception.StorageObjectNotAvailable;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.BucketName;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageKey;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageLocation;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageMetadata;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageQuota;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageUsage;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StoreObject;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StoreObject.StorageSessionStatus;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.UserStorage;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.ObjectStorageService;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.StorageRepository;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.UserStorageRepository;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

/**
 * El use case es dueño de la transición física de Storage: S3 DELETE →
 * CAS COMPLETED→DELETED → cuota. No valida ownership (eso es del endpoint).
 */
class DeleteStoredObjectTest {

  private final ObjectStorageService objectStoragePort = mock(ObjectStorageService.class);
  private final StorageRepository storageRepository = mock(StorageRepository.class);
  private final UserStorageRepository userStorageRepository = mock(UserStorageRepository.class);
  private final TerminalUploadTransition terminalTransition = mock(TerminalUploadTransition.class);

  private final DeleteStoredObject useCase = new DeleteStoredObject(
      objectStoragePort, storageRepository, userStorageRepository, terminalTransition);

  private static final UserStorage PEPE_STORAGE =
      new UserStorage(
          1L, BucketName.of("movies"), "pepe", StorageQuota.ofGigabytes(10), new StorageUsage(10));

  private static StoreObject completed(long storageId) {
    return new StoreObject(
        "pepe",
        new StorageKey("k" + storageId),
        new StorageMetadata("video/mp4", 1024, null, Instant.now()),
        Instant.now(),
        storageId,
        StorageSessionStatus.COMPLETED);
  }

  @Test
  void deletesBlobThenTransitionsToDeleted() {
    StoreObject object = completed(7L);

    when(this.storageRepository.findById(7L)).thenReturn(Mono.just(object));
    when(this.userStorageRepository.findByOwnerUsername("pepe")).thenReturn(Mono.just(PEPE_STORAGE));
    when(this.terminalTransition.transitionAndRelease(object, StorageSessionStatus.COMPLETED))
        .thenReturn(Mono.just(object));

    StepVerifier.create(this.useCase.execute(new DeleteStoredObjectCommand(7L)))
        .verifyComplete();

    verify(this.objectStoragePort)
        .delete(new StorageLocation(BucketName.of("movies"), new StorageKey("k7")));
    // La transición (CAS + cuota) se delega exactamente UNA vez.
    verify(this.terminalTransition, times(1))
        .transitionAndRelease(object, StorageSessionStatus.COMPLETED);
    assertThat(object.getStorageObjectStatus()).isEqualTo(StorageSessionStatus.DELETED);
  }

  @Test
  void alreadyDeletedIsIdempotentNoOp() {
    StoreObject object = completed(7L);
    object.markDeleted(); // ya DELETED

    when(this.storageRepository.findById(7L)).thenReturn(Mono.just(object));
    when(this.userStorageRepository.findByOwnerUsername("pepe")).thenReturn(Mono.just(PEPE_STORAGE));

    StepVerifier.create(this.useCase.execute(new DeleteStoredObjectCommand(7L)))
        .verifyComplete();

    // No se toca ni el blob ni la cuota/estado.
    verifyNoInteractions(this.objectStoragePort);
    verifyNoInteractions(this.terminalTransition);
  }

  @Test
  void alreadyDeletedIsIdempotent() {
    StoreObject object = completed(7L);
    object.markDeleted();

    when(this.storageRepository.findById(7L)).thenReturn(Mono.just(object));
    when(this.userStorageRepository.findByOwnerUsername("pepe")).thenReturn(Mono.just(PEPE_STORAGE));

    StepVerifier.create(this.useCase.execute(new DeleteStoredObjectCommand(7L), "pepe", "k7"))
        .expectNextMatches(deletion -> deletion.releasedBytes() == 0
            && "ALREADY_ABSENT".equals(deletion.deletionStatus()))
        .verifyComplete();

    verifyNoInteractions(this.objectStoragePort);
    verifyNoInteractions(this.terminalTransition);
  }

  @Test
  void s3DownFailsFastWithoutTouchingDb() {
    StoreObject object = completed(7L);

    when(this.storageRepository.findById(7L)).thenReturn(Mono.just(object));
    when(this.userStorageRepository.findByOwnerUsername("pepe")).thenReturn(Mono.just(PEPE_STORAGE));
    doThrow(new StorageException("minio down"))
        .when(this.objectStoragePort)
        .delete(any(StorageLocation.class));

    StepVerifier.create(this.useCase.execute(new DeleteStoredObjectCommand(7L)))
        .expectError(StorageException.class)
        .verify();

    // Sin DELETE no hay transición: la DB/cuota quedan intactas.
    verifyNoInteractions(this.terminalTransition);
  }

  @Test
  void retryAfterTransitionFailureCompletes() {
    StoreObject first = completed(7L);
    StoreObject retry = completed(7L);

    when(this.storageRepository.findById(7L))
        .thenReturn(Mono.just(first), Mono.just(retry));
    when(this.userStorageRepository.findByOwnerUsername("pepe")).thenReturn(Mono.just(PEPE_STORAGE));
    when(this.terminalTransition.transitionAndRelease(any(StoreObject.class), any()))
        .thenReturn(
            Mono.error(new RuntimeException("db connection lost")),
            Mono.just(retry));

    StepVerifier.create(this.useCase.execute(new DeleteStoredObjectCommand(7L)))
        .expectError(RuntimeException.class)
        .verify();
    StepVerifier.create(this.useCase.execute(new DeleteStoredObjectCommand(7L)))
        .verifyComplete();

    // El DELETE es idempotente: se repite en cada intento sin efectos extra.
    verify(this.objectStoragePort, times(2))
        .delete(new StorageLocation(BucketName.of("movies"), new StorageKey("k7")));
    assertThat(retry.getStorageObjectStatus()).isEqualTo(StorageSessionStatus.DELETED);
  }

  @Test
  void notFoundFails() {
    when(this.storageRepository.findById(99L)).thenReturn(Mono.empty());

    StepVerifier.create(this.useCase.execute(new DeleteStoredObjectCommand(99L)))
        .expectError(StorageObjectNotAvailable.class)
        .verify();

    verify(this.objectStoragePort, never()).delete(any(StorageLocation.class));
  }
}
