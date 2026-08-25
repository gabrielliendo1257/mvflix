package com.guille.media.bff.experience.shell.application;

import org.springframework.lang.Nullable;

/**
 * Uso de cuota para el indicador del navbar. Dato propiedad de storage
 * (fuente de verdad del consumo); si no está disponible, {@code available}
 * es false y la UI oculta el indicador en lugar de romper el shell.
 */
public record ShellQuota(
    boolean available,
    @Nullable Long usedBytes,
    @Nullable Long limitBytes,
    @Nullable Integer usedPercent) {

  public static ShellQuota unavailable() {
    return new ShellQuota(false, null, null, null);
  }
}
