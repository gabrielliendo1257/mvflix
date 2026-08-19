package com.gcorp.service.app.mvflix_movies.application.movie;

import com.gcorp.service.app.mvflix_movies.app.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieRepository;
import com.gcorp.service.app.mvflix_movies.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieStatus;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieVisibility;

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
                        new Movie(
                                null,
                                user.subject(),
                                command.metadata().title(),
                                MovieStatus.DRAFT,
                                EnrichmentStatus.RAW,
                                null,
                                command.metadata(),
                                MovieVisibility.PRIVATE)))
                .doOnNext(movie -> log.info("Pelicula creada: id={} owner={}", movie.getId(),
                        movie.getOwnerUsername()));
    }
}