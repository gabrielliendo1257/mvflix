package com.guille.media.reproductor.uploader.storage.domain.exceptions;

public class ExceededQuotaException extends RuntimeException {
    public ExceededQuotaException(String message) {
        super(message);
    }
}
