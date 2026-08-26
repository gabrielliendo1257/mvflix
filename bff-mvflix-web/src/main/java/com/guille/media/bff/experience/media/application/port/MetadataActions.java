package com.guille.media.bff.experience.media.application.port;

import com.guille.media.bff.experience.media.application.MetadataPatch;

import reactor.core.publisher.Mono;

/** Edición manual de metadata; merge y autorización viven en movies. */
public interface MetadataActions {

  Mono<Void> updateMetadata(long mediaId, MetadataPatch patch);
}
