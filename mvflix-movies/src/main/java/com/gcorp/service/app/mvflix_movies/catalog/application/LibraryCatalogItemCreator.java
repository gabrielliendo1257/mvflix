package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.gcorp.service.app.mvflix_movies.catalog.domain.item.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItem;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemRepository;
import com.gcorp.service.app.mvflix_movies.library.application.CatalogItemKind;
import com.gcorp.service.app.mvflix_movies.library.application.port.CatalogItemCreator;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/** Implementación de Catalog para la creación solicitada por Library Ingestion. */
@Service
@RequiredArgsConstructor
public class LibraryCatalogItemCreator implements CatalogItemCreator {

    private final CatalogItemRepository movieRepository;

    @Override
    public Mono<CatalogItemId> createFromLibrary(
            String ownerUsername, String title, CatalogItemKind kind) {
        CatalogItem movie = CatalogItem.fromLibraryAsset(
                ownerUsername, MovieMetadata.onlyTitle(title), toMediaKind(kind));
        return this.movieRepository
                .save(movie)
                .map(saved -> CatalogItemId.of(saved.getId().value()));
    }

    private static MediaKind toMediaKind(CatalogItemKind kind) {
        return switch (kind) {
            case MOVIE -> MediaKind.MOVIE;
            case VIDEO -> MediaKind.VIDEO;
        };
    }
}
