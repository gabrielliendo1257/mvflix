-- ============================================================
-- mvflix_movies: tipo de contenido del catálogo.
-- "Movie" es un tipo, no la estructura: OTHER cubre media que no
-- representa una película (y el enum crecerá: SERIES, CLIP, ...).
-- ============================================================

ALTER TABLE movies ADD COLUMN kind VARCHAR(32) NOT NULL DEFAULT 'MOVIE';

CREATE INDEX idx_movies_kind ON movies (kind);
