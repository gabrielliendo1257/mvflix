-- Tareas durables para limpiar objetos huérfanos en MinIO (blob sin fila o
-- borrado fallido). Una tarea se encola cuando un DELETE best-effort falla o
-- cuando se descarta una URL presignada con blob tardío; el scheduler la
-- reintenta y marca processed_at al conseguirlo.
CREATE TABLE orphan_cleanup_tasks (
    id            BIGSERIAL PRIMARY KEY,
    bucket        VARCHAR(255)  NOT NULL,
    object_key    VARCHAR(1024) NOT NULL,
    owner_username VARCHAR(255),
    reason        VARCHAR(64)   NOT NULL,
    attempts      INT           NOT NULL DEFAULT 0,
    last_error    VARCHAR(512),
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    processed_at  TIMESTAMPTZ,
    CONSTRAINT uq_orphan_pending UNIQUE (bucket, object_key, processed_at)
);
CREATE INDEX idx_orphan_pending ON orphan_cleanup_tasks (created_at)
    WHERE processed_at IS NULL;
