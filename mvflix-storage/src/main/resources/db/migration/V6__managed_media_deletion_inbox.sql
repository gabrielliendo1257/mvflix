CREATE TABLE managed_media_deletion_inbox (
    event_id UUID PRIMARY KEY,
    status VARCHAR(32) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    last_error TEXT
);

CREATE INDEX idx_managed_media_deletion_inbox_status
ON managed_media_deletion_inbox(status);
