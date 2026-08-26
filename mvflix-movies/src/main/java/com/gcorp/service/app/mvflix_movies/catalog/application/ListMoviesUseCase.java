package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.gcorp.service.app.mvflix_movies.shared.application.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

/**
 * Listado del catálogo con dos lecturas de acceso:
 *
 * <ul>
 *   <li>{@code visible}: PUBLIC + propias + compartidas (Home/Search global).
 *       La política la decide {@code Movie.isVisibleTo} y su traducción SQL.
 *   <li>{@code owned}: solo contenido propio. Es la lectura de
 *       ADMINISTRACIÓN: nunca mezcla contenido ajeno con acciones de
 *       edición/borrado, aunque sea visible para el usuario.
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ListMoviesUseCase {

    static final int MAX_LIMIT = 50;

    private final MovieRepository movieRepository;
    private final UserProvider userProvider;

    public Flux<Movie> execute(String scope, int limit) {
        int capped = Math.min(limit, MAX_LIMIT);
        return this.userProvider
                .getAuthenticatedUser()
                .flatMapMany(user -> "owned".equalsIgnoreCase(scope)
                        ? this.movieRepository.findByOwner(user.subject(), capped)
                        : this.movieRepository.findVisibleMovies(user.subject(), capped));
    }
}
