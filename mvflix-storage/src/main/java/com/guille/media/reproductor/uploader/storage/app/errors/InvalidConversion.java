package com.guille.media.reproductor.uploader.storage.app.errors;

public class InvalidConversion extends RuntimeException {
    public InvalidConversion(String message, Exception cause) {
        super(message, cause);
    }
}
