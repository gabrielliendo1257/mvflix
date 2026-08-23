package com.guille.media.reproductor.uploader.storage.domain.models;

import java.util.Objects;

/**
 * Value Object que representa el límite máximo de almacenamiento para un usuario.
 *
 * <p>Encapsula la cuota en bytes y ofrece operaciones seguras para verificar disponibilidad,
 * evitando cálculos dispersos en el dominio.
 */
public final class StorageQuota {

  private static final long KB = 1024L;
  private static final long MB = KB * 1024L;
  private static final long GB = MB * 1024L;

  private final long maxBytes;

  /**
   * Crea una nueva cuota de almacenamiento.
   *
   * @param maxBytes cantidad máxima de bytes permitidos.
   * @throws IllegalArgumentException si {@code maxBytes} es negativo.
   */
  public StorageQuota(long maxBytes) {
    if (maxBytes < 0) {
      throw new IllegalArgumentException("Storage quota cannot be negative");
    }
    this.maxBytes = maxBytes;
  }

  public static StorageQuota ofGigabytes(long gigabytes) {
    if (gigabytes < 0) {
      throw new IllegalArgumentException("Gigabytes cannot be negative");
    }
    return new StorageQuota(gigabytes * GB);
  }

  /**
   * Retorna el límite máximo en bytes. Alias semántico de {@link #maxBytes()} para compatibilidad
   * con el mapper existente.
   */
  public long getUserBytesQuota() {
    return maxBytes;
  }

  /** Retorna el límite máximo en bytes. */
  public long maxBytes() {
    return maxBytes;
  }

  /**
   * Calcula los bytes disponibles restantes.
   *
   * @param usedBytes bytes actualmente utilizados.
   * @return bytes disponibles (nunca negativo).
   */
  public long remainingBytes(long usedBytes) {
    return Math.max(0, maxBytes - usedBytes);
  }

  /** Indica si la cuota es ilimitada ({@link Long#MAX_VALUE}). */
  public boolean isUnlimited() {
    return maxBytes == Long.MAX_VALUE;
  }

  // ── equals / hashCode / toString ────────────────────────────────────────

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof StorageQuota other)) return false;
    return maxBytes == other.maxBytes;
  }

  @Override
  public int hashCode() {
    return Objects.hash(maxBytes);
  }

  @Override
  public String toString() {
    return isUnlimited() ? "StorageQuota[unlimited]" : "StorageQuota[" + maxBytes + " bytes]";
  }
}
