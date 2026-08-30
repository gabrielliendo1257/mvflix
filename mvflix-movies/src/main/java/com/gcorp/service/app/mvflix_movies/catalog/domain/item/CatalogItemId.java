package com.gcorp.service.app.mvflix_movies.catalog.domain.item;

/** Identificador tipado del agregado CatalogItem (evita {@code Long} desnudo en el dominio). */
public record CatalogItemId(Long value) {

    public CatalogItemId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Catalog item id must be positive");
        }
    }

    public static CatalogItemId of(Long value) {
        return new CatalogItemId(value);
    }
}
