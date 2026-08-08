# ADR 0001: La cuota es politica de user-service; el uso real vive en storage-service

**Estado:** Aceptado
**Fecha:** 2026-08-08
**Ámbito:** `mvflix-users`, `mvflix-storage`

## Contexto

Históricamente el dominio de `mvflix-users` mantenía el contador de uso del
usuario (`storageUsed`) y su límite (`storageQuota`) en la misma entidad
(`User`), con un flujo de "reservas" (`StorageReservations`) que pretendía
validar cuota contra uso en cada operación.

Este diseno tenía tres problemas:

1. **Doble fuente de verdad**: tanto `mvflix-users` como `mvflix-storage`
   persistían versiones del uso de almacenamiento. Much nos se sincronizaban y
   nunca se sabia cuál era fiable (se ató: `mvflix-users` guardaba el "uso"
   que le avisaba storage por WebClient, sin transacción que lo garantizara).
2. **Un acoplamiento invertido**: para saber si un usuario podía consumir más,
   storage preguntaba a users por el *uso*, que users no era capaz de
   calcular (no es quien reserva/libera bytes).
3. **Duplicacion de reglas**: la politica de límite (plan → bytes) estaba
   calculada en dos dominios con constantes diferentes (`PRO_MAX_UPLOAD_SIZE`
   divergente, `ENTERPRISE` menor que `PRO`), lo que producia bugs silenciosos.

## Decision

- **`mvflix-users` = política.** Solo identidad, `plan`, `enabled`. El límite
  de almacenamiento se **deriva del plan** (`StorageQuota.getQuota(Plan)`) y
  se expone de forma inmutable.
- **`mvflix-storage` = uso real (fuente de verdad transaccional).** Es quien
  reserva, libera y registra el consumo de bytes de forma atomica y con
  optimistic lock (transición `PENDING → COMPLETED` idempotente), y quien
  reporta `usedBytes`.
- La comunicacion quedó: storage **notifica a users una cuota propuesta** y
  users **valida** contra la política `POST /api/v1/users/quota` → `204`/`409`;
  storage persiste su `UserStorage` con la cuota asignada.

## Decisión

1. Eliminar de `User` `storageUsed`/`storageQuota`/`consumeStorage`/
   `releaseStorage`/`canUpload` y todo el flujo de reservas
   (`StorageReservations`, `ReservationStatus`, `ReservationStorageRepository`
   y sus adaptadores).
2. Guardar solo la identidad y el plan en `users`; la cuota se deriva del
   plan (única constante `KV` en `StorageQuota`).
3. La columna `storage_used`/`storage_quota` de la tabla `users` se retiró de
   la migración V1 (nunca se aplicó en prod).
4. Contracto entre servicios: `POST /api/v1/users/quota?subject=&quota=`
   (con `scope=SCOPE_users.write` del storage):
   - 204 si la cuota propuesta cabe en el plan
   - 409 `ExceededQuotaException` si la excede
   - 404 si el usuario no existe

## Consecuencias

**Positivas**
- Una única fuente de verdad por concepto (policy en users, uso en storage).
- El dominio de users ya no necesita conocer el estado transaccional del
  bucket; las reglas de límite viven en un solo lugar (derivado del plan).
- El acoplamiento "storage → users / validacion de política" es uniforme y el
  contrato se teste (users y storage).
- El storage puede evolucionar (multi-bucket, percent) sin cambios en users.

**Negativas / riesgos**
- Para mostrar la cuota Y el uso en un mismo response, un cliente necesita
  componer dos llamadas (users por la cuota, storage por el uso); se sugiere
  dejarlo en el BFF (`mvflix-web`).
- Cambiar de plan requiere reprovisionar la cuota en storage vía el contrato
  /quota (aún no existe un `PATCH /users/{id}/plan`; se añadirá cuando el
  flujo de planes lo requiera).
- Los tests R2DBC de users deben levant Foto postgres real (Testcontainers);
  sin el contrator, `applyQuota` no puede validar.

## Alternativas consideradas

- **Hacer de users la fuente de verdad del uso**: desechado porque users no
  realiza ninguna operación de archivo ni reserva; duplicaba estado sin
  transacción y obligaba al storage a llamar users en cada mutación (latencia
  y acoplamiento en el hot path del upload).
- **Centralizar todo en storage (users solo identidad)**: desechado porque la
  política de plan (qué puede subir cada usuario) es negocio de usuarios y
  autorización; al final el componente `authorization` conoce la identidad.