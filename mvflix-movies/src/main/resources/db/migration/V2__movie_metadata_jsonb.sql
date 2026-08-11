-- ============================================================
-- mvflix_movies: metadata de peliculas como documento JSONB
-- ============================================================

ALTER TABLE movies
    DROP COLUMN IF EXISTS synopsis,
    DROP COLUMN IF EXISTS genre,
    DROP COLUMN IF EXISTS release_year,
    DROP COLUMN IF EXISTS content_type,
    DROP COLUMN IF EXISTS content_length,
    DROP COLUMN IF EXISTS checksum;

ALTER TABLE movies
    ADD COLUMN metadata JSONB NOT NULL DEFAULT '{}'::jsonb;