# Distributed managed-deletion E2E

This stack runs Movies, Storage, Kafka, PostgreSQL, MinIO, and a WireMock OIDC
stub as separate containers. The runner is a black-box Maven test and does not
start Spring application contexts in its JVM.

## Run

From the repository root:

```bash
./scripts/e2e.sh
```

The script starts the stack with `docker compose up --wait --build`, runs the
runner with the repository Maven wrapper, and always removes the stack with a
cleanup trap. To run against an already-started stack, invoke
`./mvnw -f e2e/runner/pom.xml test` directly.

The test creates a user and upload through the public APIs, uploads four real
bytes to MinIO using the presigned URL, completes the upload, creates and
completes a movie, sends DELETE twice, and polls until the movie and managed
object are gone. It also checks the Storage inbox and database state.

The RSA key in `oidc-stub/test-private-key.pem` is test-only material used by
the runner and the static Movies M2M token mapping. It must never be reused by
any deployed authorization service.

The default host ports are Movies `14040`, Storage `16060`, PostgreSQL `15432`,
MinIO `19000`, and JWKS `18080`. Override `MOVIES_URL`, `STORAGE_URL`, and
`USERS_URL` when running the runner against an already-started stack.

All published ports bind to `127.0.0.1` by default. For a runner in Termux or
another host on the LAN, opt in explicitly with
`E2E_BIND_ADDRESS=0.0.0.0 ./scripts/e2e.sh`.
