package com.gcorp.service.app.mvflix_movies.catalog.application.port;

import reactor.core.publisher.Mono;

/** Puerto único para eventos semánticos del catálogo y la solicitud managed legacy. */
public interface CatalogSemanticOutbox {
    Mono<Void> append(CatalogSemanticEvent event);
}
