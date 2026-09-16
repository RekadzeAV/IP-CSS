param(
  [ValidateSet("strict", "non-strict")]
  [string]$Mode = "strict",
  [string]$LighthouseDir = "",
  [string]$C4InputPath = "",
  [switch]$SkipC4,
  [switch]$CleanupTestArtifacts,
  [int]$KeepArtifacts = 30,
  [switch]$CleanupApplyDeletion,
  [string]$OutputPrefix = "WEB_INTERFACE_PIPELINE",
  [switch]$ShowHelp
)

if ($ShowHelp) {
  Write-Host "Run web interface acceptance pipeline with presets."
  Write-Host ""
  Write-Host "Usage:"
  Write-Host "  .\scripts\run-web-interface-acceptance-pipeline.ps1 -ShowHelp"
  Write-Host "  .\scripts\run-web-interface-acceptance-pipeline.ps1 -Mode strict"
  Write-Host "  .\scripts\run-web-interface-acceptance-pipeline.ps1 -Mode non-strict -SkipC4"
  Write-Host "  .\scripts\run-web-interface-acceptance-pipeline.ps1 -Mode strict -CleanupTestArtifacts -KeepArtifacts 20"
  Write-Host ""
  Write-Host "Modes:"
  Write-Host "  strict: auto F2 from lighthouse + require valid metrics (exit 2 on invalid)"
  Write-Host "  non-strict: auto F2 from lighthouse without strict gate"
  Write-Host ""
  Write-Host "Artifacts:"
  Write-Host "  Writes run/report/summary/status/history + artifact index markdown in docs/status"
  exit 0
}

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ($KeepArtifacts -lt 1) {
  throw "KeepArtifacts must be >= 1"
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$statusDir = Join-Path $repoRoot "docs/status"
$fullScript = Join-Path $repoRoot "scripts/run-web-interface-acceptance-full.ps1"

if (-not (Test-Path -Path $fullScript)) {
  throw "Missing script: $fullScript"
}

if ([string]::IsNullOrWhiteSpace($LighthouseDir)) {
  $LighthouseDir = Join-Path $statusDir "lighthouse"
}
if ([string]::IsNullOrWhiteSpace($C4InputPath)) {
  $C4InputPath = Join-Path $statusDir "C4_RESULTS_INPUT_TEMPLATE.txt"
}

$timestamp = Get-Date -Format "yyyy-MM-dd_HH-mm-ss"
$runTag = "${OutputPrefix}_${timestamp}"

$runReportPath = Join-Path $statusDir "${runTag}_RUN.md"
$summaryPath = Join-Path $statusDir "${runTag}_SUMMARY.json"
$statusDocPath = Join-Path $statusDir "${runTag}_STATUS.md"
$historyPath = Join-Path $statusDir "${runTag}_HISTORY.md"
$f2OutputPath = Join-Path $statusDir "${runTag}_F2_RESULTS.txt"
$f2ValidationPath = Join-Path $statusDir "${runTag}_F2_VALIDATION.json"
$indexPath = Join-Path $statusDir "${runTag}_ARTIFACT_INDEX.md"

$fullArgs = @{
  AutoF2FromLighthouse = $true
  LighthouseDir = $LighthouseDir
  AutoF2OutputPath = $f2OutputPath
  F2ValidationReportPath = $f2ValidationPath
  SummaryJsonPath = $summaryPath
  AutomationStatusDocPath = $statusDocPath
  HistoryLogPath = $historyPath
  OutputPath = $runReportPath
}

if ($Mode -eq "strict") {
  $fullArgs["RequireValidF2Metrics"] = $true
}

if ($SkipC4) {
  $fullArgs["SkipC4"] = $true
} else {
  $fullArgs["C4InputPath"] = $C4InputPath
}

if ($CleanupTestArtifacts) {
  $fullArgs["CleanupTestArtifacts"] = $true
  $fullArgs["KeepArtifacts"] = $KeepArtifacts
  if ($CleanupApplyDeletion) {
    $fullArgs["CleanupApplyDeletion"] = $true
  }
}

$startedAt = Get-Date
$exitCode = 0
$errorText = ""

try {
  & $fullScript @fullArgs
  $lastExitCodeVar = Get-Variable -Name LASTEXITCODE -ErrorAction SilentlyContinue
  if ($null -ne $lastExitCodeVar) {
    $exitCode = [int]$lastExitCodeVar.Value
  } else {
    $exitCode = 0
  }
  if ($exitCode -ne 0 -and (Test-Path -Path $summaryPath)) {
    try {
      $summaryOnFailure = Get-Content -Path $summaryPath -Raw | ConvertFrom-Json
      if ($null -ne $summaryOnFailure.error) {
        $errorText = "$($summaryOnFailure.error)"
      }
    } catch {}
  }
} catch {
  $errorText = $_.Exception.Message
  if ($errorText.Contains("invalid F2 metrics")) {
    $exitCode = 2
  } else {
    $exitCode = 1
  }
}

$finishedAt = Get-Date

$summaryExists = Test-Path -Path $summaryPath
$summaryStatus = "N/A"
if ($summaryExists) {
  try {
    $summaryJson = Get-Content -Path $summaryPath -Raw | ConvertFrom-Json
    $summaryStatus = "$($summaryJson.runStatus) / $($summaryJson.errorCode)"
  } catch {
    $summaryStatus = "unreadable"
  }
}

$errorLabel = if ([string]::IsNullOrWhiteSpace($errorText)) { "-" } else { $errorText }
$effectiveArgsText = ($fullArgs.GetEnumerator() | ForEach-Object { "-$($_.Key) $($_.Value)" } | Sort-Object) -join "`r`n"

$indexLines = @(
  "# Web Interface Acceptance Pipeline Artifacts",
  "",
  "- Mode: $Mode",
  "- Started at: $($startedAt.ToString('yyyy-MM-dd HH:mm:ss'))",
  "- Finished at: $($finishedAt.ToString('yyyy-MM-dd HH:mm:ss'))",
  "- Exit code: $exitCode",
  "- Summary status: $summaryStatus",
  "- Error: $errorLabel",
  "",
  "## Artifact Paths",
  "",
  "- Run report: $runReportPath",
  "- Summary JSON: $summaryPath",
  "- Status doc: $statusDocPath",
  "- History log: $historyPath",
  "- F2 input (auto): $f2OutputPath",
  "- F2 validation JSON: $f2ValidationPath",
  "",
  "## Effective Args",
  "",
  '```text',
  $effectiveArgsText,
  '```'
)

$newLine = [System.Environment]::NewLine
$indexContent = ($indexLines -join $newLine) + $newLine
[System.IO.File]::WriteAllText($indexPath, $indexContent, [System.Text.Encoding]::UTF8)
Write-Host "Pipeline artifact index: $indexPath"
Write-Host "Pipeline exit code: $exitCode"

exit $exitCode
