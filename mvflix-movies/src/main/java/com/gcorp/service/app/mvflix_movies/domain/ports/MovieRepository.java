package com.gcorp.service.app.mvflix_movies.domain.ports;

import com.gcorp.service.app.mvflix_movies.domain.model.Movie;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MovieRepository {

    Mono<Movie> save(Movie movie);

    Mono<Movie> findById(Long id);

    Flux<Movie> findByOwner(String ownerUsername, int limit);
}
