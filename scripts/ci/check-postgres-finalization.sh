#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

echo "[check] PostgreSQL finalization invariants"

require_pattern() {
  local pattern="$1"
  local file="$2"
  local label="$3"
  if ! rg --quiet --fixed-strings "$pattern" "$file"; then
    echo "[fail] $label"
    echo "       missing pattern: $pattern"
    echo "       file: $file"
    exit 1
  fi
  echo "[ok] $label"
}

require_pattern "DB_MODE=\${DB_MODE:-postgres}" "docker-compose.yml" "docker-compose enforces postgres DB mode by default"
require_pattern "DB_MODE=postgres" ".env.example" ".env.example documents postgres DB mode"

require_pattern "validateDatabaseRequirementsForServerStartup()" \
  "server/api/src/main/kotlin/com/company/ipcamera/server/Application.kt" \
  "application startup runs DB preflight"

require_pattern "validateDatabaseRequirementsForServerStartup()" \
  "server/api/src/main/kotlin/com/company/ipcamera/server/di/AppModule.kt" \
  "DI bootstrap validates DB requirements"

require_pattern "Production startup blocked: ServerUserRepository requires PostgreSQL data source." \
  "server/api/src/main/kotlin/com/company/ipcamera/server/di/AppModule.kt" \
  "production blocks in-memory ServerUserRepository fallback"

require_pattern "isPostgresModeEnabled()" \
  "server/api/src/main/kotlin/com/company/ipcamera/server/config/DatabaseConfig.kt" \
  "DatabaseConfig exposes explicit postgres mode check"

require_pattern "Production startup blocked: set ADMIN_PASSWORD_HASH or ADMIN_PASSWORD for initial admin bootstrap." \
  "server/api/src/main/kotlin/com/company/ipcamera/server/repository/ServerUserRepositoryPostgres.kt" \
  "admin bootstrap is hardened in production"

echo "[pass] PostgreSQL finalization contract checks passed"
