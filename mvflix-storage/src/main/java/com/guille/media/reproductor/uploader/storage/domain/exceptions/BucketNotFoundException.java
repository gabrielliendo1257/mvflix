package com.guille.media.reproductor.uploader.storage.domain.exceptions;

public class BucketNotFoundException extends RuntimeException {
    public BucketNotFoundException(String message) {
        super(message);
    }
}
