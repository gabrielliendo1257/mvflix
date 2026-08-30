package com.gcorp.service.app.mvflix_movies.catalog.domain.asset;

import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId;

import reactor.core.publisher.Mono;

public interface MediaRepository {

    Mono<ManagedMediaAsset> save(ManagedMediaAsset media);

    Mono<ManagedMediaAsset> findByCatalogItemId(CatalogItemId catalogItemId);
}
