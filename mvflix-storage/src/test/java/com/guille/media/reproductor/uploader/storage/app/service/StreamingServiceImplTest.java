package com.guille.media.reproductor.uploader.storage.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.guille.media.reproductor.uploader.storage.app.commands.requests.StreamingCommand;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.StorageObjectNotAvailable;
import com.guille.media.reproductor.uploader.storage.domain.models.StorageQuota;
import com.guille.media.reproductor.uploader.storage.domain.models.StorageUsage;
import com.guille.media.reproductor.uploader.storage.domain.models.StoreObject;
import com.guille.media.reproductor.uploader.storage.domain.models.StoreObject.StorageSessionStatus;
import com.guille.media.reproductor.uploader.storage.domain.models.UploadConfiguration;
import com.guille.media.reproductor.uploader.storage.domain.models.UploadType;
import com.guille.media.reproductor.uploader.storage.domain.models.UserStorage;
import com.guille.media.reproductor.uploader.storage.domain.ports.ObjectStorageService;
import com.guille.media.reproductor.uploader.storage.domain.ports.StorageRepository;
import com.guille.media.reproductor.uploader.storage.domain.ports.UserStorageRepository;
import com.guille.media.reproductor.uploader.storage.domain.service.UploadPolicy;
import com.guille.media.reproductor.uploader.storage.domain.vos.BucketName;
import com.guille.media.reproductor.uploader.storage.domain.vos.MimeType;
import com.guille.media.reproductor.uploader.storage.domain.vos.PermissionUrl;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageKey;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageLocation;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageMetadata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

class StreamingServiceImplTest {

  private final ObjectStorageService objectStoragePort = mock(ObjectStorageService.class);
  private final UploadPolicy uploadPolicy = mock(UploadPolicy.class);
  private final StorageRepository storageRepository = mock(StorageRepository.class);
  private final UserStorageRepository userStorageRepository = mock(UserStorageRepository.class);

  private final StreamingServiceImpl service =
      new StreamingServiceImpl(objectStoragePort, uploadPolicy, storageRepository, userStorageRepository);

  private static final UserStorage PEPE_STORAGE =
      new UserStorage(
          1L, BucketName.of("movies"), "pepe", StorageQuota.ofGigabytes(10), new StorageUsage(10));

  @BeforeEach
  void setUp() {
    when(this.uploadPolicy.resolve(anyLong(), any(MimeType.class)))
        .thenReturn(
            new UploadConfiguration(
                Duration.ofMinutes(15), UploadType.SIMPLE, null, MimeType.of("video/mp4")));
  }

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
        })
        .verifyComplete();

    verify(this.storageRepository).touchLastSeen(org.mockito.ArgumentMatchers.eq(7L), any(Instant.class));
  }

  @Test
  void generateStreamingSessionRejectsObjectNotAvailable() {
    StoreObject pending = this.object(7L, StorageSessionStatus.PENDING);

    when(this.storageRepository.findById(7L)).thenReturn(Mono.just(pending));

    StepVerifier.create(this.service.generateStreamingSession(new StreamingCommand("7")))
        .expectError(StorageObjectNotAvailable.class)
        .verify();

    verifyNoInteractions(this.userStorageRepository);
    verifyNoInteractions(this.objectStoragePort);
  }
}