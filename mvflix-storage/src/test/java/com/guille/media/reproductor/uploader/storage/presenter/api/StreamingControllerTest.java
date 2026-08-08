package com.guille.media.reproductor.uploader.storage.presenter.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.guille.media.reproductor.uploader.advisors.GlobalExceptionHandler;
import com.guille.media.reproductor.uploader.storage.app.commands.response.StreamingSession;
import com.guille.media.reproductor.uploader.storage.domain.service.StreamingService;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageKey;
import com.guille.media.reproductor.uploader.storage.presenter.dto.response.StreamingSessionResponse;
import com.guille.media.reproductor.uploader.storage.presenter.mapper.UploadMapper;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;

import java.time.Instant;

class StreamingControllerTest {

  private static final String BASE = "/api/v1/movie/storage";

  private final StreamingService streamingService = mock(StreamingService.class);
  private final UploadMapper uploadMapper = mock(UploadMapper.class);
  private final StreamingController controller =
      new StreamingController(streamingService, uploadMapper);

  private final WebTestClient client =
      WebTestClient.bindToController(controller)
          .controllerAdvice(new GlobalExceptionHandler())
          .build();

  @Test
  void streamingEndpointReturns200() {
    StreamingSession session =
        new StreamingSession(
            "1", "http://minio/stream", new StorageKey("k1"), Instant.now(), "GET");
    when(this.streamingService.generateStreamingSession(any())).thenReturn(Mono.just(session));
    when(this.uploadMapper.toStreamingSessionResponse(session))
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
}