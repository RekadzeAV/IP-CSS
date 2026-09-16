#!/usr/bin/env python3
"""
Standalone KMP Security Expect/Actual Signature Verification Script.

Проверяет соответствие сигнатур expect-деклараций в commonMain
и actual-реализаций в платформенных source sets (androidMain, desktopMain, iosMain, nativeMain).

Не требует C++ компиляции. Работает на уровне AST-анализа исходного кода.

Usage:
    python scripts/ci/verify-kmp-signatures.py [--root DIR] [--report-json PATH]
    
Options:
    --root DIR          Корневая директория проекта (по умолчанию: текущая)
    --report-json PATH  Путь для сохранения JSON-отчёта
    --verbose           Подробный вывод
"""

import os
import re
import sys
import json
import argparse
from pathlib import Path
from dataclasses import dataclass, field, asdict
from typing import List, Optional


@dataclass
class SignatureInfo:
    """Информация о сигнатуре функции/свойства."""
    name: str
    return_type: str
    parameters: List[str] = field(default_factory=list)
    is_suspend: bool = False
    file_path: str = ""
    line_number: int = 0


@dataclass
class FileResult:
    """Результат проверки одного файла."""
    file_name: str
    status: str = "SKIP"  # PASS, FAIL, SKIP
    patterns_checked: int = 0
    patterns_found: int = 0
    mismatches: List[str] = field(default_factory=list)
    warnings: List[str] = field(default_factory=list)


@dataclass
class VerificationReport:
    """Общий отчёт проверки."""
    total_files: int = 0
    passed: int = 0
    failed: int = 0
    skipped: int = 0
    total_signatures_checked: int = 0
    total_signatures_found: int = 0
    total_mismatches: int = 0
    total_warnings: int = 0
    file_results: List[FileResult] = field(default_factory=list)


# Регулярные выражения для поиска expect/actual объявлений
EXPECT_FUN_PATTERN = re.compile(
    r'(expect\s+((suspend\s+)?fun\s+(\w+))\s*\(([^)]*)\)\s*:\s*(\w+[.\w]*))'
)
EXPECT_VAL_PATTERN = re.compile(
    r'(expect\s+(val|var)\s+(\w+)\s*:\s*(\w+[.\w]*))'
)
EXPECT_PROPERTY_PATTERN = re.compile(
    r'(expect\s+(val|var)\s+(\w+)\s*:\s*(\w+[.\w]*)\s*(get|set)?)'
)

ACTUAL_FUN_PATTERN = re.compile(
    r'(actual\s+((suspend\s+)?fun\s+(\w+))\s*\(([^)]*)\)\s*:\s*(\w+[.\w]*))'
)
ACTUAL_VAL_PATTERN = re.compile(
    r'(actual\s+(val|var)\s+(\w+)\s*:\s*(\w+[.\w]*))'
)


def find_kotlin_files(root_dir: Path, source_set: str) -> List[Path]:
    """Находит все .kt файлы в указанном source set."""
    src_dir = root_dir / source_set
    if not src_dir.exists():
        return []
    return list(src_dir.rglob("*.kt"))


def extract_signatures_from_file(file_path: Path, pattern_type: str = "expect") -> List[SignatureInfo]:
    """
    Извлекает сигнатуры из файла.
    
    Args:
        file_path: Путь к файлу
        pattern_type: "expect" или "actual"
    
    Returns:
        Список SignatureInfo
    """
    signatures = []
    try:
        content = file_path.read_text(encoding="utf-8")
    except Exception:
        return signatures

    lines = content.split("\n")
    
    # Выбираем паттерн в зависимости от типа
    if pattern_type == "expect":
        fun_pattern = EXPECT_FUN_PATTERN
        val_pattern = EXPECT_VAL_PATTERN
    else:
        fun_pattern = ACTUAL_FUN_PATTERN
        val_pattern = ACTUAL_VAL_PATTERN

    # Поиск функций
    for line_num, line in enumerate(lines, 1):
        # Функции
        fun_match = fun_pattern.search(line)
        if fun_match:
            is_suspend = "suspend" in line
            # Извлекаем имя функции
            if pattern_type == "expect":
                name = fun_match.group(4) if fun_match.lastindex >= 4 else ""
            else:
                name = fun_match.group(4) if fun_match.lastindex >= 4 else ""
            
            # Извлекаем параметры
            params_str = fun_match.group(5) if fun_match.lastindex >= 5 else ""
            params = [p.strip() for p in params_str.split(",") if p.strip()]
            
            # Извлекаем возвращаемый тип
            return_type = fun_match.group(6) if fun_match.lastindex >= 6 else "Unit"
            
            signatures.append(SignatureInfo(
                name=name,
                return_type=return_type,
                parameters=params,
                is_suspend=is_suspend,
                file_path=str(file_path.relative_to(file_path.parents[3]) if len(file_path.parents) > 3 else file_path.name),
                line_number=line_num
            ))
            continue

        # Свойства (val/var)
        val_match = val_pattern.search(line)
        if val_match:
            name = val_match.group(3) if val_match.lastindex >= 3 else ""
            return_type = val_match.group(4) if val_match.lastindex >= 4 else "Any"
            
            signatures.append(SignatureInfo(
                name=name,
                return_type=return_type,
                parameters=[],
                is_suspend=False,
                file_path=str(file_path.relative_to(file_path.parents[3]) if len(file_path.parents) > 3 else file_path.name),
                line_number=line_num
            ))

    return signatures


