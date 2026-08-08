package com.guille.media.reproductor.users.domain.models;

import com.guille.media.reproductor.users.domain.exceptions.EmptyVaribleException;
import com.guille.media.reproductor.users.domain.exceptions.SizeVaribleException;

public record Username(String value) {

    public Username {
        if(value == null)
            throw new EmptyVaribleException("El username no puede ser null.", "username");

        if(value.isBlank() || value.isBlank())
            throw new EmptyVaribleException("El username no puede estar en blanco.", "username");

        if(value.length() < 4 || value.length() > 30)
            throw new SizeVaribleException("username", 30, 4);
    }

}