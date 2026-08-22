package com.gcorp.service.app.mvflix_movies.catalog.domain.movie;

/** Identificador tipado del agregado Movie (evita {@code Long} desnudo en el dominio). */
public record MovieId(Long value) {

    public static MovieId of(Long value) {
        return new MovieId(value);
    }
}