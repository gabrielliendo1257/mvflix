package com.gcorp.service.app.mvflix_movies.catalog.domain.media;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemId;

import reactor.core.publisher.Mono;

public interface MediaRepository {

    Mono<ManagedMediaAsset> save(ManagedMediaAsset media);

    Mono<ManagedMediaAsset> findByMovieId(CatalogItemId movieId);
}
