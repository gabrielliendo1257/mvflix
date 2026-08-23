package com.guille.media.bff.experience.addmedia.application;

import com.guille.media.bff.app.dto.MovieEnrichmentPreviewDto;
import com.guille.media.bff.experience.addmedia.application.port.AddMediaMovies;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/** Detalle del candidato seleccionado antes de iniciar el alta. */
@Service
public class PreviewMovieCandidate {

  private final AddMediaMovies movies;

  public PreviewMovieCandidate(AddMediaMovies movies) {
    this.movies = movies;
  }

  public Mono<MovieEnrichmentPreviewDto> preview(Long providerId) {
    return this.movies.previewCandidate(providerId);
  }
}
