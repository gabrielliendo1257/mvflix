package com.guille.media.reproductor.users.domain.models;

import java.time.LocalDate;

import com.guille.media.reproductor.users.domain.exceptions.EmptyVariableException;

/**
 * Ciclo de facturación del {@link User}.
 *
 * <p>Todo cambio de plan que implique un descenso (downgrade) o cancelación se
 * efectúa al final del ciclo vigente: la política de cobro no permite abandonar
 * un plan antes de que termine el periodo ya facturado.
 */
public record BillingCycle(LocalDate periodStart, LocalDate periodEnd) {

    private static final int BILLING_DAYS = 30;

    public BillingCycle {
        if (periodStart == null)
            throw new EmptyVariableException("El inicio del ciclo no puede ser null.", "periodStart");
        if (periodEnd == null)
            throw new EmptyVariableException("El fin del ciclo no puede ser null.", "periodEnd");
        if (!periodEnd.isAfter(periodStart))
            throw new IllegalArgumentException(
                    "periodEnd debe ser posterior a periodStart: " + periodStart + " -> " + periodEnd);
    }

    /** @return true si {@code date} cae dentro de este ciclo. */
    public boolean isActiveAt(LocalDate date) {
        return !date.isBefore(periodStart) && date.isBefore(periodEnd);
    }

    /** @return el ciclo siguiente, de la misma duracion que el actual. */
    public BillingCycle next() {
        return new BillingCycle(periodEnd, periodEnd.plusDays(BILLING_DAYS));
    }

    /** @return el primer ciclo para {@code start} con la duracion estandar. */
    public static BillingCycle startingAt(LocalDate start) {
        return new BillingCycle(start, start.plusDays(BILLING_DAYS));
    }
}