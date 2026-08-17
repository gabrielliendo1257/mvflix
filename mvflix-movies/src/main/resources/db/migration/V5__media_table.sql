-- ============================================================
-- mvflix_movies: el objeto de video sale de movies y pasa a media
-- (una pelicula podra tener N media: trailer, version principal, ...)
-- ============================================================

CREATE TABLE media (
    id         BIGSERIAL PRIMARY KEY,
    movie_id   BIGINT NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
    object_id  BIGINT NOT NULL,
    object_key VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_media_movie_id ON media (movie_id);

INSERT INTO media (movie_id, object_id, object_key, created_at)
SELECT id, object_id, object_key, updated_at
FROM movies
WHERE object_id IS NOT NULL;

ALTER TABLE movies
    DROP COLUMN IF EXISTS object_id,
    DROP COLUMN IF EXISTS object_key;