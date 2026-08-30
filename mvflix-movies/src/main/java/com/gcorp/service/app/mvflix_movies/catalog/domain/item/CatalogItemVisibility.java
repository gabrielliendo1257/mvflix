package com.gcorp.service.app.mvflix_movies.catalog.domain.item;

/**
 * Visibilidad del catalogo: PUBLIC lo ve cualquiera, PRIVATE solo el dueño y
 * SHARED el dueño + la lista de movie_shares. Toda movie nace PRIVATE.
 */
public enum CatalogItemVisibility {
    PUBLIC,
    PRIVATE,
    SHARED
}
