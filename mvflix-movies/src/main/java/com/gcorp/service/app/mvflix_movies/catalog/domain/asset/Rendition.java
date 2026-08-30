package com.gcorp.service.app.mvflix_movies.catalog.domain.asset;

import java.util.Objects;

/** A derived playable representation, independent from the source MediaAsset object. */
public final class Rendition {

    private final RenditionId id;
    private final MediaAssetId mediaAssetId;
    private final RenditionOrigin origin;
    private final StorageObjectId storageObjectId;
    private final String profile;
    private final RenditionStatus status;
    private final RenditionTechnicalMetadata technicalMetadata;

    public Rendition(RenditionId id, MediaAssetId mediaAssetId, RenditionOrigin origin,
            StorageObjectId storageObjectId, String profile, RenditionStatus status,
            RenditionTechnicalMetadata technicalMetadata) {
        this.id = id;
        this.mediaAssetId = Objects.requireNonNull(mediaAssetId, "mediaAssetId");
        this.origin = Objects.requireNonNull(origin, "origin");
        this.storageObjectId = storageObjectId;
        this.profile = Objects.requireNonNull(profile, "profile");
        if (profile.isBlank()) {
            throw new IllegalArgumentException("Rendition profile cannot be blank");
        }
        this.status = Objects.requireNonNull(status, "status");
        if (status == RenditionStatus.READY && storageObjectId == null) {
            throw new IllegalArgumentException("A ready rendition requires a storage object");
        }
        this.technicalMetadata = technicalMetadata;
    }

    public static Rendition requested(MediaAssetId mediaAssetId, RenditionOrigin origin, String profile) {
        return new Rendition(null, mediaAssetId, origin, null, profile, RenditionStatus.REQUESTED, null);
    }

    public Rendition processing() {
        requireStatus(RenditionStatus.REQUESTED, "processing");
        return new Rendition(this.id, this.mediaAssetId, this.origin, this.storageObjectId, this.profile,
                RenditionStatus.PROCESSING, this.technicalMetadata);
    }

    public Rendition ready(StorageObjectId storageObjectId, RenditionTechnicalMetadata metadata) {
        requireStatus(RenditionStatus.PROCESSING, "ready");
        return new Rendition(this.id, this.mediaAssetId, this.origin, storageObjectId, this.profile,
                RenditionStatus.READY, metadata);
    }

    public Rendition failed() {
        if (this.status != RenditionStatus.REQUESTED && this.status != RenditionStatus.PROCESSING) {
            throw new IllegalStateException("Only requested or processing renditions can fail");
        }
        return new Rendition(this.id, this.mediaAssetId, this.origin, this.storageObjectId, this.profile,
                RenditionStatus.FAILED, this.technicalMetadata);
    }

    private void requireStatus(RenditionStatus expected, String target) {
        if (this.status != expected) {
            throw new IllegalStateException("Only " + expected + " renditions can become " + target);
        }
    }

    public RenditionId getId() { return this.id; }
    public MediaAssetId getMediaAssetId() { return this.mediaAssetId; }
    public RenditionOrigin getOrigin() { return this.origin; }
    public StorageObjectId getStorageObjectId() { return this.storageObjectId; }
    public String getProfile() { return this.profile; }
    public RenditionStatus getStatus() { return this.status; }
    public RenditionTechnicalMetadata getTechnicalMetadata() { return this.technicalMetadata; }
}
