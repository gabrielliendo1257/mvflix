package com.gcorp.service.app.mvflix_movies.domain.movie;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

public interface MovieRepository {

    Mono<Movie> save(Movie movie);

    Mono<Movie> findById(MovieId id);

    /**
     * Pelicula visible para el usuario: PUBLIC, propia o compartida con el.
     * Vacio si no existe o no es visible.
     */
    Mono<Movie> findByIdVisible(MovieId id, String username);

    /** Catalogo visible para el usuario (PUBLIC + propias + compartidas). */
    Flux<Movie> findVisibleMovies(String username, int limit);

    Flux<Movie> findByOwner(String ownerUsername, int limit);

    /**
     * Compare-and-set: pasa a READY solo si la película sigue en DRAFT y pertenece al dueño.
     * Vacío si no hubo fila (no existe, otro dueño o ya no está en DRAFT).
     * El media (object_id/object_key) lo persiste {@code MediaRepository} en el use-case.
     */
    Mono<Movie> completeIfDraft(MovieId id, String ownerUsername);

    /** {@code true} si borró la fila del dueño. */
    Mono<Boolean> deleteById(MovieId id, String ownerUsername);

    /** Aplica metadata enriquecida + estado de enriquecimiento (idempotente). */
    Mono<Movie> updateEnrichment(MovieId id, String ownerUsername, MovieMetadata metadata,
            EnrichmentStatus enrichmentStatus);

    /** Cambia la visibilidad (solo el dueño). Vacío si la fila no es del dueño. */
    Mono<Movie> updateVisibility(MovieId id, String ownerUsername, MovieVisibility visibility);

    /**
     * Reemplaza la lista de compartidos (solo el dueño). Vacío si la movie
     * no existe o no es del dueño.
     */
    Mono<Movie> replaceShares(MovieId id, String ownerUsername, List<String> usernames);

    /** Catalogo pendiente de enriquecer (para el scheduler), limitado y estable. */
    Flux<Movie> findByEnrichmentStatus(EnrichmentStatus enrichmentStatus, int limit);

    /**
     * Purga películas DRAFT creadas antes del corte (metadata huérfana cuyo flujo de subida
     * nunca se completó ni se canceló). Devuelve la cantidad de filas borradas.
     */
    Mono<Long> deleteDraftsCreatedBefore(Instant cutoff);
}