package com.guille.media.reproductor.uploader.storage.presenter.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.guille.media.reproductor.uploader.advisors.GlobalExceptionHandler;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.StorageObjectNotAvailable;
import com.guille.media.reproductor.uploader.storage.app.service.ObjectCleanupService;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;

class ObjectCleanupControllerTest {

  private static final String BASE = "/api/v1/movie/storage";

  private final ObjectCleanupService objectCleanupService = mock(ObjectCleanupService.class);
  private final ObjectCleanupController controller =
      new ObjectCleanupController(objectCleanupService);

  private final WebTestClient client =
      WebTestClient.bindToController(controller)
          .controllerAdvice(new GlobalExceptionHandler())
          .build();

  @Test
  void deleteObjectReturns204() {
    when(this.objectCleanupService.deleteObject(7L)).thenReturn(Mono.empty());

    client.delete().uri(BASE + "/7").exchange().expectStatus().isNoContent();
  }

  @Test
  void deleteObjectNotFoundPropagates404() {
    when(this.objectCleanupService.deleteObject(99L))
        .thenReturn(Mono.error(new StorageObjectNotAvailable("Storage object not available: 99")));

    client.delete().uri(BASE + "/99").exchange().expectStatus().isNotFound();
  }
}