#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path


def run(command: list[str], cwd: Path) -> int:
    print(f"\n[RUN] {' '.join(command)}", flush=True)
    completed = subprocess.run(command, cwd=cwd)
    if completed.returncode != 0:
        print(f"[FAIL] {' '.join(command)} (exit={completed.returncode})", flush=True)
        return completed.returncode
    print(f"[OK] {' '.join(command)}", flush=True)
    return 0


def main() -> int:
    gradlew = "gradlew.bat" if sys.platform.startswith("win") else "./gradlew"

    parser = argparse.ArgumentParser(description="Run all KMP Phase 1 verification gates.")
    parser.add_argument("--root", default=".", help="Repository root path")
    parser.add_argument(
        "--skip-gradle",
        action="store_true",
        help="Run only Python checks without Gradle tasks",
    )
    parser.add_argument(
        "--strict-runtime-matrix",
        action="store_true",
        help="Treat enabled scenarios with empty playlistUrls as errors for local runtime matrix config",
    )
    parser.add_argument(
        "--ci-profile",
        action="store_true",
        help="Run with CI-equivalent Python-gates options (implies --skip-gradle --strict-runtime-matrix)",
    )
    parser.add_argument(
        "--report-json",
        help="Optional path to write machine-readable verification report JSON",
    )
    args = parser.parse_args()

    root = Path(args.root).resolve()
    if not root.exists():
        print(f"Root path does not exist: {root}", file=sys.stderr)
        return 2

    strict_runtime_matrix = args.strict_runtime_matrix or args.ci_profile
    skip_gradle = args.skip_gradle or args.ci_profile

    runtime_matrix_cmd = [sys.executable, "scripts/ci/check-video-runtime-matrix-config.py", "--root", "."]
    if strict_runtime_matrix:
        runtime_matrix_cmd.append("--strict-enabled-playlists")

    python_checks = [
        [sys.executable, "scripts/ci/check-commonmain-forbidden-imports.py", "."],
        [sys.executable, "scripts/ci/check-security-expect-actual-signatures.py", "."],
        [sys.executable, "scripts/ci/check-no-jvm-deps-in-native-source-sets.py", "."],
        runtime_matrix_cmd,
        [sys.executable, "scripts/ci/validate-video-e2e-profile.py", "--root", "."],
    ]

    gradle_checks = [
        [
            gradlew,
            ":core:common:compileKotlinMetadata",
            ":core:network:compileKotlinMetadata",
            ":shared:compileKotlinMetadata",
            "--no-daemon",
        ],
        [
            gradlew,
            ":core:common:desktopTest",
            "--no-daemon",
        ],
        [
            gradlew,
            ":core:common:compileKotlinNativeWindows",
            "--no-daemon",
        ],
    ]

    mode_name = "ci-profile" if args.ci_profile else ("strict-runtime-matrix" if strict_runtime_matrix else "default")
    print("== KMP Phase 1 Verification ==", flush=True)
    print(f"Repository root: {root}", flush=True)
    print(f"Mode: {mode_name}", flush=True)
    print(f"Skip gradle: {skip_gradle}", flush=True)

    executed_commands: list[dict[str, object]] = []
    overall_ok = True
    failed_command: str | None = None

    for cmd in python_checks:
        code = run(cmd, root)
        executed_commands.append(
            {
                "command": cmd,
                "stage": "python-gates",
                "exit_code": code,
                "ok": code == 0,
            }
        )
        if code != 0:
            overall_ok = False
            failed_command = " ".join(cmd)
            break

    if overall_ok and not skip_gradle:
        for cmd in gradle_checks:
            code = run(cmd, root)
            executed_commands.append(
                {
                    "command": cmd,
                    "stage": "gradle-gates",
                    "exit_code": code,
                    "ok": code == 0,
                }
            )
            if code != 0:
                overall_ok = False
                failed_command = " ".join(cmd)
                break

    if args.report_json:
        report_path = (root / args.report_json).resolve() if not Path(args.report_json).is_absolute() else Path(args.report_json)
        report_path.parent.mkdir(parents=True, exist_ok=True)
        payload = {
            "generated_at_utc": datetime.now(timezone.utc).isoformat(),
            "root": str(root),
            "mode": mode_name,
            "skip_gradle": skip_gradle,
            "overall_ok": overall_ok,
            "failed_command": failed_command,
            "checks": executed_commands,
        }
        report_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"Report written: {report_path}", flush=True)

    if overall_ok:
        print("\nAll selected KMP Phase 1 checks passed.", flush=True)
        return 0

    print(f"\nKMP Phase 1 verification failed. First failed command: {failed_command}", flush=True)
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
