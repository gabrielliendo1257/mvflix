package com.gcorp.service.app.mvflix_movies.catalog.domain.movie;

/** Acceso denegado a una pelicula: no es PUBLIC, no es del dueño ni esta compartida. */
public class MovieAccessDeniedException extends RuntimeException {

    public MovieAccessDeniedException(String message) {
        super(message);
    }
}