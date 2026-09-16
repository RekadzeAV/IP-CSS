#!/usr/bin/env python3
"""
Validate local markdown links in active project scope.

Active scope intentionally excludes:
- node_modules
- docs/archive
- diagnostics
- build artifacts
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path


LINK_RE = re.compile(r"\[[^\]]+\]\(([^)]+)\)")


def is_excluded_path(path: Path, project_root: Path) -> bool:
    rel = path.relative_to(project_root).as_posix().lower()
    if "/node_modules/" in f"/{rel}/":
        return True
    if rel.startswith("docs/archive/"):
        return True
    # Root-level archives are not active scope either (see module docstring).
    if rel.startswith("archive/") or "/archive/" in f"/{rel}/":
        return True
    if rel.startswith("_to_be_archived/"):
        return True
    if rel.startswith("diagnostics/"):
        return True
    if rel.startswith("build/") or "/build/" in f"/{rel}/":
        return True
    return False


def is_external_or_anchor(link: str) -> bool:
    return link.startswith(("http://", "https://", "mailto:", "#"))


def normalize_target(link: str) -> str:
    target = link.split("#", 1)[0].strip()
    return target


def scan_markdown_links(project_root: Path) -> dict:
    markdown_files = []
    for md_file in project_root.rglob("*.md"):
        if not md_file.is_file():
            continue
        if is_excluded_path(md_file, project_root):
            continue
        markdown_files.append(md_file)

    issues: list[dict] = []
    total_links = 0
    total_local_links = 0

    for md_file in markdown_files:
        try:
            content = md_file.read_text(encoding="utf-8", errors="ignore")
        except OSError as exc:
            issues.append(
                {
                    "file": md_file.relative_to(project_root).as_posix(),
                    "link": "<read-error>",
                    "error": str(exc),
                }
            )
            continue

        for match in LINK_RE.finditer(content):
            raw_link = match.group(1).strip()
            total_links += 1

            if is_external_or_anchor(raw_link):
                continue

            target = normalize_target(raw_link)
            if not target:
                continue

            total_local_links += 1
            resolved = (md_file.parent / target).resolve()

            if not resolved.exists():
                issues.append(
                    {
                        "file": md_file.relative_to(project_root).as_posix(),
                        "link": raw_link,
                        "resolved": str(resolved),
                    }
                )

    return {
        "markdown_files_scanned": len(markdown_files),
        "markdown_links_found": total_links,
        "local_links_checked": total_local_links,
        "missing_links": len(issues),
        "issues": issues,
    }


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Check local markdown links in active project scope."
    )
    parser.add_argument(
        "--root",
        default=None,
        help="Project root path (defaults to repository root by script location).",
    )
    parser.add_argument(
        "--output-json",
        default=None,
        help="Optional output file path for JSON report.",
    )
    args = parser.parse_args()

    if args.root:
        project_root = Path(args.root).resolve()
    else:
        project_root = Path(__file__).resolve().parents[2]

    result = scan_markdown_links(project_root)

    print(f"Markdown files scanned: {result['markdown_files_scanned']}")
    print(f"Markdown links found: {result['markdown_links_found']}")
    print(f"Local links checked: {result['local_links_checked']}")
    print(f"Missing local links: {result['missing_links']}")

    if args.output_json:
        output_path = Path(args.output_json)
        if not output_path.is_absolute():
            output_path = (project_root / output_path).resolve()
        output_path.parent.mkdir(parents=True, exist_ok=True)
        output_path.write_text(
            json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8"
        )
        print(f"JSON report written: {output_path}")

    if result["missing_links"] > 0:
        print("\nBroken links detected in active scope:")
        for issue in result["issues"][:200]:
            file_path = issue.get("file", "<unknown>")
            link = issue.get("link", "<unknown>")
            print(f"- {file_path} -> {link}")
        if len(result["issues"]) > 200:
            print(f"... and {len(result['issues']) - 200} more")
        return 1

    print("\nNo broken local links in active scope.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
