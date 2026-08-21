-- ============================================================
-- mvflix_movies: separar la presencia en disco del estado de
-- identificación. MISSING deja de ser un valor de status y pasa a
-- ser una dimensión propia (present boolean).
-- ============================================================

ALTER TABLE media_assets ADD COLUMN present BOOLEAN NOT NULL DEFAULT true;

UPDATE media_assets SET present = false WHERE status = 'MISSING';

UPDATE media_assets
   SET status = CASE WHEN movie_id IS NULL THEN 'UNIDENTIFIED' ELSE 'IDENTIFIED' END
 WHERE status = 'MISSING';

CREATE INDEX idx_media_assets_present ON media_assets (present);
