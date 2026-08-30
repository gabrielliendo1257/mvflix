CREATE TABLE add_media_processes (
  id VARCHAR(100) PRIMARY KEY,
  owner_subject VARCHAR(255) NOT NULL CHECK (btrim(owner_subject) <> ''),
  idempotency_key VARCHAR(255) NOT NULL CHECK (btrim(idempotency_key) <> ''),
  fingerprint VARCHAR(255) NOT NULL,
  movie_id BIGINT,
  upload_id BIGINT,
  phase VARCHAR(32) NOT NULL CHECK (phase IN ('STARTING','PREPARING','WAITING_FOR_UPLOAD','VERIFYING_UPLOAD','FINALIZING','READY','FAILED','CANCELLING','CANCELLED')),
  failure_code VARCHAR(128),
  version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_add_media_process_owner_key UNIQUE (owner_subject, idempotency_key),
  CONSTRAINT ck_add_media_process_ids CHECK ((movie_id IS NULL OR movie_id > 0) AND (upload_id IS NULL OR upload_id > 0))
);
CREATE INDEX ix_add_media_process_phase_updated ON add_media_processes (phase, updated_at);
CREATE INDEX ix_add_media_process_owner ON add_media_processes (owner_subject);
