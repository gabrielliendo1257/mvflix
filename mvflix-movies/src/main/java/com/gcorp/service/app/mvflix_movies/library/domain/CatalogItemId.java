package com.gcorp.service.app.mvflix_movies.library.domain;

/** Referencia local de Library al elemento asociado en Catalog. */
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
