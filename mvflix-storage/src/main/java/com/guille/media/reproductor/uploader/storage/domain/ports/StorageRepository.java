package com.guille.media.reproductor.uploader.storage.domain.ports;

import com.guille.media.reproductor.uploader.storage.domain.models.StoreObject;

import java.time.Instant;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface StorageRepository {
  Mono<StoreObject> save(StoreObject storageObject);

  Mono<StoreObject> findById(Long storageId);

  Mono<StoreObject> markCompleted(Long storageId);

  Mono<StoreObject> markExpired(Long storageId);

  Flux<StoreObject> findPendingCreatedBefore(Instant cutoff);

  /**
   * Actualiza {@code last_modified_at} como historial de "última actividad"
   * (p. ej. cuando se genera una sesión de streaming).
   */
  Mono<Void> touchLastSeen(Long storageId, Instant seenAt);
}
