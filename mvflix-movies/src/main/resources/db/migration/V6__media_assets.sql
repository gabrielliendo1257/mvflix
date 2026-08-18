-- ============================================================
-- mvflix_movies: activos de biblioteca del media server
-- (catalog entry del filesystem local: UNIDENTIFIED -> IDENTIFIED -> MISSING)
-- ============================================================

CREATE TABLE media_assets (
    id            BIGSERIAL PRIMARY KEY,
    storage_id    BIGINT NOT NULL,
    relative_path VARCHAR(1024) NOT NULL,
    size          BIGINT NOT NULL,
    mime_type     VARCHAR(128) NOT NULL,
    status        VARCHAR(32) NOT NULL DEFAULT 'UNIDENTIFIED',
    movie_id      BIGINT REFERENCES movies(id) ON DELETE SET NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (storage_id, relative_path)
);

CREATE INDEX idx_media_assets_storage ON media_assets (storage_id);
CREATE INDEX idx_media_assets_status ON media_assets (status);