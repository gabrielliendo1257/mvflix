-- ============================================================
-- mvflix_storage: bibliotecas del media server (operator-owned).
-- LOCAL  -> root es un directorio del filesystem del operador
-- MANAGED-> root es un bucket/prefix S3-compatible (reservado)
-- ============================================================

CREATE TABLE media_libraries (
    id         BIGSERIAL PRIMARY KEY,
    type       VARCHAR(16) NOT NULL,
    root_path  VARCHAR(1024) NOT NULL UNIQUE,
    enabled    BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);