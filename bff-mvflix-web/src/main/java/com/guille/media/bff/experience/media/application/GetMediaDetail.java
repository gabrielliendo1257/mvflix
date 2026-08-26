package com.guille.media.bff.experience.media.application;

import com.guille.media.bff.experience.media.application.port.MediaDetailProjection;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/**
 * "Entender profundamente esta media": editorial + ciclo de vida + acceso +
 * vínculo con proveedor + menú de acciones. Composición de dos lecturas ya
 * autorizadas por movies; sin URL de reproducción.
 */
@Service
@RequiredArgsConstructor
public class GetMediaDetail {

  private final MediaDetailProjection projection;

  public Mono<MediaDetail> execute(long mediaId) {
    return this.projection.detail(mediaId);
  }
}
