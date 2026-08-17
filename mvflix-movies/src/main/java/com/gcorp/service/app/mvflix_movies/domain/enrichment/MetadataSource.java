package com.gcorp.service.app.mvflix_movies.domain.enrichment;

import reactor.core.publisher.Mono;

/**
 * Fuente externa de metadatos del catalogo (TMDB es el adapter concreto).
 * El dominio depende de este port, no de detalles de infraestructura.
 */
public interface MetadataSource {

    /**
     * Busca una pelicula por titulo (+ año opcional). Vacío si no hay match
     * (el use-case deja la pelicula sin enriquecer en lugar de inventar datos).
     */
    Mono<ExternalMovieSearch> search(String title, Integer year);

    /** Detalle completo de una pelicula ya matcheada (id externo estable). */
    Mono<ExternalMovieDetail> findById(long tmdbId);
}