# Distributed managed-deletion E2E

This stack runs Movies, Storage, Kafka, PostgreSQL, MinIO, and a WireMock OIDC
stub as separate containers. The runner is a black-box Maven test and does not
start Spring application contexts in its JVM.

## Run

From the repository root:

```bash
docker compose -f e2e/docker-compose-e2e.yml up -d --build
mvn -f e2e/runner/pom.xml test
docker compose -f e2e/docker-compose-e2e.yml down -v
```

The test creates a user and upload through the public APIs, uploads four real
bytes to MinIO using the presigned URL, completes the upload, creates and
completes a movie, sends DELETE twice, and polls until the movie and managed
object are gone. It also checks the Storage inbox and database state.

The RSA key in `oidc-stub/test-private-key.pem` is test-only material used by
the runner and the static Movies M2M token mapping. It must never be reused by
any deployed authorization service.

The default host ports are Movies `14040`, Storage `16060`, PostgreSQL `15432`,
MinIO `19000`, and JWKS `18080`. Override `MOVIES_URL` and `STORAGE_URL` when
running the runner against an already-started stack.
