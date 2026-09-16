#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="${1:-.}"
python scripts/ci/check-security-expect-actual-signatures.py "$ROOT_DIR"
