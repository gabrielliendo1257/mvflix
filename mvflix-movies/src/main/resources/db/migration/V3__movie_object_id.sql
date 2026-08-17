-- ============================================================
-- mvflix_movies: id del objeto en storage para streaming
-- (la key del objeto NUNCA se expone al front; solo el object_id)
-- ============================================================

ALTER TABLE movies
    ADD COLUMN object_id BIGINT;