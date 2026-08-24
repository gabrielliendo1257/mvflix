#!/usr/bin/env bash
# Prepara un Termux nativo para compilar y ejecutar los microservicios Java.
# PostgreSQL y MinIO deben ejecutarse fuera de Termux (otra maquina, proot/VM
# o un host Docker) y configurarse mediante envs/.env.
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"

if ! command -v pkg >/dev/null 2>&1; then
  echo "Este setup debe ejecutarse dentro de Termux nativo (comando pkg no encontrado)." >&2
  exit 1
fi

echo "== Actualizando paquetes de Termux =="
pkg update -y

echo "== Instalando herramientas requeridas =="
pkg install -y \
  bash \
  coreutils \
  curl \
  git \
  jq \
  make \
  openjdk-21 \
  procps \
  unzip

chmod +x "${PROJECT_ROOT}/mvnw" "${PROJECT_ROOT}/scripts/stack-dev.sh"
mkdir -p "${PROJECT_ROOT}/envs"

if [ ! -f "${PROJECT_ROOT}/envs/.env" ]; then
  cp "${PROJECT_ROOT}/envs/.env.example" "${PROJECT_ROOT}/envs/.env"
  echo "[CREATED] envs/.env desde envs/.env.example"
else
  echo "[SKIP] envs/.env ya existe"
fi

echo "== Verificando Java y Maven Wrapper =="
java -version
"${PROJECT_ROOT}/mvnw" -version

cat <<'MESSAGE'

Termux quedo preparado.

Siguientes pasos:
  1. Configura DB_HOST, DB_PORT y MINIO_URL en envs/.env.
  2. Configura las variables LAN/OAuth2 del mismo archivo.
  3. Ejecuta: ./scripts/stack-dev.sh start
  4. Consulta: ./scripts/stack-dev.sh status
  5. Detenlo: ./scripts/stack-dev.sh stop

Nota: Android restringe la visibilidad de /proc y `ss -p` no puede mostrar
todos los procesos. stack-dev.sh usa probes HTTP/TCP y PID files propios, por
lo que no depende de esa informacion.

Docker Compose no funciona de forma soportada en Termux nativo. PostgreSQL y
MinIO deben estar en un host accesible o en un entorno Linux que soporte Docker.
MESSAGE
