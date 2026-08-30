package com.gcorp.service.app.mvflix_movies.catalog.application;

public class IdempotencyKeyReusedException extends RuntimeException {
    public IdempotencyKeyReusedException(String key) {
        super("Idempotency-Key reused with a different request: " + key);
    }
}
