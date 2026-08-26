package com.guille.media.bff.experience.media.application;

import com.guille.media.bff.experience.media.application.port.AccessActions;
import com.guille.media.bff.experience.media.application.port.MediaDetailProjection;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/**
 * Cambia el acceso completo (visibilidad + compartidos) de la media y
 * devuelve el detalle ya actualizado. Un solo contrato consistente para las
 * capabilities changeVisibility y manageSharing.
 *
 * <p>El refresco va en {@code then(Mono.defer(...))}: la mutación es
 * {@code Mono<Void>} y el detalle SOLO se construye si la mutación triunfó.
 */
@Service
@RequiredArgsConstructor
public class ChangeMediaAccess {

  private final AccessActions actions;
  private final MediaDetailProjection projection;

  public Mono<MediaDetail> execute(
      long mediaId, String visibility, java.util.List<String> sharedWith) {
    return this.actions.updateAccess(mediaId, visibility, sharedWith)
        .then(Mono.defer(() -> this.projection.detail(mediaId)));
  }
}
