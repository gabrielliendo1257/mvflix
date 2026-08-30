package com.gcorp.service.app.mvflix_movies.catalog.domain.movie;

/** Identificador tipado del agregado CatalogItem (evita {@code Long} desnudo en el dominio). */
public record CatalogItemId(Long value) {

    public static CatalogItemId of(Long value) {
        return new CatalogItemId(value);
    }
}