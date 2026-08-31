# Distributed E2E

This stack runs Movies, Storage, Kafka, PostgreSQL, MinIO, BFF, and a WireMock
OIDC stub as separate containers. Add Media uses the deliberately bounded
`users-policy-stub` for `/api/v1/users/me` and
`/api/v1/users/{username}/policy`; it is not a replacement for the Users
service in production. The runner is a black-box Maven test and does not start
Spring application contexts in its JVM.

## Run

From the repository root:

```bash
./scripts/e2e.sh
```

The script starts the stack with `docker compose up --wait --build`, runs the
runner with the repository Maven wrapper, and always removes the stack with a
cleanup trap. To run against an already-started stack, invoke
`./mvnw -f e2e/runner/pom.xml test` directly.

The managed-deletion test creates a user and upload through the public APIs,
while the Add Media tests start through BFF, upload four real bytes to MinIO,
complete the ingestion, verify replay idempotency, reject a same-key payload
conflict, and restart Ingestion after completion has entered its finalization
path.

The RSA key in `oidc-stub/test-private-key.pem` is test-only material used by
the runner and the static Movies M2M token mapping. It must never be reused by
any deployed authorization service.

The default host ports are Movies `14040`, Storage `16060`, PostgreSQL `15432`,
MinIO `19000`, and JWKS `18080`. Override `MOVIES_URL`, `STORAGE_URL`, and
`USERS_URL` when running the runner against an already-started stack.

All published ports bind to `127.0.0.1` by default. For a runner in Termux or
another host on the LAN, opt in explicitly with
`E2E_BIND_ADDRESS=0.0.0.0 ./scripts/e2e.sh`.
