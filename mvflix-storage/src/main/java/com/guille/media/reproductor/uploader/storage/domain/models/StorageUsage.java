package com.guille.media.reproductor.uploader.storage.domain.models;

import lombok.Getter;

/**
 * Uso acumulado de bytes de una cuenta. Es un valor de lectura (proyección):
 * las mutaciones autoritativas ocurren en la base de datos mediante los
 * updates atómicos de {@code UserStorageRepository}.
 */
@Getter
public class StorageUsage {
    private final long currentBytesUsage;

    public StorageUsage(long currentBytesUsage) {
        this.currentBytesUsage = currentBytesUsage;
    }
}
