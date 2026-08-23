package com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception;

import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageKey;

public class ObjectAlreadyExistsException extends RuntimeException {
    public StorageKey key;

    public ObjectAlreadyExistsException(StorageKey key) {
        super();
        this.key = key;
    }
}
