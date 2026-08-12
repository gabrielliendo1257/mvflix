package com.guille.media.reproductor.uploader.storage.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guille.media.reproductor.uploader.storage.app.commands.requests.CreateUploadCommand;
import com.guille.media.reproductor.uploader.storage.app.commands.response.UploadCompletionResult;
import com.guille.media.reproductor.uploader.storage.app.commands.response.UploadSummary;
import com.guille.media.reproductor.uploader.storage.app.user.UserServiceCommandPort;
import com.guille.media.reproductor.uploader.storage.app.security.AuthenticatedUser;
import com.guille.media.reproductor.uploader.storage.app.security.UserProvider;
import com.guille.media.reproductor.uploader.storage.domain.events.UploadCompletedEvent;
import com.guille.media.reproductor.uploader.storage.domain.events.UploadFailedEvent;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.BucketNotFoundException;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.ExceededQuotaException;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.InvalidObjectContentError;
import com.guille.media.reproductor.uploader.storage.domain.models.StorageKeyGenerator;
import com.guille.media.reproductor.uploader.storage.domain.models.StorageQuota;
import com.guille.media.reproductor.uploader.storage.domain.models.StorageUsage;
import com.guille.media.reproductor.uploader.storage.domain.models.StoreObject;
import com.guille.media.reproductor.uploader.storage.domain.models.StoreObject.StorageSessionStatus;
import com.guille.media.reproductor.uploader.storage.domain.models.UploadConfiguration;
import com.guille.media.reproductor.uploader.storage.domain.models.UploadType;
import com.guille.media.reproductor.uploader.storage.domain.models.UserStorage;
import com.guille.media.reproductor.uploader.storage.domain.ports.ObjectStorageService;
import com.guille.media.reproductor.uploader.storage.domain.ports.StorageEventPublisher;
import com.guille.media.reproductor.uploader.storage.domain.ports.StorageRepository;
import com.guille.media.reproductor.uploader.storage.domain.ports.UserStorageRepository;
import com.guille.media.reproductor.uploader.storage.domain.service.UploadPolicy;
import com.guille.media.reproductor.uploader.storage.domain.vos.BucketName;
import com.guille.media.reproductor.uploader.storage.domain.vos.MimeType;
import com.guille.media.reproductor.uploader.storage.domain.vos.PermissionUrl;
import com.guille.media.reproductor.uploader.storage.domain.vos.PresignedUploadRequest;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageKey;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageLocation;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageMetadata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

class UploadServiceImplTest {

  private final ObjectStorageService objectStoragePort = mock(ObjectStorageService.class);
  private final StorageKeyGenerator storageKeyGenerator = new StorageKeyGenerator();
  private final UploadPolicy uploadPolicy = mock(UploadPolicy.class);
  private final StorageRepository storageRepository = mock(StorageRepository.class);
  private final UserProvider userProvider = mock(UserProvider.class);
  private final UserStorageRepository userStorageRepository = mock(UserStorageRepository.class);
  private final StorageEventPublisher eventPublisher = mock(StorageEventPublisher.class);

  private final UploadServiceImpl service =
      new UploadServiceImpl(
          objectStoragePort,
          storageKeyGenerator,
          uploadPolicy,
          storageRepository,
          userProvider,
          userStorageRepository,
          eventPublisher);

  private static final AuthenticatedUser PEPE = new AuthenticatedUser("pepe", "pepe@mvflix.dev");

  private static final UserStorage PEPE_STORAGE =
      new UserStorage(
          1L, BucketName.of("movies"), "pepe", StorageQuota.ofGigabytes(10), new StorageUsage(0));

  @BeforeEach
  void setUp() {
    when(this.uploadPolicy.resolve(anyLong(), any(MimeType.class)))
        .thenReturn(
            new UploadConfiguration(
                Duration.ofMinutes(15), UploadType.SIMPLE, null, MimeType.of("video/mp4")));
  }

  private StoreObject pendingObject(long storageId) {
    return new StoreObject(
        "pepe",
        new StorageKeyGenerator().generate("pepe", com.guille.media.reproductor.uploader.storage.domain.vos.StorageFolder.from(MimeType.of("video/mp4"))),
        new StorageMetadata("video/mp4", 1024, null, Instant.now()),
        Instant.now(),
        storageId,
        StorageSessionStatus.PENDING);
  }

