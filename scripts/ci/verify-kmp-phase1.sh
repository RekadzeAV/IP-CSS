#!/usr/bin/env bash
set -euo pipefail

# Usage examples:
#   ./scripts/ci/verify-kmp-phase1.sh
#   ./scripts/ci/verify-kmp-phase1.sh --skip-gradle
#   ./scripts/ci/verify-kmp-phase1.sh --strict-runtime-matrix
#   ./scripts/ci/verify-kmp-phase1.sh --ci-profile
#   ./scripts/ci/verify-kmp-phase1.sh --ci-profile --report-json diagnostics/kmp/verify-report.json

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SCRIPT_PATH="$ROOT_DIR/scripts/ci/verify-kmp-phase1.py"

if [[ ! -f "$SCRIPT_PATH" ]]; then
  echo "Script not found: $SCRIPT_PATH" >&2
  exit 2
fi

echo "Running KMP Phase 1 verifier..."
echo "Repository: $ROOT_DIR"

python "$SCRIPT_PATH" "$@"

echo "KMP Phase 1 verification completed successfully."
