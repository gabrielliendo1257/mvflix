package com.guille.media.reproductor.uploader.storage.domain.exceptions;

public class InvalidObjectContentError extends RuntimeException {

    public InvalidObjectContentError() {
        super("Uploaded object content does not match the expected metadata");
    }
}
