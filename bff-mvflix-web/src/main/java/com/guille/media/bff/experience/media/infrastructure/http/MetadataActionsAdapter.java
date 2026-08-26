package com.guille.media.bff.experience.media.infrastructure.http;

import com.guille.media.bff.app.dto.MovieUpdateRequest;
import com.guille.media.bff.app.ports.MoviesWebClient;
import com.guille.media.bff.experience.media.application.MetadataPatch;
import com.guille.media.bff.experience.media.application.port.MetadataActions;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

/**
 * Adapter de edición hacia el puerto global legacy de movies. Traduce el
 * parche de la experiencia (camelCase puro) al cuerpo que la API espera
 * (snake en poster/release).
 */
@Component
@RequiredArgsConstructor
public class MetadataActionsAdapter implements MetadataActions {

  private final MoviesWebClient movies;

  @Override
  public Mono<Void> updateMetadata(long mediaId, MetadataPatch patch) {
    return this.movies.updateMovie(mediaId, new MovieUpdateRequest(
            patch.title(), patch.originalTitle(), patch.year(),
            patch.genres(), patch.duration(), patch.director(),
            patch.cast(), patch.overview(),
            patch.posterUrl(), patch.releaseDate(),
            patch.country(), patch.language(),
            patch.awards(), patch.popularity(), patch.kind()))
        .then();
  }
}
