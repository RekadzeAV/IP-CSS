#!/bin/bash
# Wrapper to build Synology SPK package.
# Usage: ./scripts/build-synology-spk.sh [x86_64|arm64] [version]
# Example: ./scripts/build-synology-spk.sh x86_64 Alfa-0.1.1

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ARCH="${1:-x86_64}"
VERSION="${2:-Alfa-0.1.1}"
exec "$SCRIPT_DIR/build-nas-package.sh" synology "$ARCH" "$VERSION"
