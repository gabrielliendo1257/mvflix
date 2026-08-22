package com.gcorp.service.app.mvflix_movies.catalog.domain.movie;

public class MovieNotFoundException extends RuntimeException {
    public MovieNotFoundException(String message) {
        super(message);
    }
}
