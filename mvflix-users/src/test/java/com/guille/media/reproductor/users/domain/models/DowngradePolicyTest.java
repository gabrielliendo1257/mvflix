package com.guille.media.reproductor.users.domain.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.guille.media.reproductor.users.domain.exceptions.DowngradeBlockedByUsageException;

import org.junit.jupiter.api.Test;

class DowngradePolicyTest {

    @Test
    void upgradeIsNotLimitedByUsage() {
        assertEquals(
                PlanChangeDecision.UPGRADE_IMMEDIATE,
                DowngradePolicy.evaluate(Plan.FREE, Plan.PRO, Long.MAX_VALUE));
    }

    @Test
    void downgradeWithinQuotaIsAllowed() {
        long proQuota = StorageQuota.getQuota(Plan.PRO).getUserBytesQuota();
        assertEquals(
                PlanChangeDecision.DOWNGRADE_END_OF_CYCLE,
                DowngradePolicy.evaluate(Plan.ENTERPRISE, Plan.PRO, proQuota));
    }

    @Test
    void downgradeBeyondQuotaIsBlocked() {
        long proQuota = StorageQuota.getQuota(Plan.PRO).getUserBytesQuota();
        assertThrows(
                DowngradeBlockedByUsageException.class,
                () -> DowngradePolicy.evaluate(Plan.ENTERPRISE, Plan.PRO, proQuota + 1));
    }

    @Test
    void samePlanIgnoresUsage() {
        assertEquals(
                PlanChangeDecision.NO_CHANGE,
                DowngradePolicy.evaluate(Plan.FREE, Plan.FREE, Long.MAX_VALUE));
    }
}