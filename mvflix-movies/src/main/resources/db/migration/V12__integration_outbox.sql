CREATE TABLE outbox_events (
    event_id       UUID PRIMARY KEY,
    event_type     VARCHAR(120) NOT NULL,
    event_version  INTEGER NOT NULL,
    aggregate_type VARCHAR(120) NOT NULL,
    aggregate_id   VARCHAR(120) NOT NULL,
    occurred_at    TIMESTAMPTZ NOT NULL,
    payload        JSONB NOT NULL,
    published_at   TIMESTAMPTZ NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_outbox_events_pending
    ON outbox_events (created_at, event_id)
    WHERE published_at IS NULL;
