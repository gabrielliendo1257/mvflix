package com.guille.media.reproductor.uploader.storage.domain.models;

import java.util.Objects;

/**
 * Value Object que representa el límite máximo de almacenamiento para un usuario.
 *
 * <p>Encapsula la cuota en bytes y ofrece operaciones seguras para
 * verificar disponibilidad, evitando cálculos dispersos en el dominio.
 */
public final class StorageQuota {

    private static final long KB = 1024L;
    private static final long MB = KB * 1024L;
    private static final long GB = MB * 1024L;

    private static final long PRO_QUOTA       = 100L * GB;
    private static final long ENTERPRISE_QUOTA = 50L * GB;
    private static final long FREE_QUOTA       = 500L * MB;

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

    /**
     * Retorna el límite máximo en bytes.
     * Alias semántico de {@link #maxBytes()} para compatibilidad con el mapper existente.
     */
    public long getUserBytesQuota() {
        return maxBytes;
    }

    /**
     * Retorna el límite máximo en bytes.
     */
    public long maxBytes() {
        return maxBytes;
    }

    /**
     * Determina si el uso supera la cuota.
     *
     * @param storageUsage uso actual de almacenamiento.
     * @return {@code true} si el uso supera el límite.
     */
    public boolean isExceeded(StorageUsage storageUsage) {
        return maxBytes < storageUsage.getCurrentBytesUsage();
    }

    /**
     * Determina si es posible agregar bytes adicionales.
     *
     * @param usedBytes       bytes actualmente usados.
     * @param additionalBytes bytes que se quieren añadir.
     * @return {@code true} si la suma no supera la cuota.
     */
    public boolean canAllocate(long usedBytes, long additionalBytes) {
        if (usedBytes < 0 || additionalBytes < 0) {
            throw new IllegalArgumentException("Bytes values cannot be negative");
        }
        return usedBytes + additionalBytes <= maxBytes;
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

    /**
     * Indica si la cuota es ilimitada ({@link Long#MAX_VALUE}).
     */
    public boolean isUnlimited() {
        return maxBytes == Long.MAX_VALUE;
    }

    // ── Factory methods ──────────────────────────────────────────────────────

    public static StorageQuota getQuota(Plan plan) {
        return switch (plan) {
            case FREE       -> new StorageQuota(FREE_QUOTA);
            case ENTERPRISE -> new StorageQuota(ENTERPRISE_QUOTA);
            case PRO        -> new StorageQuota(PRO_QUOTA);
        };
    }

    public static StorageQuota unlimited() {
        return new StorageQuota(Long.MAX_VALUE);
    }

    public static StorageQuota ofGigabytes(long gigabytes) {
        if (gigabytes < 0) {
            throw new IllegalArgumentException("Gigabytes cannot be negative");
        }
        return new StorageQuota(gigabytes * GB);
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
        return isUnlimited()
                ? "StorageQuota[unlimited]"
                : "StorageQuota[" + maxBytes + " bytes]";
    }
}
