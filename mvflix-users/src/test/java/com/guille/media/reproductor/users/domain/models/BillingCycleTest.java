package com.guille.media.reproductor.users.domain.models;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class BillingCycleTest {

    @Test
    void rejectsNullBounds() {
        assertThrows(
                com.guille.media.reproductor.users.domain.exceptions.EmptyVariableException.class,
                () -> new BillingCycle(null, LocalDate.of(2026, 1, 30)));
        assertThrows(
                com.guille.media.reproductor.users.domain.exceptions.EmptyVariableException.class,
                () -> new BillingCycle(LocalDate.of(2026, 1, 1), null));
    }

    @Test
    void rejectsEndNotAfterStart() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new BillingCycle(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1)));
    }

    @Test
    void isActiveInsideTheCycle() {
        BillingCycle cycle =
                new BillingCycle(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertTrue(cycle.isActiveAt(LocalDate.of(2026, 1, 1)));
        assertTrue(cycle.isActiveAt(LocalDate.of(2026, 1, 30)));
        assertFalse(cycle.isActiveAt(LocalDate.of(2026, 1, 31)));
        assertFalse(cycle.isActiveAt(LocalDate.of(2025, 12, 31)));
    }

    @Test
    void nextRollsOverSameDuration() {
        BillingCycle cycle =
                new BillingCycle(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        BillingCycle next = cycle.next();

        assertTrue(next.isActiveAt(LocalDate.of(2026, 1, 31)));
        assertFalse(next.isActiveAt(LocalDate.of(2026, 1, 30)));
    }
}