  private StoreObject completedObject(long storageId) {
    return new StoreObject(
        "pepe",
        new StorageKeyGenerator().generate("pepe", com.guille.media.reproductor.uploader.storage.domain.vos.StorageFolder.from(MimeType.of("video/mp4"))),
        new StorageMetadata("video/mp4", 1024, null, Instant.now()),
        Instant.now(),
        storageId,
        StorageSessionStatus.COMPLETED);
  }

  @Test
  void createUploadSessionReservesQuotaAndReturnsPresignedUrl() {
    StoreObject saved = this.pendingObject(1L);

    when(this.userProvider.getAuthenticatedUser()).thenReturn(Mono.just(PEPE));
    when(this.userStorageRepository.findByOwnerUsername("pepe")).thenReturn(Mono.just(PEPE_STORAGE));
    when(this.objectStoragePort.bucketExists(BucketName.of("movies"))).thenReturn(Mono.just(true));
    when(this.objectStoragePort.createUploadUrl(any(PresignedUploadRequest.class), any(StorageLocation.class)))
        .thenReturn(Mono.just(new PermissionUrl("http://minio/upload", "PUT", Map.of())));
    when(this.userStorageRepository.consumeStorage(any(String.class), anyLong())).thenReturn(Mono.just(1L));
    when(this.storageRepository.save(any(StoreObject.class))).thenReturn(Mono.just(saved));

    StepVerifier.create(
            this.service.createUploadSession(new CreateUploadCommand("a.mp4", 1024, MimeType.of("video/mp4"))))
        .assertNext(session -> {
          assertThat(session.uploadId()).isEqualTo("1");
          assertThat(session.storageKey().key()).startsWith("pepe/videos/");
          assertThat(session.uploadUrl()).isEqualTo("http://minio/upload");
          assertThat(session.method()).isEqualTo("PUT");
          assertThat(session.currentStatus()).isEqualTo(StorageSessionStatus.PENDING);
        })
        .verifyComplete();

    verify(this.userStorageRepository).consumeStorage("pepe", 1024L);
  }

  @Test
  void createUploadSessionFailsWhenBucketDoesNotExist() {
    when(this.userProvider.getAuthenticatedUser()).thenReturn(Mono.just(PEPE));
    when(this.userStorageRepository.findByOwnerUsername("pepe")).thenReturn(Mono.just(PEPE_STORAGE));
    when(this.objectStoragePort.bucketExists(BucketName.of("movies"))).thenReturn(Mono.just(false));

    StepVerifier.create(
            this.service.createUploadSession(new CreateUploadCommand("a.mp4", 1024, MimeType.of("video/mp4"))))
        .expectError(BucketNotFoundException.class)
        .verify();
  }

  @Test
  void createUploadSessionFailsWhenQuotaExceeded() {
    when(this.userProvider.getAuthenticatedUser()).thenReturn(Mono.just(PEPE));
    when(this.userStorageRepository.findByOwnerUsername("pepe")).thenReturn(Mono.just(PEPE_STORAGE));
    when(this.objectStoragePort.bucketExists(BucketName.of("movies"))).thenReturn(Mono.just(true));
    when(this.objectStoragePort.createUploadUrl(any(PresignedUploadRequest.class), any(StorageLocation.class)))
        .thenReturn(Mono.just(new PermissionUrl("http://minio/upload", "PUT", Map.of())));
    when(this.userStorageRepository.consumeStorage(any(String.class), anyLong())).thenReturn(Mono.just(0L));
    when(this.storageRepository.save(any(StoreObject.class)))
        .thenReturn(Mono.just(this.pendingObject(1L)));

    StepVerifier.create(
            this.service.createUploadSession(new CreateUploadCommand("a.mp4", 1024, MimeType.of("video/mp4"))))
        .expectError(ExceededQuotaException.class)
        .verify();
  }

