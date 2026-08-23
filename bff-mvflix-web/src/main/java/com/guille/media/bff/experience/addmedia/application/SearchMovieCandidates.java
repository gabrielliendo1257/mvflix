package com.guille.media.bff.experience.addmedia.application;

import com.guille.media.bff.app.dto.MovieEnrichmentSearchDto;
import com.guille.media.bff.app.ports.MoviesWebClient;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

/**
 * Búsqueda de candidatos en el proveedor de metadata. TMDB vive detrás de
 * movies (ADR 0002): el BFF nunca habla con el proveedor directamente.
 */
@Service
@RequiredArgsConstructor
public class SearchMovieCandidates {

  private final MoviesWebClient movies;

  public Flux<MovieEnrichmentSearchDto> search(String query, Integer year) {
    return this.movies.searchCandidates(query, year);
  }
}
