#!/usr/bin/env bash

set -euo pipefail

COMPOSE_FILE="docker-compose.integration.yml"
POSTGRES_CONTAINER="ipcss-it-postgres"
REDIS_CONTAINER="ipcss-it-redis"

cleanup() {
  docker compose -f "${COMPOSE_FILE}" down -v --remove-orphans || true
}

wait_for_health() {
  local container="$1"
  local retries=40
  local delay_seconds=3

  for ((i=1; i<=retries; i++)); do
    status="$(docker inspect -f '{{.State.Health.Status}}' "${container}" 2>/dev/null || true)"
    if [[ "${status}" == "healthy" ]]; then
      echo "[ci] ${container} is healthy"
      return 0
    fi
    echo "[ci] waiting for ${container} health (${i}/${retries}) current='${status}'"
    sleep "${delay_seconds}"
  done

  echo "[ci] ${container} did not become healthy in time"
  return 1
}

trap cleanup EXIT

echo "[ci] starting integration dependencies via docker compose"
docker compose -f "${COMPOSE_FILE}" up -d postgres redis

wait_for_health "${POSTGRES_CONTAINER}"
wait_for_health "${REDIS_CONTAINER}"

echo "[ci] running server API integration smoke tests"
DB_MODE=postgres \
DATABASE_URL=jdbc:postgresql://localhost:55432/ipcss_test \
DATABASE_USER=ipcss \
DATABASE_PASSWORD=ipcss \
ENABLE_FLYWAY=true \
REDIS_HOST=localhost \
REDIS_PORT=56379 \
API_GLOBAL_RATE_LIMIT_ENABLED=false \
./gradlew :server:api:test \
  --tests "*DatabaseComposeIntegrationTest" \
  --tests "*CameraRoutesAuthIntegrationTest" \
  --tests "*AuditRoutesIntegrationTest" \
  --tests "*HealthReadyIntegrationTest" \
  --no-daemon
