CREATE TABLE activity_inbox (
  event_id UUID PRIMARY KEY, event_type VARCHAR(150) NOT NULL,
  status VARCHAR(16) NOT NULL CHECK (status IN ('RECEIVED','COMPLETED','FAILED')),
  received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), completed_at TIMESTAMPTZ,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), last_error TEXT
);
