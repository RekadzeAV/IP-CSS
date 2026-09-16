#!/usr/bin/env python3
"""
Fail-fast validation for audit integrity artifacts.

Ensures repository contains:
- migration for integrity chain columns,
- hasher and verifier sources,
- related tests.
"""

from __future__ import annotations

import sys
from pathlib import Path


REQUIRED_FILES = [
    "server/api/src/main/resources/db/migration/V5__Add_audit_log_integrity_chain.sql",
    "server/api/src/main/kotlin/com/company/ipcamera/server/security/AuditIntegrityHasher.kt",
    "server/api/src/main/kotlin/com/company/ipcamera/server/security/AuditIntegrityVerifier.kt",
    "server/api/src/test/kotlin/com/company/ipcamera/server/security/AuditIntegrityHasherTest.kt",
    "server/api/src/test/kotlin/com/company/ipcamera/server/security/AuditIntegrityVerifierTest.kt",
]


def main() -> int:
    root = Path(__file__).resolve().parents[2]
    missing: list[str] = []
    for relative in REQUIRED_FILES:
        if not (root / relative).exists():
            missing.append(relative)

    if missing:
        print("[audit-integrity] Missing required artifacts:")
        for item in missing:
            print(f"- {item}")
        return 1

    print("[audit-integrity] Required artifacts are present")
    return 0


if __name__ == "__main__":
    sys.exit(main())
