package com.gcorp.service.app.mvflix_movies.catalog.application.port;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemId;

import reactor.core.publisher.Mono;

/** Capacidad de Library requerida antes de eliminar un item del Catalog. */
public interface LibraryAssetLinks {

    Mono<Long> unlinkByMovieId(CatalogItemId movieId);
}
