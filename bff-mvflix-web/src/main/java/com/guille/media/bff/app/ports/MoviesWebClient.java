package com.guille.media.bff.app.ports;

import com.guille.media.bff.app.dto.CreateMovieRequest;
import com.guille.media.bff.app.dto.MovieDto;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Contrato hacia mvflix-movies (catálogo de películas del usuario). */
public interface MoviesWebClient {

  Flux<MovieDto> listMovies(int limit);

  Mono<MovieDto> movieById(Long movieId);

  Mono<MovieDto> createMovie(CreateMovieRequest request);

  /** Transición DRAFT -> READY con el object_key final (la llama el orquestador del BFF). */
  Mono<MovieDto> completeMovie(Long movieId, String objectKey);

  /** Rollback: elimina la película del dueño. */
  Mono<Void> deleteMovie(Long movieId);
}
