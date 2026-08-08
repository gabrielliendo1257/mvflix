package com.guille.media.reproductor.users.domain.models;

/**
 * Decision sobre un cambio de plan solicitado.
 *
 * <p>Reglas (política de facturación):
 * <ul>
 *   <li><b>Upgrade</b> (subir de plan): se aplica de inmediato y se prorratea el
 *       coste del periodo restante del ciclo actual.</li>
 *   <li><b>Downgrade</b> (bajar de plan) y <b>cancelación</b>: se aplican al final
 *       del ciclo vigente, ya que el ciclo actual ya fue facturado. Así se evita
 *       que un usuario abandone el plan para eludir el cobro del periodo en
 *       curso.</li>
 * </ul>
 */
public enum PlanChangeDecision {
    UPGRADE_IMMEDIATE("Upgrade: se aplica al instante con prorrateo."),
    DOWNGRADE_END_OF_CYCLE("Downgrade/cancelación: se aplica al final del ciclo de facturación."),
    NO_CHANGE("Sin cambio: el plan solicitado coincide con el actual.");

    private final String detail;

    PlanChangeDecision(String detail) {
        this.detail = detail;
    }

    public String detail() {
        return detail;
    }

    public static PlanChangeDecision evaluate(Plan current, Plan requested) {
        if (current == requested) {
            return NO_CHANGE;
        }
        return current.rank() < requested.rank()
                ? UPGRADE_IMMEDIATE
                : DOWNGRADE_END_OF_CYCLE;
    }
}