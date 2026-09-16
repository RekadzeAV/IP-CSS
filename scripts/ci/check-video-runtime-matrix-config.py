#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path


def load_json(path: Path) -> dict:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:  # noqa: BLE001
        raise ValueError(f"Invalid JSON in {path}: {exc}") from exc


def validate_matrix(
    path: Path,
    *,
    strict_enabled_playlists: bool,
) -> tuple[list[str], list[str]]:
    errors: list[str] = []
    warnings: list[str] = []

    if not path.exists():
        errors.append(f"Config file not found: {path}")
        return errors, warnings

    data = load_json(path)
    if not isinstance(data, dict):
        errors.append(f"{path}: root must be an object")
        return errors, warnings

    defaults = data.get("defaults")
    scenarios = data.get("scenarios")

    if not isinstance(defaults, dict):
        errors.append(f"{path}: 'defaults' must be an object")
    if not isinstance(scenarios, list) or len(scenarios) == 0:
        errors.append(f"{path}: 'scenarios' must be a non-empty array")
        return errors, warnings

    if isinstance(defaults, dict):
        for key in ("baseUrl", "username", "password", "durationMinutes", "intervalSec", "healthPath"):
            if key not in defaults:
                errors.append(f"{path}: defaults missing '{key}'")

    for i, scenario in enumerate(scenarios):
        prefix = f"{path}: scenarios[{i}]"
        if not isinstance(scenario, dict):
            errors.append(f"{prefix} must be an object")
            continue

        for key in ("name", "platform", "enabled"):
            if key not in scenario:
                errors.append(f"{prefix} missing '{key}'")

        playlist_urls = scenario.get("playlistUrls")
        if playlist_urls is not None and not isinstance(playlist_urls, list):
            errors.append(f"{prefix}.playlistUrls must be an array when provided")
        if isinstance(playlist_urls, list):
            for j, url in enumerate(playlist_urls):
                if not isinstance(url, str) or not url.strip():
                    errors.append(f"{prefix}.playlistUrls[{j}] must be non-empty string")

        enabled = scenario.get("enabled") is True
        if enabled:
            if not isinstance(playlist_urls, list) or len(playlist_urls) == 0:
                message = f"{prefix} is enabled but has empty playlistUrls"
                if strict_enabled_playlists:
                    errors.append(message)
                else:
                    warnings.append(message)

    return errors, warnings


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate video-runtime-matrix config files.")
    parser.add_argument(
        "--strict-enabled-playlists",
        action="store_true",
        help="Fail if enabled scenarios have empty playlistUrls",
    )
    parser.add_argument(
        "--root",
        default=".",
        help="Repository root path",
    )
    args = parser.parse_args()

    root = Path(args.root).resolve()
    example_path = root / "config/video-runtime-matrix.example.json"
    local_path = root / "config/video-runtime-matrix.local.json"

    errors: list[str] = []
    warnings: list[str] = []

    e1, w1 = validate_matrix(example_path, strict_enabled_playlists=True)
    errors.extend(e1)
    warnings.extend(w1)

    if local_path.exists():
        e2, w2 = validate_matrix(
            local_path,
            strict_enabled_playlists=args.strict_enabled_playlists,
        )
        errors.extend(e2)
        warnings.extend(w2)
    else:
        warnings.append(f"Local config not found (optional): {local_path}")

    for warning in warnings:
        print(f"[WARN] {warning}")
    if errors:
        for error in errors:
            print(f"[ERROR] {error}")
        return 1

    print("OK: video runtime matrix configs are valid.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