  @Test
  void completeUploadTransitionsToCompletedAndPublishesEvent() {
    StoreObject pending = this.pendingObject(7L);

    when(this.storageRepository.findById(7L)).thenReturn(Mono.just(pending));
    when(this.userStorageRepository.findByOwnerUsername("pepe")).thenReturn(Mono.just(PEPE_STORAGE));
    when(this.objectStoragePort.objectExists(any(StorageLocation.class))).thenReturn(Mono.just(true));
    when(this.objectStoragePort.getMetadata(any(StorageLocation.class)))
        .thenReturn(Mono.just(new StorageMetadata("video/mp4", 1024, null, Instant.now())));
    when(this.storageRepository.updateStatus(pending, StorageSessionStatus.PENDING))
        .thenReturn(Mono.just(pending));

    StepVerifier.create(this.service.completeUpload(7L))
        .expectNext(UploadCompletionResult.completed())
        .verifyComplete();

    assertThat(pending.getStorageObjectStatus()).isEqualTo(StorageSessionStatus.COMPLETED);
    verify(this.eventPublisher).publish(any(UploadCompletedEvent.class));
  }

  @Test
  void completeUploadReleasesQuotaWhenObjectSizeMismatch() {
    StoreObject pending = this.pendingObject(7L);

    when(this.storageRepository.findById(7L)).thenReturn(Mono.just(pending));
    when(this.userStorageRepository.findByOwnerUsername("pepe")).thenReturn(Mono.just(PEPE_STORAGE));
    when(this.objectStoragePort.objectExists(any(StorageLocation.class))).thenReturn(Mono.just(true));
    when(this.objectStoragePort.getMetadata(any(StorageLocation.class)))
        .thenReturn(Mono.just(new StorageMetadata("video/mp4", 42, null, Instant.now())));
    when(this.userStorageRepository.releaseStorage(any(String.class), anyLong()))
        .thenReturn(Mono.just(1L));
    when(this.storageRepository.updateStatus(any(StoreObject.class), any(StorageSessionStatus.class)))
        .thenReturn(Mono.just(pending));

    StepVerifier.create(this.service.completeUpload(7L))
        .expectError(InvalidObjectContentError.class)
        .verify();

    verify(this.objectStoragePort)
        .delete(new StorageLocation(BucketName.of("movies"), pending.getStorageKey()));
    verify(this.userStorageRepository).releaseStorage("pepe", 1024L);
    verify(this.storageRepository).updateStatus(any(StoreObject.class), any(StorageSessionStatus.class));
    verify(this.eventPublisher).publish(any(UploadFailedEvent.class));
    assertThat(pending.getStorageObjectStatus()).isEqualTo(StorageSessionStatus.FAILED);
  }

  @Test
  void completeUploadByKeyTransitionsAndPublishesEvent() {
    StoreObject pending = this.pendingObject(7L);

    when(this.storageRepository.findByObjectKey("pepe/videos/a.mp4")).thenReturn(Mono.just(pending));
    when(this.userStorageRepository.findByOwnerUsername("pepe")).thenReturn(Mono.just(PEPE_STORAGE));
    when(this.objectStoragePort.objectExists(any(StorageLocation.class))).thenReturn(Mono.just(true));
    when(this.objectStoragePort.getMetadata(any(StorageLocation.class)))
        .thenReturn(Mono.just(new StorageMetadata("video/mp4", 1024, null, Instant.now())));
    when(this.storageRepository.updateStatus(pending, StorageSessionStatus.PENDING))
        .thenReturn(Mono.just(pending));

    StepVerifier.create(this.service.completeUploadByKey("pepe/videos/a.mp4")).verifyComplete();

    assertThat(pending.getStorageObjectStatus()).isEqualTo(StorageSessionStatus.COMPLETED);
    verify(this.eventPublisher).publish(any(UploadCompletedEvent.class));
  }

  @Test
  void completeUploadByKeyDoesNotPublishWhenAlreadyCompleted() {
    StoreObject completed = this.completedObject(7L);

    when(this.storageRepository.findByObjectKey("pepe/videos/a.mp4"))
        .thenReturn(Mono.just(completed));
    when(this.userStorageRepository.findByOwnerUsername("pepe")).thenReturn(Mono.just(PEPE_STORAGE));
    when(this.objectStoragePort.objectExists(any(StorageLocation.class))).thenReturn(Mono.just(true));
    when(this.objectStoragePort.getMetadata(any(StorageLocation.class)))
        .thenReturn(Mono.just(new StorageMetadata("video/mp4", 1024, null, Instant.now())));

    StepVerifier.create(this.service.completeUploadByKey("pepe/videos/a.mp4")).verifyComplete();

    assertThat(completed.getStorageObjectStatus()).isEqualTo(StorageSessionStatus.COMPLETED);
    verify(this.storageRepository, never()).updateStatus(any(StoreObject.class), any());
    verify(this.eventPublisher, never()).publish(any(UploadCompletedEvent.class));
  }