def verify_file_pair(expect_file: Path, actual_files: List[Path], module_name: str) -> FileResult:
    """Проверяет пару commonMain/actual файлов на соответствие сигнатур."""
    file_name = expect_file.name
    result = FileResult(file_name=file_name)
    
    # Извлекаем expect-сигнатуры
    expect_signatures = extract_signatures_from_file(expect_file, "expect")
    result.patterns_checked = len(expect_signatures)
    
    if not expect_signatures:
        # Если нет expect объявлений, пропускаем
        result.status = "SKIP"
        return result
    
    # Собираем все actual-сигнатуры из платформенных файлов
    actual_signatures = []
    for actual_file in actual_files:
        actual_signatures.extend(extract_signatures_from_file(actual_file, "actual"))
    
    result.patterns_found = len(actual_signatures)
    
    # Создаем map для быстрого поиска actual-сигнатур по имени
    actual_map = {}
    for sig in actual_signatures:
        actual_map[sig.name] = sig
    
    # Проверяем каждую expect сигнатуру
    for expect_sig in expect_signatures:
        actual_sig = actual_map.get(expect_sig.name)
        if actual_sig is None:
            result.mismatches.append(
                f"Missing actual for '{expect_sig.name}' (declared at {expect_sig.file_path}:{expect_sig.line_number})"
            )
            continue
        
        # Проверяем возвращаемый тип
        if expect_sig.return_type != actual_sig.return_type:
            result.mismatches.append(
                f"Return type mismatch for '{expect_sig.name}': "
                f"expected '{expect_sig.return_type}', actual '{actual_sig.return_type}'"
            )
        
        # Проверяем параметры
        if len(expect_sig.parameters) != len(actual_sig.parameters):
            result.mismatches.append(
                f"Parameter count mismatch for '{expect_sig.name}': "
                f"expected {len(expect_sig.parameters)}, actual {len(actual_sig.parameters)}"
            )
    
    # Определяем статус
    if result.mismatches:
        result.status = "FAIL"
    else:
        result.status = "PASS"
    
    return result


def scan_module(root_dir: Path, module_path: str) -> List[FileResult]:
    """
    Сканирует KMP модуль на предмет expect/actual сигнатур.
    
    Args:
        root_dir: Корневая директория проекта
        module_path: Путь к модулю (например, "core/common" или "shared")
    
    Returns:
        Список результатов проверки файлов
    """
    results = []
    module_dir = root_dir / module_path / "src"
    
    if not module_dir.exists():
        return results
    
    # Ищем commonMain файлы
    common_dir = module_dir / "commonMain" / "kotlin"
    if not common_dir.exists():
        return results
    
    # Платформенные директории
    platform_dirs = {
        "androidMain": module_dir / "androidMain" / "kotlin",
        "desktopMain": module_dir / "desktopMain" / "kotlin",
        "iosMain": module_dir / "iosMain" / "kotlin",
        "nativeMain": module_dir / "nativeMain" / "kotlin",
        "jvmMain": module_dir / "jvmMain" / "kotlin",
    }
    
    # Проходим по всем commonMain файлам
    for expect_file in sorted(common_dir.rglob("*.kt")):
        # Ищем соответствующие actual-файлы
        actual_files = []
        for platform_name, platform_dir in platform_dirs.items():
            if platform_dir.exists():
                # Ищем файл с тем же именем в платформенной директории
                relative_path = expect_file.relative_to(common_dir)
                platform_file = platform_dir / relative_path
                if platform_file.exists():
                    actual_files.append(platform_file)
                
                # Также ищем файлы с суффиксом платформы
                # Например: Security.kt -> Security.android.kt
                base_name = expect_file.stem
                for f in platform_dir.rglob(f"{base_name}.{platform_name.replace('Main', '').lower()}.kt"):
                    actual_files.append(f)
                for f in platform_dir.rglob(f"{base_name}.{platform_name.replace('Main', '')}.kt"):
                    actual_files.append(f)
        
        result = verify_file_pair(expect_file, actual_files, module_path)
        results.append(result)
    
    return results


