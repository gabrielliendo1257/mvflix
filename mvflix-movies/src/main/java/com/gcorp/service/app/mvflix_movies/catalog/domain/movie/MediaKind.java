package com.gcorp.service.app.mvflix_movies.catalog.domain.movie;

/**
 * Tipo de contenido del catálogo. "Movie" es un tipo, no la estructura del
 * dominio: otros tipos (series, clips, musica, etc.) podrán sumarse sin
 * reestructurar el catálogo.
 */
public enum MediaKind {
    MOVIE,
    OTHER
}