#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="${1:-.}"
python scripts/ci/check-commonmain-forbidden-imports.py "$ROOT_DIR"
