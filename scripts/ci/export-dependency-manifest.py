#!/usr/bin/env python3
"""Экспорт манифеста зависимостей проекта IP-CSS для аудита уязвимостей.

Автономный (offline) шаг: парсит gradle/libs.versions.toml — единый источник версий,
и выдаёт список координат group:artifact:version в формате, пригодном для
сопоставления с базами CVE/NVD (или ручного сверения с advisory GitHub).

Использование:
    python scripts/ci/export-dependency-manifest.py [--output docs/dependency-manifest.csv] [--json]

Формат CSV: group,artifact,version,catalog_key
"""
from __future__ import annotations

import argparse
import csv
import io
import json
import re
import sys
from pathlib import Path

if sys.platform == "win32":
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8", errors="replace")

# Тома: координаты библиотек из [libraries]
LIB_PATTERN = re.compile(
    r"^([a-zA-Z0-9_-]+)\s*=\s*\{\s*module\s*=\s*[\"']([^:\"']+):([^\"']+)[\"']"
    r"(?:\s*,\s*version\.ref\s*=\s*[\"']([a-zA-Z0-9_-]+)[\"']|\s*,\s*version\s*=\s*[\"']([^\"']+)[\"'])?",
    re.MULTILINE,
)


def parse_versions(toml_text: str) -> dict:
    versions: dict = {}
    for m in re.finditer(r"^([a-zA-Z0-9_-]+)\s*=\s*\"([^\"]+)\"", toml_text, re.MULTILINE):
        versions[m.group(1)] = m.group(2)
    return versions


def parse_libraries(toml_text: str) -> list[dict]:
    libs: list[dict] = []
    for m in LIB_PATTERN.finditer(toml_text):
        key = m.group(1)
        group = m.group(2)
        artifact = m.group(3)
        version_ref = m.group(4)
        inline_version = m.group(5)
        libs.append(
            {
                "key": key,
                "group": group,
                "artifact": artifact,
                "version_ref": version_ref,
                "inline_version": inline_version,
            }
        )
    return libs


def main() -> int:
    parser = argparse.ArgumentParser(description="Экспорт манифеста зависимостей проекта.")
    parser.add_argument("--root", default=".", help="Корень репозитория")
    parser.add_argument("--output", default=None, help="CSV-файл для сохранения манифеста")
    parser.add_argument("--json", action="store_true", help="Вывод также в JSON")
    args = parser.parse_args()

    root = Path(args.root).resolve()
    toml_path = root / "gradle" / "libs.versions.toml"
    if not toml_path.exists():
        print(f"ERROR: {toml_path} не найден", file=sys.stderr)
        return 2

    toml_text = toml_path.read_text(encoding="utf-8")
    versions = parse_versions(toml_text)
    libs = parse_libraries(toml_text)

    rows = []
    for lib in libs:
        ver = lib["inline_version"] or versions.get(lib["version_ref"], "?")
        rows.append(
            {
                "group": lib["group"],
                "artifact": lib["artifact"],
                "version": ver,
                "catalog_key": lib["key"],
            }
        )

    # Дедупликация по group:artifact
    seen = set()
    unique = []
    for r in rows:
        coord = (r["group"], r["artifact"])
        if coord in seen:
            continue
        seen.add(coord)
        unique.append(r)

    print(f"=== Манифест зависимостей: {len(libs)} записей (catalog), {len(unique)} уникальных координат ===")

    if args.output:
        out_path = Path(args.output)
        out_path.parent.mkdir(parents=True, exist_ok=True)
        with open(out_path, "w", newline="", encoding="utf-8") as f:
            writer = csv.DictWriter(f, fieldnames=["group", "artifact", "version", "catalog_key"])
            writer.writeheader()
            writer.writerows(unique)
        print(f"CSV сохранён: {out_path}")

    if args.json:
        print(json.dumps(unique, ensure_ascii=False, indent=2))

    # Быстрый вывод на экран
    for r in unique:
        print(f"  {r['group']}:{r['artifact']}:{r['version']}  (catalog.{r['catalog_key']})")

    return 0


if __name__ == "__main__":
    sys.exit(main())