[CmdletBinding()]
param(
    [switch]$ShowHelp,
    [string]$OutputDir = "diagnostics\security",
    [string]$SecurityReportPath = "release-build\test\security-mvp-readiness-report.md",
    [string]$SecurityEnvFilePath = ".env.example"
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir

if ($ShowHelp) {
    Write-Host "Security field/staging validation (gate + config presence)"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\security-field-staging-validation.ps1"
    Write-Host "  .\scripts\security-field-staging-validation.ps1 -OutputDir diagnostics\security"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -SecurityReportPath  Path to security MVP readiness markdown (repo-relative ok)."
    Write-Host "  -SecurityEnvFilePath  Passed to security-mvp-readiness-check.ps1 (default .env.example)."
    Write-Host "  -OutputDir  Where to write security-field-staging-validation-<run-id>.md|.json"
    Write-Host ""
    Write-Host "Steps:"
    Write-Host "  - Invokes scripts/security-mvp-readiness-check.ps1"
    Write-Host "  - Checks config/certificate-pins.production.example.json and config/https-baseline.example.env"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Overall PASS"
    Write-Host "  1  Overall FAIL (missing critical config or gate hard fail)"
    Write-Host "  3  Overall PARTIAL (e.g. security gate non-zero treated as partial)"
    exit 0
}

$securityGateScript = Join-Path $scriptDir "security-mvp-readiness-check.ps1"
$resolvedOutputDir = Join-Path $projectRoot $OutputDir
$resolvedSecurityReport = Join-Path $projectRoot $SecurityReportPath
$resolvedSecurityEnvFile = if ([System.IO.Path]::IsPathRooted($SecurityEnvFilePath)) {
    $SecurityEnvFilePath
} else {
    Join-Path $projectRoot $SecurityEnvFilePath
}

if (-not (Test-Path $securityGateScript)) {
    Write-Host "security-mvp-readiness-check.ps1 not found: $securityGateScript" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path $resolvedOutputDir)) {
    New-Item -ItemType Directory -Path $resolvedOutputDir -Force | Out-Null
}

$runId = Get-Date -Format "yyyyMMdd-HHmmss"
$summaryJson = Join-Path $resolvedOutputDir ("security-field-staging-validation-{0}.json" -f $runId)
$summaryMd = Join-Path $resolvedOutputDir ("security-field-staging-validation-{0}.md" -f $runId)

$statuses = [ordered]@{
    securityGate = "NOT_RUN"
    certificatePinsConfig = "NOT_RUN"
    httpsBaselineConfig = "NOT_RUN"
}
$notes = New-Object System.Collections.Generic.List[string]
$overall = "PASS"

& $securityGateScript -ReportPath $resolvedSecurityReport -EnvFilePath $resolvedSecurityEnvFile
if ($LASTEXITCODE -eq 0) {
    $statuses.securityGate = "PASS"
} else {
    $statuses.securityGate = "CONDITIONAL"
    $overall = "PARTIAL"
    $notes.Add("Security gate returned non-zero status; see report.") | Out-Null
}

$pinsConfig = Join-Path $projectRoot "config\certificate-pins.production.example.json"
if (Test-Path $pinsConfig) {
    $statuses.certificatePinsConfig = "PASS"
} else {
    $statuses.certificatePinsConfig = "FAIL"
    $overall = "FAIL"
    $notes.Add("Missing config: $pinsConfig") | Out-Null
}

$httpsBaselineConfig = Join-Path $projectRoot "config\https-baseline.example.env"
if (Test-Path $httpsBaselineConfig) {
    $statuses.httpsBaselineConfig = "PASS"
} else {
    $statuses.httpsBaselineConfig = "FAIL"
    $overall = "FAIL"
    $notes.Add("Missing config: $httpsBaselineConfig") | Out-Null
}

if ($overall -eq "PASS" -and $statuses.securityGate -ne "PASS") {
    $overall = "PARTIAL"
}

$summary = [ordered]@{
    runId = $runId
    timestampUtc = [DateTime]::UtcNow.ToString("o")
    statuses = @{
        overall = $overall
        securityGate = $statuses.securityGate
        certificatePinsConfig = $statuses.certificatePinsConfig
        httpsBaselineConfig = $statuses.httpsBaselineConfig
    }
    artifacts = @{
        securityReport = $resolvedSecurityReport
        securityEnvFile = $resolvedSecurityEnvFile
    }
    notes = @($notes)
}
$summary | ConvertTo-Json -Depth 6 | Set-Content -Path $summaryJson -Encoding UTF8

$lines = @(
    "# Security Field/Staging Validation Report",
    "",
    "- Run ID: $runId",
    "- Overall: **$overall**",
    "- securityGate: **$($statuses.securityGate)**",
    "- certificatePinsConfig: **$($statuses.certificatePinsConfig)**",
    "- httpsBaselineConfig: **$($statuses.httpsBaselineConfig)**",
    "- securityReport: $resolvedSecurityReport",
    "- securityEnvFile: $resolvedSecurityEnvFile",
    "",
    "## Notes"
)
if ($notes.Count -eq 0) {
    $lines += "- none"
} else {
    foreach ($n in $notes) { $lines += "- $n" }
}
$lines -join [Environment]::NewLine | Set-Content -Path $summaryMd -Encoding UTF8

Write-Host ("Security validation summary: {0}" -f $summaryMd) -ForegroundColor Green
Write-Host ("Security validation json: {0}" -f $summaryJson) -ForegroundColor Green

if ($overall -eq "FAIL") { exit 1 }
if ($overall -eq "PARTIAL") { exit 3 }
exit 0
