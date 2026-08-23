package com.guille.media.bff.experience.addmedia.application.port;

import com.guille.media.bff.experience.addmedia.model.AddMediaId;
import com.guille.media.bff.experience.addmedia.model.AddMediaProcess;

import reactor.core.publisher.Mono;

/**
 * Persistencia del estado de proceso Add Media, propiedad del BFF. La clave
 * (ownerSubject, idempotencyKey) es ÚNICA: replays del mismo intento devuelven
 * el proceso original en lugar de crear otro draft/upload.
 */
public interface AddMediaProcessRepository {

  /**
   * Devuelve el proceso existente para el par (owner, idempotencyKey) o crea
   * uno nuevo en fase STARTING. Atómico respecto a la unicidad. Si la key
   * existe con OTRO fingerprint, falla con IdempotencyConflictException: la
   * misma key solo es válida para el MISMO payload.
   */
  Mono<AddMediaProcess> createIfAbsent(
      String ownerSubject, String idempotencyKey, String requestFingerprint);

  Mono<AddMediaProcess> findById(AddMediaId id);

  /**
   * Guarda con verificación optimista por {@code version}: falla si el proceso
   * fue modificado concurrentemente desde que se leyó.
   */
  Mono<AddMediaProcess> save(AddMediaProcess process);

  /**
   * Reclamo atómico STARTING -> PREPARING. Verdadero solo para el ganador:
   * los perdedores NO deben ejecutar los side effects del alta.
   */
  Mono<Boolean> tryClaim(AddMediaId id);

  /**
   * Reclamo atómico de finalización (WAITING_FOR_UPLOAD | VERIFYING ->
   * FINALIZING). Verdadero solo para el ganador de la carrera complete vs
   * cancel; el perdedor NO ejecuta side effects.
   */
  Mono<Boolean> tryFinalizeClaim(AddMediaId id);

  /**
   * Reclamo atómico de cancelación desde fases activas
   * (WAITING_FOR_UPLOAD | VERIFYING_UPLOAD -> CANCELLING).
   */
  Mono<Boolean> tryCancelClaim(AddMediaId id);

  /** Devuelve un reclamo fallido a STARTING para habilitar el reintento. */
  Mono<AddMediaProcess> releaseClaim(AddMediaId id);
}
