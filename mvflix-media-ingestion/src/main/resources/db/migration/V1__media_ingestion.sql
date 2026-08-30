CREATE TABLE media_ingestions (
  ingestion_id UUID PRIMARY KEY, actor_id VARCHAR(255) NOT NULL, catalog_item_id BIGINT, upload_id VARCHAR(255), upload_url TEXT,
 phase VARCHAR(40) NOT NULL, failure_code VARCHAR(120), version BIGINT NOT NULL, retry_count INTEGER NOT NULL DEFAULT 0,
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, next_attempt_at TIMESTAMPTZ NOT NULL,
 idempotency_key VARCHAR(255) NOT NULL, file_name VARCHAR(1024) NOT NULL, file_size BIGINT NOT NULL, mime_type VARCHAR(255) NOT NULL,
 correlation_id UUID NOT NULL UNIQUE, UNIQUE(actor_id,idempotency_key)
);
CREATE INDEX media_ingestions_upload_idx ON media_ingestions(upload_id);
CREATE TABLE media_ingestion_outbox (event_id UUID PRIMARY KEY, event_type VARCHAR(100) NOT NULL, aggregate_id UUID NOT NULL, payload JSONB NOT NULL, published_at TIMESTAMPTZ);
CREATE TABLE media_ingestion_inbox (event_id UUID PRIMARY KEY, event_type VARCHAR(100) NOT NULL, received_at TIMESTAMPTZ NOT NULL DEFAULT now(), completed_at TIMESTAMPTZ);
