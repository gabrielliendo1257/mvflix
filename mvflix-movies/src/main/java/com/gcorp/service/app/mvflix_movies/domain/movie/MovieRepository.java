package com.gcorp.service.app.mvflix_movies.domain.movie;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

public interface MovieRepository {

    Mono<Movie> save(Movie movie);

    Mono<Movie> findById(MovieId id);

    /**
     * Catalogo visible para el usuario (PUBLIC + propias + compartidas).
     * Traducción SQL de {@link Movie#isVisibleTo(String)}: la regla de negocio
     * vive en el dominio; acá solo se filtra para no traer todo a memoria.
     */
    Flux<Movie> findVisibleMovies(String username, int limit);

    Flux<Movie> findByOwner(String ownerUsername, int limit);

    /**
     * Compare-and-set: pasa a READY solo si la película sigue en DRAFT.
     * Vacío si no hubo fila (no existe o ya no está en DRAFT).
     * La autoría la decide el use-case con {@link Movie#isOwnedBy(String)}.
     * El media (object_id/object_key) lo persiste {@code MediaRepository} en el use-case.
     */
    Mono<Movie> completeIfDraft(MovieId id);

    /** {@code true} si borró la fila. */
    Mono<Boolean> deleteById(MovieId id);

    /** Aplica metadata enriquecida + estado de enriquecimiento (idempotente). */
    Mono<Movie> updateEnrichment(MovieId id, MovieMetadata metadata,
            EnrichmentStatus enrichmentStatus);

    /** Cambia la visibilidad del catálogo. Vacío si la fila no existe. */
    Mono<Movie> updateVisibility(MovieId id, MovieVisibility visibility);

    /**
     * Reemplaza la lista de compartidos. Vacío si la movie no existe.
     */
    Mono<Movie> replaceShares(MovieId id, List<String> usernames);

    /** Catalogo pendiente de enriquecer (para el scheduler), limitado y estable. */
    Flux<Movie> findByEnrichmentStatus(EnrichmentStatus enrichmentStatus, int limit);

    /**
     * Purga películas DRAFT creadas antes del corte (metadata huérfana cuyo flujo de subida
     * nunca se completó ni se canceló). Devuelve la cantidad de filas borradas.
     */
    Mono<Long> deleteDraftsCreatedBefore(Instant cutoff);
}