package com.gcorp.service.app.mvflix_movies.library.application.port;

import com.gcorp.service.app.mvflix_movies.library.application.CatalogItemKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId;

import reactor.core.publisher.Mono;

/** Contrato que Library necesita para incorporar un archivo al catálogo. */
public interface CatalogItemCreator {

    Mono<CatalogItemId> createFromLibrary(
            String ownerUsername, String title, CatalogItemKind kind);
}
