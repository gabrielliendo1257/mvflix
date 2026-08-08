package com.guille.media.reproductor.users.domain.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StorageQuotaTest {

    @Test
    void quotaIsDerivedFromPlan() {
        assertEquals(500L * 1024 * 1024, StorageQuota.getQuota(Plan.FREE).getUserBytesQuota());
        assertEquals(100L * 1024 * 1024 * 1024, StorageQuota.getQuota(Plan.PRO).getUserBytesQuota());
        assertEquals(
                1024L * 1024 * 1024 * 1024,
                StorageQuota.getQuota(Plan.ENTERPRISE).getUserBytesQuota());
    }

    @Test
    void quotaCannotBeNegative() {
        assertThrows(IllegalArgumentException.class, () -> new StorageQuota(-1L));
    }

    @Test
    void isExceededWhenUsageTopsQuota() {
        StorageQuota quota = new StorageQuota(100L);

        assertTrue(quota.isExceeded(101L));
        assertFalse(quota.isExceeded(100L));
        assertFalse(quota.isExceeded(99L));
    }

    @Test
    void remainingIsQuotaMinusUsage() {
        StorageQuota quota = new StorageQuota(100L);

        assertEquals(75L, quota.remaining(25L));
        assertEquals(0L, quota.remaining(200L));
    }

    @Test
    void freeUserQuotaAcceptsUpToItsLimit() {
        StorageQuota free = StorageQuota.getQuota(Plan.FREE);

        assertFalse(free.isExceeded(free.getUserBytesQuota()));
        assertTrue(free.isExceeded(free.getUserBytesQuota() + 1));
    }
}