package com.guille.media.reproductor.uploader.storage.managedstorage.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.guille.media.reproductor.uploader.storage.shared.security.AuthenticatedUser;
import com.guille.media.reproductor.uploader.storage.shared.security.UserProvider;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception.IllegalStateTransitionException;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception.StorageException;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception.StorageObjectNotAvailable;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageQuota;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageUsage;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StoreObject;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StoreObject.StorageSessionStatus;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.UserStorage;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.ObjectStorageService;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.StorageRepository;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.UserStorageRepository;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.BucketName;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageKey;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageLocation;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageMetadata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.transaction.reactive.TransactionalOperator;

import java.time.Instant;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

class ObjectCleanupServiceImplTest {

  private final UserProvider userProvider = mock(UserProvider.class);
  private final ObjectStorageService objectStoragePort = mock(ObjectStorageService.class);
  private final StorageRepository storageRepository = mock(StorageRepository.class);
  private final UserStorageRepository userStorageRepository = mock(UserStorageRepository.class);
  private final TransactionalOperator transactionalOperator =
      mock(TransactionalOperator.class);
  private final TerminalUploadTransition terminalTransition =
      new TerminalUploadTransition(storageRepository, userStorageRepository, transactionalOperator);
  private final ManagedDeletionTransaction managedDeletionTransaction = mock(ManagedDeletionTransaction.class);
  private final com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.OrphanCleanupQueue
      orphanQueue = mock(com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.OrphanCleanupQueue.class);

  private final DeleteStoredObject deleteStoredObject =
      new DeleteStoredObject(
           objectStoragePort, storageRepository, userStorageRepository, terminalTransition,
           managedDeletionTransaction);

  private final ObjectCleanupServiceImpl service =
      new ObjectCleanupServiceImpl(
          userProvider,
          objectStoragePort,
          storageRepository,
          userStorageRepository,
          terminalTransition,
          orphanQueue,
          deleteStoredObject);

