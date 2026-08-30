# Media ingestion

The ingestion aggregate is persisted with optimistic CAS. Kafka input is recorded in
`media_ingestion_inbox` and output is written to `media_ingestion_outbox`; neither
side relies on an in-memory acknowledgement.

`FINALIZING_CATALOG` failures become `RECONCILIATION_REQUIRED` because the current
downstream contract has no status/reconcile operation. `DISCARD_DRAFT` is recorded
as a durable compensation, but remains retryable and observable as failed until
Movies exposes that operation.
