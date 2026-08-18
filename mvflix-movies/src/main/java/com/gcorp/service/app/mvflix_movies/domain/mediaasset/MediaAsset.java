package com.gcorp.service.app.mvflix_movies.domain.mediaasset;

import com.gcorp.service.app.mvflix_movies.domain.movie.MovieId;

import java.time.Instant;

/**
 * Entrada de catalogo de una biblioteca del operador. Es la contraparte
 * "media server" de {@code Media} (que pertenece al flujo de upload): el
 * archivo no esta en el storage-managed del uploader sino en un root que el
 * operador confia al scan.
 *
 * <p>Ciclo de vida: UNIDENTIFIED -> IDENTIFIED (vinculo a Movie) y MISSING
 * cuando el scan ya no encuentra el archivo.
 */
public class MediaAsset {

    private final MediaAssetId id;
    private final Long storageId;
    private final String relativePath;
    private final long size;
    private final String mimeType;
    private final MediaAssetStatus status;
    private final MovieId movieId;
    private final Instant createdAt;
    private final Instant updatedAt;

    public MediaAsset(
            MediaAssetId id,
            Long storageId,
            String relativePath,
            long size,
            String mimeType,
            MediaAssetStatus status,
            MovieId movieId,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.storageId = storageId;
        this.relativePath = relativePath;
        this.size = size;
        this.mimeType = mimeType;
        this.status = status;
        this.movieId = movieId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static MediaAsset create(Long storageId, ScannedFile file) {
        return new MediaAsset(
                null,
                storageId,
                file.relativePath(),
                file.size(),
                file.mimeType(),
                MediaAssetStatus.UNIDENTIFIED,
                null,
                Instant.now(),
                Instant.now());
    }

    public boolean isIdentified() {
        return this.status == MediaAssetStatus.IDENTIFIED;
    }

    public boolean isMissing() {
        return this.status == MediaAssetStatus.MISSING;
    }

    /** Vincula el activo a una pelicula (idempotente: ya identificado no cambia). */
    public MediaAsset identify(MovieId movieId) {
        if (this.isIdentified() && this.movieId != null) {
            return this;
        }
        return new MediaAsset(
                this.id,
                this.storageId,
                this.relativePath,
                this.size,
                this.mimeType,
                MediaAssetStatus.IDENTIFIED,
                movieId,
                this.createdAt,
                Instant.now());
    }

    /** El scan ya no encontro el archivo: pasa a MISSING sin tocar el vinculo. */
    public MediaAsset markMissing() {
        if (this.isMissing()) {
            return this;
        }
        return new MediaAsset(
                this.id,
                this.storageId,
                this.relativePath,
                this.size,
                this.mimeType,
                MediaAssetStatus.MISSING,
                this.movieId,
                this.createdAt,
                Instant.now());
    }

    /** El scan volvio a encontrar el archivo: regresa al estado anterior al MISSING. */
    public MediaAsset markPresent() {
        if (!this.isMissing()) {
            return this;
        }
        return new MediaAsset(
                this.id,
                this.storageId,
                this.relativePath,
                this.size,
                this.mimeType,
                this.movieId != null ? MediaAssetStatus.IDENTIFIED : MediaAssetStatus.UNIDENTIFIED,
                this.movieId,
                this.createdAt,
                Instant.now());
    }

    /** Refresca lo que el filesystem dice (size/mime pueden cambiar en disco). */
    public MediaAsset refresh(long size, String mimeType) {
        if (this.size == size && this.mimeType.equals(mimeType)) {
            return this;
        }
        return new MediaAsset(
                this.id,
                this.storageId,
                this.relativePath,
                size,
                mimeType,
                this.status,
                this.movieId,
                this.createdAt,
                Instant.now());
    }

    public MediaAssetId getId() {
        return this.id;
    }

    public Long getStorageId() {
        return this.storageId;
    }

    public String getRelativePath() {
        return this.relativePath;
    }

    public long getSize() {
        return this.size;
    }

    public String getMimeType() {
        return this.mimeType;
    }

    public MediaAssetStatus getStatus() {
        return this.status;
    }

    public MovieId getMovieId() {
        return this.movieId;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }
}
