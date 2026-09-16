[CmdletBinding()]
param(
    [switch]$ShowHelp,
    [string]$Date = (Get-Date -Format "yyyy-MM-dd"),
    [string]$ReportsDir = "docs/reports"
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Validate NAS field reports readiness for finalization."
    Write-Host "Usage: .\scripts\nas-field-readiness-check.ps1 -Date 2026-04-27"
    Write-Host "Exit codes: 0=ready, 2=reports missing or pending placeholders"
    exit 0
}

function Resolve-ProjectPath {
    param([string]$PathValue)
    $projectRoot = Split-Path -Parent $PSScriptRoot
    if ([System.IO.Path]::IsPathRooted($PathValue)) { return $PathValue }
    return (Join-Path $projectRoot $PathValue)
}

function Get-ScenarioValue {
    param([string]$RawContent, [string]$ScenarioId)
    $m = [regex]::Match($RawContent, "(?m)^- $ScenarioId [^:]*:\s*(.+?)\s*$")
    if ($m.Success) { return $m.Groups[1].Value.Trim() }
    return "MISSING"
}

function Test-PendingValue {
    param([string]$Value)
    if ($Value -match "PENDING|TBD|UNKNOWN|MISSING|PRECHECK PASS") { return $true }
    if ($Value -match "\|") { return $true }
    return $false
}

$reportsAbs = Resolve-ProjectPath -PathValue $ReportsDir
$reports = @(
    @{ Platform = "Synology"; Path = Join-Path $reportsAbs "NAS_FIELD_REPORT_SYNOLOGY_$Date.md" },
    @{ Platform = "QNAP"; Path = Join-Path $reportsAbs "NAS_FIELD_REPORT_QNAP_$Date.md" },
    @{ Platform = "Asustor"; Path = Join-Path $reportsAbs "NAS_FIELD_REPORT_ASUSTOR_$Date.md" },
    @{ Platform = "TrueNAS"; Path = Join-Path $reportsAbs "NAS_FIELD_REPORT_TRUENAS_$Date.md" }
)

$issues = New-Object System.Collections.Generic.List[string]

foreach ($r in $reports) {
    if (-not (Test-Path -LiteralPath $r.Path)) {
        $issues.Add("$($r.Platform): report missing ($($r.Path))")
        continue
    }

    $raw = Get-Content -LiteralPath $r.Path -Raw
    foreach ($scenario in @("S2", "S3", "S4", "S5", "S6")) {
        $value = Get-ScenarioValue -RawContent $raw -ScenarioId $scenario
        if (Test-PendingValue -Value $value) {
            $issues.Add("$($r.Platform): $scenario is not finalized ('$value')")
        }
    }

    $decisionMatch = [regex]::Match($raw, "(?m)^- Result:\s*(.+?)\s*$")
    $decision = if ($decisionMatch.Success) { $decisionMatch.Groups[1].Value.Trim() } else { "MISSING" }
    if ($decision -match "PRELIMINARY|PENDING|TBD|UNKNOWN|MISSING|\|") {
        $issues.Add("$($r.Platform): Result is not finalized ('$decision')")
    }
}

if ($issues.Count -gt 0) {
    Write-Host "NAS field readiness check: NOT READY" -ForegroundColor Yellow
    Write-Host "Finalize the following items before final GO/NO-GO aggregation:" -ForegroundColor Yellow
    foreach ($issue in $issues) {
        Write-Host " - $issue"
    }
    exit 2
}

Write-Host "NAS field readiness check: READY" -ForegroundColor Green
Write-Host "All reports contain finalized S2-S6 and Result values."
exit 0

