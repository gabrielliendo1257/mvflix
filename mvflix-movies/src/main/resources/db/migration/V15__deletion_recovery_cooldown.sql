ALTER TABLE movies
    ADD COLUMN last_recovery_attempt_at TIMESTAMPTZ NULL;

CREATE INDEX idx_movies_deletion_recovery
    ON movies (last_recovery_attempt_at, id)
    WHERE status = 'DELETING';
