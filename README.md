# mvflix

Plataforma de streaming/video por microservicios (Java 17, WebFlux). Modelo hexagonal en cada servicio, comunicacion reactiva y seguridad por JWT (OAuth2 Authorization Server).

## Modulos

| Modulo | Puerto | Rol |
|---|---|---|
| `mvflix-authorization` | 9090 | Authorization Server OAuth2/OIDC: emite JWT, expone `/oauth2/jwks` |
| `mvflix-users` | 8080 | Identidad, plan y politica de cuota de los usuarios |
| `mvflix-storage` | 6060 | Uploads, streaming, uso real del almacenamiento (MinIO) |
| `bff-mvflix-web` | 9091 | Punto de entrada web (login OAuth2, sesion httpOnly) |

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

## Puesta en marcha (entorno dev)

Todas las aplicaciones leen su configuracion del archivo de perfil
(`application-dev.y(a)ml` en los 4 servicios; `mvflix-storage` ademas tiene
`application-sandbox.yml`), por lo que **siempre hay que activar el perfil `dev`**
al arrancarlas con Maven.

### 1. Infraestructura (postgres + MinIO)

```bash
make up-dev
```

Levanta postgres (crea `mvflix_users_db`, `mvflix_uploads_db`, `mvflix_authorized_db`, ...)
y MinIO con el bucket raiz y el webhook hacia el storage en `:6060`.
Requiere `infra/docker/.env` (gitignored; hay una copia con valores dev).

### 2. Aplicaciones (una terminal por servicio, en este orden)

```bash
# Authorization Server (IdP OAuth2/OIDC)  -> http://localhost:9090
./mvnw -pl mvflix-authorization spring-boot:run -Dspring-boot.run.profiles=dev

# users (identidad + cuota)               -> http://localhost:8080
./mvnw -pl mvflix-users spring-boot:run -Dspring-boot.run.profiles=dev

# storage (uploads + streaming)           -> http://localhost:6060
./mvnw -pl mvflix-storage  spring-boot:run -Dspring-boot.run.profiles=dev

# BFF web (login OAuth2 en el navegador)  -> http://localhost:9091
./mvnw -pl bff-mvflix-web spring-boot:run -Dspring-boot.run.profiles=dev
```

Equivalent with env var: `SPRING_PROFILES_ACTIVE=dev ./mvnw -pl mvflix-storage spring-boot:run`.

### 3. Probar el flujo login completo (BFF + OAuth2)

1. `GET http://localhost:9091/web/session` -> devuelve `{"authenticated":false}` (o 401 JSON si no hay sesion).
2. Abrir `http://localhost:9091/web/home` en el navegador: redirige al IdP (`localhost:9090/login`),
   autentica con un usuario real de `customers`, vuelve al BFF y queda la cookie de sesion.
3. El cliente OAuth2 `movie-bff` debe existir en `mvflix_authorized_db` con scopes
   `openid,profile,users.read,users.write` y PKCE obligatorio (seed automatico en el arranque,
   en una DB ya existente hay que crearlo/responder a mano).

### Perfiles disponibles

| Perfil | Se usa para | Requiere |
|---|---|---|
| `dev` | Flujo completo: todos los servicios + auth + MinIO + Users | postgres + minio (`make up-dev`), idP en 9090 |
| `sandbox` | Solo `mvflix-storage` aislado (sin authorization ni users) | postgres + minio; `make sandbox-run` |

### Tests

```bash
make sandbox-test                      # smoke test del storage (perfil sandbox)
./mvnw test                            # suite completa por modulo
./mvnw test -pl mvflix-users           # integracion con Testcontainers
```

### Variables de entorno utiles (todas con default dev)

`AUTHORIZATION_ISSUER_URL` (9090), `SERVICES_USERS_URL` (8080), `SERVICES_STORAGE_URL` (6060),
`MINIO_URL` (9000), `BFF_ADDRESS` (http://127.0.0.1:9091, redirect del cliente OAuth2).

## API

- `docs/openapi/users.openapi.yaml` — endpoints del user-service
- `docs/openapi/storage.openapi.yaml` — endpoints del storage
- `docs/openapi/authorization.openapi.yaml` — Authorization Server

## Docs

- `docs/adr/` — historial de decisiones (ADR 0001: cuota = politica en users, uso = storage)