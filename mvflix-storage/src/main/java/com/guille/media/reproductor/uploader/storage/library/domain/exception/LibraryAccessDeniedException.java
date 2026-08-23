package com.guille.media.reproductor.uploader.storage.library.domain.exception;

public class LibraryAccessDeniedException extends RuntimeException {

    public LibraryAccessDeniedException(String message) {
        super(message);
    }
}