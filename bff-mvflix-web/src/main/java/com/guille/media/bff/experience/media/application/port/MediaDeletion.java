package com.guille.media.bff.experience.media.application.port;

import com.guille.media.bff.experience.media.application.DeletionOutcome;

import reactor.core.publisher.Mono;

/**
 * Solicitud de borrado del item. Movies decide el lifecycle y sus efectos.
 */
public interface MediaDeletion {

  /**
   * @return {@link DeletionOutcome.Completed} o {@link DeletionOutcome.Pending}.
   */
  Mono<DeletionOutcome> requestDeletion(long mediaId);
}
