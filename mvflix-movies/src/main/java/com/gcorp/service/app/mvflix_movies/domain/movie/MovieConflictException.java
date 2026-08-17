package com.gcorp.service.app.mvflix_movies.domain.movie;

public class MovieConflictException extends RuntimeException {

    public MovieConflictException(String message) {
        super(message);
    }
}
