package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.library;

import com.gcorp.service.app.mvflix_movies.catalog.application.port.LibraryMovieIds;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemId;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAsset;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;

import java.util.List;

/** Adapter in-process que traduce assets identificados de Library a ids de Catalog. */
@Component
@RequiredArgsConstructor
public class LibraryMovieIdsAdapter implements LibraryMovieIds {

    private final MediaAssetRepository mediaAssetRepository;

    @Override
    public Flux<CatalogItemId> findIdentifiedByLibraryIds(List<Long> libraryIds) {
        return Flux.fromIterable(libraryIds)
                .flatMap(this.mediaAssetRepository::findAllByLibraryId)
                .mapNotNull(MediaAsset::getCatalogItemId)
                .map(catalogItemId -> CatalogItemId.of(catalogItemId.value()))
                .distinct();
    }
}
