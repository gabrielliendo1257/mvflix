package com.guille.media.reproductor.uploader.storage.domain.models;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.guille.media.reproductor.uploader.storage.domain.vos.StorageKey;

@Component
public class StorageKeyGenerator {
    public StorageKey generate() {
        String key = UUID.randomUUID().toString();
        return new StorageKey(key);
    }
}
