package com.gcorp.service.app.mvflix_movies.catalog.domain.asset;

import java.util.Objects;

/** Identifier of the source asset, regardless of which media origin owns it. */
public record MediaAssetId(Long value) {

    public MediaAssetId {
        Objects.requireNonNull(value, "value");
        if (value <= 0) {
            throw new IllegalArgumentException("Media asset id must be positive");
        }
    }

    public static MediaAssetId of(Long value) {
        return new MediaAssetId(value);
    }
}
