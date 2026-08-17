package com.gcorp.service.app.mvflix_movies.domain.movie;

public class Movie {

    private final MovieId id;
    private final String ownerUsername;
    private final String title;
    private final MovieStatus status;
    private final EnrichmentStatus enrichmentStatus;
    private final Long objectId;
    private final String objectKey;
    private final MovieMetadata metadata;

    public Movie(
        MovieId id,
        String ownerUsername,
        String title,
        MovieStatus status,
        EnrichmentStatus enrichmentStatus,
        Long objectId,
        String objectKey,
        MovieMetadata metadata) {
        this.id = id;
        this.ownerUsername = ownerUsername;
        this.title = title;
        this.status = status;
        this.enrichmentStatus = enrichmentStatus;
        this.objectId = objectId;
        this.objectKey = objectKey;
        this.metadata = metadata;
    }

    public MovieId getId() {
        return this.id;
    }

    public String getOwnerUsername() {
        return this.ownerUsername;
    }

    public Long getObjectId() {
        return this.objectId;
    }

    public String getTitle() {
        return this.title;
    }

    public MovieStatus getStatus() {
        return this.status;
    }

    public EnrichmentStatus getEnrichmentStatus() {
        return this.enrichmentStatus;
    }

    public String getObjectKey() {
        return this.objectKey;
    }

    public MovieMetadata getMetadata() {
        return this.metadata;
    }

    public boolean isDraft() {
        return this.status == MovieStatus.DRAFT;
    }

    public boolean isEnriched() {
        return this.enrichmentStatus == EnrichmentStatus.ENRICHED;
    }

    /** Transición de dominio: una película en borrador pasa a lista cuando se le asigna su objeto. */
    public Movie complete(Long objectId, String objectKey) {
        return new Movie(
                this.id,
                this.ownerUsername,
                this.title,
                MovieStatus.READY,
                this.enrichmentStatus,
                objectId,
                objectKey,
                this.metadata);
    }

    /** Transición de dominio: marca el catálogo como enriquecido (idempotente). */
    public Movie enrich(EnrichmentStatus enrichmentStatus) {
        return new Movie(
                this.id,
                this.ownerUsername,
                this.title,
                this.status,
                enrichmentStatus,
                this.objectId,
                this.objectKey,
                this.metadata);
    }
}