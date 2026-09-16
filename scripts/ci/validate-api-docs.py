#!/usr/bin/env python3
"""
Validate API documentation artifacts required by CI.

Checks:
- docs/api/openapi.yaml exists and is parseable YAML with OpenAPI fields.
- docs/api/websocket-subscription-protocol.md exists and has required sections.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

import yaml


def fail(message: str) -> int:
    print(f"[api-docs] ERROR: {message}")
    return 1


def main() -> int:
    root = Path(__file__).resolve().parents[2]
    openapi_file = root / "docs" / "api" / "openapi.yaml"
    ws_protocol_file = root / "docs" / "api" / "websocket-subscription-protocol.md"

    if not openapi_file.exists():
        return fail("Missing docs/api/openapi.yaml")
    if not ws_protocol_file.exists():
        return fail("Missing docs/api/websocket-subscription-protocol.md")

    try:
        parsed = yaml.safe_load(openapi_file.read_text(encoding="utf-8"))
    except Exception as exc:
        return fail(f"Failed to parse openapi.yaml: {exc}")

    if not isinstance(parsed, dict):
        return fail("openapi.yaml must be a YAML object")
    if not str(parsed.get("openapi", "")).startswith("3."):
        return fail("openapi.yaml must declare OpenAPI 3.x")
    if "paths" not in parsed or not isinstance(parsed["paths"], dict) or len(parsed["paths"]) == 0:
        return fail("openapi.yaml must contain non-empty paths section")

    ws_content = ws_protocol_file.read_text(encoding="utf-8")
    required_markers = [
        r"(?im)^#\s+WebSocket Subscription Protocol",
        r"(?im)^##\s+Client -> Server messages",
        r"(?im)^##\s+Server -> Client messages",
        r"(?im)^##\s+Supported channels",
    ]
    for marker in required_markers:
        if re.search(marker, ws_content) is None:
            return fail(f"websocket-subscription-protocol.md missing required section: {marker}")

    print("[api-docs] API documentation validation passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
