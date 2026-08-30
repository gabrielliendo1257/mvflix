package com.gcorp.service.app.mvflix_movies.catalog.domain.movie;

public class CatalogItemConflictException extends RuntimeException {

    public CatalogItemConflictException(String message) {
        super(message);
    }
}
