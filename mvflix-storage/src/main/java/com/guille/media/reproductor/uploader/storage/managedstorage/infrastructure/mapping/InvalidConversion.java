package com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.mapping;

public class InvalidConversion extends RuntimeException {
    public InvalidConversion(String message, Exception cause) {
        super(message, cause);
    }
}
