#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path


def validate_profile(path: Path) -> list[str]:
    errors: list[str] = []
    if not path.exists():
        errors.append(f"Missing required profile file: {path}")
        return errors

    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:  # noqa: BLE001
        errors.append(f"Invalid JSON in {path}: {exc}")
        return errors

    profile_name = data.get("profileName")
    if not isinstance(profile_name, str) or not profile_name.strip():
        errors.append(f"{path}: profileName must be a non-empty string")

    release = data.get("release")
    if not isinstance(release, dict):
        errors.append(f"{path}: release section must be an object")
        return errors

    optional_control_ids = release.get("optionalControlIds")
    if optional_control_ids is None:
        errors.append(f"{path}: release.optionalControlIds is required")
        return errors
    if not isinstance(optional_control_ids, list):
        errors.append(f"{path}: release.optionalControlIds must be an array")
        return errors

    for i, item in enumerate(optional_control_ids):
        if not isinstance(item, str) or not item.strip():
            errors.append(f"{path}: release.optionalControlIds[{i}] must be a non-empty string")

    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate video e2e acceptance profile configs.")
    parser.add_argument("--root", default=".", help="Repository root path")
    args = parser.parse_args()

    root = Path(args.root).resolve()
    example_path = root / "config/video-e2e-acceptance-profile.example.json"
    local_path = root / "config/video-e2e-acceptance-profile.local.json"
    mvp_ci_path = root / "config/video-e2e-acceptance-profile.mvp-ci.json"

    errors: list[str] = []
    errors.extend(validate_profile(example_path))
    errors.extend(validate_profile(mvp_ci_path))
    if local_path.exists():
        errors.extend(validate_profile(local_path))
    else:
        print(f"[video-e2e-profile] WARN: local profile not found (optional): {local_path}")

    if errors:
        for error in errors:
            print(f"[video-e2e-profile] ERROR: {error}")
        return 1

    print("[video-e2e-profile] OK: profile structure is valid (example, mvp-ci, local if present)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
