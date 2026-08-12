package com.gcorp.service.app.mvflix_movies.domain.service;

import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Limpieza de metadata huérfana de películas: purga películas DRAFT cuyo flujo de subida
 * nunca llegó a completarse.
 */
public interface MovieCleanupService {

    /**
     * Borra películas DRAFT creadas antes del corte.
     *
     * @return cantidad de películas purgadas.
     */
    Mono<Long> purgeDrafts(Instant cutoff);
}