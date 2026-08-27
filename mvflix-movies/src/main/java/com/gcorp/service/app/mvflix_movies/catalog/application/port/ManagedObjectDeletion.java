package com.gcorp.service.app.mvflix_movies.catalog.application.port;

import reactor.core.publisher.Mono;

/**
 * Capacidad de borrado de objetos MANAGED en Storage (M2M {@code movies-catalog}).
 * Movies declara la intención sin conocer WebClient ni la infraestructura de
 * Storage: el adapter traduce HTTP a errores de aplicación y la idempotencia
 * (404 = ya ausente) queda aquí, no en el orquestador.
 */
public interface ManagedObjectDeletion {

    /**
     * Borra el objeto referenciado. Idempotente: si ya no existe responde éxito.
     *
     * @throws ManagedObjectDeletionInconsistentException si owner/objectKey no
     *     coinciden (asociación corrupta) o el cliente carece de scope.
     * @throws ManagedObjectDeletionUnavailableException si storage no está
     *     alcanzable (reintentable).
     */
    Mono<Void> delete(ManagedObjectReference reference);
}
