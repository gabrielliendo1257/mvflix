package com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception;

public class InvalidObjectContentError extends RuntimeException {

    public InvalidObjectContentError() {
        super("Uploaded object content does not match the expected metadata");
    }

    public InvalidObjectContentError(String message) {
        super(message);
    }
}
