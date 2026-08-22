package com.gcorp.service.app.mvflix_movies.library.domain;

/** Activo de biblioteca inexistente. */
public class MediaAssetNotFoundException extends RuntimeException {

    public MediaAssetNotFoundException(String message) {
        super(message);
    }
}
