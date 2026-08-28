# ADR 0005: Estrategia de integración HTTP y eventos

Estado: Aceptado
Fecha: 2026-08-27
Ámbito: `bff-mvflix-web`, `mvflix-movies`, `mvflix-storage`, `mvflix-users`

## Contexto

Mvflix tiene colaboraciones entre bounded contexts con necesidades distintas:

- algunas operaciones necesitan una respuesta inmediata para completar una
  experiencia HTTP;
- otras son trabajo de fondo o notificaciones que no deben depender de que el
  consumidor esté disponible en ese instante;
- una operación puede escribir la base local y publicar información para otros
  servicios, pero no existe una transacción distribuida entre sus bases y
  Kafka.

El transporte no debe cambiar el ownership. En particular, el BFF no debe
decidir sobre el catálogo, los objetos físicos ni la cuota. Tampoco se debe
resolver la consistencia distribuida con llamadas HTTP encadenadas dentro de
una transacción local.

## Decisión

Mvflix usará una estrategia híbrida: HTTP para comandos síncronos y eventos
para integración asíncrona durable.

### HTTP

Se usa HTTP cuando el consumidor necesita una respuesta para terminar la
request actual, validar una decisión o devolver un resultado de negocio:

- el BFF expresa intenciones a Movies;
- Movies consulta o modifica su propio catálogo;
- un servicio solicita una operación puntual a otro servicio con un contrato
  explícito y timeout/retry controlados;
- la respuesta debe distinguir éxito, rechazo, conflicto o trabajo pendiente.

HTTP no debe usarse para mantener una transacción abierta mientras se espera a
otro servicio. Las llamadas remotas ocurren fuera de las transacciones locales.

### Eventos

Se usa Kafka para hechos de negocio que otros servicios pueden procesar después,
para trabajo de fondo y para desacoplar productores de consumidores:

- cambios de ciclo de vida del catálogo;
- finalización o fallo de procesos durables;
- cambios que requieren proyecciones o acciones independientes;
- notificaciones donde el productor no necesita esperar el resultado del
  consumidor.

Un evento describe un hecho ya ocurrido, no una orden disfrazada. Los comandos
que requieren una respuesta inmediata permanecen en HTTP.

### Outbox transaccional

Cada servicio que publica eventos escribe el cambio de estado y el evento en
su propia base de datos, dentro de la misma transacción local, en una tabla
`outbox`. Un relay independiente publica los registros pendientes en Kafka.

No se publican eventos directamente desde el código de dominio ni se hace un
dual-write `base de datos + Kafka` desde la request. El relay puede publicar
un mismo registro más de una vez; marcarlo como publicado es una optimización,
no una garantía de entrega única.

La outbox debe contener como mínimo:

- `event_id` único e inmutable;
- `event_type` y `event_version`;
- `aggregate_type` y `aggregate_id`;
- `occurred_at` y, si aplica, `causation_id`/`correlation_id`;
- payload versionado.

En Kafka, estos campos se transportan dentro del envelope JSON como
`eventId`, `eventType`, `eventVersion`, `aggregate` y los demás campos del
contrato AsyncAPI. No se exige duplicarlos como headers Kafka.

El estado durable `DELETING` y su scheduler siguen siendo una estrategia válida
para el borrado de media. Una futura publicación de eventos puede complementar
o reemplazar el disparo, pero no elimina la obligación de que el proceso sea
reanudable.

## Entrega at-least-once

La entrega de eventos será **at-least-once**. Los consumidores deben asumir
duplicados, reordenamiento entre agregados y reintentos después de un crash.

Cada consumidor debe:

1. identificar el evento por `event_id`;
2. aplicar el efecto de forma idempotente, usando CAS, claves únicas o una tabla
   de inbox/deduplicación cuando sea necesario;
3. confirmar el offset solo después de persistir el efecto local;
4. reintentar fallos transitorios y enviar a una DLQ los mensajes que requieren
   intervención o exceden el límite de reintentos.

El orden solo se garantiza dentro de una partición. Los eventos del mismo
 agregado deben usar su identificador como key de Kafka cuando el orden sea
 relevante. Ningún consumidor debe depender del orden global del topic.

## Idempotencia

La idempotencia se diseña en el caso de uso, no en el controller ni en el
broker:

- repetir una intención HTTP no debe duplicar efectos;
- repetir un evento no debe liberar cuota, borrar un objeto o crear una fila
  dos veces;
- los comandos de borrado deben tolerar que el recurso ya no exista;
- las transiciones de estado deben protegerse con condiciones atómicas, por
  ejemplo `READY -> DELETING` y `DELETE WHERE status = 'DELETING'`;
- las operaciones remotas deben aceptar reintentos sin depender de memoria
  local.

En el borrado MANAGED, Storage es idempotente sobre `storage_id` y Movies solo
finaliza una película que todavía está en `DELETING`. Una segunda instancia
puede observar el mismo pendiente sin convertir la liberación de cuota en una
segunda operación efectiva.

## Ownership y contratos

| Contexto | Propietario |
| --- | --- |
| BFF | Experiencias web e intención del usuario; no datos de dominio ni Storage |
| Movies | Películas, media, metadata, estado y autorización del catálogo |
| Storage | Objetos físicos, uploads, filesystem y uso/cuota efectiva |
| Users | Identidad, plan y política de cuota |
| Kafka/outbox | Transporte y entrega; no es fuente de verdad de negocio |

Cada consumidor define su puerto de aplicación y su adapter de infraestructura.
Los contratos de eventos se publican con nombre, versión y esquema explícitos;
no se comparten entidades de persistencia ni DTOs internos entre servicios.

En el borrado MANAGED, el contrato síncrono actual conserva este orden:

1. BFF solicita el borrado a Movies por HTTP.
2. Movies persiste `DELETING`.
3. Movies solicita a Storage la eliminación del objeto, o reanuda ese trabajo
   mediante el scheduler/evento.
4. Storage libera el objeto y la cuota de forma idempotente.
5. Movies elimina sus asociaciones y el registro del catálogo.

El BFF nunca llama directamente a Storage y no decide si la media es MANAGED o
LOCAL.

## Consecuencias

- HTTP mantiene contratos simples para operaciones interactivas y respuestas
  `204`/`202`.
- Kafka permite agregar consumidores sin acoplarlos al request del productor.
- La consistencia es eventual entre servicios; no se promete ACID distribuido.
- Outbox, relay, retries, DLQ y deduplicación son responsabilidades operativas
  adicionales.
- Los consumidores deben ser idempotentes desde su primera versión.
- El estado durable de un proceso sigue siendo necesario aunque el disparo pase
  de scheduler a eventos.
- Los eventos no autorizan a un servicio a escribir datos propiedad de otro.

## Alternativas consideradas

- **Solo HTTP síncrono:** descartado para trabajos largos y notificaciones; crea
  acoplamiento temporal y cadenas frágiles de disponibilidad.
- **Kafka para todos los comandos:** descartado porque complica respuestas
  inmediatas, errores de validación y semántica de autorización.
- **Dual-write directo desde cada request:** descartado por la ventana de fallo
  entre la confirmación de la base y la publicación en Kafka.
- **Exactly-once como garantía global:** descartado; se prefiere at-least-once
  con efectos idempotentes y deduplicación explícita.
- **Compartir una base o tablas entre servicios:** descartado porque elimina
  los límites de ownership y acopla las migraciones.
