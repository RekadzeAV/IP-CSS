[CmdletBinding()]
param(
    [string]$BaseUrl = "http://localhost:8080",
    [System.Management.Automation.PSCredential]$Credential,
    [int]$MaxDiscoverFailures = 0,
    [int]$MaxAuthFailures = 5,
    [int]$MaxEventStatisticsFailures = 0,
    [string]$ReportPath = "",
    [string]$JsonReportPath = ""
)

$ErrorActionPreference = "Stop"

if ($null -eq $Credential) {
    $secure = ConvertTo-SecureString "ADMIN_PASSWORD@" -AsPlainText -Force
    $Credential = New-Object System.Management.Automation.PSCredential("admin", $secure)
}

if ([string]::IsNullOrWhiteSpace($ReportPath)) {
    $stamp = Get-Date -Format "yyyy-MM-dd_HHmmss"
    $ReportPath = "docs/reports/DOCKER_PRE_RELEASE_GATE_REPORT_$stamp.md"
}
if ([string]::IsNullOrWhiteSpace($JsonReportPath)) {
    $JsonReportPath = [System.IO.Path]::ChangeExtension($ReportPath, ".json")
}

function Invoke-Step {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][scriptblock]$Action
    )

    try {
        & $Action
        return [pscustomobject]@{
            Name = $Name
            Status = "PASS"
            Error = ""
        }
    } catch {
        return [pscustomobject]@{
            Name = $Name
            Status = "FAIL"
            Error = $_.Exception.Message
        }
    }
}

$steps = @()

$steps += Invoke-Step -Name "Auth/Discovery Smoke" -Action {
    powershell -ExecutionPolicy Bypass -File "scripts/server-auth-discovery-smoke.ps1" `
        -BaseUrl $BaseUrl | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "server-auth-discovery-smoke failed with exit code $LASTEXITCODE"
    }
}

$steps += Invoke-Step -Name "Metrics Guard" -Action {
    powershell -ExecutionPolicy Bypass -File "scripts/server-metrics-guard.ps1" `
        -BaseUrl $BaseUrl `
        -MaxDiscoverFailures $MaxDiscoverFailures `
        -MaxAuthFailures $MaxAuthFailures `
        -MaxEventStatisticsFailures $MaxEventStatisticsFailures | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "server-metrics-guard failed with exit code $LASTEXITCODE"
    }
}

$overallPass = ($steps | Where-Object { $_.Status -eq "FAIL" }).Count -eq 0
$overall = if ($overallPass) { "GO" } else { "NO-GO" }
$generatedAt = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss zzz")

$lines = @(
    "# Docker Pre-Release Gate Report",
    "",
    "- Generated at: $generatedAt",
    "- Base URL: ``$BaseUrl``",
    "- Overall decision: **$overall**",
    "",
    "## Checks",
    "",
    "| Check | Status | Notes |",
    "|---|---|---|"
)

foreach ($step in $steps) {
    $notes = if ([string]::IsNullOrWhiteSpace($step.Error)) { "ok" } else { $step.Error.Replace("|", "/") }
    $lines += "| $($step.Name) | $($step.Status) | $notes |"
}

if (-not $overallPass) {
    $lines += ""
    $lines += "## Action required"
    $lines += ""
    $lines += "- Fix failed checks and rerun ``scripts/server-pre-release-gate.ps1``."
}

$reportDir = Split-Path -Parent $ReportPath
if (-not [string]::IsNullOrWhiteSpace($reportDir) -and -not (Test-Path $reportDir)) {
    New-Item -ItemType Directory -Path $reportDir -Force | Out-Null
}
$jsonDir = Split-Path -Parent $JsonReportPath
if (-not [string]::IsNullOrWhiteSpace($jsonDir) -and -not (Test-Path $jsonDir)) {
    New-Item -ItemType Directory -Path $jsonDir -Force | Out-Null
}

$lines -join [Environment]::NewLine | Set-Content -Path $ReportPath -Encoding UTF8

$jsonPayload = [pscustomobject]@{
    generatedAt = $generatedAt
    baseUrl = $BaseUrl
    result = $overall
    steps = $steps
    reportPath = $ReportPath
}
$jsonPayload | ConvertTo-Json -Depth 6 | Set-Content -Path $JsonReportPath -Encoding UTF8

Write-Host ""
Write-Host "PRE-RELEASE GATE: $overall" -ForegroundColor $(if ($overallPass) { "Green" } else { "Red" })
Write-Host "Report: $ReportPath"
Write-Host "JSON: $JsonReportPath"

if (-not $overallPass) {
    exit 1
}

exit 0
