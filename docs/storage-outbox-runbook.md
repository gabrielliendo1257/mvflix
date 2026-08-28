# Storage Outbox Runbook

## Health

Inspect the Storage service metrics:

- `mvflix_outbox_pending{service="storage"}`: unpublished events below `maxAttempts`.
- `mvflix_outbox_exhausted{service="storage"}`: unpublished events at or above `maxAttempts`.
- `mvflix_outbox_oldest_age_seconds{service="storage"}`: age of the oldest unpublished event.

An exhausted event is quarantined and is not retried automatically.

## Reactivation

After confirming the cause has been fixed, an operator with `ROLE_ADMIN` may start a new delivery cycle:

```text
POST /admin/outbox/stored-object-deleted/reactivate
```

The operation resets only unpublished exhausted rows. Published rows are never changed. Check the exhausted metric and publisher logs after reactivation.
