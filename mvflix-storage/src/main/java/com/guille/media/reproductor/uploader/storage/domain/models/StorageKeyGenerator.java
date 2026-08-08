package com.guille.media.reproductor.uploader.storage.domain.models;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.guille.media.reproductor.uploader.storage.domain.vos.StorageFolder;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageKey;

@Component
public class StorageKeyGenerator {

    /**
     * Genera una clave dentro del espacio del usuario:
     * {@code <username>/<carpeta>/<uuid>}.
     */
    public StorageKey generate(String username, StorageFolder folder) {
        String key = username + "/" + folder.path() + UUID.randomUUID();
        return new StorageKey(key);
    }
}
