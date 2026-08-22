package com.gcorp.service.app.mvflix_movies.library.application.port;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId;

import reactor.core.publisher.Mono;

/** Capacidad de Catalog requerida por Library para proteger el acceso a un media asset. */
public interface CatalogItemAccess {

    Mono<Void> requireVisible(MovieId movieId, String username);
}
