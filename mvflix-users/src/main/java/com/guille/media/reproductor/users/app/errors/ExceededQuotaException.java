package com.guille.media.reproductor.users.app.errors;

public class ExceededQuotaException extends RuntimeException {

    public ExceededQuotaException(String args) {
        super(args);
    }
}
