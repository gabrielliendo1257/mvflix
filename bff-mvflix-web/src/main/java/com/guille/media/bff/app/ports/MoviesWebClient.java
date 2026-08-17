package com.guille.media.bff.app.ports;

import com.guille.media.bff.app.dto.CreateMovieRequest;
import com.guille.media.bff.app.dto.EnrichMovieSearchDto;
import com.guille.media.bff.app.dto.MovieDto;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Contrato hacia mvflix-movies (catálogo de películas del usuario). */
public interface MoviesWebClient {

  Flux<MovieDto> listMovies(int limit);

  Mono<MovieDto> movieById(Long movieId);

  Mono<MovieDto> createMovie(CreateMovieRequest request);

  /** Transición DRAFT -> READY con el object_id (visible al front) y object_key (solo servidores). */
  Mono<MovieDto> completeMovie(Long movieId, Long objectId, String objectKey);

  /** Rollback: elimina la película del dueño. */
  Mono<Void> deleteMovie(Long movieId);

  /** Candidatos de la fuente externa para el autocompletado interactivo. */
  Flux<EnrichMovieSearchDto> searchCandidates(String query, Integer year);

  /** Autocompletado con el candidato elegido por el usuario. */
  Mono<MovieDto> enrichMovie(Long movieId, Long tmdbId);
}
