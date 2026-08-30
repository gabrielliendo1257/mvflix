package com.gcorp.service.app.mvflix_movies.library.domain;

import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MediaAssetRepository {

    Mono<MediaAsset> save(MediaAsset asset);

    Mono<MediaAsset> findById(MediaAssetId id);

    /** Compare-and-set: vincula únicamente si el asset continúa sin identificar. */
    Mono<MediaAsset> identifyIfUnidentified(MediaAssetId assetId, CatalogItemId catalogItemId);

    Mono<MediaAsset> findByCatalogItemId(CatalogItemId catalogItemId);

    /** Desvincula los assets de una película sin borrar el catálogo del filesystem. */
    Mono<Long> unlinkByCatalogItemId(CatalogItemId catalogItemId);

    Mono<MediaAsset> findByLibraryAndPath(Long libraryId, String relativePath);

    Flux<MediaAsset> findAllByLibraryId(Long libraryId);

    Flux<MediaAsset> findAllByLibraryIdAndStatus(Long libraryId, MediaAssetStatus status);
}
