package com.guille.media.reproductor.users.domain.exceptions;

public class DisabledUserException extends RuntimeException {

    public DisabledUserException(String args) {
        super(args);
    }
}
