package com.gcorp.service.app.mvflix_movies.domain.model;

public class Movie {

    private final Long id;
    private final String ownerUsername;
    private final String title;
    private final MovieStatus status;
    private final String objectKey;
    private final MovieMetadata metadata;

    public Movie(
        Long id,
        String ownerUsername,
        String title,
        MovieStatus status,
        String objectKey,
        MovieMetadata metadata) {
        this.id = id;
        this.ownerUsername = ownerUsername;
        this.title = title;
        this.status = status;
        this.objectKey = objectKey;
        this.metadata = metadata;
    }

    public Long getId() {
        return this.id;
    }

    public String getOwnerUsername() {
        return this.ownerUsername;
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
}
