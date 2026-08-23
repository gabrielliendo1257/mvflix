package com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guille.media.reproductor.uploader.advisors.GlobalExceptionHandler;
import com.guille.media.reproductor.uploader.storage.managedstorage.application.command.response.UploadSession;
import com.guille.media.reproductor.uploader.storage.managedstorage.application.command.response.UploadSummary;
import com.guille.media.reproductor.uploader.storage.managedstorage.application.command.response.UploadCompletionResult;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StoreObject.StorageSessionStatus;
import com.guille.media.reproductor.uploader.storage.managedstorage.application.UploadService;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageKey;
import com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.web.UploadResponse;
import com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.web.UploadSummaryResponse;
import com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.web.UploadMapper;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

class UploadControllerTest {

  private static final String BASE = "/api/v1/movie/storage";

  private final UploadService uploadService = mock(UploadService.class);
  private final UploadMapper uploadMapper = mock(UploadMapper.class);
  private final UploadController controller = new UploadController(uploadService, uploadMapper);

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
    when(this.uploadService.createUploadSession(any())).thenReturn(Mono.just(session));
    when(this.uploadMapper.toUploadResponse(session))
        .thenReturn(new UploadResponse("1", "http://minio/upload", "k1", "PUT", StorageSessionStatus.PENDING, null));

    this.client
        .post()
        .uri(BASE + "/upload")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue("{\"filename\":\"a.mp4\",\"file_size\":100,\"mime_type\":\"video/mp4\"}")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.uploadId")
        .isEqualTo("1")
        .jsonPath("$.storageKey")
        .isEqualTo("k1");

    verify(this.uploadService).createUploadSession(any());
  }

  @Test
  void uploadStatusReturns200() {
    UploadSession session =
        new UploadSession(
            "42",
            null,
            new StorageKey("k42"),
            null,
            null,
            StorageSessionStatus.FAILED,
            null);
    when(this.uploadService.getUploadStatus(42L)).thenReturn(Mono.just(session));
    when(this.uploadMapper.toUploadResponse(session))
        .thenReturn(new UploadResponse("42", null, "k42", null, StorageSessionStatus.FAILED, null));

    this.client
        .get()
        .uri(BASE + "/upload/42")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.uploadId")
        .isEqualTo("42")
        .jsonPath("$.storageKey")
        .isEqualTo("k42");

    verify(this.uploadService).getUploadStatus(42L);
  }

  @Test
  void completeUploadReturns200() {
    when(this.uploadService.completeUpload(42L)).thenReturn(Mono.empty());

    client
        .post()
        .uri(BASE + "/upload/42/complete")
        .exchange()
        .expectStatus()
        .isOk();
  }
  @Test
  void completeUploadReturns202WhenVerificationIsPending() {
    when(this.uploadService.completeUpload(42L))
        .thenReturn(Mono.just(UploadCompletionResult.pendingVerification()));

    client
        .post()
        .uri(BASE + "/upload/42/complete")
        .exchange()
        .expectStatus()
        .isAccepted();
  }

  @Test
  void cancelUploadReturns200() {
    when(this.uploadService.cancelUpload(42L)).thenReturn(Mono.empty());

    client
        .post()
        .uri(BASE + "/upload/42/cancel")
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  void uploadsListReturnsUploadSummaries() {
    UploadSummary summary =
        new UploadSummary(42L, "pepe/videos/a.mp4", StorageSessionStatus.PENDING, 1024L, Instant.now());
    when(this.uploadService.listUploads(20)).thenReturn(Flux.just(summary));
    when(this.uploadMapper.toUploadSummaryResponse(summary))
        .thenReturn(
            new UploadSummaryResponse(42L, "pepe/videos/a.mp4", StorageSessionStatus.PENDING, 1024L, Instant.now()));

    client
        .get()
        .uri(BASE + "/uploads")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$[0].storageId")
        .isEqualTo(42);
  }

}