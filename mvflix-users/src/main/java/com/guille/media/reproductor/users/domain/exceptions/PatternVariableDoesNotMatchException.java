package com.guille.media.reproductor.users.domain.exceptions;

import lombok.Getter;

public class PatternVariableDoesNotMatchException extends RuntimeException {

    @Getter
    private String variablename;

    public PatternVariableDoesNotMatchException(String variableName) {
        super("El patron de la variable " + variableName + " es invalido.");
        this.variablename = variableName;
    }
}
