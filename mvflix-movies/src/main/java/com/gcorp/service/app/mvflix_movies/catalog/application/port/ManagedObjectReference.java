package com.gcorp.service.app.mvflix_movies.catalog.application.port;

/**
 * Referencia a un objeto MANAGED en Storage: el id (público, equivale a
 * {@code CatalogItem.objectId}), el owner esperado y la key interna (de la tabla
 * {@code media}, nunca en {@code CatalogItem}). El guard owner+objectKey del lado de
 * Storage protege contra borrar el objeto equivocado.
 */
public record ManagedObjectReference(
        long storageId,
        String ownerUsername,
        String objectKey) {}
