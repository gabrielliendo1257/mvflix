package com.guille.media.reproductor.users.domain.exceptions;

public class UserIsDisableException extends RuntimeException {
    public UserIsDisableException(String message) {
        super(message);
    }
}
