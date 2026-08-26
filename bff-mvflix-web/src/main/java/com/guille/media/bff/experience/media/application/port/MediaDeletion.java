package com.guille.media.bff.experience.media.application.port;

import reactor.core.publisher.Mono;

/**
 * Borrado de la entrada de catálogo, IDEMPOTENTE.
 *
 * <p>Sin transacción distribuida ACID: el catálogo (movies) es la única
 * fuente de verdad de existencia y se borra en una sola mutación local. El
 * objeto MANAGED en storage NO se toca aquí; queda huérfano. Storage ya tiene
 * una cola durable de huérfanos ({@code OrphanCleanupQueue}/{@code OrphanCleanupJob})
 * pero el encolado al borrar catálogo es un TODO explícito — esta operación
 * no lo resuelve, solo lo deja señalizado.
 */
public interface MediaDeletion {

  /**
   * @return {@code true} si se borró ahora, {@code false} si ya no existía.
   *     Nunca lanza 404 por ausencia (eso es la semántica idempotente).
   */
  Mono<Boolean> deleteCatalog(long mediaId);
}
