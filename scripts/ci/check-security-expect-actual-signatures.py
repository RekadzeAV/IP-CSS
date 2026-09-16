#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import TypedDict

# Fix Unicode encoding for Windows
if sys.platform == "win32":
    import codecs
    sys.stdout = codecs.getwriter("utf-8")(sys.stdout.buffer, "strict")
    sys.stderr = codecs.getwriter("utf-8")(sys.stderr.buffer, "strict")


class SignatureInfo(TypedDict):
    """Информация о сигнатуре метода."""
    method_name: str
    params: str
    return_type: str
    modifiers: str


class CheckResult(TypedDict):
    """Результат проверки одного файла."""
    file: str
    status: str  # "PASS" or "FAIL"
    patterns_checked: int
    patterns_found: int
    missing_patterns: list[str]
    signature_mismatches: list[str]  # Новый: несоответствия сигнатур
    warnings: list[str]  # Новый: предупреждения


def contains(path: Path, pattern: str) -> bool:
    if not path.exists():
        return False
    text = path.read_text(encoding="utf-8", errors="ignore")
    return re.search(pattern, text, flags=re.MULTILINE) is not None


def find_all_matches(path: Path, pattern: str) -> list[str]:
    """Найти все совпадения паттерна в файле."""
    if not path.exists():
        return []
    text = path.read_text(encoding="utf-8", errors="ignore")
    matches = re.findall(pattern, text, flags=re.MULTILINE)
    return matches


def extract_method_signatures(text: str) -> list[SignatureInfo]:
    """Извлечь сигнатуры методов из Kotlin кода."""
    signatures = []
    
    # Паттерн для функций: fun methodName(params): ReturnType
    pattern = r'(?:override\s+)?(open|private|internal)?\s*fun\s+(\w+)\s*\(([^)]*)\)\s*:\s*(\w+)'
    
    for match in re.finditer(pattern, text):
        modifiers, name, params, return_type = match.groups()
        signatures.append({
            "method_name": name,
            "params": params.strip(),
            "return_type": return_type,
            "modifiers": modifiers or ""
        })
    
    return signatures


def compare_signatures(expect_sigs: list[SignatureInfo], actual_sigs: list[SignatureInfo]) -> list[str]:
    """Сравнить сигнатуры expect/actual и найти несоответствия."""
    mismatches = []
    
    expect_map = {sig["method_name"]: sig for sig in expect_sigs}
    actual_map = {sig["method_name"]: sig for sig in actual_sigs}
    
    # Проверка на отсутствующие методы
    for name, sig in expect_map.items():
        if name not in actual_map:
            mismatches.append(f"Missing actual method: {name}({sig['params']}): {sig['return_type']}")
        else:
            actual_sig = actual_map[name]
            # Проверка параметров
            if sig["params"] != actual_sig["params"]:
                mismatches.append(
                    f"Parameter mismatch for {name}:\n"
                    f"  expect: {sig['params']}\n"
                    f"  actual: {actual_sig['params']}"
                )
            # Проверка возвращаемого типа
            if sig["return_type"] != actual_sig["return_type"]:
                mismatches.append(
                    f"Return type mismatch for {name}:\n"
                    f"  expect: {sig['return_type']}\n"
                    f"  actual: {actual_sig['return_type']}"
                )
    
    # Проверка на лишние методы
    for name in actual_map:
        if name not in expect_map:
            mismatches.append(f"Extra actual method (not in expect): {name}")
    
    return mismatches


def check_file(path: Path, patterns: list[str]) -> CheckResult:
    """Проверить файл на наличие всех паттернов."""
    result: CheckResult = {
        "file": str(path),
        "status": "PASS",
        "patterns_checked": len(patterns),
        "patterns_found": 0,
        "missing_patterns": [],
        "signature_mismatches": [],
        "warnings": []
    }
    
    if not path.exists():
        result["status"] = "FAIL"
        result["missing_patterns"].append(f"FILE MISSING")
        result["warnings"].append(f"File not found: {path.name}")
        return result
    
    text = path.read_text(encoding="utf-8", errors="ignore")
    
    for pattern in patterns:
        if contains(path, pattern):
            result["patterns_found"] += 1
        else:
            result["status"] = "FAIL"
            result["missing_patterns"].append(pattern)
    
    return result


