package com.guille.media.reproductor.users.domain.ports;

import reactor.core.publisher.Mono;

/** Consulta el uso real (bytes consumidos) de un usuario al storage-service. */
public interface StorageUsagePort {

    /** @return bytes realmente consumidos por {@code username} (fuente: storage-service). */
    Mono<Long> usedBytesBy(String username);
}