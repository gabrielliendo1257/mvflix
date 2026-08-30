package com.gcorp.service.app.mvflix_movies.catalog.domain.item;

public class CatalogItemNotFoundException extends RuntimeException {
    public CatalogItemNotFoundException(String message) {
        super(message);
    }
}
