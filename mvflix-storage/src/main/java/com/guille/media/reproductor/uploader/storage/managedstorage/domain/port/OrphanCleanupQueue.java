package com.guille.media.reproductor.uploader.storage.managedstorage.domain.port;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Cola DURABLE de objetos huérfanos: blobs en MinIO sin fila viva que deben
 * eliminarse. Reemplaza el best-effort silencioso: si el DELETE falla, la
 * tarea queda persistida y un scheduler reintenta hasta lograrlo.
 */
public interface OrphanCleanupQueue {

    /** Encola (o re-encola) una tarea; idempotente por (bucket, key) pendiente. */
    Mono<Void> enqueue(String bucket, String objectKey, String ownerUsername, String reason);

    /** Tareas pendientes más antiguas primero, limitadas por corrida. */
    Flux<OrphanTask> pending(int limit);

    /** Marca la tarea como procesada tras conseguir borrar el blob. */
    Mono<Void> markProcessed(long taskId);

    record OrphanTask(long id, String bucket, String objectKey, String ownerUsername,
                      String reason, int attempts) {}
}
