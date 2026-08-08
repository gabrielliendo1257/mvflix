package com.guille.media.reproductor.users.domain.exceptions;

import lombok.Getter;

public class EmptyVaribleException extends RuntimeException {
    @Getter
    private final String varibleName;

    public EmptyVaribleException(String message, String variableName) {
        super(message);
        this.varibleName = variableName;
    }
}
