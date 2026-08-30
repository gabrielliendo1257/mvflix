package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemNotFoundException;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemRepository;
import com.gcorp.service.app.mvflix_movies.library.application.port.CatalogItemEnricher;
import com.gcorp.service.app.mvflix_movies.library.domain.CatalogItemId;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/** Implementación de Enrichment para la capacidad solicitada por Library. */
@Service
@RequiredArgsConstructor
public class LibraryCatalogItemEnricher implements CatalogItemEnricher {

    private final CatalogItemRepository movieRepository;
    private final EnrichMovieUseCase enrichMovieUseCase;

    @Override
    public Mono<Void> enrich(
            com.gcorp.service.app.mvflix_movies.library.domain.CatalogItemId catalogItemId,
            Long externalMetadataId) {
        com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemId movieId =
                com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemId.of(catalogItemId.value());
        return this.movieRepository
                .findById(movieId)
                .switchIfEmpty(Mono.error(new CatalogItemNotFoundException(
                        "Movie not found: " + movieId.value())))
                .flatMap(movie -> this.enrichMovieUseCase.enrich(
                        movie, externalMetadataId))
                .then();
    }
}
