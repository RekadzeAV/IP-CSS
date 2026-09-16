param(
  [string]$LighthouseDir = "",
  [string]$OutputPath = "",
  [string]$ValidationReportPath = "",
  [switch]$RequireValidMetrics,
  [switch]$ShowHelp
)

if ($ShowHelp) {
  Write-Host "Generate F2_RESULTS input from Lighthouse JSON reports."
  Write-Host ""
  Write-Host "Usage:"
  Write-Host "  .\scripts\generate-f2-results-from-lighthouse.ps1"
  Write-Host "  .\scripts\generate-f2-results-from-lighthouse.ps1 -RequireValidMetrics"
  Write-Host "  .\scripts\generate-f2-results-from-lighthouse.ps1 -LighthouseDir <dir> -OutputPath <path> -ValidationReportPath <path>"
  exit 0
}

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($LighthouseDir)) { $LighthouseDir = Join-Path $repoRoot "docs/status/lighthouse" }
if ([string]::IsNullOrWhiteSpace($OutputPath)) { $OutputPath = Join-Path $repoRoot "docs/status/F2_RESULTS_INPUT_AUTO.txt" }
if ([string]::IsNullOrWhiteSpace($ValidationReportPath)) { $ValidationReportPath = Join-Path $repoRoot "docs/status/WEB_INTERFACE_F2_VALIDATION_LATEST.json" }
if (-not (Test-Path -Path $LighthouseDir)) { throw "Lighthouse directory not found: $LighthouseDir" }

function Get-ObjectPropertyValue([object]$Object, [string]$PropertyName) {
  if ($null -eq $Object) { return $null }
  $property = $Object.PSObject.Properties[$PropertyName]
  if ($null -eq $property) { return $null }
  return $property.Value
}

function Convert-NullableMsToSecondsText([object]$Value) {
  if ($null -eq $Value) { return "NO_LCP" }
  $number = 0.0
  if ([double]::TryParse("$Value", [ref]$number)) {
    return ([math]::Round(($number / 1000.0), 2)).ToString([System.Globalization.CultureInfo]::InvariantCulture)
  }
  return "NO_LCP"
}

function Convert-NullableToIntegerText([object]$Value) {
  if ($null -eq $Value) { return "n/a" }
  $number = 0.0
  if ([double]::TryParse("$Value", [ref]$number)) {
    return ([math]::Round($number, 0)).ToString([System.Globalization.CultureInfo]::InvariantCulture)
  }
  return "n/a"
}

$pages = @("dashboard", "cameras", "events", "recordings")
$lines = @("F2_RESULTS")
$items = @()

foreach ($page in $pages) {
  $filePath = Join-Path $LighthouseDir "$page.json"
  if (-not (Test-Path -Path $filePath)) { throw "Missing lighthouse report for page '$page': $filePath" }

  $json = Get-Content -Path $filePath -Raw | ConvertFrom-Json
  $perfRaw = Get-ObjectPropertyValue (Get-ObjectPropertyValue (Get-ObjectPropertyValue $json "categories") "performance") "score"
  $perf = if ($null -eq $perfRaw) { 0 } else { [math]::Round(([double]$perfRaw * 100.0), 0) }

  $audits = Get-ObjectPropertyValue $json "audits"
  $lcp = Convert-NullableMsToSecondsText (Get-ObjectPropertyValue (Get-ObjectPropertyValue $audits "largest-contentful-paint") "numericValue")
  $inp = Convert-NullableToIntegerText (Get-ObjectPropertyValue (Get-ObjectPropertyValue $audits "interaction-to-next-paint") "numericValue")
  $tbt = Convert-NullableToIntegerText (Get-ObjectPropertyValue (Get-ObjectPropertyValue $audits "total-blocking-time") "numericValue")
  $inpOrTbt = if ($inp -ne "n/a") { "inp=$inp / tbt=$tbt" } else { "tbt=$tbt" }

  $clsRaw = Get-ObjectPropertyValue (Get-ObjectPropertyValue $audits "cumulative-layout-shift") "numericValue"
  $cls = if ($null -eq $clsRaw) { "n/a" } else { ([math]::Round([double]$clsRaw, 3)).ToString([System.Globalization.CultureInfo]::InvariantCulture) }
  $runtimeError = Get-ObjectPropertyValue $json "runtimeError"
  $longTasks = if ($null -ne $runtimeError) { "runtime_error" } elseif ($tbt -eq "n/a") { "unknown" } elseif ([double]$tbt -ge 200) { "blocking" } else { "none" }

  $lines += "/${page}: perf=$perf, lcp=$lcp, inp_or_tbt=$inpOrTbt, cls=$cls, longTasks=$longTasks"

  $reasons = @()
  if ($lcp -eq "NO_LCP") { $reasons += "NO_LCP" }
  if ($tbt -eq "n/a") { $reasons += "NO_TBT" }
  if ($perf -eq 0) { $reasons += "PERF_ZERO" }
  if ($longTasks -eq "runtime_error") { $reasons += "RUNTIME_ERROR" }

  $items += [ordered]@{
    page = $page
    perf = $perf
    lcp = $lcp
    inp_or_tbt = $inpOrTbt
    cls = $cls
    longTasks = $longTasks
    isValid = ($reasons.Count -eq 0)
    reasons = $reasons
  }
}

[System.IO.File]::WriteAllText($OutputPath, (($lines -join "`r`n") + "`r`n"), [System.Text.Encoding]::UTF8)

$invalidPages = @($items | Where-Object { -not $_.isValid } | ForEach-Object { $_.page })
$payload = [ordered]@{
  generatedAt = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
  lighthouseDir = $LighthouseDir
  outputPath = $OutputPath
  requireValidMetrics = [bool]$RequireValidMetrics
  valid = ($invalidPages.Count -eq 0)
  invalidPages = $invalidPages
  pages = $items
}

[System.IO.File]::WriteAllText($ValidationReportPath, (($payload | ConvertTo-Json -Depth 8) + "`r`n"), [System.Text.Encoding]::UTF8)

if ($RequireValidMetrics -and $invalidPages.Count -gt 0) {
  Write-Error ("Invalid lighthouse metrics for pages: " + ($invalidPages -join ", ") + ". Report: " + $ValidationReportPath)
  exit 2
}
