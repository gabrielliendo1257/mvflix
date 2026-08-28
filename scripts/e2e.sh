#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="${PROJECT_ROOT}/e2e/docker-compose-e2e.yml"
COMPOSE=(docker compose -f "${COMPOSE_FILE}" -p mvflix-e2e)

cleanup() {
  status=$?
  "${COMPOSE[@]}" down -v --remove-orphans
  exit "${status}"
}
trap cleanup EXIT

"${COMPOSE[@]}" up --wait --build -d
"${PROJECT_ROOT}/mvnw" -f "${PROJECT_ROOT}/e2e/runner/pom.xml" test
