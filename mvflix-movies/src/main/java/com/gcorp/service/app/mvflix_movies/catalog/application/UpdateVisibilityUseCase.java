package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.gcorp.service.app.mvflix_movies.app.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieAccessDeniedException;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieVisibility;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/**
 * Cambia la visibilidad (PUBLIC/PRIVATE/SHARED) de una pelicula del catalogo.
 * Solo el dueño (lo decide {@link Movie#isOwnedBy(String)}); el resto ve 403.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateVisibilityUseCase {

    private final MovieRepository movieRepository;
    private final UserProvider userProvider;

    public Mono<Movie> execute(MovieId id, MovieVisibility visibility) {
        return this.userProvider
                .getAuthenticatedUser()
                .flatMap(user -> this.movieRepository
                        .findById(id)
                        .switchIfEmpty(Mono.error(new MovieAccessDeniedException(
                                "Movie not accessible: " + id.value())))
                        .filter(movie -> movie.isOwnedBy(user.subject()))
                        .switchIfEmpty(Mono.error(new MovieAccessDeniedException(
                                "Movie not owned: " + id.value())))
                        .map(movie -> movie.withVisibility(visibility))
                        .flatMap(movie -> this.movieRepository
                                .updateVisibility(id, visibility))
                        .doOnNext(updated -> log.info(
                                "Movie {} visibilidad {} -> {}",
                                id.value(), visibility, updated.getVisibility())));
    }
}