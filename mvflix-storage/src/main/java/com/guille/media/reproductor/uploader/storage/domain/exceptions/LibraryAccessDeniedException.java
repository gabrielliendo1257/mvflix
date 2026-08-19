package com.guille.media.reproductor.uploader.storage.domain.exceptions;

public class LibraryAccessDeniedException extends RuntimeException {

    public LibraryAccessDeniedException(String message) {
        super(message);
    }
}