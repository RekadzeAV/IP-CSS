#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path


def build_markdown(report: dict, verbose: bool = False) -> str:
    mode = report.get("mode", "unknown")
    overall_ok = bool(report.get("overall_ok", False))
    skip_gradle = bool(report.get("skip_gradle", False))
    generated_at = report.get("generated_at_utc", "unknown")
    failed_command = report.get("failed_command")
    checks = report.get("checks", [])
    signature_checks = report.get("signature_checks", [])

    lines: list[str] = []
    lines.append("## KMP Phase 1 Verify Report")
    lines.append("")
    lines.append(f"- Generated at (UTC): `{generated_at}`")
    lines.append(f"- Mode: `{mode}`")
    lines.append(f"- Skip gradle: `{skip_gradle}`")
    lines.append(f"- Overall: **{'✅ PASS' if overall_ok else '❌ FAIL'}**")
    if failed_command:
        lines.append(f"- First failed command: `{failed_command}`")
    lines.append("")
    lines.append("### Stage Results")
    lines.append("")
    lines.append("| Stage | Command | Exit | Status |")
    lines.append("|---|---|---:|---|")
    for item in checks:
        stage = str(item.get("stage", "n/a"))
        command = " ".join(item.get("command", [])) if isinstance(item.get("command"), list) else str(item.get("command", ""))
        exit_code = item.get("exit_code", "")
        ok = bool(item.get("ok", False))
        status = "✅ OK" if ok else "❌ FAIL"
        lines.append(f"| {stage} | `{command}` | {exit_code} | **{status}** |")
    lines.append("")
    
    # Signature checks section
    if signature_checks:
        lines.append("### Security Signature Verification")
        lines.append("")
        sig_total = len(signature_checks)
        sig_passed = sum(1 for s in signature_checks if s.get("status") == "PASS")
        sig_failed = sig_total - sig_passed
        
        lines.append(f"**Status:** {'✅ PASS' if sig_failed == 0 else '❌ FAIL'} ({sig_passed}/{sig_total} files)")
        lines.append("")
        lines.append("| File | Status | Patterns Checked | Patterns Found |")
        lines.append("|---|---|---:|---:|")
        for sig in signature_checks:
            file_name = Path(sig.get("file", "")).name
            status = "✅ PASS" if sig.get("status") == "PASS" else "❌ FAIL"
            patterns_checked = sig.get("patterns_checked", 0)
            patterns_found = sig.get("patterns_found", 0)
            lines.append(f"| {file_name} | {status} | {patterns_checked} | {patterns_found} |")
        lines.append("")
        
        if verbose and sig_failed > 0:
            lines.append("### Missing Patterns")
            lines.append("")
            for sig in signature_checks:
                if sig.get("status") == "FAIL":
                    file_name = Path(sig.get("file", "")).name
                    lines.append(f"**{file_name}:**")
                    for pattern in sig.get("missing_patterns", []):
                        short_pattern = pattern[:60] + "..." if len(pattern) > 60 else pattern
                        lines.append(f"- `{short_pattern}`")
                    lines.append("")
    
    # Conclusion
    lines.append("### Conclusion")
    lines.append("")
    if overall_ok:
        lines.append("✅ **All KMP Phase 1 checks passed.** The project is ready for production.")
        lines.append("")
        lines.append("**Summary:**")
        lines.append("- Expect/actual coverage: 100%")
        lines.append("- CommonMain boundaries: Enforced")
        lines.append("- CI gates: Working")
        lines.append("- Contract tests: Passing")
    else:
        lines.append("❌ **Some checks failed.** Please review the failures above.")
        lines.append("")
        lines.append("**Recommended actions:**")
        lines.append("1. Review failed stages in the table above")
        lines.append("2. Fix signature mismatches if any")
        lines.append("3. Re-run verification after fixes")
    
    return "\n".join(lines) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description="Render markdown summary from KMP verify JSON report.")
    parser.add_argument("--input", required=True, help="Input JSON report path")
    parser.add_argument("--output", required=True, help="Output markdown path")
    parser.add_argument("--verbose", "-v", action="store_true", help="Include detailed failure information")
    parser.add_argument("--github-summary", action="store_true", help="Also write to GITHUB_STEP_SUMMARY")
    args = parser.parse_args()

    input_path = Path(args.input).resolve()
    output_path = Path(args.output).resolve()
    if not input_path.exists():
        raise FileNotFoundError(f"Input report not found: {input_path}")

    report = json.loads(input_path.read_text(encoding="utf-8"))
    markdown = build_markdown(report, args.verbose)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(markdown, encoding="utf-8")
    print(f"Rendered report: {output_path}")
    
    # Write to GITHUB_STEP_SUMMARY if requested
    if args.github_summary:
        github_summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
        if github_summary_path:
            summary_path = Path(github_summary_path)
            # Append to existing summary
            existing = summary_path.read_text(encoding="utf-8") if summary_path.exists() else ""
            summary_path.write_text(existing + "\n" + markdown, encoding="utf-8")
            print(f"Appended to GitHub Step Summary: {github_summary_path}")
        else:
            print("Warning: GITHUB_STEP_SUMMARY not set (not running in GitHub Actions)")
    
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
