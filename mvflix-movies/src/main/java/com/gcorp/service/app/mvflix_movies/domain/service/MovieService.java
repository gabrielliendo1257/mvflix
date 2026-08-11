package com.gcorp.service.app.mvflix_movies.domain.service;

import com.gcorp.service.app.mvflix_movies.domain.model.Movie;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MovieService {

    Mono<Movie> create(CreateMovieCommand command);

    Mono<Movie> findById(Long id);

    Flux<Movie> list(int limit);
}
