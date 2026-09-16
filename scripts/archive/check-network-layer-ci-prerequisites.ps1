param(
    [switch]$RequireLiveChecks,
    [switch]$RequireStatusSyncAudit,
    [switch]$LocalSmoke,
    [string]$ReportDir = "diagnostics/network-layer-ci-prerequisites",
    [switch]$AsJson,
    [switch]$ShowHelp
)

if ($ShowHelp) {
    Write-Host "Check prerequisites for network-layer CI gates."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\check-network-layer-ci-prerequisites.ps1 -ShowHelp"
    Write-Host "  .\scripts\check-network-layer-ci-prerequisites.ps1 [-RequireLiveChecks] [-RequireStatusSyncAudit] [-LocalSmoke] [-ReportDir <dir>] [-AsJson]"
    Write-Host ""
    Write-Host "What it checks:"
    Write-Host "  - Repo variable emulation via environment:"
    Write-Host "      ENABLE_NETWORK_LAYER_LIVE_CHECKS"
    Write-Host "      ENABLE_NETWORK_LAYER_STATUS_SYNC_AUDIT"
    Write-Host "  - Required secrets emulation via environment for live checks:"
    Write-Host "      TEST_RTSP_URL, TEST_RTSP_USERNAME, TEST_RTSP_PASSWORD"
    Write-Host "      TEST_WEBSOCKET_URL, TEST_WEBSOCKET_AUTH_TOKEN, TEST_WEBSOCKET_SUBSCRIBE_CHANNEL"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0 - prerequisites satisfied for requested checks"
    Write-Host "  1 - missing required values"
    exit 0
}

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$runId = Get-Date -Format "yyyyMMdd-HHmmss"
$resolvedReportRoot = if ([System.IO.Path]::IsPathRooted($ReportDir)) { $ReportDir } else { Join-Path $projectRoot $ReportDir }
$resolvedRunDir = Join-Path $resolvedReportRoot $runId
New-Item -ItemType Directory -Path $resolvedRunDir -Force | Out-Null

function Test-EnabledFlag([string]$value) {
    return $value -and $value.Equals("true", [System.StringComparison]::OrdinalIgnoreCase)
}

$liveVar = $env:ENABLE_NETWORK_LAYER_LIVE_CHECKS
$syncVar = $env:ENABLE_NETWORK_LAYER_STATUS_SYNC_AUDIT
$liveEnabled = Test-EnabledFlag $liveVar
$syncEnabled = Test-EnabledFlag $syncVar

$missing = New-Object System.Collections.Generic.List[string]
$warnings = New-Object System.Collections.Generic.List[string]

if ($RequireLiveChecks -and -not $liveEnabled) {
    $missing.Add("ENABLE_NETWORK_LAYER_LIVE_CHECKS=true")
}
if ($RequireStatusSyncAudit -and -not $syncEnabled) {
    $missing.Add("ENABLE_NETWORK_LAYER_STATUS_SYNC_AUDIT=true")
}

$liveSecretKeys = @(
    "TEST_RTSP_URL",
    "TEST_RTSP_USERNAME",
    "TEST_RTSP_PASSWORD",
    "TEST_WEBSOCKET_URL",
    "TEST_WEBSOCKET_AUTH_TOKEN",
    "TEST_WEBSOCKET_SUBSCRIBE_CHANNEL"
)

if ($RequireLiveChecks) {
    foreach ($key in $liveSecretKeys) {
        $v = [Environment]::GetEnvironmentVariable($key)
        if ([string]::IsNullOrWhiteSpace($v)) {
            $missing.Add($key)
        }
    }
} elseif ($liveEnabled) {
    foreach ($key in $liveSecretKeys) {
        $v = [Environment]::GetEnvironmentVariable($key)
        if ([string]::IsNullOrWhiteSpace($v)) {
            $warnings.Add("Live checks enabled but missing: $key")
        }
    }
}

$mode = if ($LocalSmoke) { "LOCAL_SMOKE" } else { "STRICT" }
$status = if ($missing.Count -eq 0) {
    "PASS"
} elseif ($LocalSmoke) {
    "WARN"
} else {
    "FAIL"
}
if ($LocalSmoke -and $missing.Count -gt 0) {
    $warnings.Add("LocalSmoke mode: missing values are reported as WARN and do not fail the script")
}

$report = [PSCustomObject]@{
    runId = $runId
    generatedAt = (Get-Date).ToString("s")
    status = $status
    mode = $mode
    required = [PSCustomObject]@{
        requireLiveChecks = [bool]$RequireLiveChecks
        requireStatusSyncAudit = [bool]$RequireStatusSyncAudit
    }
    detected = [PSCustomObject]@{
        enableNetworkLayerLiveChecks = $liveVar
        enableNetworkLayerStatusSyncAudit = $syncVar
    }
    missing = $missing.ToArray()
    warnings = $warnings.ToArray()
}

$jsonPath = Join-Path $resolvedRunDir "network-layer-ci-prerequisites.json"
$mdPath = Join-Path $resolvedRunDir "network-layer-ci-prerequisites.md"
$report | ConvertTo-Json -Depth 6 | Set-Content -Path $jsonPath -Encoding UTF8

$md = @()
$md += "# Network Layer CI Prerequisites"
$md += ""
$md += "- runId: $runId"
$md += "- status: $status"
$md += "- mode: $mode"
$md += "- generatedAt: $($report.generatedAt)"
$md += ""
$md += "## Required Modes"
$md += ""
$md += "- requireLiveChecks: $([bool]$RequireLiveChecks)"
$md += "- requireStatusSyncAudit: $([bool]$RequireStatusSyncAudit)"
$md += ""
$md += "## Detected Toggles"
$md += ""
$md += "- ENABLE_NETWORK_LAYER_LIVE_CHECKS: $liveVar"
$md += "- ENABLE_NETWORK_LAYER_STATUS_SYNC_AUDIT: $syncVar"
$md += ""
$md += "## Missing"
$md += ""
if ($missing.Count -eq 0) {
    $md += "- none"
} else {
    foreach ($m in $missing) { $md += "- $m" }
}
$md += ""
$md += "## Warnings"
$md += ""
if ($warnings.Count -eq 0) {
    $md += "- none"
} else {
    foreach ($w in $warnings) { $md += "- $w" }
}

$md | Set-Content -Path $mdPath -Encoding UTF8

if ($AsJson) {
    $report | ConvertTo-Json -Depth 6
} else {
    Write-Host "Prerequisite status: $status" -ForegroundColor $(if ($status -eq "PASS") { "Green" } else { "Red" })
    Write-Host "Report JSON: $jsonPath" -ForegroundColor DarkCyan
    Write-Host "Report MD:   $mdPath" -ForegroundColor DarkCyan
    if ($warnings.Count -gt 0) {
        Write-Host "Warnings: $($warnings.Count)" -ForegroundColor Yellow
    }
}

if ($status -eq "FAIL") {
    exit 1
}
exit 0
