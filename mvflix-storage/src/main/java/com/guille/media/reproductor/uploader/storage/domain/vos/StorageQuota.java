package com.guille.media.reproductor.uploader.storage.domain.vos;

import java.util.Objects;

public class StorageQuota {

    /**
     * Cantidad máxima de bytes permitidos.
     */
    private final long maxBytes;

    /**
     * Crea una nueva cuota de almacenamiento.
     *
     * @param maxBytes cantidad máxima de bytes permitidos.
     * @throws IllegalArgumentException si {@code maxBytes} es negativo.
     */
    public StorageQuota(long maxBytes) {
        if (maxBytes < 0) {
            throw new IllegalArgumentException(
                    "Storage quota cannot be negative");
        }
        this.maxBytes = maxBytes;
    }

    /**
     * Devuelve el límite máximo expresado en bytes.
     *
     * @return cantidad máxima de bytes permitidos.
     */
    public long maxBytes() {
        return maxBytes;
    }

    /**
     * Determina si el tamaño utilizado se encuentra dentro del límite.
     *
     * @param usedBytes bytes actualmente utilizados.
     * @return {@code true} si el uso es menor o igual al límite.
     * @throws IllegalArgumentException si {@code usedBytes} es negativo.
     */
    public boolean allows(long usedBytes) {
        if (usedBytes < 0) {
            throw new IllegalArgumentException(
                    "Used bytes cannot be negative");
        }
        return usedBytes <= maxBytes;
    }

    /**
     * Determina si es posible agregar bytes adicionales al uso actual.
     *
     * @param usedBytes       bytes actualmente utilizados.
     * @param additionalBytes bytes adicionales que se desean consumir.
     * @return {@code true} si la suma no supera la cuota.
     * @throws IllegalArgumentException si algún valor es negativo.
     */
    public boolean canAllocate(long usedBytes, long additionalBytes) {
        if (usedBytes < 0) {
            throw new IllegalArgumentException(
                    "Used bytes cannot be negative");
        }
        if (additionalBytes < 0) {
            throw new IllegalArgumentException(
                    "Additional bytes cannot be negative");
        }

        return usedBytes + additionalBytes <= maxBytes;
    }

    /**
     * Calcula la cantidad de bytes aún disponibles.
     *
     * @param usedBytes bytes actualmente utilizados.
     * @return bytes restantes disponibles.
     * @throws IllegalArgumentException si {@code usedBytes} es negativo.
     */
    public long remainingBytes(long usedBytes) {
        if (usedBytes < 0) {
            throw new IllegalArgumentException(
                    "Used bytes cannot be negative");
        }
        return Math.max(0, maxBytes - usedBytes);
    }

    /**
     * Indica si la cuota es ilimitada.
     *
     * <p>
     * Por convención, {@link Long#MAX_VALUE} representa una cuota sin límite.
     *
     * @return {@code true} si la cuota es ilimitada.
     */
    public boolean isUnlimited() {
        return maxBytes == Long.MAX_VALUE;
    }

    /**
     * Crea una cuota ilimitada.
     *
     * @return instancia de cuota sin límite.
     */
    public static StorageQuota unlimited() {
        return new StorageQuota(Long.MAX_VALUE);
    }

    /**
     * Crea una cuota expresada en gigabytes binarios (GiB).
     *
     * @param gigabytes cantidad de GiB.
     * @return nueva cuota equivalente.
     * @throws IllegalArgumentException si {@code gigabytes} es negativo.
     */
    public static StorageQuota ofGigabytes(long gigabytes) {
        if (gigabytes < 0) {
            throw new IllegalArgumentException(
                    "Gigabytes cannot be negative");
        }
        return new StorageQuota(gigabytes * 1024L * 1024L * 1024L);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StorageQuota other)) {
            return false;
        }
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
