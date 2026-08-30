# Observabilidad operativa

## SLO iniciales

Estos objetivos son una primera línea base y deben recalibrarse con tráfico real:

| Servicio/capacidad | SLO | Ventana |
| --- | ---: | --- |
| Endpoints HTTP de lectura | 99.5% de respuestas no 5xx | 30 días |
| Entrega de outbox | 99% publicada en menos de 5 minutos | 30 días |
| Eventos sin DLT | 99.9% | 30 días |
| Proyección Activity | 99% aplicada en menos de 2 minutos | 30 días |

Prometheus contiene las reglas iniciales en
`infra/docker/observability/rules/mvflix-alerts.yml`. La entrega de notificaciones
requiere conectar Alertmanager o el sistema de paging de la plataforma.

## Dashboard

Grafana provisiona `Mvflix Overview` automáticamente desde
`infra/docker/observability/grafana/dashboards/mvflix-overview.json`. Muestra
disponibilidad de targets, backlog/edad de outbox, fallos Kafka, DLT, latencia de
borrado administrado y fallos de Activity.

Arranque local:

```bash
make up-observability-d
```

## Runbook de incidentes

### Servicio down

1. Revisar `up` y `/actuator/health/readiness`.
2. Consultar logs del contenedor y dependencias PostgreSQL/Kafka.
3. Reiniciar solo después de preservar los logs y confirmar que las migraciones son compatibles.

### Outbox pendiente o agotada

1. Consultar `mvflix_outbox_pending`, `mvflix_outbox_oldest_age_seconds` y `mvflix_outbox_exhausted`.
2. Verificar conectividad y errores Kafka.
3. No borrar filas de outbox.
4. Tras corregir la causa, usar el endpoint administrativo de reactivación documentado en `docs/storage-outbox-runbook.md`.

### Mensajes en DLT

1. Capturar topic, `eventId`, `eventType` y causa.
2. Validar el contrato AsyncAPI y el payload antes de reprocesar.
3. Corregir el consumidor o el dato productor; no hacer replay masivo sin acotar el evento.

### Activity atrasada

1. Revisar fallos de Inbox y lag del consumer group `mvflix-activity`.
2. Comprobar que PostgreSQL está disponible y que la secuencia del evento no está siendo rechazada por un productor antiguo.
3. Confirmar que el replay conserva el mismo `eventId`.

### Borrado administrado atascado

1. Revisar estado `DELETING`, outbox y DLT en Movies/Storage.
2. Verificar Inbox de Storage y la confirmación `StoredObjectDeleted`.
3. No eliminar manualmente la película ni liberar cuota fuera de sus servicios propietarios.
4. Seguir `docs/storage-outbox-runbook.md` para reactivar entregas agotadas.

## Escalado

Las alertas críticas deben crear una incidencia con servicio, `eventId`, `correlationId`,
ventana temporal y último error. Los cambios de umbral se revisan junto con el
error budget mensual, no durante el incidente.
