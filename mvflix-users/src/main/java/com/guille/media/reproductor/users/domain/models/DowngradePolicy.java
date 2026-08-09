package com.guille.media.reproductor.users.domain.models;

/**
 * Regla de descenso de plan: no se permite un downgrade si el uso real ya queda
 * por encima de la cuota del plan solicitado. El uso real lo reporta el
 * storage-service (fuente de verdad del consumo); aqui solo se aplica la
 * politica del plan.
 */
public final class DowngradePolicy {

    private DowngradePolicy() {}

    /**
     * Evalúa el cambio de plan y lanza {@code DowngradeBlockedByUsageException}
     * si el plan solicitado es un downgrade que no puede absorber el uso real.
     *
     * @return la decisión del cambio cuando el downgrade es admisible.
     */
    public static PlanChangeDecision evaluate(Plan current, Plan requested, long usedBytes) {
        PlanChangeDecision decision = PlanChangeDecision.evaluate(current, requested);
        if (decision == PlanChangeDecision.DOWNGRADE_END_OF_CYCLE
                && storageLimitOf(requested) < usedBytes) {
            throw new com.guille.media.reproductor.users.domain.exceptions
                    .DowngradeBlockedByUsageException(
                    "Downgrade to "
                            + requested
                            + " blocked: usage "
                            + usedBytes
                            + " exceeds the quota "
                            + storageLimitOf(requested));
        }
        return decision;
    }

    private static long storageLimitOf(Plan plan) {
        return StorageQuota.getQuota(plan).getUserBytesQuota();
    }
}