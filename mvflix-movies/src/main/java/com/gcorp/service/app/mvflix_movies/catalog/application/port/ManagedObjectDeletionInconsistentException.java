package com.gcorp.service.app.mvflix_movies.catalog.application.port;

/**
 * La asociación movies↔storage está corrupta: el owner/objectKey del objeto no
 * coinciden con lo que el catálogo espera, o el cliente M2M carece del scope.
 * NO es reintentable: requiere reconciliación de datos o corrección de despliegue.
 */
public class ManagedObjectDeletionInconsistentException extends RuntimeException {

    public ManagedObjectDeletionInconsistentException(String message) {
        super(message);
    }
}
