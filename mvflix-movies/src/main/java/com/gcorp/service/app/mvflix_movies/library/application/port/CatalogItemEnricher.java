package com.gcorp.service.app.mvflix_movies.library.application.port;

import com.gcorp.service.app.mvflix_movies.domain.movie.MovieId;

import reactor.core.publisher.Mono;

/** Capacidad de Catalog que Library usa después de identificar un asset. */
public interface CatalogItemEnricher {

    Mono<Void> enrich(MovieId movieId, Long externalMetadataId);
}
