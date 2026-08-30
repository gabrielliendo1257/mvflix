package com.gcorp.service.app.mvflix_movies.catalog.domain.media;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemId;

import java.time.Instant;

/**
 * Objeto de video de una película (trailer, versión principal, ...).
 * {@code objectKey} es la referencia interna al storage: nunca se expone
 * al front; solo {@code objectId} viaja en las respuestas de la API.
 */
public class Media {

    private final MediaId id;
    private final CatalogItemId movieId;
    private final Long objectId;
    private final String objectKey;
    private final Instant createdAt;

    public Media(MediaId id, CatalogItemId movieId, Long objectId, String objectKey,
            Instant createdAt) {
        this.id = id;
        this.movieId = movieId;
        this.objectId = objectId;
        this.objectKey = objectKey;
        this.createdAt = createdAt;
    }

    public static Media create(CatalogItemId movieId, Long objectId, String objectKey) {
        return new Media(null, movieId, objectId, objectKey, Instant.now());
    }

    public MediaId getId() {
        return this.id;
    }

    public CatalogItemId getMovieId() {
        return this.movieId;
    }

    public Long getObjectId() {
        return this.objectId;
    }

    public String getObjectKey() {
        return this.objectKey;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }
}