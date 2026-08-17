package com.gcorp.service.app.mvflix_movies.domain.movie;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface MovieRepository {

    Mono<Movie> save(Movie movie);

    Mono<Movie> findById(MovieId id);

    Flux<Movie> findByOwner(String ownerUsername, int limit);

    /**
     * Compare-and-set: pasa a READY solo si la película sigue en DRAFT y pertenece al dueño.
     * Vacío si no hubo fila (no existe, otro dueño o ya no está en DRAFT).
     * El media (object_id/object_key) lo persiste {@code MediaRepository} en el use-case.
     */
    Mono<Movie> completeIfDraft(MovieId id, String ownerUsername);

    /** {@code true} si borró la fila del dueño. */
    Mono<Boolean> deleteById(MovieId id, String ownerUsername);

    /**
     * Purga películas DRAFT creadas antes del corte (metadata huérfana cuyo flujo de subida
     * nunca se completó ni se canceló). Devuelve la cantidad de filas borradas.
     */
    Mono<Long> deleteDraftsCreatedBefore(Instant cutoff);
}