# ADR 0004: Borrado durable de media MANAGED (sin transacción distribuida)

Estado: Aceptado

## Contexto

Eliminar una media MANAGED toca a dos dueños distintos: el catálogo (movies)
y el objeto físico más su cuota (storage). No existe una transacción
distribuida ACID entre ambos servicios, y no se va a introducir una.

El BFF, al borrar solo el catálogo, dejaba el objeto en MinIO huérfano y la
cuota reservada. El primer paso fue bloquear ese camino (véase
`DeleteMedia`: 409 `MANAGED_DELETE_BLOCKED`). Falta definir cómo será el
borrado definitivo, para que nadie reintroduzca la eliminación directa en el
BFF ni una llamada frágil movies→storage.

## Decisión

1. **Ownership**. Movies posee el ciclo de vida del catálogo. Storage posee el
   objeto y la cuota. El BFF orquesta la intención, pero no garantiza la
   consistencia distribuida.
2. **Sin transacción distribuida ACID.** La consistencia entre catálogo y
   objeto se logra con compensación durable, no con 2PC.
3. **Orden del borrado: Storage → finalización en Movies.** Primero se borra
   el objeto (y se libera la cuota); después se finaliza la entrada de
   catálogo. Si el flujo muere entre ambos pasos, la película sigue apuntando
   a un objeto ya ausente, estado que el catálogo ya representa como MISSING
   y que un retry cierra. La alternativa (movies primero) es la que genera
   huérfanos con cuota reservada.
4. **DELETING representa trabajo pendiente.** El borrado es un estado durable,
   no una cadena síncrona: una media pasa a DELETING, se ejecuta el borrado de
   storage y recién entonces se finaliza en movies.
5. **Retries idempotentes.** Repetir el borrado de un objeto ya ausente es
   no-op; finalizar una media ya finalizada es no-op. Cualquier reintento puede
   ejecutarse sin efectos colaterales.
6. **LOCAL nunca se borra del filesystem.** Los archivos de biblioteca solo se
   desvinculan (vuelven a UNIDENTIFIED); el operador es dueño del disco.

## Consecuencias

- El BFF no implementará la eliminación de objetos; solo expresa la intención
  y deja que cada dueño finalice su parte de forma durable.
- El mecanismo de compensación es el estado DELETING (o un outbox/evento
  equivalente), que storage ya puede absorber vía `OrphanCleanupQueue` +
  `OrphanCleanupJob`.
- Mientras no exista DELETING, `DeleteMedia` seguirá bloqueando MANAGED con
  409 `MANAGED_DELETE_BLOCKED`; LOCAL/DRAFT se borran (solo desvinculan).
- Cualquier implementación futura de borrado debe respetar este orden y la
  idempotencia; una llamada directa BFF→storage o movies→storage queda
  descartada por esta decisión.

## Recuperación de altas interrumpidas

El recovery de Add Media consulta Storage por `(principal, idempotencyKey)` cuando
el draft existe pero el `uploadId` no llegó a persistirse. Storage deriva el
principal del token y nunca acepta un owner enviado por el cliente. Un `404` del
lookup es una ausencia confirmada de sesión y permite compensar el draft; un
error de red, `5xx` o cualquier respuesta ambigua deja el proceso en `PREPARING`.
Si la sesión existe, el proceso se reclama con CAS, se encolan ambas
compensaciones y solo después de completarlas pasa a `CANCELLED`.
