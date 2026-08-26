package com.guille.media.bff.experience.media.application;

import com.guille.media.bff.experience.media.application.port.MediaDetailProjection;
import com.guille.media.bff.experience.media.application.port.MetadataActions;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/**
 * Edición manual de metadata del dueño: aplica el parche (merge en movies) y
 * devuelve el detalle ya actualizado en una sola llamada.
 */
@Service
@RequiredArgsConstructor
public class EditMediaMetadata {

  private final MetadataActions actions;
  private final MediaDetailProjection projection;

  public Mono<MediaDetail> execute(long mediaId, MetadataPatch patch) {
    return this.actions.updateMetadata(mediaId, patch)
        .then(Mono.defer(() -> this.projection.detail(mediaId)));
  }
}
