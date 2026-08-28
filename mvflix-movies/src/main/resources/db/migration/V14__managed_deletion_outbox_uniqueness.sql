CREATE UNIQUE INDEX ux_outbox_managed_deletion_aggregate
    ON outbox_events (event_type, aggregate_id)
    WHERE event_type = 'ManagedMediaDeletionRequested';
