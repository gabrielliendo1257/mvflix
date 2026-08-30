package com.gcorp.service.app.mvflix_movies.shared.domain.media;

import java.util.Objects;

/** Stable domain reference to the file used by a playback adapter. */
public record MediaAssetReference(String value) {

    public MediaAssetReference {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Media asset reference cannot be blank");
        }
    }
}
