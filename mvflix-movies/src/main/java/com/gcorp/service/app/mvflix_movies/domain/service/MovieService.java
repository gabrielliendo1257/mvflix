package com.gcorp.service.app.mvflix_movies.domain.service;

import com.gcorp.service.app.mvflix_movies.domain.model.Movie;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MovieService {

    Mono<Movie> create(CreateMovieCommand command);

    Mono<Movie> findById(Long id);

    Flux<Movie> list(int limit);

    /** Transición DRAFT -> READY con asignación de object_key. Idempotente si ya está READY. */
    Mono<Movie> complete(Long id, String objectKey);

    /** Rollback: elimina la película del dueño. */
    Mono<Void> delete(Long id);
}