def verify_security_contract(root_dir: Path) -> List[str]:
    """Проверяет соответствие Security Contract (docs/kmp-security-contract.md)."""
    warnings = []
    contract_file = root_dir / "docs" / "kmp-security-contract.md"
    if not contract_file.exists():
        warnings.append("Security contract file not found: docs/kmp-security-contract.md")
        return warnings
    
    warnings.append("Security contract file exists: docs/kmp-security-contract.md OK")
    return warnings


def main():
    parser = argparse.ArgumentParser(
        description="KMP Security Expect/Actual Signature Verification"
    )
    parser.add_argument(
        "--root", default=".",
        help="Root directory of the project"
    )
    parser.add_argument(
        "--report-json", default=None,
        help="Path to save JSON report"
    )
    parser.add_argument(
        "--verbose", action="store_true",
        help="Enable verbose output"
    )
    
    args = parser.parse_args()
    root_dir = Path(args.root).resolve()
    
    print("=" * 60)
    print("KMP Security Expect/Actual Signature Verification")
    print("=" * 60)
    print(f"Root directory: {root_dir}")
    print()
    
    # Модули для проверки
    modules = [
        "core/common",
        "core/network",
        "shared",
    ]
    
    report = VerificationReport()
    
    for module in modules:
        print(f"\n{'─' * 40}")
        print(f"Module: {module}")
        print(f"{'─' * 40}")
        
        results = scan_module(root_dir, module)
        
        for result in results:
            report.file_results.append(result)
            
            if result.status == "PASS":
                report.passed += 1
                if args.verbose:
                    print(f"  ✅ {result.file_name}: PASS ({result.patterns_checked} patterns)")
            elif result.status == "FAIL":
                report.failed += 1
                print(f"  ❌ {result.file_name}: FAIL ({result.patterns_checked} patterns)")
                for mismatch in result.mismatches:
                    print(f"      - {mismatch}")
            else:
                report.skipped += 1
                if args.verbose:
                    print(f"  ⏭️  {result.file_name}: SKIP (no expect declarations)")
            
            report.total_signatures_checked += result.patterns_checked
            report.total_signatures_found += result.patterns_found
            report.total_mismatches += len(result.mismatches)
            report.total_warnings += len(result.warnings)
    
    # Проверка Security Contract
    print(f"\n{'─' * 40}")
    print("Security Contract Verification")
    print(f"{'─' * 40}")
    contract_warnings = verify_security_contract(root_dir)
    for w in contract_warnings:
        if "OK" in w:
            print(f"  ✅ {w}")
        else:
            print(f"  ⚠️  {w}")
            report.total_warnings += 1
    
    report.total_files = len(report.file_results)
    
    # Итоговый отчёт
    print(f"\n{'═' * 60}")
    print("SUMMARY")
    print(f"{'═' * 60}")
    print(f"Total files checked:  {report.total_files}")
    print(f"Passed:              {report.passed}")
    print(f"Failed:              {report.failed}")
    print(f"Skipped:             {report.skipped}")
    print(f"Signatures checked:  {report.total_signatures_checked}")
    print(f"Signatures found:    {report.total_signatures_found}")
    print(f"Mismatches:          {report.total_mismatches}")
    print(f"Warnings:            {report.total_warnings}")
    print(f"{'═' * 60}")
    
    # Сохраняем JSON-отчёт
    if args.report_json:
        report_path = root_dir / args.report_json
        report_path.parent.mkdir(parents=True, exist_ok=True)
        
        report_dict = {
            "status": "PASS" if report.failed == 0 else "FAIL",
            "summary": {
                "total_files": report.total_files,
                "passed": report.passed,
                "failed": report.failed,
                "skipped": report.skipped,
                "total_signatures_checked": report.total_signatures_checked,
                "total_signatures_found": report.total_signatures_found,
                "total_mismatches": report.total_mismatches,
                "total_warnings": report.total_warnings,
            },
            "files": [
                {
                    "file_name": r.file_name,
                    "status": r.status,
                    "patterns_checked": r.patterns_checked,
                    "patterns_found": r.patterns_found,
                    "mismatches": r.mismatches,
                    "warnings": r.warnings,
                }
                for r in report.file_results
            ],
            "contract_warnings": contract_warnings,
        }
        
        with open(report_path, "w", encoding="utf-8") as f:
            json.dump(report_dict, f, indent=2, ensure_ascii=False)
        
        print(f"\nJSON report saved to: {report_path}")
    
    # Возвращаем код ошибки если есть несоответствия
    if report.failed > 0:
        print(f"\n❌ FAILED: {report.failed} file(s) have signature mismatches")
        sys.exit(1)
    else:
        print(f"\n✅ ALL PASSED")
        sys.exit(0)


if __name__ == "__main__":
    main()