def generate_markdown_report(results: list[CheckResult], verbose: bool = False) -> str:
    """Генерировать markdown отчет по результатам проверок."""
    lines = ["## KMP Security Expect/Actual Signature Verification Report\n"]
    
    # Summary
    total = len(results)
    passed = sum(1 for r in results if r["status"] == "PASS")
    failed = total - passed
    
    # Считаем общие метрики
    total_checks = sum(r["patterns_checked"] for r in results)
    total_found = sum(r["patterns_found"] for r in results)
    total_mismatches = sum(len(r.get("signature_mismatches", [])) for r in results)
    total_warnings = sum(len(r.get("warnings", [])) for r in results)
    
    lines.append(f"\n**Status:** {'✅ PASS' if failed == 0 else '❌ FAIL'} ({passed}/{total} files)\n")
    lines.append("\n### Summary\n")
    lines.append(f"| Metric | Value |")
    lines.append(f"|--------|-------|")
    lines.append(f"| Total files checked | {total} |")
    lines.append(f"| Passed | {passed} |")
    lines.append(f"| Failed | {failed} |")
    lines.append(f"| Total signatures checked | {total_checks} |")
    lines.append(f"| Signatures found | {total_found} |")
    lines.append(f"| Signature mismatches | {total_mismatches} |")
    lines.append(f"| Warnings | {total_warnings} |")
    
    # Detailed results
    lines.append("\n### Detailed Results\n")
    
    for result in results:
        status_icon = "✅" if result["status"] == "PASS" else "❌"
        file_path = Path(result['file'])
        lines.append(f"\n#### {status_icon} `{file_path.name}`\n")
        lines.append(f"- **Status:** {result['status']}")
        lines.append(f"- **Patterns checked:** {result['patterns_checked']}")
        lines.append(f"- **Patterns found:** {result['patterns_found']}")
        
        # Signature mismatches (если есть)
        if result.get("signature_mismatches"):
            lines.append(f"- **Signature mismatches:** {len(result['signature_mismatches'])}")
            for mismatch in result['signature_mismatches']:
                # Экранирование markdown
                mismatch_escaped = mismatch.replace("`", "\\`").replace("|", "\\|")
                lines.append(f"  - `{mismatch_escaped}`")
        
        # Warnings (если есть и verbose или есть warnings)
        if result.get("warnings") and (verbose or result["warnings"]):
            lines.append(f"- **Warnings:**")
            for warning in result['warnings']:
                lines.append(f"  - ⚠️ {warning}")
        
        if verbose or result["status"] == "FAIL":
            if result["missing_patterns"]:
                lines.append(f"- **Missing patterns:**")
                for pattern in result["missing_patterns"]:
                    if pattern != "FILE MISSING":
                        # Укоротить паттерн для отображения
                        short_pattern = pattern[:80] + "..." if len(pattern) > 80 else pattern
                        lines.append(f"  - `{short_pattern}`")
                    else:
                        lines.append(f"  - **FILE NOT FOUND**")
    
    # Platform coverage matrix
    lines.append("\n### Platform Coverage Matrix\n")
    lines.append("| Class | commonMain | android | desktop/jvm | ios | native |")
    lines.append("|-------|------------|---------|-------------|-----|--------|")
    
    # Проверяем coverage для security классов
    security_classes = [
        ("SecureLocalDataEncryption", "core/common"),
        ("SecurePasswordEncryption", "core/common"),
        ("SecureMobileSecurityLogger", "core/common"),
        ("DigestCrypto", "core/network"),
    ]
    
    for class_name, base_path in security_classes:
        coverage = []
        platforms = ["commonMain", "android", "desktop/jvm", "ios", "native"]
        for platform in platforms:
            if platform == "commonMain":
                path = Path(f"{base_path}/src/commonMain/kotlin")
            elif platform == "android":
                path = Path(f"{base_path}/src/androidMain/kotlin")
            elif platform == "desktop/jvm":
                path = Path(f"{base_path}/src/desktopMain/kotlin")
            elif platform == "ios":
                path = Path(f"{base_path}/src/iosMain/kotlin")
            else:  # native
                path = Path(f"{base_path}/src/nativeMain/kotlin")
            
            # Проверяем наличие файла
            files = list(path.glob(f"*{class_name}*.kt")) if path.exists() else []
            coverage.append("✅" if files else "❌")
        
        short_name = class_name.replace("Secure", "").replace("Crypto", "")
        lines.append(f"| {short_name} | " + " | ".join(coverage) + " |")
    
    # Conclusion
    lines.append("\n### Conclusion\n")
    if failed == 0 and total_mismatches == 0:
        lines.append("✅ All expect/actual signature checks passed. KMP security modules are properly aligned across platforms.")
        if total_warnings > 0:
            lines.append(f"\n⚠️ {total_warnings} warning(s) found. Review the warnings above for potential issues.")
    else:
        lines.append(f"❌ {failed} file(s) have signature mismatches. Please review the missing patterns above.")
        if total_mismatches > 0:
            lines.append(f"\n⚠️ {total_mismatches} signature mismatch(es) detected. Review the detailed results above.")
        lines.append("\n**Recommended actions:**")
        lines.append("1. Review the expect declarations in commonMain")
        lines.append("2. Verify actual implementations match the expect signatures")
        lines.append("3. Ensure all platform-specific files are present")
        lines.append("4. Check for parameter type mismatches")
    
    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser(description="Check KMP security expect/actual signatures")
    parser.add_argument("root", nargs="?", default=".", help="Root directory of the project")
    parser.add_argument("--verbose", "-v", action="store_true", help="Show detailed output")
    parser.add_argument("--markdown", "-m", action="store_true", help="Generate markdown report")
    parser.add_argument("--json", "-j", action="store_true", help="Output results as JSON")
    parser.add_argument("--output", "-o", type=str, help="Output file for markdown report")
    args = parser.parse_args()
    
    root = Path(args.root)
    results: list[CheckResult] = []
    
    # Helper to add results
    def add_results(path: Path, patterns: list[str]):
        result = check_file(path, patterns)
        results.append(result)
    
    # LocalDataEncryption
    print("Checking LocalDataEncryption...")
    add_results(
        root / "core/common/src/commonMain/kotlin/com/company/ipcamera/core/common/security/LocalDataEncryption.kt",
        [
            r"expect class SecureLocalDataEncryption\(\)\s*:\s*LocalDataEncryption",
            r"override fun encrypt\(data: ByteArray\): ByteArray",
            r"override fun decrypt\(encryptedData: ByteArray\): ByteArray",
            r"override fun encryptString\(data: String\): String",
            r"override fun decryptString\(encryptedData: String\): String",
            r"override fun isEncrypted\(data: ByteArray\): Boolean",
        ],
    )

    for p in [
        "core/common/src/androidMain/kotlin/com/company/ipcamera/core/common/security/LocalDataEncryption.android.kt",
        "core/common/src/desktopMain/kotlin/com/company/ipcamera/core/common/security/LocalDataEncryption.jvm.kt",
        "core/common/src/iosMain/kotlin/com/company/ipcamera/core/common/security/LocalDataEncryption.ios.kt",
        "core/common/src/nativeMain/kotlin/com/company/ipcamera/core/common/security/LocalDataEncryption.native.kt",
    ]:
        add_results(
            root / p,
            [
                r"actual class SecureLocalDataEncryption",
                r"actual override fun encrypt\(data: ByteArray\): ByteArray",
                r"actual override fun decrypt\(encryptedData: ByteArray\): ByteArray",
                r"actual override fun encryptString\(data: String\): String",
                r"actual override fun decryptString\(encryptedData: String\): String",
                r"actual override fun isEncrypted\(data: ByteArray\): Boolean",
            ],
        )

    # PasswordEncryption
    print("Checking PasswordEncryption...")
    add_results(
        root / "core/common/src/commonMain/kotlin/com/company/ipcamera/core/common/security/PasswordEncryption.kt",
        [
            r"expect class SecurePasswordEncryption\(\)\s*:\s*PasswordEncryption",
            r"override fun encrypt\(password: String\): String",
            r"override fun decrypt\(encryptedPassword: String\): String",
            r"override fun isEncrypted\(value: String\): Boolean",
        ],
    )

    for p in [
        "core/common/src/androidMain/kotlin/com/company/ipcamera/core/common/security/PasswordEncryption.android.kt",
        "core/common/src/desktopMain/kotlin/com/company/ipcamera/core/common/security/PasswordEncryption.jvm.kt",
        "core/common/src/iosMain/kotlin/com/company/ipcamera/core/common/security/PasswordEncryption.ios.kt",
        "core/common/src/nativeMain/kotlin/com/company/ipcamera/core/common/security/PasswordEncryption.native.kt",
    ]:
        add_results(
            root / p,
            [
                r"actual class SecurePasswordEncryption",
                r"actual override fun encrypt\(password: String\): String",
                r"actual override fun decrypt\(encryptedPassword: String\): String",
                r"actual override fun isEncrypted\(value: String\): Boolean",
            ],
        )

    # MobileSecurityLogger
    print("Checking MobileSecurityLogger...")
    add_results(
        root / "core/common/src/commonMain/kotlin/com/company/ipcamera/core/common/security/MobileSecurityLogger.kt",
        [
            r"expect class SecureMobileSecurityLogger\(\)\s*:\s*MobileSecurityLogger",
            r"override fun log\(event: MobileSecurityEvent\)",
        ],
    )

    for p in [
        "core/common/src/androidMain/kotlin/com/company/ipcamera/core/common/security/MobileSecurityLogger.android.kt",
        "core/common/src/desktopMain/kotlin/com/company/ipcamera/core/common/security/MobileSecurityLogger.jvm.kt",
        "core/common/src/iosMain/kotlin/com/company/ipcamera/core/common/security/MobileSecurityLogger.ios.kt",
        "core/common/src/nativeMain/kotlin/com/company/ipcamera/core/common/security/MobileSecurityLogger.native.kt",
    ]:
        add_results(
            root / p,
            [
                r"actual class SecureMobileSecurityLogger",
                r"actual override fun log\(event: MobileSecurityEvent\)",
            ],
        )

    # DigestCrypto
    print("Checking DigestCrypto...")
    add_results(
        root / "core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/auth/DigestCrypto.kt",
        [
            r"expect object DigestCrypto",
            r"fun md5Hex\(input: String\): String",
            r"fun sha256Hex\(input: String\): String",
            r"fun secureRandomHex\(byteCount: Int = 16\): String",
        ],
    )

    # NB: в core/network иерархия KMP даёт Android-таргету актуалы из jvmMain,
    # поэтому отдельный DigestCrypto.android.kt не требуется (проверяется через jvmMain ниже).
    for p in [
        "core/network/src/jvmMain/kotlin/com/company/ipcamera/core/network/auth/DigestCrypto.jvm.kt",
        "core/network/src/iosMain/kotlin/com/company/ipcamera/core/network/auth/DigestCrypto.ios.kt",
        "core/network/src/nativeLinuxMain/kotlin/com/company/ipcamera/core/network/auth/DigestCrypto.nativeLinux.kt",
        "core/network/src/nativeMacosArm64Main/kotlin/com/company/ipcamera/core/network/auth/DigestCrypto.nativeMacos.kt",
        "core/network/src/nativeMacosX64Main/kotlin/com/company/ipcamera/core/network/auth/DigestCrypto.nativeMacos.kt",
        "core/network/src/nativeWindowsMain/kotlin/com/company/ipcamera/core/network/auth/DigestCrypto.nativeWindows.kt",
    ]:
        add_results(
            root / p,
            [
                r"actual object DigestCrypto",
                r"actual fun md5Hex\(input: String\): String",
                r"actual fun sha256Hex\(input: String\): String",
                r"actual fun secureRandomHex\(byteCount: Int\): String",
            ],
        )

    # Print results
    total = len(results)
    passed = sum(1 for r in results if r["status"] == "PASS")
    failed = total - passed
    
    print(f"\nResults: {passed}/{total} files passed")
    
    # Print failures
    if failed > 0:
        print("\nFailed checks:")
        for result in results:
            if result["status"] == "FAIL":
                print(f"\n  ❌ {Path(result['file']).name}")
                if result["missing_patterns"]:
                    for pattern in result["missing_patterns"]:
                        if pattern != "FILE MISSING":
                            short = pattern[:60] + "..." if len(pattern) > 60 else pattern
                            print(f"     Missing: `{short}`")
                        else:
                            print(f"     **FILE NOT FOUND**")
    
    # JSON output
    if args.json:
        print("\n" + json.dumps(results, indent=2))
    
    # Markdown output
    if args.markdown:
        md_report = generate_markdown_report(results, args.verbose)
        if args.output:
            Path(args.output).write_text(md_report, encoding="utf-8")
            print(f"\nMarkdown report written to: {args.output}")
        else:
            print("\n--- Markdown Report ---\n")
            print(md_report)
    
    # Final status
    if failures := [r for r in results if r["status"] == "FAIL"]:
        if not args.json:
            print("\n❌ Security expect/actual signature checks FAILED")
        return 1
    else:
        print("\n✅ All security expect/actual signature checks PASSED")
        return 0


if __name__ == "__main__":
    raise SystemExit(main())
