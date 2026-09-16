param(
    [string]$OutputDir = "docs/reports",
    [switch]$ShowHelp
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Run Phase 3 desktop auto-execution and publish status report."
    Write-Host "Usage: .\scripts\phase3-desktop-auto-execution.ps1"
    exit 0
}

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

$runScript = Join-Path $projectRoot "scripts\desktop-video-event-longrun-smoke.ps1"
if (-not (Test-Path $runScript)) {
    throw "Desktop smoke runner not found: $runScript"
}

$dateStamp = Get-Date -Format "yyyy-MM-dd"
$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
$outFile = Join-Path $projectRoot "$OutputDir\PHASE3_DESKTOP_AUTO_EXECUTION_STATUS_$dateStamp.md"
$diagOutput = "diagnostics\platform-smoke\desktop"
$diagAbs = Join-Path $projectRoot $diagOutput

if (-not (Test-Path $diagAbs)) {
    New-Item -ItemType Directory -Path $diagAbs -Force | Out-Null
}

& $runScript -CompileAndTestOnly -OutputDir $diagOutput
$desktopExit = $LASTEXITCODE

$overall = if ($desktopExit -eq 0) { "PASS" } elseif ($desktopExit -eq 3) { "PARTIAL" } else { "FAIL" }
$decision = if ($overall -eq "PASS") { "CONDITIONAL GO (Desktop compile/tests)" } else { "NO-GO" }

$lines = @(
    "# Phase 3 Desktop Auto-Execution Status ($dateStamp)",
    "",
    "## Execution Snapshot",
    "",
    "- Timestamp: $timestamp",
    "- Runner: scripts/desktop-video-event-longrun-smoke.ps1 -CompileAndTestOnly",
    "- Exit code: $desktopExit",
    "- Overall: $overall",
    "- Decision: $decision",
    "",
    "## Story 3.2 Coverage",
    "",
    "- 3.2.1 Live stability baseline: validated by desktop compile + shared/network desktop tests.",
    "- 3.2.2 Recordings/events UX runtime: PARTIAL (runtime long-run not included in compile-only mode).",
    "- 3.2.3 System integration (tray/autostart): PENDING (manual/OS-level acceptance required).",
    "- 3.2.4 ARM parity: PENDING (requires dedicated ARM smoke run).",
    "",
    "## Evidence",
    "",
    "- Diagnostics directory: $diagOutput",
    "- Related plan: docs/planning/PHASE_3_DETAILED_BACKLOG.md",
    "- Phase 3 NAS precheck: docs/reports/PHASE3_AUTO_EXECUTION_STATUS_$dateStamp.md"
)

Set-Content -Path $outFile -Value $lines -Encoding UTF8
Write-Host "Desktop Phase 3 auto report generated: $outFile" -ForegroundColor Green

if ($overall -eq "FAIL") { exit 1 }
if ($overall -eq "PARTIAL") { exit 3 }
exit 0
