[CmdletBinding()]
param(
    [switch]$ShowHelp,
    [string]$Date = (Get-Date -Format "yyyy-MM-dd"),
    [string]$ReportsDir = "docs/reports"
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Aggregate NAS field reports (S1-S6 + decisions) into NAS_FIELD_AGGREGATOR_<date>.md"
    Write-Host "Usage: .\scripts\nas-field-aggregate.ps1 -Date 2026-04-27"
    Write-Host "Exit codes: 0=success, 1=aggregator/report missing, 2=parse failure"
    exit 0
}

function Resolve-ProjectPath {
    param([string]$PathValue)
    $projectRoot = Split-Path -Parent $PSScriptRoot
    if ([System.IO.Path]::IsPathRooted($PathValue)) { return $PathValue }
    return (Join-Path $projectRoot $PathValue)
}

function Get-FieldReportState {
    param(
        [string]$Path,
        [string]$Platform
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        return [PSCustomObject]@{
            Platform = $Platform
            S1 = "MISSING REPORT"
            S2 = "MISSING REPORT"
            S3 = "MISSING REPORT"
            S4 = "MISSING REPORT"
            S5 = "MISSING REPORT"
            S6 = "MISSING REPORT"
            Decision = "NO-GO"
            IsPending = $true
        }
    }

    $raw = Get-Content -LiteralPath $Path -Raw
    $getScenario = {
        param([string]$Id)
        $m = [regex]::Match($raw, "(?m)^- $Id [^:]*:\s*(.+?)\s*$")
        if ($m.Success) { return $m.Groups[1].Value.Trim() }
        return "UNKNOWN"
    }
    $s1 = & $getScenario "S1"
    $s2 = & $getScenario "S2"
    $s3 = & $getScenario "S3"
    $s4 = & $getScenario "S4"
    $s5 = & $getScenario "S5"
    $s6 = & $getScenario "S6"

    $d = [regex]::Match($raw, "(?m)^- Result:\s*(.+?)\s*$")
    $decision = if ($d.Success) { $d.Groups[1].Value.Trim() } else { "PRELIMINARY" }

    $isPending = @($s2,$s3,$s4,$s5,$s6) | Where-Object { $_ -match "PENDING|TBD|UNKNOWN|MISSING REPORT|\|" }

    [PSCustomObject]@{
        Platform = $Platform
        S1 = $s1
        S2 = $s2
        S3 = $s3
        S4 = $s4
        S5 = $s5
        S6 = $s6
        Decision = $decision
        IsPending = ($isPending.Count -gt 0) -or ($decision -match "PRELIMINARY|\|")
    }
}

function Update-LineValue {
    param([string]$Content, [string]$Prefix, [string]$Value)
    $escaped = [regex]::Escape($Prefix)
    return [regex]::Replace($Content, "(?m)^$escaped.*$", "$Prefix$Value")
}

$reportsAbs = Resolve-ProjectPath -PathValue $ReportsDir
$aggPath = Join-Path $reportsAbs "NAS_FIELD_AGGREGATOR_$Date.md"
if (-not (Test-Path -LiteralPath $aggPath)) {
    Write-Error "Aggregator file not found: $aggPath"
    exit 1
}

$reports = @(
    @{ Platform = "Synology"; Path = Join-Path $reportsAbs "NAS_FIELD_REPORT_SYNOLOGY_$Date.md" },
    @{ Platform = "QNAP"; Path = Join-Path $reportsAbs "NAS_FIELD_REPORT_QNAP_$Date.md" },
    @{ Platform = "Asustor"; Path = Join-Path $reportsAbs "NAS_FIELD_REPORT_ASUSTOR_$Date.md" },
    @{ Platform = "TrueNAS"; Path = Join-Path $reportsAbs "NAS_FIELD_REPORT_TRUENAS_$Date.md" }
) | ForEach-Object { Get-FieldReportState -Path $_.Path -Platform $_.Platform }

$allFinal = ($reports | Where-Object { -not $_.IsPending }).Count -eq $reports.Count
$anyNoGo = ($reports | Where-Object { $_.Decision -match "NO-GO" }).Count -gt 0
$anyConditional = ($reports | Where-Object { $_.Decision -match "CONDITIONAL GO" }).Count -gt 0

$fieldValidation = if (-not $allFinal) {
    "PENDING"
} elseif ($anyNoGo) {
    "NO-GO"
} elseif ($anyConditional) {
    "CONDITIONAL GO"
} else {
    "GO"
}

$finalDecision = if ($fieldValidation -eq "GO" -or $fieldValidation -eq "CONDITIONAL GO") {
    "GO"
} elseif ($fieldValidation -eq "PENDING") {
    "NO-GO (until field validation complete)"
} else {
    "NO-GO"
}

$agg = Get-Content -LiteralPath $aggPath -Raw
$table = @(
    "| Platform | S1 | S2 | S3 | S4 | S5 | S6 | Decision |",
    "|---|---|---|---|---|---|---|---|"
)
foreach ($r in $reports) {
    $table += "| $($r.Platform) | $($r.S1) | $($r.S2) | $($r.S3) | $($r.S4) | $($r.S5) | $($r.S6) | $($r.Decision) |"
}
$tableBlock = ($table -join [Environment]::NewLine)
$agg = [regex]::Replace(
    $agg,
    "(?ms)^\| Platform \| S1 \| S2 \| S3 \| S4 \| S5 \| S6 \| Decision \|\r?\n^\|---\|---\|---\|---\|---\|---\|---\|---\|.*?(?=^\s*$|\r?\n## )",
    $tableBlock
)

$agg = Update-LineValue -Content $agg -Prefix "- Field validation: " -Value $fieldValidation
$agg = Update-LineValue -Content $agg -Prefix "- Final decision: " -Value $finalDecision

Set-Content -LiteralPath $aggPath -Value $agg -Encoding UTF8

Write-Host "Updated aggregator: $aggPath" -ForegroundColor Green
Write-Host "Field validation: $fieldValidation" -ForegroundColor Cyan
Write-Host "Final decision: $finalDecision" -ForegroundColor Cyan
exit 0

