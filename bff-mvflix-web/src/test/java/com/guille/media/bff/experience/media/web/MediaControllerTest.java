package com.guille.media.bff.experience.media.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.guille.media.bff.experience.media.application.MediaDetail;
import com.guille.media.bff.experience.media.application.GetMediaDetail;
import com.guille.media.bff.experience.media.application.LinkMediaProvider;
import com.guille.media.bff.experience.media.application.MediaDetailNotFoundException;
import com.guille.media.bff.experience.media.application.UnlinkMediaProvider;
import com.guille.media.bff.experience.media.application.port.MediaDetailProjection;
import com.guille.media.bff.experience.media.application.port.ProviderActions;
import com.guille.media.bff.presenter.api.ApiExceptionHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;

import java.util.List;

class MediaControllerTest {

  private final GetMediaDetail getMediaDetail = mock(GetMediaDetail.class);
  private final ProviderActions actions = mock(ProviderActions.class);
  private final MediaDetailProjection projection = mock(MediaDetailProjection.class);
  private WebTestClient client;

  @BeforeEach
  void setUp() {
    // Use cases reales sobre el puerto de acciones mockeado: el test cubre
    // acción → refresco del detalle → respuesta en un solo flujo.
    this.client = WebTestClient.bindToController(new MediaController(
            this.getMediaDetail,
            new LinkMediaProvider(this.actions, this.projection),
            new UnlinkMediaProvider(this.actions, this.projection)))
        .controllerAdvice(new ApiExceptionHandler())
        .build();
  }

  private MediaDetail sample() {
    return MediaDetail.from(new MediaDetail.Source(
        42L, "Coraline", null, 2009, "1h 40m", "/c.jpg", "texto",
        List.of("Fantasía"), "Henry Selick", List.of("Dakota Fanning"),
        "MOVIE", "PRIVATE", "READY",
        77L, null, null, 57892L));
  }

  @Test
  void returnsGroupedDetailWithoutAnyPlaybackUrl() {
    when(this.getMediaDetail.execute(42L)).thenReturn(Mono.just(sample()));

    this.client.get()
        .uri("/web/media/42")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.overview.title").isEqualTo("Coraline")
        .jsonPath("$.overview.director").isEqualTo("Henry Selick")
        .jsonPath("$.media.mediaId").isEqualTo(42)
        .jsonPath("$.media.displayStatus").isEqualTo("READY")
        .jsonPath("$.access.source").isEqualTo("MANAGED")
        .jsonPath("$.provider.status").isEqualTo("LINKED")
        .jsonPath("$.provider.providerId").isEqualTo(57892)
        .jsonPath("$.capabilities.play").isEqualTo(true)
        .jsonPath("$.capabilities.unlinkProvider").isEqualTo(true)
        // La URL de reproducción NUNCA vive en el detalle.
        .jsonPath("$.playbackUrl").doesNotExist()
        .jsonPath("$.url").doesNotExist();
  }

  @Test
  void linkProviderMutatesThenReturnsRefreshedDetail() {
    when(this.actions.link(42L, 57892L)).thenReturn(Mono.empty());
    when(this.projection.detail(42L)).thenReturn(Mono.just(linkedSample()));

    this.client.post()
        .uri("/web/media/42/provider")
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
        .bodyValue(new MediaController.LinkProviderRequest(57892L))
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.provider.status").isEqualTo("LINKED")
        .jsonPath("$.provider.providerId").isEqualTo(57892)
        .jsonPath("$.capabilities.unlinkProvider").isEqualTo(true);
  }

  @Test
  void unlinkProviderMutatesThenReturnsRefreshedDetail() {
    when(this.actions.unlink(42L)).thenReturn(Mono.empty());
    when(this.projection.detail(42L))
        .thenReturn(Mono.just(MediaDetail.from(new MediaDetail.Source(
            42L, "Coraline", null, 2009, "1h 40m", "/c.jpg", "texto",
            List.of(), null, List.of(), "MOVIE", "PRIVATE", "READY",
            77L, null, null, null))));

    this.client.delete()
        .uri("/web/media/42/provider")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.provider.status").isEqualTo("NONE")
        .jsonPath("$.provider.providerId").doesNotExist()
        .jsonPath("$.capabilities.linkProvider").isEqualTo(true);
  }

  private MediaDetail linkedSample() {
    return MediaDetail.from(new MediaDetail.Source(
        42L, "Coraline", null, 2009, "1h 40m", "/c.jpg", "texto",
        List.of(), null, List.of(), "MOVIE", "PRIVATE", "READY",
        77L, null, null, 57892L));
  }

  @Test
  void missingMediaMapsTo404MediaNotFound() {
    when(this.getMediaDetail.execute(99L))
        .thenReturn(Mono.error(new MediaDetailNotFoundException(99L)));

    this.client.get()
        .uri("/web/media/99")
        .exchange()
        .expectStatus().isNotFound()
        .expectBody()
        .jsonPath("$.error").isEqualTo("MEDIA_NOT_FOUND");
  }
}
