package com.guille.media.reproductor.uploader.storage.presenter.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guille.media.reproductor.uploader.advisors.GlobalExceptionHandler;
import com.guille.media.reproductor.uploader.storage.app.commands.response.StreamingSession;
import com.guille.media.reproductor.uploader.storage.app.commands.response.UploadSession;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.StorageObjectNotAvailable;
import com.guille.media.reproductor.uploader.storage.domain.models.StorageQuota;
import com.guille.media.reproductor.uploader.storage.domain.models.StorageUsage;
import com.guille.media.reproductor.uploader.storage.domain.models.StoreObject.StorageSessionStatus;
import com.guille.media.reproductor.uploader.storage.domain.models.UserStorage;
import com.guille.media.reproductor.uploader.storage.domain.service.StorageService;
import com.guille.media.reproductor.uploader.storage.domain.vos.BucketName;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageKey;
import com.guille.media.reproductor.uploader.storage.presenter.dto.response.StreamingSessionResponse;
import com.guille.media.reproductor.uploader.storage.presenter.dto.response.UploadResponse;
import com.guille.media.reproductor.uploader.storage.presenter.mapper.UploadMapper;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;

import java.time.Instant;

class StorageRestControllerTest {

  private static final String BASE = "/api/v1/movie/storage";

  private final StorageService storageService = mock(StorageService.class);
  private final UploadMapper uploadMapper = mock(UploadMapper.class);
  private final StorageRestController controller =
      new StorageRestController(storageService, uploadMapper);

  private final WebTestClient client =
      WebTestClient.bindToController(controller)
          .controllerAdvice(new GlobalExceptionHandler())
          .build();

  @Test
  void uploadSessionDelegateAndReturns200() {
    UploadSession session =
        new UploadSession(
            "1",
            "http://minio/upload",
            new StorageKey("k1"),
            "PUT",
            Instant.now(),
            StorageSessionStatus.PENDING,
            null);
    when(storageService.createUploadSession(any())).thenReturn(Mono.just(session));
    when(uploadMapper.toUploadResponse(session))
        .thenReturn(new UploadResponse("1", "http://minio/upload", "k1", "PUT", null));

    client
        .post()
        .uri(BASE + "/upload")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"filename\":\"a.mp4\",\"file_size\":100,\"mime_type\":\"video/mp4\"}")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.uploadId")
        .isEqualTo("1")
        .jsonPath("$.storageKey")
        .isEqualTo("k1");

    verify(storageService).createUploadSession(any());
  }

  @Test
  void completeUploadReturns200() {
    when(storageService.completeUpload(42L)).thenReturn(Mono.empty());

    client
        .post()
        .uri(BASE + "/upload/42/complete")
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  void streamingEndpointReturns200() {
    StreamingSession session =
        new StreamingSession(
            "1", "http://minio/stream", new StorageKey("k1"), Instant.now(), "GET");
    when(storageService.generateStreamingSession(any())).thenReturn(Mono.just(session));
    when(uploadMapper.toStreamingSessionResponse(session))
        .thenReturn(
            new StreamingSessionResponse(
                "1", "http://minio/stream", "k1", session.expiresAt().toString(), "GET"));

    client
        .post()
        .uri(BASE + "/streaming")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"objectId\":\"1\"}")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.streamingUrl")
        .isEqualTo("http://minio/stream");
  }

  @Test
  void quotaEndpointReturnsQuotaOfAuthenticatedUser() {
    UserStorage userStorage =
        new UserStorage(
            1L,
            BucketName.of("movies"),
            "pepe",
            StorageQuota.ofGigabytes(10),
            new StorageUsage(1024));
    when(storageService.getUserStorage()).thenReturn(Mono.just(userStorage));

    long quotaBytes = 10L * 1024 * 1024 * 1024;

    client
        .get()
        .uri(BASE + "/quota")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.ownerUsername")
        .isEqualTo("pepe")
        .jsonPath("$.bucketName")
        .isEqualTo("movies")
        .jsonPath("$.quotaBytes")
        .isEqualTo(quotaBytes)
        .jsonPath("$.usedBytes")
        .isEqualTo(1024)
        .jsonPath("$.remainingBytes")
        .isEqualTo(quotaBytes - 1024);
  }

  @Test
  void deleteObjectReturns204() {
    when(storageService.deleteObject(7L)).thenReturn(Mono.empty());

    client.delete().uri(BASE + "/7").exchange().expectStatus().isNoContent();
  }

  @Test
  void deleteObjectNotFoundPropagatesError() {
    when(storageService.deleteObject(99L))
        .thenReturn(Mono.error(new StorageObjectNotAvailable("Storage object not available: 99")));

    client.delete().uri(BASE + "/99").exchange().expectStatus().is5xxServerError();
  }
}