package com.guille.media.reproductor.users.domain.exceptions;

public class UserDisabledException extends RuntimeException {
    public UserDisabledException(String message) {
        super(message);
    }
}
