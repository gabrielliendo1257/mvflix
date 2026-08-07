package com.guille.media.reproductor.uploader.storage.domain.ports;

import com.guille.media.reproductor.uploader.storage.domain.models.StoreObject;
import reactor.core.publisher.Mono;

public interface StorageRepository {
    Mono<StoreObject> save(StoreObject storageObject);

    Mono<StoreObject> findById(Long storageId);

    Mono<StoreObject> markCompleted(Long storageId);
}
