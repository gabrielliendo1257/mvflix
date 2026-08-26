package com.guille.media.bff.experience.media.infrastructure.http;

import com.guille.media.bff.app.ports.MoviesWebClient;
import com.guille.media.bff.experience.media.application.port.MediaDeletion;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;

/**
 * Adapter del borrado de catálogo. La IDEMPOTENCIA se traduce aquí: el 404 de
 * movies ("no existe" o "no es tuya", sin revelar existencia) se convierte en
 * {@code false}, no en error. Un 403/5xx sí se propaga.
 */
@Component
@RequiredArgsConstructor
public class MediaDeletionAdapter implements MediaDeletion {

  private final MoviesWebClient movies;

  @Override
  public Mono<Boolean> deleteCatalog(long mediaId) {
    return this.movies.deleteMovie(mediaId)
        .thenReturn(true)
        .onErrorResume(WebClientResponseException.NotFound.class, error -> Mono.just(false));
  }
}
