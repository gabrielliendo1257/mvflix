package com.guille.media.bff.experience.playback.application.port;

import com.guille.media.bff.experience.playback.application.DirectSource;

import reactor.core.publisher.Mono;

/**
 * Acceso directo a objetos MANAGED (MinIO) via la capacidad de streaming del
 * storage. El storage ya valido disponibilidad del objeto y emite la URL
 * presigned: el navegador la consume con Range sin pasar por el BFF.
 */
public interface ManagedContentAccess {

  Mono<DirectSource> openDirect(Long objectId);
}
