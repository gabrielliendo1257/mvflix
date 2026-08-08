package com.guille.media.reproductor.users.domain.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.guille.media.reproductor.users.domain.exceptions.EmptyVariableException;
import com.guille.media.reproductor.users.domain.exceptions.PatternVariableDoesNotMatchException;
import com.guille.media.reproductor.users.domain.exceptions.SizeVariableException;

import org.junit.jupiter.api.Test;

class EmailTest {

    @Test
    void acceptsValidEmail() {
        assertEquals("pepe@example.com", new Email("pepe@example.com").value());
        assertEquals("ab@c.couk", new Email("ab@c.couk").value());
    }

    @Test
    void rejectsNullAndBlank() {
        assertThrows(EmptyVariableException.class, () -> new Email(null));
        assertThrows(EmptyVariableException.class, () -> new Email("  "));
    }

    @Test
    void rejectsWrongSize() {
        assertThrows(SizeVariableException.class, () -> new Email("a@b.c"));
        assertThrows(SizeVariableException.class, () -> new Email("a".repeat(300) + "@example.com"));
    }

    @Test
    void rejectsMalformedEmail() {
        assertThrows(PatternVariableDoesNotMatchException.class, () -> new Email("not-an-email"));
        assertThrows(PatternVariableDoesNotMatchException.class, () -> new Email("pepe@nodosis"));
    }
}