package com.guille.media.bff.experience.media.application;

import com.guille.media.bff.experience.media.application.port.MediaDetailProjection;
import com.guille.media.bff.experience.media.application.port.ProviderActions;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/**
 * Vincula la media a un candidato del proveedor y devuelve el detalle YA
 * actualizado: una llamada, la vista completa. La validación del vínculo y
 * la autorización son de movies.
 */
@Service
@RequiredArgsConstructor
public class LinkMediaProvider {

  private final ProviderActions actions;
  private final MediaDetailProjection projection;

  public Mono<MediaDetail> execute(long mediaId, long tmdbId) {
    return this.actions.link(mediaId, tmdbId)
        .then(Mono.defer(() -> this.projection.detail(mediaId)));
  }
}
