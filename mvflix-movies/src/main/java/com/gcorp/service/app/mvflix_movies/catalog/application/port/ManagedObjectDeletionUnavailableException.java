package com.gcorp.service.app.mvflix_movies.catalog.application.port;

/**
 * Storage no está alcanzable para el borrado (conexión, timeout, 5xx).
 * Reintentable: el orquestador puede reintentar la finalización más tarde.
 */
public class ManagedObjectDeletionUnavailableException extends RuntimeException {

    public ManagedObjectDeletionUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
