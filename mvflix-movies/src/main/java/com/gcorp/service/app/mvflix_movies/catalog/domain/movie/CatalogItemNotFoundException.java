package com.gcorp.service.app.mvflix_movies.catalog.domain.movie;

public class CatalogItemNotFoundException extends RuntimeException {
    public CatalogItemNotFoundException(String message) {
        super(message);
    }
}
