package com.guille.media.bff.experience.media.application.port;

import reactor.core.publisher.Mono;

import java.util.List;

/** Acceso completo de una media; la transacción vive en movies. */
public interface AccessActions {

  /** Visibilidad + compartidos se aplican juntos o no se aplican. */
  Mono<Void> updateAccess(long mediaId, String visibility, List<String> sharedWith);
}
