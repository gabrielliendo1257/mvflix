package com.guille.media.bff.experience.media.application;

import com.guille.media.bff.experience.media.application.port.MediaDetailProjection;
import com.guille.media.bff.experience.media.application.port.ProviderActions;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/**
 * Desvincula la media del proveedor (vuelve a estado sin proveedor) y
 * devuelve el detalle ya actualizado.
 */
@Service
@RequiredArgsConstructor
public class UnlinkMediaProvider {

  private final ProviderActions actions;
  private final MediaDetailProjection projection;

  public Mono<MediaDetail> execute(long mediaId) {
    return this.actions.unlink(mediaId)
        .then(this.projection.detail(mediaId));
  }
}
