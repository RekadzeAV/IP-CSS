[CmdletBinding()]
param(
    [switch]$ShowHelp,
    [string]$Date = (Get-Date -Format "yyyy-MM-dd"),
    [string]$ResultsPath = "config/nas-field-results.template.json",
    [switch]$AllowTemplateValues,
    [switch]$RunFullFinalize
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Apply structured NAS field results into platform reports."
    Write-Host "Usage:"
    Write-Host "  .\scripts\nas-field-apply-results.ps1 -Date 2026-04-27 -ResultsPath config/nas-field-results.template.json"
    Write-Host "Optional:"
    Write-Host "  -AllowTemplateValues  Allow placeholder/template texts in notes/issues"
    Write-Host "  -RunFullFinalize    Run scripts/nas-field-full-finalize.ps1 after apply"
    Write-Host "Allowed values for S2-S6: PASS, FAIL"
    Write-Host "Allowed values for Result: GO, CONDITIONAL GO, NO-GO"
    exit 0
}

function Resolve-ProjectPath {
    param([string]$PathValue)
    $projectRoot = Split-Path -Parent $PSScriptRoot
    if ([System.IO.Path]::IsPathRooted($PathValue)) { return $PathValue }
    return (Join-Path $projectRoot $PathValue)
}

function Update-LineValue {
    param([string]$Content, [string]$Prefix, [string]$Value)
    $escaped = [regex]::Escape($Prefix)
    if ($Content -match "(?m)^$escaped") {
        return [regex]::Replace($Content, "(?m)^$escaped.*$", "$Prefix$Value")
    }
    throw "Prefix not found in report: $Prefix"
}

function Assert-AllowedValue {
    param([string]$Name, [string]$Value, [string[]]$AllowedValues)
    if ($AllowedValues -contains $Value) { return }
    $allowed = ($AllowedValues -join ", ")
    throw "$Name has invalid value '$Value'. Allowed: $allowed"
}

function Test-TemplateText {
    param([string]$Text)
    if ($null -eq $Text) { return $true }
    $v = $Text.Trim()
    if ($v.Length -eq 0) { return $true }
    return $v -match "^\[.*\]$|^None$|^Field run completed\.$|^\[summary\]$|^\[description\]$"
}

function Get-PlatformReportPath {
    param([string]$ReportsAbs, [string]$PlatformId, [string]$DateValue)
    switch ($PlatformId) {
        "synology" { return (Join-Path $ReportsAbs "NAS_FIELD_REPORT_SYNOLOGY_$DateValue.md") }
        "qnap" { return (Join-Path $ReportsAbs "NAS_FIELD_REPORT_QNAP_$DateValue.md") }
        "asustor" { return (Join-Path $ReportsAbs "NAS_FIELD_REPORT_ASUSTOR_$DateValue.md") }
        "truenas" { return (Join-Path $ReportsAbs "NAS_FIELD_REPORT_TRUENAS_$DateValue.md") }
        default { throw "Unsupported platform id: $PlatformId" }
    }
}

$resultsAbs = Resolve-ProjectPath -PathValue $ResultsPath
if (-not (Test-Path -LiteralPath $resultsAbs)) {
    throw "Results JSON not found: $resultsAbs"
}

$projectRoot = Split-Path -Parent $PSScriptRoot
$reportsAbs = Join-Path $projectRoot "docs/reports"
$fullFinalizeScript = Join-Path $projectRoot "scripts/nas-field-full-finalize.ps1"
$preflightScript = Join-Path $projectRoot "scripts/nas-field-results-preflight.ps1"

if (-not (Test-Path -LiteralPath $preflightScript)) {
    throw "Preflight script not found: $preflightScript"
}

if ($AllowTemplateValues) {
    & $preflightScript -Date $Date -ResultsPath $ResultsPath -AllowTemplateValues
} else {
    & $preflightScript -Date $Date -ResultsPath $ResultsPath
}
$preflightExit = $LASTEXITCODE
if ($preflightExit -ne 0) {
    exit $preflightExit
}

$results = Get-Content -LiteralPath $resultsAbs -Raw | ConvertFrom-Json
$resultsDate = if ($results.date) { "$($results.date)" } else { $Date }
if ($resultsDate -ne $Date) {
    Write-Host "Warning: JSON date ($resultsDate) differs from -Date ($Date); using -Date value." -ForegroundColor Yellow
}

foreach ($platformId in @("synology", "qnap", "asustor", "truenas")) {
    $p = $results.platforms.$platformId
    if ($null -eq $p) {
        throw "Missing platform section in results JSON: $platformId"
    }

    foreach ($id in @("s2", "s3", "s4", "s5", "s6")) {
        Assert-AllowedValue -Name "$platformId.$id" -Value "$($p.$id)" -AllowedValues @("PASS", "FAIL")
    }
    Assert-AllowedValue -Name "$platformId.result" -Value "$($p.result)" -AllowedValues @("GO", "CONDITIONAL GO", "NO-GO")
    if (-not $AllowTemplateValues) {
        if (Test-TemplateText -Text "$($p.notes)") { throw "$platformId.notes looks like template text. Provide real note or use -AllowTemplateValues." }
        if (Test-TemplateText -Text "$($p.issue1)") { throw "$platformId.issue1 looks like template text. Provide real issue text or use -AllowTemplateValues." }
        if (Test-TemplateText -Text "$($p.issue2)") { throw "$platformId.issue2 looks like template text. Provide real issue text or use -AllowTemplateValues." }
    }

    $reportPath = Get-PlatformReportPath -ReportsAbs $reportsAbs -PlatformId $platformId -DateValue $Date
    if (-not (Test-Path -LiteralPath $reportPath)) {
        throw "Report not found: $reportPath"
    }
    $content = Get-Content -LiteralPath $reportPath -Raw

    $content = Update-LineValue -Content $content -Prefix "- S2 Basic health: " -Value "$($p.s2)"
    $content = Update-LineValue -Content $content -Prefix "- S3 Restart: " -Value "$($p.s3)"
    $content = Update-LineValue -Content $content -Prefix "- S4 Reboot persistence: " -Value "$($p.s4)"
    $content = Update-LineValue -Content $content -Prefix "- S5 Upgrade: " -Value "$($p.s5)"
    $content = Update-LineValue -Content $content -Prefix "- S6 Uninstall: " -Value "$($p.s6)"
    $content = Update-LineValue -Content $content -Prefix "- Result: " -Value "$($p.result)"

    if ($null -ne $p.notes -and "$($p.notes)".Length -gt 0) {
        $content = Update-LineValue -Content $content -Prefix "- Notes: " -Value "$($p.notes)"
    }
    if ($null -ne $p.issue1 -and "$($p.issue1)".Length -gt 0) {
        $content = Update-LineValue -Content $content -Prefix "- Issue 1: " -Value "$($p.issue1)"
    }
    if ($null -ne $p.issue2 -and "$($p.issue2)".Length -gt 0) {
        $content = Update-LineValue -Content $content -Prefix "- Issue 2: " -Value "$($p.issue2)"
    }

    Set-Content -LiteralPath $reportPath -Value $content -Encoding UTF8
    Write-Host "Updated: $reportPath" -ForegroundColor Green
}

Write-Host "All NAS field reports updated from JSON input." -ForegroundColor Green

if ($RunFullFinalize) {
    if (-not (Test-Path -LiteralPath $fullFinalizeScript)) {
        throw "Full finalize script not found: $fullFinalizeScript"
    }
    & $fullFinalizeScript -Date $Date
    exit $LASTEXITCODE
}

exit 0

