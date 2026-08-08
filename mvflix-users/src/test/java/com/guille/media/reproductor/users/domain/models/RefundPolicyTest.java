package com.guille.media.reproductor.users.domain.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class RefundPolicyTest {

    private final RefundPolicy policy = new RefundPolicy(3);
    private final LocalDate paidAt = LocalDate.of(2026, 1, 1);
    private final LocalDate acceptedAt = LocalDate.of(2026, 1, 2);

    @Test
    void refundWithinWindowWithoutTerms() {
        assertEquals(
                RefundPolicy.RefundDecision.ACCEPTED,
                policy.decide(paidAt, null, LocalDate.of(2026, 1, 3)));
    }

    @Test
    void refundRejectedAfterWindowWithoutTerms() {
        assertEquals(
                RefundPolicy.RefundDecision.REJECTED_WINDOW_EXPIRED,
                policy.decide(paidAt, null, LocalDate.of(2026, 1, 5)));
    }

    @Test
    void termsAcceptanceRenouncesRefund() {
        assertEquals(
                RefundPolicy.RefundDecision.REJECTED_TERMS_ACCEPTED,
                policy.decide(paidAt, acceptedAt, LocalDate.of(2026, 1, 4)));
    }

    @Test
    void claimBeforeAcceptanceIsStillValid() {
        assertEquals(
                RefundPolicy.RefundDecision.ACCEPTED,
                policy.decide(paidAt, acceptedAt, LocalDate.of(2026, 1, 1)));
    }

    @Test
    void rejectsInvalidWindow() {
        assertThrows(IllegalArgumentException.class, () -> new RefundPolicy(0));
        assertThrows(IllegalArgumentException.class, () -> new RefundPolicy(-1));
    }
}