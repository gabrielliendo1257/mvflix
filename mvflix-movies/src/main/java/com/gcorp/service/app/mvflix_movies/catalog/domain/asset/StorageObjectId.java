package com.gcorp.service.app.mvflix_movies.catalog.domain.asset;

import java.util.Objects;

/** Typed identifier of an object owned by the storage service. */
public record StorageObjectId(Long value) {

    public StorageObjectId {
        Objects.requireNonNull(value, "value");
        if (value <= 0) {
            throw new IllegalArgumentException("Storage object id must be positive");
        }
    }

    public static StorageObjectId of(Long value) {
        return new StorageObjectId(value);
    }
}
