package com.gcorp.service.app.mvflix_movies.catalog.domain.metadata;

import java.time.Instant;

/** Metadata for a video that is not a movie. */
public record VideoMetadata(String title, String description, Instant recordedAt)
        implements CatalogMetadata {
    public VideoMetadata {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("video title is required");
        }
    }
}
