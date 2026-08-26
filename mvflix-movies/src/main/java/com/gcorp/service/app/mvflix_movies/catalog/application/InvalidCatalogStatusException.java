package com.gcorp.service.app.mvflix_movies.catalog.application;

/** Filtro de estado fuera del vocabulario operacional del catálogo. HTTP 400. */
public class InvalidCatalogStatusException extends RuntimeException {

    public InvalidCatalogStatusException(String value) {
        super("Unknown catalog status filter: " + value);
    }
}
