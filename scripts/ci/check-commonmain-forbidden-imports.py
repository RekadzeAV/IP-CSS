#!/usr/bin/env python3
from __future__ import annotations

import re
import sys
from pathlib import Path

FORBIDDEN_RE = re.compile(
    r"(^\s*import\s+(java|javax|android)\.)|(\b(java|javax|android)\.[A-Za-z0-9_]+)",
    re.MULTILINE,
)


def is_common_main_kotlin(path: Path) -> bool:
    normalized = path.as_posix()
    if not normalized.endswith(".kt"):
        return False
    if "/src/commonMain/kotlin/" not in normalized:
        return False
    # Archive docs are not active KMP sources and may use legacy JVM APIs.
    if "/docs/archive/" in normalized or normalized.startswith("docs/archive/"):
        return False
    return True


def main() -> int:
    root = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(".")
    if not root.exists():
        print(f"Root path does not exist: {root}", file=sys.stderr)
        return 2

    violations = []
    for file_path in root.rglob("*.kt"):
        if not is_common_main_kotlin(file_path):
            continue
        text = file_path.read_text(encoding="utf-8", errors="ignore")
        for idx, line in enumerate(text.splitlines(), start=1):
            if FORBIDDEN_RE.search(line):
                violations.append(f"{file_path}:{idx}: {line.strip()}")

    if violations:
        print("Forbidden platform API usage found in commonMain:")
        for item in violations:
            print(item)
        return 1

    print("OK: no forbidden java/javax/android usage in commonMain Kotlin sources.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())