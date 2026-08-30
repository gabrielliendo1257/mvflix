package com.gcorp.service.app.mvflix_movies.catalog.domain.media;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemId;

import reactor.core.publisher.Mono;

public interface MediaRepository {

    Mono<Media> save(Media media);

    Mono<Media> findByMovieId(CatalogItemId movieId);
}