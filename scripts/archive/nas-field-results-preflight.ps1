[CmdletBinding()]
param(
    [switch]$ShowHelp,
    [string]$Date = (Get-Date -Format "yyyy-MM-dd"),
    [string]$ResultsPath = "config/nas-field-results.template.json",
    [switch]$AllowTemplateValues
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Validate NAS field results JSON without modifying reports."
    Write-Host "Usage:"
    Write-Host "  .\scripts\nas-field-results-preflight.ps1 -Date 2026-04-27 -ResultsPath <json>"
    Write-Host "Exit codes: 0=valid, 2=invalid"
    exit 0
}

function Resolve-ProjectPath {
    param([string]$PathValue)
    $projectRoot = Split-Path -Parent $PSScriptRoot
    if ([System.IO.Path]::IsPathRooted($PathValue)) { return $PathValue }
    return (Join-Path $projectRoot $PathValue)
}

function Test-TemplateText {
    param([string]$Text)
    if ($null -eq $Text) { return $true }
    $v = $Text.Trim()
    if ($v.Length -eq 0) { return $true }
    return $v -match "^\[.*\]$|^None$|^Field run completed\.$|^\[summary\]$|^\[description\]$"
}

$resultsAbs = Resolve-ProjectPath -PathValue $ResultsPath
if (-not (Test-Path -LiteralPath $resultsAbs)) {
    Write-Host "Results JSON not found: $resultsAbs" -ForegroundColor Red
    exit 2
}

try {
    $results = Get-Content -LiteralPath $resultsAbs -Raw | ConvertFrom-Json
} catch {
    Write-Host "Invalid JSON format in: $resultsAbs" -ForegroundColor Red
    exit 2
}

$issues = New-Object System.Collections.Generic.List[string]

if ($results.date -and "$($results.date)" -ne $Date) {
    Write-Host "Warning: JSON date ($($results.date)) differs from -Date ($Date)." -ForegroundColor Yellow
}

foreach ($platformId in @("synology", "qnap", "asustor", "truenas")) {
    $p = $results.platforms.$platformId
    if ($null -eq $p) {
        $issues.Add("Missing platform section: platforms.$platformId")
        continue
    }

    foreach ($id in @("s2","s3","s4","s5","s6")) {
        $value = "$($p.$id)"
        if ($value -notin @("PASS", "FAIL")) {
            $issues.Add("Invalid $platformId.$id='$value' (allowed PASS|FAIL)")
        }
    }

    $resultValue = "$($p.result)"
    if ($resultValue -notin @("GO", "CONDITIONAL GO", "NO-GO")) {
        $issues.Add("Invalid $platformId.result='$resultValue' (allowed GO|CONDITIONAL GO|NO-GO)")
    }
    $hasFail = @("$($p.s2)","$($p.s3)","$($p.s4)","$($p.s5)","$($p.s6)") -contains "FAIL"
    if ($hasFail -and $resultValue -eq "GO") {
        $issues.Add("Inconsistent $platformId.result='GO' while one or more scenarios are FAIL")
    }
    if (-not $hasFail -and $resultValue -eq "NO-GO") {
        Write-Host "Warning: $platformId has all PASS in S2-S6 but result is NO-GO (allowed, verify notes)." -ForegroundColor Yellow
    }

    if (-not $AllowTemplateValues) {
        if (Test-TemplateText -Text "$($p.notes)") { $issues.Add("$platformId.notes looks like template text") }
        if (Test-TemplateText -Text "$($p.issue1)") { $issues.Add("$platformId.issue1 looks like template text") }
        if (Test-TemplateText -Text "$($p.issue2)") { $issues.Add("$platformId.issue2 looks like template text") }
    }
}

if ($issues.Count -gt 0) {
    Write-Host "NAS field results preflight: INVALID" -ForegroundColor Yellow
    foreach ($issue in $issues) {
        Write-Host " - $issue"
    }
    exit 2
}

Write-Host "NAS field results preflight: VALID" -ForegroundColor Green
Write-Host "JSON is ready for apply:"
Write-Host "  .\scripts\nas-field-apply-results.ps1 -Date $Date -ResultsPath $ResultsPath"
exit 0

