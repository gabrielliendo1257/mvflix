package com.gcorp.service.app.mvflix_movies.catalog.domain.movie;

/** Stable identifier assigned to a movie by an external provider. */
public record ExternalMovieId(long value) {
    public ExternalMovieId {
        if (value <= 0) {
            throw new IllegalArgumentException("external movie id must be positive");
        }
    }

    public static ExternalMovieId of(Long value) {
        return value == null ? null : new ExternalMovieId(value);
    }
}
