package com.gcorp.service.app.mvflix_movies.domain.movie;

public class Movie {

    private final Long id;
    private final String ownerUsername;
    private final String title;
    private final MovieStatus status;
    private final Long objectId;
    private final String objectKey;
    private final MovieMetadata metadata;

    public Movie(
        Long id,
        String ownerUsername,
        String title,
        MovieStatus status,
        Long objectId,
        String objectKey,
        MovieMetadata metadata) {
        this.id = id;
        this.ownerUsername = ownerUsername;
        this.title = title;
        this.status = status;
        this.objectId = objectId;
        this.objectKey = objectKey;
        this.metadata = metadata;
    }

    public Long getId() {
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

    public String getObjectKey() {
        return this.objectKey;
    }

    public MovieMetadata getMetadata() {
        return this.metadata;
    }

    public boolean isDraft() {
        return this.status == MovieStatus.DRAFT;
    }

    /** Transición de dominio: una película en borrador pasa a lista cuando se le asigna su objeto. */
    public Movie complete(Long objectId, String objectKey) {
        return new Movie(
                this.id,
                this.ownerUsername,
                this.title,
                MovieStatus.READY,
                objectId,
                objectKey,
                this.metadata);
    }
}