  @BeforeEach
  void passThroughTransaction() {
    when(this.transactionalOperator.transactional(any(Mono.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    org.mockito.Mockito.lenient()
        .when(this.orphanQueue.enqueue(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(Mono.empty());
  }

  private static final AuthenticatedUser PEPE = new AuthenticatedUser("pepe", "pepe@mvflix.dev");

  private static final UserStorage PEPE_STORAGE =
      new UserStorage(
          1L, BucketName.of("movies"), "pepe", StorageQuota.ofGigabytes(10), new StorageUsage(10));

  private static StoreObject completedObject(long storageId, String owner) {
    return new StoreObject(
        owner,
        new StorageKey("k" + storageId),
        new StorageMetadata("video/mp4", 1024, null, Instant.now()),
        Instant.now(),
        storageId,
        StorageSessionStatus.COMPLETED);
  }

  @Test
  void deleteObjectDeletesBlobReleasesQuotaAndMarksDeletedWhenOwner() {
    StoreObject object = completedObject(7L, "pepe");

    when(this.userProvider.getAuthenticatedUser()).thenReturn(Mono.just(PEPE));
    when(this.storageRepository.findById(7L)).thenReturn(Mono.just(object));
    when(this.userStorageRepository.findByOwnerUsername("pepe")).thenReturn(Mono.just(PEPE_STORAGE));
    when(this.userStorageRepository.releaseStorage("pepe", 1024)).thenReturn(Mono.just(1L));
    when(this.storageRepository.updateStatus(object, StorageSessionStatus.COMPLETED))
        .thenReturn(Mono.just(object));

    StepVerifier.create(this.service.deleteObject(7L)).verifyComplete();

    verify(this.objectStoragePort)
        .delete(new StorageLocation(BucketName.of("movies"), new StorageKey("k7")));
    verify(this.userStorageRepository).releaseStorage("pepe", 1024);
    verify(this.storageRepository).updateStatus(object, StorageSessionStatus.COMPLETED);
    assertThat(object.getStorageObjectStatus()).isEqualTo(StorageSessionStatus.DELETED);
  }

  @Test
  void deleteObjectRejectsWhenNotOwner() {
    StoreObject object = completedObject(7L, "otra");

    when(this.userProvider.getAuthenticatedUser()).thenReturn(Mono.just(PEPE));
    when(this.storageRepository.findById(7L)).thenReturn(Mono.just(object));

    StepVerifier.create(this.service.deleteObject(7L))
        .expectError(StorageObjectNotAvailable.class)
        .verify();

    verifyNoInteractions(this.userStorageRepository);
    verifyNoInteractions(this.objectStoragePort);
  }

  @Test
  void deleteObjectRejectsWhenNotFound() {
    when(this.userProvider.getAuthenticatedUser()).thenReturn(Mono.just(PEPE));
    when(this.storageRepository.findById(99L)).thenReturn(Mono.empty());

    StepVerifier.create(this.service.deleteObject(99L))
        .expectError(StorageObjectNotAvailable.class)
        .verify();
  }

  @Test
  void deleteObjectRemovesBlobFirstAndKeepsAccountingWhenTransitionIsRejected() {
    StoreObject object = completedObject(7L, "pepe");

    when(this.userProvider.getAuthenticatedUser()).thenReturn(Mono.just(PEPE));
    when(this.storageRepository.findById(7L)).thenReturn(Mono.just(object));
    when(this.userStorageRepository.findByOwnerUsername("pepe")).thenReturn(Mono.just(PEPE_STORAGE));
    when(this.storageRepository.updateStatus(object, StorageSessionStatus.COMPLETED))
        .thenReturn(Mono.error(new IllegalStateTransitionException("already DELETED")));

    StepVerifier.create(this.service.deleteObject(7L)).verifyComplete();

    // Contrato IDEMPOTENTE: el perdedor de la carrera (fila ya DELETED)
    // responde éxito; el blob se borró y la cuota quedó intacta (solo el
    // ganador del CAS libera bytes).
    verify(this.objectStoragePort)
        .delete(new StorageLocation(BucketName.of("movies"), new StorageKey("k7")));
    verify(this.userStorageRepository, never()).releaseStorage(anyString(), anyLong());
  }

  @Test
  void failedBestEffortDeleteEnqueuesDurableOrphanTask() {
    StoreObject pending =
        new StoreObject(
            "pepe",
            new StorageKey("k1"),
            new StorageMetadata("video/mp4", 1024L, null, Instant.now()),
            Instant.now(),
            1L,
            StorageSessionStatus.PENDING);

    when(this.storageRepository.findPendingCreatedBefore(any(Instant.class)))
        .thenReturn(Flux.just(pending));
    when(this.userStorageRepository.findByOwnerUsername("pepe")).thenReturn(Mono.just(PEPE_STORAGE));
    when(this.storageRepository.updateStatus(pending, StorageSessionStatus.PENDING))
        .thenReturn(Mono.just(pending));
    when(this.userStorageRepository.releaseStorage("pepe", 1024L)).thenReturn(Mono.just(1L));
    doThrow(new StorageException("minio down"))
        .when(this.objectStoragePort)
        .delete(any(StorageLocation.class));

    StepVerifier.create(this.service.expireStaleSessions(Instant.now()))
        .expectNextCount(1)
        .verifyComplete();

    verify(this.orphanQueue).enqueue("movies", "k1", "pepe", "DELETE_FAILED");
    assertThat(pending.getStorageObjectStatus()).isEqualTo(StorageSessionStatus.EXPIRED);
  }

  @Test
  void deleteObjectFailsFastWithoutTouchingDatabaseWhenObjectStoreUnavailable() {
    StoreObject object = completedObject(7L, "pepe");

    when(this.userProvider.getAuthenticatedUser()).thenReturn(Mono.just(PEPE));
    when(this.storageRepository.findById(7L)).thenReturn(Mono.just(object));
    when(this.userStorageRepository.findByOwnerUsername("pepe")).thenReturn(Mono.just(PEPE_STORAGE));
    doThrow(new StorageException("minio down"))
        .when(this.objectStoragePort)
        .delete(any(StorageLocation.class));
    // Si la tx llegara a SUSCRIBIRSE, el test fallaría con esta aserción.
    when(this.storageRepository.updateStatus(any(StoreObject.class), any()))
        .thenReturn(Mono.error(new AssertionError("DB must not be touched when MinIO fails")));
    when(this.userStorageRepository.releaseStorage(anyString(), anyLong()))
        .thenReturn(Mono.error(new AssertionError("quota must not be released when MinIO fails")));

    StepVerifier.create(this.service.deleteObject(7L))
        .expectError(StorageException.class)
        .verify();
  }

  @Test
  void deleteObjectKeepsAccountingWhenTransitionIsRejected() {
    StoreObject object = completedObject(7L, "pepe");

    when(this.userProvider.getAuthenticatedUser()).thenReturn(Mono.just(PEPE));
    when(this.storageRepository.findById(7L)).thenReturn(Mono.just(object));
    when(this.userStorageRepository.findByOwnerUsername("pepe")).thenReturn(Mono.just(PEPE_STORAGE));
    when(this.storageRepository.updateStatus(object, StorageSessionStatus.COMPLETED))
        .thenReturn(Mono.error(new IllegalStateTransitionException("already DELETED")));

    StepVerifier.create(this.service.deleteObject(7L)).verifyComplete();

    // Carrera perdida (ya DELETED): el segundo DELETE es inofensivo y nadie
    // libera cuota dos veces.
    verify(this.objectStoragePort)
        .delete(new StorageLocation(BucketName.of("movies"), new StorageKey("k7")));
    verify(this.userStorageRepository, never()).releaseStorage(anyString(), anyLong());
  }

  @Test
  void retryAfterDatabaseFailureCompletesDeletion() {
    // El endpoint lee para ownership y el use case vuelve a leer para
    // resolver: cada findById devuelve un objeto fresco.
    when(this.userProvider.getAuthenticatedUser()).thenReturn(Mono.just(PEPE));
    when(this.storageRepository.findById(7L))
        .thenAnswer(invocation -> Mono.just(completedObject(7L, "pepe")));
    when(this.userStorageRepository.findByOwnerUsername("pepe"))
        .thenReturn(Mono.just(PEPE_STORAGE));
    // Primer intento: la tx falla tras el release. Reintento: gana el CAS.
    when(this.userStorageRepository.releaseStorage(anyString(), anyLong()))
        .thenReturn(Mono.error(new RuntimeException("db connection lost")), Mono.just(1L));
    when(this.storageRepository.updateStatus(
            any(StoreObject.class), eq(StorageSessionStatus.COMPLETED)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(this.service.deleteObject(7L))
        .expectError(RuntimeException.class)
        .verify();
    StepVerifier.create(this.service.deleteObject(7L)).verifyComplete();

    // El blob se borró en ambos intentos (idempotente); el reintento completó
    // la transición. La garantía de liberación-exactamente-una-vez se prueba
    // contra PostgreSQL real en UploadReservationRollbackTest.
    verify(this.objectStoragePort, times(2))
        .delete(new StorageLocation(BucketName.of("movies"), new StorageKey("k7")));
  }

  @Test
  void concurrentDeleteLoserSucceedsWhenRowAlreadyDeleted() {
    StoreObject object = completedObject(7L, "pepe");

    when(this.userProvider.getAuthenticatedUser()).thenReturn(Mono.just(PEPE));
    when(this.storageRepository.findById(7L)).thenReturn(Mono.just(object));
    when(this.userStorageRepository.findByOwnerUsername("pepe")).thenReturn(Mono.just(PEPE_STORAGE));
    when(this.storageRepository.updateStatus(object, StorageSessionStatus.COMPLETED))
        .thenReturn(Mono.error(new IllegalStateTransitionException("row already DELETED")));

    // El blob ya se borró (idempotente); la fila ya estaba DELETED por otro
    // hilo: resultado para ESTE llamador es éxito, no error.
    StepVerifier.create(this.service.deleteObject(7L)).verifyComplete();

    verify(this.objectStoragePort)
        .delete(new StorageLocation(BucketName.of("movies"), new StorageKey("k7")));
    verify(this.userStorageRepository, never()).releaseStorage(anyString(), anyLong());
  }

  @Test
  void expireStaleSessionsDeletesOrphanObjectReleasesQuotaAndExpiresPendingObjects() {
    StoreObject pending =
        new StoreObject(
            "pepe",
            new StorageKey("k1"),
            new StorageMetadata("video/mp4", 1024, null, Instant.now()),
            Instant.now(),
            1L,
            StorageSessionStatus.PENDING);

    Instant cutoff = Instant.parse("2026-01-01T00:00:00Z");
    when(this.storageRepository.findPendingCreatedBefore(cutoff)).thenReturn(Flux.just(pending));
    when(this.userStorageRepository.findByOwnerUsername("pepe")).thenReturn(Mono.just(PEPE_STORAGE));
    when(this.storageRepository.updateStatus(pending, StorageSessionStatus.PENDING))
        .thenReturn(Mono.just(pending));
    when(this.userStorageRepository.releaseStorage("pepe", 1024)).thenReturn(Mono.just(1L));

    StepVerifier.create(this.service.expireStaleSessions(cutoff)).expectNext(1L).verifyComplete();

    verify(this.objectStoragePort)
        .delete(new StorageLocation(BucketName.of("movies"), new StorageKey("k1")));
    verify(this.userStorageRepository).releaseStorage("pepe", 1024);
    verify(this.storageRepository).updateStatus(pending, StorageSessionStatus.PENDING);
    assertThat(pending.getStorageObjectStatus()).isEqualTo(StorageSessionStatus.EXPIRED);
  }

  @Test
  void expireStillReleasesQuotaWhenObjectDoesNotExistInBucket() {
    StoreObject pending =
        new StoreObject(
            "pepe",
            new StorageKey("k1"),
            new StorageMetadata("video/mp4", 1024, null, Instant.now()),
            Instant.now(),
            1L,
            StorageSessionStatus.PENDING);

    Instant cutoff = Instant.parse("2026-01-01T00:00:00Z");
    when(this.storageRepository.findPendingCreatedBefore(cutoff)).thenReturn(Flux.just(pending));
    when(this.userStorageRepository.findByOwnerUsername("pepe")).thenReturn(Mono.just(PEPE_STORAGE));
    when(this.storageRepository.updateStatus(pending, StorageSessionStatus.PENDING))
        .thenReturn(Mono.just(pending));
    when(this.userStorageRepository.releaseStorage("pepe", 1024)).thenReturn(Mono.just(1L));
    doThrow(new StorageException("bucket down")).when(this.objectStoragePort)
        .delete(any(StorageLocation.class));

    StepVerifier.create(this.service.expireStaleSessions(cutoff)).expectNext(1L).verifyComplete();

    verify(this.userStorageRepository).releaseStorage("pepe", 1024);
    assertThat(pending.getStorageObjectStatus()).isEqualTo(StorageSessionStatus.EXPIRED);
  }
}
