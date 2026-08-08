package com.guille.media.reproductor.users.domain.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PlanChangeDecisionTest {

    @Test
    void upgradeIsImmediate() {
        assertEquals(
                PlanChangeDecision.UPGRADE_IMMEDIATE,
                PlanChangeDecision.evaluate(Plan.FREE, Plan.PRO));
        assertEquals(
                PlanChangeDecision.UPGRADE_IMMEDIATE,
                PlanChangeDecision.evaluate(Plan.FREE, Plan.ENTERPRISE));
        assertEquals(
                PlanChangeDecision.UPGRADE_IMMEDIATE,
                PlanChangeDecision.evaluate(Plan.PRO, Plan.ENTERPRISE));
    }

    @Test
    void downgradeAppliesAtEndOfCycle() {
        assertEquals(
                PlanChangeDecision.DOWNGRADE_END_OF_CYCLE,
                PlanChangeDecision.evaluate(Plan.PRO, Plan.FREE));
        assertEquals(
                PlanChangeDecision.DOWNGRADE_END_OF_CYCLE,
                PlanChangeDecision.evaluate(Plan.ENTERPRISE, Plan.PRO));
        assertEquals(
                PlanChangeDecision.DOWNGRADE_END_OF_CYCLE,
                PlanChangeDecision.evaluate(Plan.ENTERPRISE, Plan.FREE));
    }

    @Test
    void samePlanIsNoChange() {
        assertEquals(
                PlanChangeDecision.NO_CHANGE,
                PlanChangeDecision.evaluate(Plan.PRO, Plan.PRO));
    }
}