CREATE TABLE add_media_compensations (
  id BIGSERIAL PRIMARY KEY,
  process_id VARCHAR(100) NOT NULL,
  kind VARCHAR(32) NOT NULL CHECK (kind IN ('DISCARD_DRAFT','CANCEL_UPLOAD')),
  resource_id BIGINT NOT NULL CHECK (resource_id > 0),
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','COMPLETED')),
  attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
  next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_error TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_add_media_compensation UNIQUE (process_id, kind, resource_id)
);
CREATE INDEX ix_add_media_compensation_pending
  ON add_media_compensations (status, next_attempt_at);
