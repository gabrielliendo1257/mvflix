package com.guille.media.reproductor.users.domain.exceptions;

public class ExceededQuotaException extends RuntimeException {

    public ExceededQuotaException(String args) {
        super(args);
    }
}
