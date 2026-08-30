package com.gcorp.service.app.mvflix_movies.catalog.domain.movie;

/** Acceso denegado a una pelicula: no es PUBLIC, no es del dueño ni esta compartida. */
public class CatalogItemAccessDeniedException extends RuntimeException {

    public CatalogItemAccessDeniedException(String message) {
        super(message);
    }
}