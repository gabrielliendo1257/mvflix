# Media ingestion

The ingestion aggregate is persisted with optimistic CAS. Kafka input is recorded in
`media_ingestion_inbox` and output is written to `media_ingestion_outbox`; neither
side relies on an in-memory acknowledgement.

Recovery runs every `${MVFLIX_RECOVERY_POLL_MS}` milliseconds. A PostgreSQL
`FOR UPDATE SKIP LOCKED` claim and `recovery_claimed_until` lease ensure that
multiple ingestion instances do not process the same row concurrently. It only
claims due `STARTING`, preparation, finalization, and reconciliation rows.

Finalization and reconciliation query Movies (`GET /api/v1/movies/{id}`) and
Storage (`GET /api/v1/movie/storage/upload/{uploadId}`) before taking action.
Completion is idempotent and writes the aggregate and completion outbox event in
one transaction. Unknown or unavailable state is rescheduled with exponential
backoff; recovery never compensates based only on an exception.

The current model does not persist the original draft, so interrupted starting
or preparation phases cannot be recreated safely and become durable `FAILED`
states. A pending upload may receive a cancellation only after Storage confirms
it is pending. Movies has no discard-draft endpoint, so draft cleanup remains a
manual reconciliation item; no fake discard call is made. `mvflix.recovery.enabled`
and `mvflix.compensation.enabled` can disable the respective workers.
