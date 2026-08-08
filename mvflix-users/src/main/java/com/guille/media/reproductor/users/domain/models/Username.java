package com.guille.media.reproductor.users.domain.models;

import com.guille.media.reproductor.users.domain.exceptions.EmptyVariableException;
import com.guille.media.reproductor.users.domain.exceptions.SizeVariableException;

public record Username(String value) {

    public Username {
        if (value == null)
            throw new EmptyVariableException("El username no puede ser null.", "username");
        if (value.isBlank())
            throw new EmptyVariableException("El username no puede estar en blanco.", "username");
        if (value.length() < 4 || value.length() > 30)
            throw new SizeVariableException("username", 30, 4);
    }
}