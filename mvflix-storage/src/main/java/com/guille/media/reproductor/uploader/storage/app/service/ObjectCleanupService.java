package com.guille.media.reproductor.uploader.storage.app.service;

import reactor.core.publisher.Mono;

import java.time.Instant;

/** Ciclo de vida del objeto almacenado: borrado lógico y expiración de subidas huérfanas. */
public interface ObjectCleanupService {
  /**
   * Elimina el objeto (y su blob) del que el usuario autenticado es dueño,
   * liberando la cuota asociada.
   */
  Mono<Void> deleteObject(Long storageId);

  /**
   * Expira las sesiones {@code PENDING} anteriores a {@code cutoff}, liberando
   * la cuota reservada. Devuelve el número de sesiones expiradas.
   */
  Mono<Long> expireStaleSessions(Instant cutoff);
}