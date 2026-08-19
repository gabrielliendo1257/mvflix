package com.guille.media.reproductor.uploader.storage.domain.exceptions;

public class LibraryAlreadyExistsException extends RuntimeException {

    public LibraryAlreadyExistsException(String message) {
        super(message);
    }
}