# Observabilidad local

El compose de desarrollo incluye Prometheus, Grafana, un OpenTelemetry
Collector y Tempo.

## Arranque

Desde `infra/docker`:

```bash
docker compose -f docker-compose-dev.yml up -d prometheus otel-collector tempo grafana
```

Grafana queda en `http://<IP-LAN-DE-LA-LAPTOP>:3000`. Las credenciales se
configuran con `GRAFANA_ADMIN_USER` y `GRAFANA_ADMIN_PASSWORD` en `.env`.

Prometheus queda ligado a `127.0.0.1:9095` por defecto y no se publica en la
LAN. Grafana lo consulta internamente mediante la red Docker.

## Métricas

Cada aplicación expone su management server en un puerto separado y protegido
por Basic Auth:

| Servicio | Puerto |
| --- | ---: |
| BFF | 10091 |
| Movies | 10040 |
| Storage | 10060 |
| Users | 10080 |
| Authorization | 10090 |

La ruta es `/actuator/prometheus`. Prometheus usa `ACTUATOR_METRICS_USER` y
`ACTUATOR_METRICS_PASSWORD`; no se deben usar las credenciales por defecto
fuera de desarrollo.

## Traces desde Termux

El Collector escucha en todas las interfaces en el puerto `4318` y recibe OTLP
HTTP en:

```text
http://<IP-LAN-DE-LA-LAPTOP>:4318/v1/traces
```

Desde Termux, `<IP-LAN-DE-LA-LAPTOP>` es la IP de la laptop, no `localhost` ni
la IP del teléfono. Para una aplicación ejecutada fuera de Docker, establecer:

```bash
export OTEL_EXPORTER_OTLP_ENDPOINT=http://<IP-LAN-DE-LA-LAPTOP>:4318/v1/traces
```

El Collector reenvía las trazas a Tempo; Grafana ya incluye ambos data sources.
