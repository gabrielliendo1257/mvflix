package com.gcorp.service.app.mvflix_movies.catalog.domain.media;

import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId;

import java.time.Instant;

/**
 * Objeto de video de una película (trailer, versión principal, ...).
 * {@code objectKey} es la referencia interna al storage: nunca se expone
 * al front; solo {@code objectId} viaja en las respuestas de la API.
 */
public class ManagedMediaAsset implements com.gcorp.service.app.mvflix_movies.shared.domain.media.MediaAsset {

    private final MediaId id;
    private final CatalogItemId movieId;
    private final StorageObjectId storageObjectId;
    private final String objectKey;
    private final String filename;
    private final Long duration;
    private final String container;
    private final String videoCodec;
    private final String resolution;
    private final String storageReference;
    private final Instant createdAt;

    public ManagedMediaAsset(MediaId id, CatalogItemId movieId, Long objectId, String objectKey,
            Instant createdAt) {
        this(id, movieId, StorageObjectId.of(objectId), objectKey, null, null, null, null, null,
                objectKey, createdAt);
    }

    public ManagedMediaAsset(MediaId id, CatalogItemId movieId, StorageObjectId storageObjectId,
            String objectKey, String filename, Long duration, String container, String videoCodec,
            String resolution, String storageReference, Instant createdAt) {
        this.id = id;
        this.movieId = movieId;
        this.storageObjectId = storageObjectId;
        this.objectKey = objectKey;
        this.filename = filename;
        this.duration = duration;
        this.container = container;
        this.videoCodec = videoCodec;
        this.resolution = resolution;
        this.storageReference = storageReference;
        this.createdAt = createdAt;
    }

    public static ManagedMediaAsset create(CatalogItemId movieId, Long objectId, String objectKey) {
        return new ManagedMediaAsset(null, movieId, objectId, objectKey, Instant.now());
    }

    public MediaId getId() {
        return this.id;
    }

    public CatalogItemId getMovieId() {
        return this.movieId;
    }

    public Long getObjectId() {
        return this.storageObjectId.value();
    }

    public StorageObjectId getStorageObjectId() {
        return this.storageObjectId;
    }

    public String getObjectKey() {
        return this.objectKey;
    }

    @Override
    public String getFilename() { return this.filename; }

    @Override
    public Long getDuration() { return this.duration; }

    @Override
    public String getContainer() { return this.container; }

    @Override
    public String getVideoCodec() { return this.videoCodec; }

    @Override
    public String getResolution() { return this.resolution; }

    @Override
    public String getStorageReference() { return this.storageReference; }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    @Override
    public com.gcorp.service.app.mvflix_movies.shared.domain.media.MediaAssetReference playbackReference() {
        return new com.gcorp.service.app.mvflix_movies.shared.domain.media.MediaAssetReference(this.objectKey);
    }

    @Override
    public boolean isPlayable() {
        return this.objectKey != null && !this.objectKey.isBlank();
    }
}
