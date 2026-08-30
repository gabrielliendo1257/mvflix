package com.gcorp.service.app.mvflix_movies.catalog.domain.item;

/**
 * Tipo de contenido del catálogo. "CatalogItem" es un tipo, no la estructura del
 * dominio: otros tipos (series, clips, musica, etc.) podrán sumarse sin
 * reestructurar el catálogo.
 */
public enum MediaKind {
    MOVIE,
    VIDEO
}
