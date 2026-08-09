package com.guille.media.reproductor.uploader.storage.domain.ports;

import com.guille.media.reproductor.uploader.storage.domain.models.StoreObject;
import com.guille.media.reproductor.uploader.storage.domain.models.StoreObject.StorageSessionStatus;

import java.time.Instant;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface StorageRepository {
  Mono<StoreObject> save(StoreObject storageObject);

  Mono<StoreObject> findById(Long storageId);

  /** Busca por la clave lógica del objeto en el object store (camino de eventos de MinIO). */
  Mono<StoreObject> findByObjectKey(String objectKey);

  /**
   * Persiste la transición de estado aplicada por el dominio ({@link StoreObject}).
   *
   * <p>Optimistic lock: la fila solo se actualiza si su estado actual en BD coincide con {@code
   * expectedStatus}. Si otro flujo concurrente la transicionó antes, no hay fila afectada y la
   * operación falla con {@link
   * com.guille.media.reproductor.uploader.storage.domain.exceptions.IllegalStateTransitionException}.
   *
   * @param storageObject objeto ya transicionado por el dominio.
   * @param expectedStatus estado previo esperado en BD.
   */
  Mono<StoreObject> updateStatus(StoreObject storageObject, StorageSessionStatus expectedStatus);

  Flux<StoreObject> findPendingCreatedBefore(Instant cutoff);

  /**
   * Actualiza {@code last_modified_at} como historial de "última actividad"
   * (p. ej. cuando se genera una sesión de streaming).
   */
  Mono<Void> touchLastSeen(Long storageId, Instant seenAt);
}
