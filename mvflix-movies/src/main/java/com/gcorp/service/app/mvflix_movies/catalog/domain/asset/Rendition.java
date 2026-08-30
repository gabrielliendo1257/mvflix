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

    public Rendition ready(StorageObjectId storageObjectId, RenditionTechnicalMetadata metadata) {
        return new Rendition(this.id, this.mediaAssetId, this.origin, storageObjectId, this.profile,
                RenditionStatus.READY, metadata);
    }

    public Rendition failed() {
        return new Rendition(this.id, this.mediaAssetId, this.origin, this.storageObjectId, this.profile,
                RenditionStatus.FAILED, this.technicalMetadata);
    }

    public RenditionId getId() { return this.id; }
    public MediaAssetId getMediaAssetId() { return this.mediaAssetId; }
    public RenditionOrigin getOrigin() { return this.origin; }
    public StorageObjectId getStorageObjectId() { return this.storageObjectId; }
    public String getProfile() { return this.profile; }
    public RenditionStatus getStatus() { return this.status; }
    public RenditionTechnicalMetadata getTechnicalMetadata() { return this.technicalMetadata; }
}
