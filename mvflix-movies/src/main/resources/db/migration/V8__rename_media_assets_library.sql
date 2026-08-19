-- ============================================================
-- mvflix_movies: alineación del lenguaje de dominio.
-- El activo de biblioteca apunta a la biblioteca local (library_id),
-- no al objeto subido del flujo upload (que sí se llama storage_id).
-- ============================================================

ALTER TABLE media_assets RENAME COLUMN storage_id TO library_id;
ALTER INDEX idx_media_assets_storage RENAME TO idx_media_assets_library;
ALTER TABLE media_assets RENAME CONSTRAINT media_assets_storage_id_relative_path_key
    TO media_assets_library_id_relative_path_key;