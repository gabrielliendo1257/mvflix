package com.guille.media.reproductor.uploader.storage.managedstorage.domain.model;

import java.util.UUID;

import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageFolder;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageKey;

/**
 * Genera una clave dentro del espacio del usuario:
 * {@code <username>/<carpeta>/<uuid>}. Sin dependencias de framework; se
 * registra como bean en la configuración de aplicación.
 */
public class StorageKeyGenerator {

    public StorageKey generate(String username, StorageFolder folder) {
        String key = username + "/" + folder.path() + UUID.randomUUID();
        return new StorageKey(key);
    }
}
