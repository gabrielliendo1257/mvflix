package com.guille.media.reproductor.uploader.storage.managedstorage.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception.StorageException;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageKey;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageMetadata;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StoreObject;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StoreObject.StorageSessionStatus;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.StorageRepository;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.UserStorageRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

class TerminalUploadTransitionTest {

  private final StorageRepository storageRepository = mock(StorageRepository.class);
  private final UserStorageRepository userStorageRepository =
      mock(UserStorageRepository.class);
  private final TransactionalOperator operator = mock(TransactionalOperator.class);

  private TerminalUploadTransition transition;

  @BeforeEach
  void setUp() {
    this.transition =
        new TerminalUploadTransition(storageRepository, userStorageRepository, operator);
    when(this.operator.transactional(any(Mono.class)))
        .thenAnswer(inv -> inv.getArgument(0));
  }

  private StoreObject pending(Long id, long size) {
    return new StoreObject(
        "pepe",
        new StorageKey("pepe/videos/a.mp4"),
        new StorageMetadata("video/mp4", size, null, Instant.now()),
        Instant.now(),
        id,
        StorageSessionStatus.PENDING);
  }

  @Test
  void releasesQuotaWhenExactlyOneRowWasUpdated() {
    StoreObject object = this.pending(7L, 1024L);
    object.markFailed();

    when(this.storageRepository.updateStatus(object, StorageSessionStatus.PENDING))
        .thenReturn(Mono.just(object));
    when(this.userStorageRepository.releaseStorage("pepe", 1024L))
        .thenReturn(Mono.just(1L));

    StepVerifier.create(
            this.transition.transitionAndRelease(object, StorageSessionStatus.PENDING))
        .assertNext(updated ->
            assertThat(updated.getStorageObjectStatus()).isEqualTo(StorageSessionStatus.FAILED))
        .verifyComplete();
  }

  @Test
  void zeroRowsOnReleaseFailsTheWholeTransition() {
    StoreObject object = this.pending(7L, 1024L);
    object.markFailed();

    when(this.storageRepository.updateStatus(object, StorageSessionStatus.PENDING))
        .thenReturn(Mono.just(object));
    // Cuenta inexistente / fila no encontrada: release fantasma NO aceptado.
    when(this.userStorageRepository.releaseStorage("pepe", 1024L))
        .thenReturn(Mono.just(0L));

    StepVerifier.create(
            this.transition.transitionAndRelease(object, StorageSessionStatus.PENDING))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(StorageException.class);
          assertThat(error.getMessage()).contains("affected 0 rows");
        })
        .verify();

    // El error ocurre DENTRO de la tx: el CAS se revierte con ella.
    assertThat(object.getStorageObjectStatus()).isEqualTo(StorageSessionStatus.FAILED);
  }
}