  @Test
  void completeUploadByKeyReportsFailureAndReleasesQuotaWithoutNotifying() {
    StoreObject pending = this.pendingObject(7L);

    when(this.storageRepository.findByObjectKey("pepe/videos/a.mp4"))
        .thenReturn(Mono.just(pending));
    when(this.userStorageRepository.findByOwnerUsername("pepe"))
        .thenReturn(Mono.just(PEPE_STORAGE));
    when(this.objectStoragePort.objectExists(any(StorageLocation.class)))
        .thenReturn(Mono.just(true));
    when(this.objectStoragePort.getMetadata(any(StorageLocation.class)))
        .thenReturn(Mono.just(new StorageMetadata("video/mp4", 42, null, Instant.now())));
    when(this.userStorageRepository.releaseStorage(any(String.class), anyLong()))
        .thenReturn(Mono.just(1L));
    when(this.storageRepository.updateStatus(any(StoreObject.class), any(StorageSessionStatus.class)))
        .thenReturn(Mono.just(pending));

    StepVerifier.create(this.service.completeUploadByKey("pepe/videos/a.mp4")).verifyComplete();

    assertThat(pending.getStorageObjectStatus()).isEqualTo(StorageSessionStatus.FAILED);
    verify(this.objectStoragePort)
        .delete(new StorageLocation(BucketName.of("movies"), pending.getStorageKey()));
    verify(this.userStorageRepository).releaseStorage("pepe", 1024L);
    verify(this.storageRepository).updateStatus(
        any(StoreObject.class), any(StorageSessionStatus.class));
    verify(this.eventPublisher, never()).publish(any(UploadCompletedEvent.class));
  }

  @Test
  void completeUploadByKeyDeletesOrphanObjectWhenSessionExpiredOrFailed() {
    StoreObject expired =
        new StoreObject(
            "pepe",
            new StorageKey("pepe/videos/late.mp4"),
            new StorageMetadata("video/mp4", 1024, null, Instant.now()),
            Instant.now(),
            7L,
            StorageSessionStatus.EXPIRED);

    when(this.storageRepository.findByObjectKey("pepe/videos/late.mp4"))
        .thenReturn(Mono.just(expired));
    when(this.userStorageRepository.findByOwnerUsername("pepe"))
        .thenReturn(Mono.just(PEPE_STORAGE));

    StepVerifier.create(this.service.completeUploadByKey("pepe/videos/late.mp4")).verifyComplete();

    verify(this.objectStoragePort)
        .delete(new StorageLocation(BucketName.of("movies"), new StorageKey("pepe/videos/late.mp4")));
    verify(this.storageRepository, never()).updateStatus(any(StoreObject.class), any());
    verify(this.eventPublisher, never()).publish(any());
  }

  @Test
  void completeUploadByKeyIgnoresObjectsNotRegisteredInDatabase() {
    when(this.storageRepository.findByObjectKey("stray/object.mp4")).thenReturn(Mono.empty());

    StepVerifier.create(this.service.completeUploadByKey("stray/object.mp4")).verifyComplete();

    verify(this.userStorageRepository, never()).findByOwnerUsername(anyString());
    verify(this.eventPublisher, never()).publish(any(UploadCompletedEvent.class));
  }

