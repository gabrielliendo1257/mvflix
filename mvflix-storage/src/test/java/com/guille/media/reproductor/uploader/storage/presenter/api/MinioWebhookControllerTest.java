package com.guille.media.reproductor.uploader.storage.presenter.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guille.media.reproductor.uploader.storage.domain.service.UploadService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;

class MinioWebhookControllerTest {

  private static final String TOKEN = "webhook-secret";

  private final UploadService uploadService = mock(UploadService.class);
  private final WebTestClient client =
      WebTestClient.bindToController(new MinioWebhookController(uploadService, TOKEN)).build();

  @BeforeEach
  void setUp() {
    when(uploadService.completeUploadByKey(anyString())).thenReturn(Mono.empty());
  }

  @Test
  void rejectsWhenTokenIsMissing() {
    client
        .post()
        .uri("/internal/minio/events")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(putEvent("videos/movie.mp4"))
        .exchange()
        .expectStatus()
        .isUnauthorized();

    verify(uploadService, never()).completeUploadByKey(anyString());
  }

  @Test
  void rejectsWhenTokenDoesNotMatch() {
    client
        .post()
        .uri("/internal/minio/events")
        .header("X-Minio-Token", "wrong-secret")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(putEvent("videos/movie.mp4"))
        .exchange()
        .expectStatus()
        .isUnauthorized();

    verify(uploadService, never()).completeUploadByKey(anyString());
  }

  @Test
  void completesUploadWhenObjectCreatedEventArrives() {
    client
        .post()
        .uri("/internal/minio/events")
        .header("X-Minio-Token", TOKEN)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {
              "EventName": "s3:ObjectCreated:Put",
              "Records": [
                {
                  "eventName": "s3:ObjectCreated:Put",
                  "s3": {
                    "object": { "key": "pepe/videos/movie.mp4", "size": 1048576 }
                  }
                }
              ]
            }
            """)
        .exchange()
        .expectStatus()
        .isOk();

    verify(uploadService).completeUploadByKey("pepe/videos/movie.mp4");
  }

  @Test
  void completesEveryUploadInSingleNotification() {
    client
        .post()
        .uri("/internal/minio/events")
        .header("X-Minio-Token", TOKEN)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            """
            {
              "EventName": "s3:ObjectCreated:Put",
              "Records": [
                { "eventName": "s3:ObjectCreated:Put",
                  "s3": { "object": { "key": "a/1.mp4", "size": 1 } } },
                { "eventName": "s3:ObjectCreated:CompleteMultipartUpload",
                  "s3": { "object": { "key": "b/2.mp4", "size": 2 } } }
              ]
            }
            """)
        .exchange()
        .expectStatus()
        .isOk();

    verify(uploadService).completeUploadByKey("a/1.mp4");
    verify(uploadService).completeUploadByKey("b/2.mp4");
  }

  @Test
  void ignoresEventsThatAreNotObjectCreated() {
    when(uploadService.handleObjectRemoved(anyString())).thenReturn(Mono.empty());

    client
        .post()
        .uri("/internal/minio/events")
        .header("X-Minio-Token", TOKEN)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            """
            {
              "EventName": "s3:ObjectRemoved:Delete",
              "Records": [
                { "eventName": "s3:ObjectRemoved:Delete",
                  "s3": { "object": { "key": "a/1.mp4", "size": 1 } } }
              ]
            }
            """)
        .exchange()
        .expectStatus()
        .isOk();

    verify(uploadService).handleObjectRemoved("a/1.mp4");
    verify(uploadService, never()).completeUploadByKey(anyString());
  }

  @Test
  void acceptsNotificationWithoutRecords() {
    client
        .post()
        .uri("/internal/minio/events")
        .header("X-Minio-Token", TOKEN)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"EventName\":\"s3:TestEvent\",\"Records\":[]}")
        .exchange()
        .expectStatus()
        .isOk();
  }

  private String putEvent(String key) {
    return "{\"EventName\":\"s3:ObjectCreated:Put\",\"Records\":[{\"eventName\":"
        + "\"s3:ObjectCreated:Put\",\"s3\":{\"object\":{\"key\":\""
        + key
        + "\",\"size\":1}}}]}";
  }
}