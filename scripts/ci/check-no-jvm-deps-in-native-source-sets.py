#!/usr/bin/env python3
from __future__ import annotations

import re
import sys
from pathlib import Path

FORBIDDEN_TOKENS = (
    "libs.ktor.client.java",
    "libs.ktor.client.okhttp",
    "androidx.",
    "org.bytedeco:javacv",
    "javafx",
)

SOURCE_SET_START_RE = re.compile(r"^\s*val\s+(iosMain|nativeMain)\s+by\s+(creating|gettting|getting)\s*\{")


def scan_file(path: Path) -> list[str]:
    violations: list[str] = []
    lines = path.read_text(encoding="utf-8").splitlines()

    in_target_block = False
    target_name = ""
    depth = 0

    for idx, line in enumerate(lines, start=1):
        if not in_target_block:
            match = SOURCE_SET_START_RE.search(line)
            if match:
                in_target_block = True
                target_name = match.group(1)
                depth = line.count("{") - line.count("}")
                if depth <= 0:
                    in_target_block = False
                    target_name = ""
                continue

        if in_target_block:
            for token in FORBIDDEN_TOKENS:
                if token in line:
                    violations.append(
                        f"{path}:{idx}: forbidden token '{token}' inside {target_name} dependencies block"
                    )
            depth += line.count("{") - line.count("}")
            if depth <= 0:
                in_target_block = False
                target_name = ""

    return violations


def main() -> int:
    root = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(".")
    if not root.exists():
        print(f"Root path does not exist: {root}", file=sys.stderr)
        return 2

    build_files = list(root.rglob("build.gradle.kts"))
    violations: list[str] = []
    for build_file in build_files:
        violations.extend(scan_file(build_file))

    if violations:
        print("Detected forbidden JVM/Android dependency declarations in iosMain/nativeMain:")
        for issue in violations:
            print(issue)
        return 1

    print("OK: no forbidden JVM/Android dependencies in iosMain/nativeMain blocks.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
