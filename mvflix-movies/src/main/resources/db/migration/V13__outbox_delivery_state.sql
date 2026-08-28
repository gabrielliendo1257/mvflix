ALTER TABLE outbox_events
    ADD COLUMN attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN locked_until TIMESTAMPTZ NULL,
    ADD COLUMN last_error TEXT NULL;

CREATE INDEX idx_outbox_events_claimable
    ON outbox_events (next_attempt_at, created_at, event_id)
    WHERE published_at IS NULL;
