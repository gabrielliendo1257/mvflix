package com.guille.media.reproductor.uploader.storage.managedstorage.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.guille.media.reproductor.uploader.storage.managedstorage.application.command.request.StreamingCommand;
import com.guille.media.reproductor.uploader.storage.shared.security.AuthenticatedUser;
import com.guille.media.reproductor.uploader.storage.shared.security.UserProvider;
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
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.PermissionUrl;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.PresignedUploadRequest;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageKey;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageLocation;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageMetadata;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

class StreamingServiceImplTest {

  private static final AuthenticatedUser PEPE = new AuthenticatedUser("pepe", "pepe@mvflix.dev");

  private final UserProvider userProvider = mock(UserProvider.class);
  private final ObjectStorageService objectStoragePort = mock(ObjectStorageService.class);
  private final StorageRepository storageRepository = mock(StorageRepository.class);
  private final UserStorageRepository userStorageRepository = mock(UserStorageRepository.class);

  private final StreamingServiceImpl service =
      new StreamingServiceImpl(
          userProvider,
          objectStoragePort,
          storageRepository,
          userStorageRepository,
          Duration.ofHours(3));

  private static final UserStorage PEPE_STORAGE =
      new UserStorage(
          1L, BucketName.of("movies"), "pepe", StorageQuota.ofGigabytes(10), new StorageUsage(10));

  private StoreObject object(long storageId, StorageSessionStatus status) {
    return new StoreObject(
        "pepe",
        new StorageKey("pepe/movies/movie.mp4"),
        new StorageMetadata("video/mp4", 1024, null, Instant.now()),
        Instant.now(),
        storageId,
        status);
  }

  @Test
  void generateStreamingSessionReturnsPresignedUrlAndTouchesLastSeen() {
    StoreObject completed = this.object(7L, StorageSessionStatus.COMPLETED);

    when(this.userProvider.getAuthenticatedUser()).thenReturn(Mono.just(PEPE));
    when(this.storageRepository.findById(7L)).thenReturn(Mono.just(completed));
    when(this.userStorageRepository.findByOwnerUsername("pepe")).thenReturn(Mono.just(PEPE_STORAGE));
    when(this.objectStoragePort.createStreamingUrl(any(), any(StorageLocation.class)))
        .thenReturn(Mono.just(new PermissionUrl("http://minio/stream", "GET", Map.of())));
    when(this.storageRepository.touchLastSeen(any(Long.class), any(Instant.class))).thenReturn(Mono.empty());

    StepVerifier.create(
            this.service.generateStreamingSession(new StreamingCommand("7")))
        .assertNext(session -> {
          assertThat(session.uploadId()).isEqualTo("7");
          assertThat(session.streamingUrl()).isEqualTo("http://minio/stream");
          assertThat(session.method()).isEqualTo("GET");
          assertThat(session.expiresAt()).isAfterOrEqualTo(Instant.now().plusSeconds(3599));
        })
        .verifyComplete();

    verify(this.storageRepository).touchLastSeen(org.mockito.ArgumentMatchers.eq(7L), any(Instant.class));
  }

  @Test
  void generateStreamingSessionUsesConfiguredTtl() {
    StoreObject completed = this.object(7L, StorageSessionStatus.COMPLETED);
    Duration configuredTtl = Duration.ofMinutes(2);
    when(this.userProvider.getAuthenticatedUser()).thenReturn(Mono.just(PEPE));
    StreamingServiceImpl customService =
        new StreamingServiceImpl(
            userProvider, objectStoragePort, storageRepository, userStorageRepository, configuredTtl);

    when(this.storageRepository.findById(7L)).thenReturn(Mono.just(completed));
    when(this.userStorageRepository.findByOwnerUsername("pepe")).thenReturn(Mono.just(PEPE_STORAGE));
    when(this.objectStoragePort.createStreamingUrl(any(), any(StorageLocation.class)))
        .thenReturn(Mono.just(new PermissionUrl("http://minio/stream", "GET", Map.of())));
    when(this.storageRepository.touchLastSeen(any(Long.class), any(Instant.class)))
        .thenReturn(Mono.empty());

    StepVerifier.create(customService.generateStreamingSession(new StreamingCommand("7")))
        .assertNext(session -> {
          assertThat(session.expiresAt())
              .isAfterOrEqualTo(Instant.now().plus(configuredTtl).minusSeconds(5));
        })
        .verifyComplete();

    org.mockito.ArgumentCaptor<PresignedUploadRequest> requestCaptor =
        org.mockito.ArgumentCaptor.forClass(PresignedUploadRequest.class);
    verify(this.objectStoragePort)
        .createStreamingUrl(requestCaptor.capture(), any(StorageLocation.class));
    assertThat(requestCaptor.getValue().getExpiration()).isEqualTo(configuredTtl);
  }

  @Test
  void generateStreamingSessionRejectsObjectNotAvailable() {
    StoreObject pending = this.object(7L, StorageSessionStatus.PENDING);

    when(this.userProvider.getAuthenticatedUser()).thenReturn(Mono.just(PEPE));
    when(this.storageRepository.findById(7L)).thenReturn(Mono.just(pending));

    StepVerifier.create(this.service.generateStreamingSession(new StreamingCommand("7")))
        .expectError(StorageObjectNotAvailable.class)
        .verify();

    verifyNoInteractions(this.userStorageRepository);
    verifyNoInteractions(this.objectStoragePort);
  }

  @Test
  void previewStreamingRejectsNonOwner() {
    StoreObject anasObject = new StoreObject(
        "ana",
        new StorageKey("ana/videos/movie.mp4"),
        new StorageMetadata("video/mp4", 1024, null, Instant.now()),
        Instant.now(),
        9L,
        StorageSessionStatus.COMPLETED);

    when(this.userProvider.getAuthenticatedUser()).thenReturn(Mono.just(PEPE));
    when(this.storageRepository.findById(9L)).thenReturn(Mono.just(anasObject));

    StepVerifier.create(this.service.generateStreamingSession(new StreamingCommand("9")))
        .expectError(StorageObjectNotAvailable.class)
        .verify();

    verifyNoInteractions(this.userStorageRepository);
    verifyNoInteractions(this.objectStoragePort);
  }

  @Test
  void catalogStreamingAllowsNonOwnerObjectForAuthorizedService() {
    StoreObject anasObject = new StoreObject(
        "ana",
        new StorageKey("ana/videos/movie.mp4"),
        new StorageMetadata("video/mp4", 1024, null, Instant.now()),
        Instant.now(),
        9L,
        StorageSessionStatus.COMPLETED);

    // M2M: el servicio autorizado (scope storage.stream) ya validó visibilidad;
    // no hay usuario autenticado y no se comprueba ownership.
    when(this.userStorageRepository.findByOwnerUsername("ana")).thenReturn(Mono.just(PEPE_STORAGE));
    when(this.storageRepository.findById(9L)).thenReturn(Mono.just(anasObject));
    when(this.objectStoragePort.createStreamingUrl(any(), any(StorageLocation.class)))
        .thenReturn(Mono.just(new PermissionUrl("http://minio/stream", "GET", Map.of())));
    when(this.storageRepository.touchLastSeen(any(Long.class), any(Instant.class)))
        .thenReturn(Mono.empty());

    StepVerifier.create(this.service.generateCatalogStreamingSession(new StreamingCommand("9")))
        .assertNext(session -> assertThat(session.streamingUrl()).isEqualTo("http://minio/stream"))
        .verifyComplete();

    verifyNoInteractions(this.userProvider);
  }
}