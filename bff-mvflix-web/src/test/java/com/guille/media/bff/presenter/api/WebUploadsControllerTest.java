package com.guille.media.bff.presenter.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.guille.media.bff.app.dto.UploadCreateRequest;
import com.guille.media.bff.app.dto.UploadListItem;
import com.guille.media.bff.app.dto.UploadSessionDto;
import com.guille.media.bff.app.service.WebUploadsService;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class WebUploadsControllerTest {

  private final WebUploadsService webUploadsService = mock(WebUploadsService.class);
  private final WebUploadsController controller = new WebUploadsController(this.webUploadsService);
  private final WebTestClient client = WebTestClient.bindToController(controller).build();

  @Test
  void listReturnsUploadSummaries() {
    when(webUploadsService.list(20))
        .thenReturn(
            Flux.just(
                new UploadListItem(7L, "pepe/a.mp4", "COMPLETED", 1024L, "2026-01-01T00:00:00Z")));

    client
        .get()
        .uri("/web/uploads")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$[0].storageId")
        .isEqualTo(7);
  }

  @Test
  void createDelegatesAndReturnsStorageSession() {
    when(webUploadsService.create(any(UploadCreateRequest.class)))
        .thenReturn(
            Mono.just(
                new UploadSessionDto(
                    "42", "http://minio/up", "pepe/a.mp4", "PUT", "PENDING", null)));

    client
        .post()
        .uri("/web/uploads")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"filename\":\"a.mp4\",\"file_size\":1024,\"mime_type\":\"video/mp4\"}")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.uploadId")
        .isEqualTo("42");
  }

  @Test
  void completeForwards202WhenStorageIsStillVerifying() {
    when(webUploadsService.complete(42L)).thenReturn(Mono.just(HttpStatus.ACCEPTED));

    client
        .post()
        .uri("/web/uploads/42/complete")
        .exchange()
        .expectStatus()
        .isAccepted();
  }

  @Test
  void cancelDelegatesAndReturnsOk() {
    when(webUploadsService.cancel(42L)).thenReturn(Mono.empty());

    client
        .post()
        .uri("/web/uploads/42/cancel")
        .exchange()
        .expectStatus()
        .isOk();
  }
}