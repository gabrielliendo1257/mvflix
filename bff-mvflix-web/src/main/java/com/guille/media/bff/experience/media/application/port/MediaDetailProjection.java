package com.guille.media.bff.experience.media.application.port;

import com.guille.media.bff.experience.media.application.MediaDetail;

import reactor.core.publisher.Mono;

/** Proyección de detalle bajo la identidad del usuario; movies autoriza. */
public interface MediaDetailProjection {

  Mono<MediaDetail> detail(long mediaId);
}
