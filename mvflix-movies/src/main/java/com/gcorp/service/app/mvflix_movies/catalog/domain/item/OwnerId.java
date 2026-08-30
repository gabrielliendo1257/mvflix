package com.gcorp.service.app.mvflix_movies.catalog.domain.item;

/** Identifies the owner of a catalog item. */
public record OwnerId(String value) {

    public OwnerId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("catalog item owner is required");
        }
    }

    public static OwnerId of(String value) {
        return new OwnerId(value);
    }
}
