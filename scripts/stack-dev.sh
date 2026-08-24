#!/usr/bin/env bash
# Levanta/derriba todo el ecosistema mvflix en dev (con seguridad real y dev-token).
# Orden: auth primero (el BFF hace issuer discovery al arrancar), luego el resto en
# paralelo, y el BFF al final.
#
#   ./scripts/stack-dev.sh start | stop | status
#
# Requiere: docker compose up (postgres+minio) y, opcionalmente, TMDB_API_TOKEN
# exportado para que el enriquecimiento funcione.
set -euo pipefail

# Credenciales privadas de dev (TMDB, etc.) desde envs/.env (gitignored).
# Quedan en el entorno para que las apps las lean via ${VAR:default}.
set -a
# shellcheck disable=SC1091
source "$(dirname "$0")/../envs/.env"
set +a

AUTH_PORT=9090
USERS_PORT=8080
STORAGE_PORT=6060
MOVIES_PORT=4040
BFF_PORT=9091
LOG_DIR="${XDG_STATE_HOME:-$HOME/.local/state}/mvflix-dev/logs"
mkdir -p "$LOG_DIR"

declare -A SERVICES=(
  [mvflix-authorization]=$AUTH_PORT
  [mvflix-users]=$USERS_PORT
  [mvflix-storage]=$STORAGE_PORT
  [mvflix-movies]=$MOVIES_PORT
  [bff-mvflix-web]=$BFF_PORT
)

wait_port() {
  local port=$1
  local name=$2
  for _ in $(seq 1 90); do
    if curl -s -o /dev/null "http://127.0.0.1:${port}/" 2>/dev/null; then
      return 0
    fi
    sleep 1
  done
  echo "  [WARN] ${name} no respondio en 90s (revisa ${LOG_DIR}/stack-${name}.log)"
}

port_free() {
  ! ss -tln 2>/dev/null | grep -q ":${1} "
}

start_one() {
  local name=$1
  local port=${SERVICES[$name]}
  if ! port_free "$port"; then
    echo "  [SKIP] ${name} ya escucha en :${port} (instancia tuya? no la toco)"
    return
  fi
  echo "  [START] ${name} (dev) en :${port} ..."
  TMDB_API_TOKEN="${TMDB_API_TOKEN:-}" nohup mvn -q -pl "${name}" spring-boot:run \
    -Dspring-boot.run.profiles=dev > "${LOG_DIR}/stack-${name}.log" 2>&1 &
}

stop_one() {
  local name=$1
  local port=${SERVICES[$name]}
  if port_free "$port"; then
    echo "  [STOP] ${name}: :${port} ya estaba libre"
    return
  fi
  local pid
  pid=$(ss -tlnp 2>/dev/null | grep ":${port} " | grep -oP 'pid=\K[0-9]+' | head -1)
  if [ -n "$pid" ]; then
    kill "$pid" 2>/dev/null || true
    echo "  [STOP] ${name} (pid ${pid})"
  else
    echo "  [STOP] ${name}: sin pid visible en :${port}"
  fi
}

start() {
  if ! ss -tln 2>/dev/null | grep -q ":5432 "; then
    echo "postgres no esta escuchando en :5432 -> make up-dev"
  fi
  if ! ss -tln 2>/dev/null | grep -q ":9000 "; then
    echo "minio no esta escuchando en :9000 -> make up-dev"
  fi

  echo "== Compilando e instalando modulos compartidos =="
  mvn -q -pl mvflix-devseed -am install -DskipTests

  echo "== Arrancando auth (obligatorio antes del BFF) =="
  start_one mvflix-authorization
  wait_port "$AUTH_PORT" mvflix-authorization

  echo "== Arrancando users, storage y movies en paralelo =="
  start_one mvflix-users
  start_one mvflix-storage
  start_one mvflix-movies
  wait_port "$USERS_PORT" mvflix-users
  wait_port "$STORAGE_PORT" mvflix-storage
  wait_port "$MOVIES_PORT" mvflix-movies

  echo "== Arrancando BFF (requiere auth arriba) =="
  start_one bff-mvflix-web
  wait_port "$BFF_PORT" bff-mvflix-web

  echo ""
  echo "Stack dev arriba:"
  echo "  auth    http://127.0.0.1:${AUTH_PORT}   (POST /oauth2/dev-token)"
  echo "  users   http://127.0.0.1:${USERS_PORT}"
  echo "  storage http://127.0.0.1:${STORAGE_PORT}"
  echo "  movies  http://127.0.0.1:${MOVIES_PORT}"
  echo "  bff     http://127.0.0.1:${BFF_PORT}"
  echo "Dev token: curl -s -X POST http://127.0.0.1:${AUTH_PORT}/oauth2/dev-token"
  echo "           -H 'Content-Type: application/json' -d '{\"username\":\"Javier\",\"password\":\"JavierPassword\"}'"
}

stop() {
  echo "== Deteniendo stack dev =="
  stop_one bff-mvflix-web
  stop_one mvflix-users
  stop_one mvflix-storage
  stop_one mvflix-movies
  stop_one mvflix-authorization
}

status() {
  echo "== Estado del stack dev =="
  for name in mvflix-authorization mvflix-users mvflix-storage mvflix-movies bff-mvflix-web; do
    local port=${SERVICES[$name]}
    if port_free "$port"; then
      echo "  ${name}: DOWN (:${port})"
    else
      echo "  ${name}: UP (:${port})"
    fi
  done
}

case "${1:-status}" in
  start) start ;;
  stop) stop ;;
  status) status ;;
  *) echo "uso: $0 [start|stop|status]" && exit 1 ;;
esac