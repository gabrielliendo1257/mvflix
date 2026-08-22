package com.gcorp.service.app.mvflix_movies.library.domain;

/** Conflicto optimista: otro flujo identificó el asset antes de completar el vínculo. */
public class MediaAssetAlreadyIdentifiedException extends RuntimeException {

    public MediaAssetAlreadyIdentifiedException(String message) {
        super(message);
    }
}
