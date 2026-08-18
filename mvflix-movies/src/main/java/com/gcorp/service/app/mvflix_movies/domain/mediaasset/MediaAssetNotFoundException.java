package com.gcorp.service.app.mvflix_movies.domain.mediaasset;

/** Activo de biblioteca inexistente. */
public class MediaAssetNotFoundException extends RuntimeException {

    public MediaAssetNotFoundException(String message) {
        super(message);
    }
}
