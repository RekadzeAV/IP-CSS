#!/usr/bin/env bash
set -euo pipefail

ARTIFACT_CONTRACT_PATH="${ARTIFACT_CONTRACT_PATH:-config/nas-artifact-contract.json}"
RUNTIME_ENV_CONTRACT_PATH="${RUNTIME_ENV_CONTRACT_PATH:-config/nas-runtime-env-contract.json}"
VERSION="${VERSION:-}"
PACKAGES="${PACKAGES:-synology,qnap,asustor,truenas}"
ARCHITECTURES="${ARCHITECTURES:-x86_64,arm64}"

if [[ "${1:-}" == "--help" ]]; then
  echo "Validate NAS artifact/runtime contracts and CI matrix inputs."
  echo "Usage: VERSION=Alfa-0.1.1 PACKAGES=synology,qnap ARCHITECTURES=x86_64,arm64 ./scripts/ci/validate-nas-contracts.sh"
  exit 0
fi

python3 - <<'PY'
import json
import os
import re
import sys
from pathlib import Path

artifact_path = Path(os.environ.get("ARTIFACT_CONTRACT_PATH", "config/nas-artifact-contract.json"))
runtime_path = Path(os.environ.get("RUNTIME_ENV_CONTRACT_PATH", "config/nas-runtime-env-contract.json"))
version = os.environ.get("VERSION", "")
packages = [p.strip() for p in os.environ.get("PACKAGES", "synology,qnap,asustor,truenas").split(",") if p.strip()]
architectures = [a.strip() for a in os.environ.get("ARCHITECTURES", "x86_64,arm64").split(",") if a.strip()]

def fail(message: str) -> None:
    print(f"ERROR: {message}", file=sys.stderr)
    sys.exit(1)

if not artifact_path.exists():
    fail(f"Artifact contract not found: {artifact_path}")
if not runtime_path.exists():
    fail(f"Runtime env contract not found: {runtime_path}")

artifact = json.loads(artifact_path.read_text(encoding="utf-8"))
runtime = json.loads(runtime_path.read_text(encoding="utf-8"))

if not artifact.get("schemaVersion"):
    fail("Artifact contract: schemaVersion is required")
if not runtime.get("schemaVersion"):
    fail("Runtime env contract: schemaVersion is required")
if not artifact.get("packageTypes"):
    fail("Artifact contract: packageTypes must not be empty")
if not runtime.get("required"):
    fail("Runtime env contract: required list must not be empty")

for pkg in packages:
    if pkg not in artifact["packageTypes"]:
        fail(f"Unknown package '{pkg}' is not declared in artifact contract")

for arch in architectures:
    if arch not in artifact.get("architectures", []):
        fail(f"Unknown architecture '{arch}' is not declared in artifact contract")

if version:
    release_pattern = artifact.get("releaseVersionPattern")
    if not release_pattern or not re.match(release_pattern, version):
        fail(f"Version '{version}' does not match releaseVersionPattern")
    for pkg in packages:
        if pkg == "truenas":
            continue
        pkg_def = artifact["packageTypes"][pkg]
        name_pattern = pkg_def.get("namePattern")
        ext = pkg_def.get("extension")
        if not name_pattern or not ext:
            fail(f"Package '{pkg}' contract requires namePattern and extension")
        for arch in architectures:
            candidate = f"ip-css-{version}-{pkg}-{arch}{ext}"
            if not re.match(name_pattern, candidate):
                fail(f"Artifact candidate '{candidate}' violates '{pkg}' naming contract")

for forbidden in runtime.get("forbiddenExactValues", []):
    for req in runtime.get("required", []):
        if req.get("default") == forbidden:
            fail(f"Required env '{req.get('name')}' uses forbidden default '{forbidden}'")

print("NAS contract validation passed.")
PY
