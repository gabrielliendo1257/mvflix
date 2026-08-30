-- ============================================================
-- mvflix_movies: migracion de nombres de persistencia del catalogo
-- ============================================================

ALTER TABLE movies RENAME TO catalog_items;

ALTER TABLE media RENAME COLUMN movie_id TO catalog_item_id;
ALTER TABLE media_assets RENAME COLUMN movie_id TO catalog_item_id;
ALTER TABLE movie_shares RENAME COLUMN movie_id TO catalog_item_id;

-- Los renombres de columnas conservan constraints, claves y sus indices.
ALTER INDEX idx_movies_owner_username RENAME TO idx_catalog_items_owner_username;
ALTER INDEX idx_movies_status RENAME TO idx_catalog_items_status;
ALTER INDEX idx_movies_kind RENAME TO idx_catalog_items_kind;
ALTER INDEX idx_movies_deletion_recovery RENAME TO idx_catalog_items_deletion_recovery;
ALTER INDEX idx_media_movie_id RENAME TO idx_media_catalog_item_id;
