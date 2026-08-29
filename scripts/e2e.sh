#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MAKE=(make -C "${PROJECT_ROOT}")

cleanup() {
  status=$?
  "${MAKE[@]}" down-e2e-v
  exit "${status}"
}
trap cleanup EXIT

"${MAKE[@]}" up-e2e-d
"${PROJECT_ROOT}/mvnw" -f "${PROJECT_ROOT}/e2e/runner/pom.xml" test
