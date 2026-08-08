package com.guille.media.reproductor.users.domain.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.guille.media.reproductor.users.domain.exceptions.EmptyVariableException;
import com.guille.media.reproductor.users.domain.exceptions.SizeVariableException;

import org.junit.jupiter.api.Test;

class UsernameTest {

    @Test
    void acceptsValidUsername() {
        assertEquals("pepe", new Username("pepe").value());
    }

    @Test
    void rejectsNullAndBlank() {
        assertThrows(EmptyVariableException.class, () -> new Username(null));
        assertThrows(EmptyVariableException.class, () -> new Username("   "));
    }

    @Test
    void rejectsWrongLength() {
        assertThrows(SizeVariableException.class, () -> new Username("abc"));
        assertThrows(SizeVariableException.class, () -> new Username("a".repeat(31)));
    }
}