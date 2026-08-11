-- ============================================================
-- mvflix_movies: catalogo de peliculas
-- ============================================================

CREATE TABLE movies (
    id             BIGSERIAL PRIMARY KEY,
    owner_username VARCHAR(255) NOT NULL,
    title          VARCHAR(255) NOT NULL,
    synopsis       TEXT,
    genre          VARCHAR(100),
    release_year   INT,
    object_key     VARCHAR(255) UNIQUE,
    content_type   VARCHAR(128),
    content_length BIGINT,
    checksum       VARCHAR(255),
    status         VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_movies_owner_username ON movies (owner_username);
CREATE INDEX idx_movies_status ON movies (status);