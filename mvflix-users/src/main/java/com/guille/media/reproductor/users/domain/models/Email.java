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

    private static final int MAX_EMAIL = 255;
    private static final int MIN_EMAIL = 8;

    public Email {
        if (value == null)
            throw new EmptyVariableException("El email no puede ser null.", "email");
        if (value.isBlank())
            throw new EmptyVariableException("El email no puede estar en blanco.", "email");
        if (value.length() < MIN_EMAIL || value.length() > MAX_EMAIL)
            throw new SizeVariableException("email", MAX_EMAIL, MIN_EMAIL);
        if (!EMAIL_PATTERN.matcher(value).matches())
            throw new PatternVariableDoesNotMatchException("email");
    }
}