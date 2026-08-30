package com.gcorp.service.app.mvflix_movies.catalog.domain.media;

import java.util.Objects;

public record RenditionId(Long value) {

    public RenditionId {
        Objects.requireNonNull(value, "value");
        if (value <= 0) {
            throw new IllegalArgumentException("Rendition id must be positive");
        }
    }

    public static RenditionId of(Long value) {
        return new RenditionId(value);
    }
}
