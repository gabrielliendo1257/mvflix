package com.gcorp.service.app.mvflix_movies.application.movie;

import com.gcorp.service.app.mvflix_movies.app.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieAccessDeniedException;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetMovieUseCase {

    private final MovieRepository movieRepository;
    private final UserProvider userProvider;

    /**
     * Detalle solo si la pelicula es visible para el usuario autenticado
     * (la decide {@link Movie#isVisibleTo(String)}: PUBLIC, propia o compartida);
     * si no, 403 sin revelar la existencia.
     */
    public Mono<Movie> execute(MovieId id) {
        return this.userProvider
                .getAuthenticatedUser()
                .flatMap(user -> this.movieRepository
                        .findById(id)
                        .switchIfEmpty(Mono.error(new MovieAccessDeniedException(
                                "Movie not accessible: " + id.value())))
                        .filter(movie -> movie.isVisibleTo(user.subject()))
                        .switchIfEmpty(Mono.error(new MovieAccessDeniedException(
                                "Movie not accessible: " + id.value()))));
    }
}