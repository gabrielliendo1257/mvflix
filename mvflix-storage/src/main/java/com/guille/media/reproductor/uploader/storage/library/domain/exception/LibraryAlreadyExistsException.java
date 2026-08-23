package com.guille.media.reproductor.uploader.storage.library.domain.exception;

public class LibraryAlreadyExistsException extends RuntimeException {

    public LibraryAlreadyExistsException(String message) {
        super(message);
    }
}