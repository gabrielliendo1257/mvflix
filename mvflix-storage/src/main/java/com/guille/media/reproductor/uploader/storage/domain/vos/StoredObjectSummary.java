package com.guille.media.reproductor.uploader.storage.domain.vos;

import java.time.Instant;
import java.util.Objects;

/**
 * Resumen descriptivo de un objeto listado en el almacenamiento.
 *
 * <p>Es una versión ligera de {@link StorageMetadata} para operaciones de
 * listado, sin disparar una {@code statObject} por cada elemento del bucket.
 */
public record StoredObjectSummary(
        String objectName,
        long contentLength,
        String checksum,
        Instant lastModifiedAt) {

    /**
     * @throws IllegalArgumentException si {@code contentLength} es negativo.
     * @throws NullPointerException si el nombre del objeto es nulo.
     */
    public StoredObjectSummary {
        if (contentLength < 0) {
            throw new IllegalArgumentException("Content length can not be negative");
        }
        Objects.requireNonNull(objectName, "objectName can not be null");
    }
}