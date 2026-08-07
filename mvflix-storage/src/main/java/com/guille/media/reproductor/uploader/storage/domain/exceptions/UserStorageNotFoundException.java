package com.guille.media.reproductor.uploader.storage.domain.exceptions;

public class UserStorageNotFoundException extends RuntimeException {
    public UserStorageNotFoundException(String message) {
        super(message);
    }
}
