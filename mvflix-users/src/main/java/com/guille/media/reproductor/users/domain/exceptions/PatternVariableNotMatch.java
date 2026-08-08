package com.guille.media.reproductor.users.domain.exceptions;

import lombok.Getter;

public class PatternVariableNotMatch extends RuntimeException {

    @Getter
    private String variablename;

    public PatternVariableNotMatch(String variableName) {
        super("El patron de la variable " + variableName + " es invalido.");
        this.variablename = variableName;
    }
}
