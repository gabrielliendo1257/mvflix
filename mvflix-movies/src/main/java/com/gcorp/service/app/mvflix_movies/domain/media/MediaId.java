package com.gcorp.service.app.mvflix_movies.domain.media;

public record MediaId(Long value) {

    public static MediaId of(Long value) {
        return new MediaId(value);
    }
}