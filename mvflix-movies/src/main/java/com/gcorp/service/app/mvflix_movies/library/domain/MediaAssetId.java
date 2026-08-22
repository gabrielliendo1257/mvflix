package com.gcorp.service.app.mvflix_movies.library.domain;

public record MediaAssetId(Long value) {

    public static MediaAssetId of(Long value) {
        return new MediaAssetId(value);
    }
}
