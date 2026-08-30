package com.gcorp.service.app.mvflix_movies.library.application.port;

import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId;

import reactor.core.publisher.Mono;

/** Capacidad de Catalog que Library usa después de identificar un asset. */
public interface CatalogItemEnricher {

    Mono<Void> enrich(CatalogItemId catalogItemId, Long externalMetadataId);
}
