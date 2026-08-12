package com.gcorp.service.app.mvflix_movies.domain.ports;

import com.gcorp.service.app.mvflix_movies.domain.model.Movie;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface MovieRepository {

    Mono<Movie> save(Movie movie);

    Mono<Movie> findById(Long id);

    Flux<Movie> findByOwner(String ownerUsername, int limit);

    /**
     * Compare-and-set: pasa a READY solo si la película sigue en DRAFT y pertenece al dueño.
     * Vacío si no hubo fila (no existe, otro dueño o ya no está en DRAFT).
     */
    Mono<Movie> completeIfDraft(Long id, String ownerUsername, String objectKey);

    /** {@code true} si borró la fila del dueño. */
    Mono<Boolean> deleteById(Long id, String ownerUsername);

    /**
     * Purga películas DRAFT creadas antes del corte (metadata huérfana cuyo flujo de subida
     * nunca se completó ni se canceló). Devuelve la cantidad de filas borradas.
     */
    Mono<Long> deleteDraftsCreatedBefore(Instant cutoff);
}
