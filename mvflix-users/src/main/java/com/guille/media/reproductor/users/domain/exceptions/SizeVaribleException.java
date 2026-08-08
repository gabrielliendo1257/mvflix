package com.guille.media.reproductor.users.domain.exceptions;

import lombok.Getter;

@Getter
public class SizeVaribleException extends RuntimeException {
    private final Integer max;
    private final Integer min;
    private final String variableName;

    public SizeVaribleException(String variableName, Integer max, Integer min) {
        super("El " + variableName + " debe tener entre " + min + " y " + max + " letras.");
        this.variableName = variableName;
        this.max = max;
        this.min = min;
    }
}
