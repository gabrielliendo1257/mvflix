package com.guille.media.bff.experience.media.application.port;

import reactor.core.publisher.Mono;

/** Vínculo con el proveedor externo (TMDB); la política vive en movies. */
public interface ProviderActions {

  /** Vincula la media a un candidato del proveedor (autocompleta metadata). */
  Mono<Void> link(long mediaId, long providerId);

  /** Desvincula la media del proveedor (la metadata queda como está). */
  Mono<Void> unlink(long mediaId);
}
