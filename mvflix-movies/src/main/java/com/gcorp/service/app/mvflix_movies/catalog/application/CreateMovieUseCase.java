package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.gcorp.service.app.mvflix_movies.app.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateMovieUseCase {

    private final MovieRepository movieRepository;
    private final UserProvider userProvider;

    public Mono<Movie> execute(CreateMovieCommand command) {
        return this.userProvider
                .getAuthenticatedUser()
                .doOnNext(user -> log.info("Creando pelicula en DRAFT: owner={} title={}",
                        user.subject(), command.metadata().title()))
                .flatMap(user -> this.movieRepository.save(
                        Movie.createDraft(user.subject(), command.metadata(), command.kind())))
                .doOnNext(movie -> log.info("Pelicula creada: id={} owner={}", movie.getId(),
                        movie.getOwnerUsername()));
    }
}