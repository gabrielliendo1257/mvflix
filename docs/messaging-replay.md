# Messaging Retry and DLT Operations

The managed deletion consumers use four delivery attempts: the original topic
and three retry topics. Delays are exponential (`1s`, `2s`, `4s`) and capped at
`10s`. After the fourth failure the record is routed to the `.DLT` topic.

## Topics

- `mvflix.managed-media-deletion-requested.v1` and its `.retry-0`, `.retry-1`, `.retry-2`, `.DLT` topics are consumed by Storage.
- `mvflix.stored-object-deleted.v1` and its `.retry-0`, `.retry-1`, `.retry-2`, `.DLT` topics are consumed by Movies.

## Replay Procedure

1. Inspect the DLT record and its exception headers. Confirm the root cause is fixed and that replaying the event is safe.
2. Preserve the original JSON envelope. Do not edit `eventId`; it is used for idempotency.
3. Produce the corrected envelope to the original base topic, not to a retry topic or the DLT. Use the original Kafka key (`storageId` for these events).
4. Verify the consumer log and database state. A successful replay should leave the DLT record untouched and produce the normal completion/state transition.
5. Record the DLT offset, event ID, root cause, fix, and replay operator in the incident log.

Example using the development broker:

```bash
kafka-console-producer.sh \
  --bootstrap-server 127.0.0.1:9094 \
  --topic mvflix.stored-object-deleted.v1 \
  --property parse.key=true \
  --property key.separator=$'\t'
```

Then enter `<storageId><TAB><original-json-envelope>` and finish with `Ctrl-D`.

Never reset a consumer group to replay an entire DLT without first reviewing
all records. Invalid schema, unknown event versions, and ownership mismatches
should be fixed or quarantined before replay; retrying them unchanged only
returns them to the DLT.
