package com.guille.media.reproductor.uploader.storage.domain.exceptions;

import com.guille.media.reproductor.uploader.storage.domain.vos.StorageKey;

public class ObjectAlreadyExistsException extends RuntimeException {
    public StorageKey key;

    public ObjectAlreadyExistsException(StorageKey key) {
        super();
        this.key = key;
    }
}
