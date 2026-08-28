CREATE INDEX idx_managed_media_deletion_inbox_completed
ON managed_media_deletion_inbox(completed_at)
WHERE status = 'COMPLETED';

CREATE INDEX idx_storage_outbox_published
ON storage_outbox_events(published_at)
WHERE published_at IS NOT NULL;
