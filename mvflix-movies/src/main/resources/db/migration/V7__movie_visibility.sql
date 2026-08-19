ALTER TABLE movies ADD COLUMN visibility VARCHAR(16) NOT NULL DEFAULT 'PRIVATE';

CREATE TABLE movie_shares (
    movie_id BIGINT NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
    shared_with VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (movie_id, shared_with)
);

CREATE INDEX idx_movie_shares_shared_with ON movie_shares(shared_with);