[CmdletBinding()]
param(
    [switch]$ShowHelp,
    [string]$Date = (Get-Date -Format "yyyy-MM-dd"),
    [string]$ResultsPath = "config/nas-field-results.template.json",
    [string]$AcceptanceProfilePath = "config/video-e2e-acceptance-profile.local.json",
    [switch]$AllowTemplateValues
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Run complete NAS field automation orchestration."
    Write-Host "Usage:"
    Write-Host "  .\scripts\nas-field-auto-orchestrator.ps1 -Date 2026-04-27 -ResultsPath <json>"
    Write-Host "Pipeline:"
    Write-Host "  1) JSON preflight"
    Write-Host "  2) Apply results into NAS_FIELD_REPORT_*"
    Write-Host "  3) Full finalize (aggregate + gate + docs sync)"
    Write-Host "  4) Write execution report in docs/reports/"
    Write-Host "Exit codes: 0=GO completed, 1=NO-GO finalized, 2=input/readiness blocked"
    exit 0
}

function Resolve-ProjectPath {
    param([string]$PathValue)
    $projectRoot = Split-Path -Parent $PSScriptRoot
    if ([System.IO.Path]::IsPathRooted($PathValue)) { return $PathValue }
    return (Join-Path $projectRoot $PathValue)
}

function Get-LineValue {
    param([string]$Content, [string]$Pattern, [string]$DefaultValue)
    $m = [regex]::Match($Content, $Pattern)
    if ($m.Success) { return $m.Groups[1].Value.Trim() }
    return $DefaultValue
}

$projectRoot = Split-Path -Parent $PSScriptRoot
$preflightScript = Resolve-ProjectPath -PathValue "scripts/nas-field-results-preflight.ps1"
$applyScript = Resolve-ProjectPath -PathValue "scripts/nas-field-apply-results.ps1"
$fullFinalizeScript = Resolve-ProjectPath -PathValue "scripts/nas-field-full-finalize.ps1"
$aggPath = Resolve-ProjectPath -PathValue "docs/reports/NAS_FIELD_AGGREGATOR_$Date.md"
$reportPath = Resolve-ProjectPath -PathValue "docs/reports/NAS_FIELD_AUTOMATION_EXECUTION_$Date.md"
$startedAt = Get-Date

foreach ($required in @($preflightScript, $applyScript, $fullFinalizeScript)) {
    if (-not (Test-Path -LiteralPath $required)) {
        throw "Required script not found: $required"
    }
}

if ($AllowTemplateValues) {
    & $preflightScript -Date $Date -ResultsPath $ResultsPath -AllowTemplateValues
} else {
    & $preflightScript -Date $Date -ResultsPath $ResultsPath
}
$preflightExit = $LASTEXITCODE
if ($preflightExit -ne 0) {
    $body = @"
# NAS Field Automation Execution ($Date)

- Started at: $($startedAt.ToString("yyyy-MM-dd HH:mm:ss zzz"))
- Finished at: $((Get-Date).ToString("yyyy-MM-dd HH:mm:ss zzz"))
- Stage reached: preflight
- Result: BLOCKED
- Exit code: $preflightExit
- Notes: JSON preflight failed; reports were not modified.
"@
    Set-Content -LiteralPath $reportPath -Value $body -Encoding UTF8
    Write-Host "Execution report written: $reportPath" -ForegroundColor Yellow
    exit $preflightExit
}

if ($AllowTemplateValues) {
    & $applyScript -Date $Date -ResultsPath $ResultsPath -AllowTemplateValues
} else {
    & $applyScript -Date $Date -ResultsPath $ResultsPath
}
$applyExit = $LASTEXITCODE
if ($applyExit -ne 0) {
    $body = @"
# NAS Field Automation Execution ($Date)

- Started at: $($startedAt.ToString("yyyy-MM-dd HH:mm:ss zzz"))
- Finished at: $((Get-Date).ToString("yyyy-MM-dd HH:mm:ss zzz"))
- Stage reached: apply
- Result: BLOCKED
- Exit code: $applyExit
- Notes: Apply step failed after preflight.
"@
    Set-Content -LiteralPath $reportPath -Value $body -Encoding UTF8
    Write-Host "Execution report written: $reportPath" -ForegroundColor Yellow
    exit $applyExit
}

& $fullFinalizeScript -Date $Date -AcceptanceProfilePath $AcceptanceProfilePath
$finalizeExit = $LASTEXITCODE

$fieldValidation = "UNKNOWN"
$finalDecision = "UNKNOWN"
if (Test-Path -LiteralPath $aggPath) {
    $agg = Get-Content -LiteralPath $aggPath -Raw
    $fieldValidation = Get-LineValue -Content $agg -Pattern "(?m)^- Field validation:\s*(.+?)\s*$" -DefaultValue "UNKNOWN"
    $finalDecision = Get-LineValue -Content $agg -Pattern "(?m)^- Final decision:\s*(.+?)\s*$" -DefaultValue "UNKNOWN"
}

$resultLabel = if ($finalizeExit -eq 0) { "GO" } elseif ($finalizeExit -eq 1) { "NO-GO" } else { "BLOCKED" }
$body = @"
# NAS Field Automation Execution ($Date)

- Started at: $($startedAt.ToString("yyyy-MM-dd HH:mm:ss zzz"))
- Finished at: $((Get-Date).ToString("yyyy-MM-dd HH:mm:ss zzz"))
- Stage reached: finalize
- Result: $resultLabel
- Exit code: $finalizeExit
- Field validation: $fieldValidation
- Final decision: $finalDecision
- Inputs:
  - Results JSON: $ResultsPath
  - Acceptance profile: $AcceptanceProfilePath
"@
Set-Content -LiteralPath $reportPath -Value $body -Encoding UTF8
Write-Host "Execution report written: $reportPath" -ForegroundColor Green
exit $finalizeExit

