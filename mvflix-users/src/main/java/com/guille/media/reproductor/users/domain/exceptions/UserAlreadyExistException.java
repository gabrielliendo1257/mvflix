package com.guille.media.reproductor.users.domain.exceptions;

import lombok.Getter;

@Getter
public class UserAlreadyExistException extends RuntimeException {
    private final String username;

    public UserAlreadyExistException(String username) {
        this.username = username;
    }
}
