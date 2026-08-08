package com.guille.media.reproductor.users.domain.exceptions;

import lombok.Getter;

public class EmptyVariableException extends RuntimeException {
    @Getter
    private final String varibleName;

    public EmptyVariableException(String message, String variableName) {
        super(message);
        this.varibleName = variableName;
    }
}
