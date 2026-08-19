package com.guille.media.reproductor.uploader.storage.domain.exceptions;

public class LibraryPathNotAllowedException extends RuntimeException {

    public LibraryPathNotAllowedException(String message) {
        super(message);
    }
}