package com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception;

public class UserStorageNotFoundException extends RuntimeException {
    public UserStorageNotFoundException(String message) {
        super(message);
    }
}
