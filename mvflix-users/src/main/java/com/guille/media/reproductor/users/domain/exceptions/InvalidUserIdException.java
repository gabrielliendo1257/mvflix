package com.guille.media.reproductor.users.domain.exceptions;

public class InvalidUserIdException extends RuntimeException {

    public InvalidUserIdException(String message) {
        super(message);
    }
}
