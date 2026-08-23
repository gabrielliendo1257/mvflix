package com.gcorp.service.app.mvflix_movies.library.domain;

import java.time.Instant;

/**
 * Entrada de catalogo de una biblioteca del operador. Es la contraparte
 * "media server" de {@code Media} (que pertenece al flujo de upload): el
 * archivo no esta en el storage-managed del uploader sino en un root que el
 * operador confia al scan.
 *
 * <p>Dos dimensiones ortogonales: la identificación ({@link MediaAssetStatus}
 * UNIDENTIFIED -> IDENTIFIED) y la presencia en disco ({@code present}); un
 * archivo desaparecido conserva su vínculo a la película.
 */
public class MediaAsset {

    private final MediaAssetId id;
    private final Long libraryId;
    private final String relativePath;
    private final long size;
    private final String mimeType;
    private final MediaAssetStatus status;
    private final CatalogItemId catalogItemId;
    private final boolean present;
    private final Instant createdAt;
    private final Instant updatedAt;

    public MediaAsset(
            MediaAssetId id,
            Long libraryId,
            String relativePath,
            long size,
            String mimeType,
            MediaAssetStatus status,
            CatalogItemId catalogItemId,
            boolean present,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.libraryId = libraryId;
        this.relativePath = relativePath;
        this.size = size;
        this.mimeType = mimeType;
        this.status = status;
        this.catalogItemId = catalogItemId;
        this.present = present;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static MediaAsset create(Long libraryId, ScannedFile file) {
        return new MediaAsset(
                null,
                libraryId,
                file.relativePath(),
                file.size(),
                file.mimeType(),
                MediaAssetStatus.UNIDENTIFIED,
                null,
                true,
                Instant.now(),
                Instant.now());
    }

    public boolean isIdentified() {
        return this.status == MediaAssetStatus.IDENTIFIED;
    }

    public boolean isPresent() {
        return this.present;
    }

    public boolean isMissing() {
        return !this.present;
    }

    /** Vincula el activo a una pelicula (idempotente: ya identificado no cambia). */
    public MediaAsset identify(CatalogItemId catalogItemId) {
        if (this.isIdentified() && this.catalogItemId != null) {
            return this;
        }
        return new MediaAsset(
                this.id,
                this.libraryId,
                this.relativePath,
                this.size,
                this.mimeType,
                MediaAssetStatus.IDENTIFIED,
                catalogItemId,
                this.present,
                this.createdAt,
                Instant.now());
    }

    /** Desvincula el activo cuando su película se elimina; el archivo sigue catalogado. */
    public MediaAsset unidentify() {
        if (!this.isIdentified() && this.catalogItemId == null) {
            return this;
        }
        return new MediaAsset(
                this.id,
                this.libraryId,
                this.relativePath,
                this.size,
                this.mimeType,
                MediaAssetStatus.UNIDENTIFIED,
                null,
                this.present,
                this.createdAt,
                Instant.now());
    }

    /** El scan ya no encontro el archivo: marca ausencia sin tocar el vinculo. */
    public MediaAsset markMissing() {
        if (this.isMissing()) {
            return this;
        }
        return new MediaAsset(
                this.id,
                this.libraryId,
                this.relativePath,
                this.size,
                this.mimeType,
                this.status,
                this.catalogItemId,
                false,
                this.createdAt,
                Instant.now());
    }

    /** El scan volvio a encontrar el archivo: marca presencia sin tocar el vinculo. */
    public MediaAsset markPresent() {
        if (this.isPresent()) {
            return this;
        }
        return new MediaAsset(
                this.id,
                this.libraryId,
                this.relativePath,
                this.size,
                this.mimeType,
                this.status,
                this.catalogItemId,
                true,
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
                this.libraryId,
                this.relativePath,
                size,
                mimeType,
                this.status,
                this.catalogItemId,
                this.present,
                this.createdAt,
                Instant.now());
    }

    public MediaAssetId getId() {
        return this.id;
    }

    public Long getLibraryId() {
        return this.libraryId;
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

    public CatalogItemId getCatalogItemId() {
        return this.catalogItemId;
    }

    public boolean getPresent() {
        return this.present;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }
}
