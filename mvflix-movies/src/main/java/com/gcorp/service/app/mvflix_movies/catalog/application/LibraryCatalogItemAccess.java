package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemAccessDeniedException;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemRepository;
import com.gcorp.service.app.mvflix_movies.library.application.port.CatalogItemAccess;
import com.gcorp.service.app.mvflix_movies.library.domain.CatalogItemId;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/** Implementación de Catalog para la autorización solicitada por Library. */
@Service
@RequiredArgsConstructor
public class LibraryCatalogItemAccess implements CatalogItemAccess {

    private final CatalogItemRepository movieRepository;

    @Override
    public Mono<Void> requireVisible(
            com.gcorp.service.app.mvflix_movies.library.domain.CatalogItemId catalogItemId,
            String username) {
        com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemId movieId =
                com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemId.of(catalogItemId.value());
        return this.movieRepository
                .findById(movieId)
                .filter(movie -> movie.isVisibleTo(username))
                .switchIfEmpty(Mono.error(new CatalogItemAccessDeniedException(
                        "Movie not accessible: " + movieId.value())))
                .then();
    }
}
