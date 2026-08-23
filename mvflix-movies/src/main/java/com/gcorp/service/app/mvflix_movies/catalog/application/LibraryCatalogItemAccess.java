package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieAccessDeniedException;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieRepository;
import com.gcorp.service.app.mvflix_movies.library.application.port.CatalogItemAccess;
import com.gcorp.service.app.mvflix_movies.library.domain.CatalogItemId;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/** Implementación de Catalog para la autorización solicitada por Library. */
@Service
@RequiredArgsConstructor
public class LibraryCatalogItemAccess implements CatalogItemAccess {

    private final MovieRepository movieRepository;

    @Override
    public Mono<Void> requireVisible(CatalogItemId catalogItemId, String username) {
        MovieId movieId = MovieId.of(catalogItemId.value());
        return this.movieRepository
                .findById(movieId)
                .filter(movie -> movie.isVisibleTo(username))
                .switchIfEmpty(Mono.error(new MovieAccessDeniedException(
                        "Movie not accessible: " + movieId.value())))
                .then();
    }
}
