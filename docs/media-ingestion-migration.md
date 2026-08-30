# Media ingestion

`mvflix-media-ingestion` is the gradual orchestration boundary for the new BFF facade. The existing BFF `add_media_processes` and schedulers remain active; routing is not changed by this migration. The future facade should forward its stable draft/file command and `Idempotency-Key` to this service, then expose the returned `ingestionId` and presigned URL.

Movies has no idempotency-key support in its current HTTP API, so `create-catalog-draft` and `complete-catalog` are protected only by the local durable state/CAS. Storage receives `{ingestionId}:prepare-upload` and already persists that key. `UploadCompleted.v1` is correlated by envelope `correlationId` when it is an ingestion UUID; legacy messages without it can be correlated only if Storage adds `payload.uploadId`. A storage event containing only `storageId` cannot be safely mapped and is ignored for reconciliation.

Downstream failures are deliberately propagated to Kafka/HTTP callers rather than converted to empty publishers. Recovery scheduling and a durable publisher are the next operational increment; `FINALIZING_CATALOG` must be reconciled manually and marked failed if Movies cannot be proven idempotent. No Search, Users, quota, renditions or FFmpeg calls are made.