  @Test
  void completeUploadDefersCompletionWhenObjectIsNotYetInObjectStore() {
    StoreObject pending = this.pendingObject(7L);

    when(this.storageRepository.findById(7L)).thenReturn(Mono.just(pending));
    when(this.userStorageRepository.findByOwnerUsername("pepe")).thenReturn(Mono.just(PEPE_STORAGE));
    when(this.objectStoragePort.objectExists(any(StorageLocation.class))).thenReturn(Mono.just(false));

    UploadCompletionResult result = this.service.completeUpload(7L).block();

    assertThat(result.status())
        .isEqualTo(UploadCompletionResult.UploadCompletionStatus.PENDING_VERIFICATION);
    assertThat(pending.getStorageObjectStatus()).isEqualTo(StorageSessionStatus.PENDING);
    verify(this.eventPublisher, never()).publish(any(UploadCompletedEvent.class));
    verify(this.storageRepository, never())
        .updateStatus(any(StoreObject.class), any(StorageSessionStatus.class));
  }
  @Test
  void cancelUploadReleasesQuotaDeletesObjectAndMarksFailed() {
    StoreObject pending = this.pendingObject(7L);

    when(this.storageRepository.findById(7L)).thenReturn(Mono.just(pending));
    when(this.userStorageRepository.findByOwnerUsername("pepe")).thenReturn(Mono.just(PEPE_STORAGE));
    when(this.storageRepository.updateStatus(pending, StorageSessionStatus.PENDING))
        .thenReturn(Mono.just(pending));
    when(this.userStorageRepository.releaseStorage("pepe", 1024L)).thenReturn(Mono.just(1L));

    StepVerifier.create(this.service.cancelUpload(7L)).verifyComplete();

    assertThat(pending.getStorageObjectStatus()).isEqualTo(StorageSessionStatus.FAILED);
    verify(this.objectStoragePort)
        .delete(new StorageLocation(BucketName.of("movies"), pending.getStorageKey()));
    verify(this.userStorageRepository).releaseStorage("pepe", 1024L);
    verify(this.storageRepository).updateStatus(pending, StorageSessionStatus.PENDING);
    verify(this.eventPublisher).publish(any(UploadFailedEvent.class));
  }

  @Test
  void cancelUploadIsNoOpWhenSessionIsNoLongerPending() {
    StoreObject completed = this.completedObject(7L);

    when(this.storageRepository.findById(7L)).thenReturn(Mono.just(completed));

    StepVerifier.create(this.service.cancelUpload(7L)).verifyComplete();

    assertThat(completed.getStorageObjectStatus()).isEqualTo(StorageSessionStatus.COMPLETED);
    verify(this.userStorageRepository, never()).releaseStorage(anyString(), anyLong());
    verify(this.eventPublisher, never()).publish(any());
  }

  @Test
  void handleObjectRemovedMarksPendingAsFailedAndReleasesQuota() {
    StoreObject pending = this.pendingObject(7L);

    when(this.storageRepository.findByObjectKey(anyString())).thenReturn(Mono.just(pending));
    when(this.userStorageRepository.releaseStorage("pepe", 1024L)).thenReturn(Mono.just(1L));
    when(this.storageRepository.updateStatus(pending, StorageSessionStatus.FAILED))
        .thenReturn(Mono.just(pending));

    StepVerifier.create(this.service.handleObjectRemoved("k1")).verifyComplete();

    assertThat(pending.getStorageObjectStatus()).isEqualTo(StorageSessionStatus.FAILED);
    verify(this.userStorageRepository).releaseStorage("pepe", 1024L);
    verify(this.storageRepository).updateStatus(pending, StorageSessionStatus.FAILED);
    verify(this.eventPublisher).publish(any(UploadFailedEvent.class));
  }

  @Test
  void handleObjectRemovedIgnoresCompletedObjects() {
    StoreObject completed = this.completedObject(7L);

    when(this.storageRepository.findByObjectKey(anyString())).thenReturn(Mono.just(completed));

    StepVerifier.create(this.service.handleObjectRemoved("k1")).verifyComplete();

    assertThat(completed.getStorageObjectStatus()).isEqualTo(StorageSessionStatus.COMPLETED);
    verify(this.userStorageRepository, never()).releaseStorage(anyString(), anyLong());
    verify(this.eventPublisher, never()).publish(any());
  }

  @Test
  void listUploadsReturnsRecentSessionsOfAuthenticatedUser() {
    StoreObject pending = this.pendingObject(7L);

    when(this.userProvider.getAuthenticatedUser()).thenReturn(Mono.just(PEPE));
    when(this.storageRepository.findRecentByOwner("pepe", 20)).thenReturn(Flux.just(pending));

    StepVerifier.create(this.service.listUploads(20))
        .expectNext(
            new UploadSummary(
                7L,
                pending.getStorageKey().key(),
                StorageSessionStatus.PENDING,
                1024L,
                pending.getCreatedAt()))
        .verifyComplete();
  }

}

