package com.guille.media.reproductor.users.domain.models;

import java.util.regex.Pattern;

import com.guille.media.reproductor.users.domain.exceptions.EmptyVariableException;
import com.guille.media.reproductor.users.domain.exceptions.PatternVariableDoesNotMatchException;
import com.guille.media.reproductor.users.domain.exceptions.SizeVariableException;

public record Email(String value) {
    private static final String EMAIL_REGEX =
        "^(?=.{1,64}@)[A-Za-z0-9_+.-]+(\\.[A-Za-z0-9_+.-]+)*@" +
        "[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    private static final Integer MAX_EMAIL = 50;
    private static final Integer MIN_EMAIL = 11;

    public Email {
        if(value == null)
            throw new EmptyVariableException("El email no puede ser null.", "username");
        if(value.isBlank() || value.isBlank())
            throw new EmptyVariableException("El email no puede estar en blanco.", "username");
        if(value.length() < MIN_EMAIL || value.length() > MAX_EMAIL)
            throw new SizeVariableException("email", MAX_EMAIL, MIN_EMAIL);

        Pattern pattern = Pattern.compile(value);
        pattern.matcher(value);
        if(!EMAIL_PATTERN.matcher(value).matches())
            throw new PatternVariableDoesNotMatchException("email");
    }

}