CREATE TABLE movie_idempotency_keys (
    actor_id         VARCHAR(255) NOT NULL,
    operation        VARCHAR(100) NOT NULL,
    idempotency_key  VARCHAR(255) NOT NULL,
    request_hash     VARCHAR(64) NOT NULL,
    movie_id         BIGINT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (actor_id, operation, idempotency_key),
    CONSTRAINT fk_movie_idempotency_movie
        FOREIGN KEY (movie_id) REFERENCES catalog_items(id) ON DELETE CASCADE
);
