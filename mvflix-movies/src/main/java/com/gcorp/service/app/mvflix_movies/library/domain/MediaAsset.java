package com.gcorp.service.app.mvflix_movies.library.domain;

import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId;

import java.time.Instant;

import com.gcorp.service.app.mvflix_movies.shared.domain.media.MediaAssetReference;

/**
 * Entrada de catalogo de una biblioteca del operador. Es la contraparte
 * "media server" de {@code ManagedMediaAsset} (que pertenece al flujo de upload): el
 * archivo no esta en el storage-managed del uploader sino en un root que el
 * operador confia al scan.
 *
 * <p>Dos dimensiones ortogonales: la identificación ({@link MediaAssetStatus}
 * UNIDENTIFIED -> IDENTIFIED) y la presencia en disco ({@code present}); un
 * archivo desaparecido conserva su vínculo a la película.
 *
 * <p>{@code discoveredBy} sella quién pidió el scan que trajo el archivo:
 * base de la autorización de gestión (cada quien lista sus descubrimientos;
 * admin ve todo). Null en assets previos al sello = solo admin.
 */
public class MediaAsset implements com.gcorp.service.app.mvflix_movies.shared.domain.media.MediaAsset {

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
    private final String discoveredBy;
    private final String filename;
    private final Long duration;
    private final String container;
    private final String videoCodec;
    private final String resolution;
    private final String storageReference;

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
            Instant updatedAt,
            String discoveredBy) {
        this(id, libraryId, relativePath, size, mimeType, status, catalogItemId, present, createdAt,
                updatedAt, discoveredBy, null, null, null, null, null, null);
    }

    public MediaAsset(
            MediaAssetId id, Long libraryId, String relativePath, long size, String mimeType,
            MediaAssetStatus status, CatalogItemId catalogItemId, boolean present, Instant createdAt,
            Instant updatedAt, String discoveredBy, String filename, Long duration, String container,
            String videoCodec, String resolution, String storageReference) {
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
        this.discoveredBy = discoveredBy;
        this.filename = filename;
        this.duration = duration;
        this.container = container;
        this.videoCodec = videoCodec;
        this.resolution = resolution;
        this.storageReference = storageReference;
    }

    public static MediaAsset create(Long libraryId, ScannedFile file, String discoveredBy) {
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
                Instant.now(),
                discoveredBy);
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
                Instant.now(),
                this.discoveredBy);
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
                Instant.now(),
                this.discoveredBy);
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
                Instant.now(),
                this.discoveredBy);
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
                Instant.now(),
                this.discoveredBy);
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
                Instant.now(),
                this.discoveredBy);
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

    /** Quién pidió el scan que lo trajo; null = previo al sello (solo admin). */
    public String getDiscoveredBy() {
        return this.discoveredBy;
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

    @Override
    public MediaAssetReference playbackReference() {
        return new MediaAssetReference(this.relativePath);
    }

    @Override
    public boolean isPlayable() {
        return this.isIdentified() && this.isPresent();
    }
}
