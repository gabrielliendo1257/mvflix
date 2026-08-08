package com.guille.media.reproductor.users.domain.models;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Política de devolución (derecho de desistimiento).
 *
 * <p>Regla: la devolución solo es exigible dentro de la ventana legal contada
 * desde el pago <b>y</b> siempre que el usuario no haya aceptado los términos.
 * Aceptar los términos es renunciar al derecho de devolución: una vez aceptados,
 * el pago no se devuelve. Reclamar antes de aceptar (o sin aceptar) y dentro de
 * la ventana confirma la devolución.
 */
public record RefundPolicy(int windowDays) {

    public RefundPolicy {
        if (windowDays <= 0) {
            throw new IllegalArgumentException("windowDays debe ser > 0: " + windowDays);
        }
    }

    /**
     * @param paidAt fecha del pago de la suscripción (no null).
     * @param termsAcceptedAt fecha de aceptación de términos; null si aún no acepta.
     * @param claimDate fecha en que se reclama la devolución (no null).
     * @return decisión de la política de devolución.
     */
    public RefundDecision decide(LocalDate paidAt, LocalDate termsAcceptedAt, LocalDate claimDate) {
        if (paidAt == null || claimDate == null) {
            throw new IllegalArgumentException("paidAt y claimDate no pueden ser null.");
        }

        if (termsAcceptedAt != null) {
            return claimDate.isBefore(termsAcceptedAt)
                    ? RefundDecision.ACCEPTED
                    : RefundDecision.REJECTED_TERMS_ACCEPTED;
        }

        long daysSincePayment = ChronoUnit.DAYS.between(paidAt, claimDate);
        return daysSincePayment <= windowDays
                ? RefundDecision.ACCEPTED
                : RefundDecision.REJECTED_WINDOW_EXPIRED;
    }

    public enum RefundDecision {
        ACCEPTED,
        REJECTED_TERMS_ACCEPTED,
        REJECTED_WINDOW_EXPIRED
    }
}