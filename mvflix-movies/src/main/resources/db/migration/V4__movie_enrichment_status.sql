-- ============================================================
-- mvflix_movies: estado de enriquecimiento ortogonal al de publicacion
-- (RAW = solo lo que creo el usuario; ENRICHED = metadatos externos aplicados)
-- ============================================================

ALTER TABLE movies
    ADD COLUMN enrichment_status VARCHAR(32) NOT NULL DEFAULT 'RAW';