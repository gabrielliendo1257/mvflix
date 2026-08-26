package com.gcorp.service.app.mvflix_movies.catalog.domain.movie;

/** Acceso inválido declarado por el cliente: p.ej. SHARED sin usuarios. HTTP 400. */
public class InvalidMovieAccessException extends RuntimeException {

    public InvalidMovieAccessException(String message) {
        super(message);
    }
}
