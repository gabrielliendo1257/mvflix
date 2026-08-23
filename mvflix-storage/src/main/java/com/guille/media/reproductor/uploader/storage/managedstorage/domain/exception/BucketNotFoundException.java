package com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception;

public class BucketNotFoundException extends RuntimeException {
    public BucketNotFoundException(String message) {
        super(message);
    }
}
