# mvflix

Plataforma de streaming/video por microservicios (Java 17, WebFlux). Modelo hexagonal en cada servicio, comunicacion reactiva y seguridad por JWT (OAuth2 Authorization Server).

## Modulos

| Modulo | Puerto | Rol |
|---|---|---|
| `mvflix-authorization` | 9090 | Authorization Server OAuth2/OIDC: emite JWT, expone `/oauth2/jwks` |
| `mvflix-users` | 8080 | Identidad, plan y politica de cuota de los usuarios |
| `mvflix-storage` | 6060 | Uploads, streaming, uso real del almacenamiento (MinIO) |
| `bff-mvflix-web` | - | Punto de entrada web (esqueleto) |

## Decisiones de dominio

- `users` es la **politica**: identidad, plan y cuota derivada del plan.
- `storage` es la **fuente de verdad del uso**: reserva/libera bytes de forma atomica.
- Ver `docs/adr/0001-*`.

## Arquitectura

```
auth (9090) --JWT--> users (8080) <--quota contract--> storage (6060/8080 API dev) --> MinIO
```

- `users` valida el JWT del `authorization` (jwks) y expone `POST /api/v1/users/quota` que el storage consume con `client_credentials` (scope `users.write`).
- `storage` se integra con users via WebClient + OAuth2 (sin Feign).
- Flyway corre con usuario `db_migrator` (JDBC, solo en arranque); el negocio es 100% R2DBC con `db_rw`.

## Requisitos

- Docker (Postgres + MinIO)
- Java 17+, Maven (wrapper incluido)

## Puesta en marcha

```bash
# Infra (postgres, minio)
make up-dev

# Perfil sandbox: storage sin authorization-service
make sandbox-run

# Tests
make sandbox-test
mvn test -pl mvflix-users      # tests de integracion con Testcontainers
mvn test -pl mvflix-storage

# Apps (por modulo)
mvn -pl mvflix-authorization spring-boot:run
mvn -pl mvflix-users spring-boot:run
```

## API

- `docs/openapi/users.openapi.yaml` — endpoints del user-service
- `docs/openapi/storage.openapi.yaml` — endpoints del storage
- `docs/openapi/authorization.openapi.yaml` — Authorization Server

## Docs

- `docs/adr/` — historial de decisiones (ADR 0001: cuota = politica en users, uso = storage)