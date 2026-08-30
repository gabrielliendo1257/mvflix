# Identidad de los envelopes de eventos

Los eventos de integración usan `eventId` UUID inmutable y `correlationId` UUID
estable durante una operación. En Movies, `actorId` conserva el `subject` del
`UserProvider` como string estable porque el contrato de autenticación actual
identifica usuarios por username/subject, no por UUID.

Los caminos internos de ingestión de biblioteca y reconciliación no reciben un
actor HTTP. Esos eventos usan explícitamente `actorId: system`; no se inventa un
usuario. `UploadCompleted` sigue el mismo criterio para las confirmaciones
internas de Storage y deriva su `correlationId` determinísticamente del upload.

La solicitud managed conserva actor y correlación en el envelope inicial. La
finalización posterior a la confirmación de Storage ocurre en un consumidor
interno que todavía no transporta esos campos, por lo que `CatalogItemDeleted`
usa `system` y una correlación nueva de esa finalización. Es una limitación
conocida de esta iteración; el evento se publica solo después del delete local
confirmado y no se publica una confirmación prematura.
