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
   * uno nuevo en fase STARTING. Atómico respecto a la unicidad.
   */
  Mono<AddMediaProcess> createIfAbsent(String ownerSubject, String idempotencyKey);

  Mono<AddMediaProcess> findById(AddMediaId id);

  /**
   * Guarda con verificación optimista por {@code version}: falla si el proceso
   * fue modificado concurrentemente desde que se leyó.
   */
  Mono<AddMediaProcess> save(AddMediaProcess process);
}
