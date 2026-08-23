package com.gcorp.service.app.mvflix_movies.catalog.domain.movie;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

public interface MovieRepository {

    Mono<Movie> save(Movie movie);

    /**
     * Alta atómica del borrador identificado: INSERT de la película junto a su
     * acceso inicial (visibilidad + compartidos) como una sola unidad. La
     * decisión de negocio la tomó el agregado; acá solo se garantiza que no
     * exista un instante con película visible pero sin sus shares.
     */
    Mono<Movie> saveDraftWithAccess(Movie movie);

    Mono<Movie> findById(MovieId id);

    /**
     * Catalogo visible para el usuario (PUBLIC + propias + compartidas).
     * Traducción SQL de {@link Movie#isVisibleTo(String)}: la regla de negocio
     * vive en el dominio; acá solo se filtra para no traer todo a memoria.
     */
    Flux<Movie> findVisibleMovies(String username, int limit);

    Flux<Movie> findByOwner(String ownerUsername, int limit);

    /**
     * Películas del dueño por lista de ids (para cambios en lote).
     * Las ajenas no aparecen: la política de autoría se resuelve por consulta.
     */
    Flux<Movie> findByOwnerAndIds(String ownerUsername, List<MovieId> ids);

    /**
     * Compare-and-set: pasa a READY solo si la película sigue en DRAFT.
     * Vacío si no hubo fila (no existe o ya no está en DRAFT).
     * La autoría la decide el use-case con {@link Movie#isOwnedBy(String)}.
     * El media (object_id/object_key) lo persiste {@code MediaRepository} en el use-case.
     */
    Mono<Movie> completeIfDraft(MovieId id);

    /** {@code true} si borró la fila. */
    Mono<Boolean> deleteById(MovieId id);

    /** Persiste la transición de proveedor ya decidida por el agregado. */
    Mono<Movie> updateEnrichment(Movie movie);

    /** Persiste conjuntamente metadata, clasificación y estado de enriquecimiento. */
    Mono<Movie> updateDetails(Movie movie);

    /** Persiste la visibilidad ya decidida por el agregado. */
    Mono<Movie> updateVisibility(Movie movie);

    /** Persiste la lista de compartidos ya decidida por el agregado. */
    Mono<Movie> replaceShares(Movie movie);

    /** Para SHARED, persiste visibilidad y compartidos como una unidad transaccional. */
    Mono<Movie> updateAccess(Movie movie);

    /** Catalogo pendiente de enriquecer (para el scheduler), limitado y estable. */
    Flux<Movie> findByEnrichmentStatus(EnrichmentStatus enrichmentStatus, int limit);

    /**
     * Purga películas DRAFT creadas antes del corte (metadata huérfana cuyo flujo de subida
     * nunca se completó ni se canceló). Devuelve la cantidad de filas borradas.
     */
    Mono<Long> deleteDraftsCreatedBefore(Instant cutoff);
}
