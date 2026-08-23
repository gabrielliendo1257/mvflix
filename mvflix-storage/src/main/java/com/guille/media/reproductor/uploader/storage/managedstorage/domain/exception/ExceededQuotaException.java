package com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception;

public class ExceededQuotaException extends RuntimeException {
    public ExceededQuotaException(String message) {
        super(message);
    }
}
