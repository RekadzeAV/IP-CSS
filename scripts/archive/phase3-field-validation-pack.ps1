[CmdletBinding()]
param(
    [string]$Version = "Alfa-0.1.1",
    [string]$Tester = "TBD",
    [string]$OutputDir = "docs/reports",
    [switch]$ForceOverwrite,
    [switch]$ShowHelp
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Generate one-command Phase 3 field validation pack (S2-S6)."
    Write-Host "Usage: .\scripts\phase3-field-validation-pack.ps1 -Version Alfa-0.1.1 -Tester <name> [-ForceOverwrite]"
    exit 0
}

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

$dateStamp = Get-Date -Format "yyyy-MM-dd"
$outputAbs = Join-Path $projectRoot $OutputDir
if (-not (Test-Path $outputAbs)) {
    New-Item -ItemType Directory -Path $outputAbs -Force | Out-Null
}

function New-FieldTemplate {
    param(
        [string]$Path,
        [string]$Platform,
        [string]$Artifact,
        [string]$Device
    )
    $content = @(
        "# NAS Field Validation Report - $Platform",
        "",
        "- Date: $dateStamp",
        "- Tester: $Tester",
        "- Version: $Version",
        "- Platform: $Platform",
        "- Device model: $Device",
        "- Artifact: $Artifact",
        "",
        "## Preflight",
        "",
        "- Checksum verified: PASS|FAIL",
        "- Java/runtime requirements: PASS|FAIL",
        "- Free disk >= 2GB: PASS|FAIL",
        "- Ports 8080/8081 available: PASS|FAIL",
        "",
        "## Scenarios",
        "",
        "- S1 Fresh install: PASS|FAIL",
        "- S2 Basic health: PASS|FAIL",
        "- S3 Restart: PASS|FAIL",
        "- S4 Reboot persistence: PASS|FAIL",
        "- S5 Upgrade: PASS|FAIL",
        "- S6 Uninstall: PASS|FAIL",
        "",
        "## Evidence",
        "",
        "- Web UI (`http://[NAS-IP]:8080`): PASS|FAIL",
        "- API health (`http://[NAS-IP]:8081/health`): PASS|FAIL",
        "- Logs path: [path]",
        "",
        "## Issues",
        "",
        "- Issue 1: [description]",
        "- Issue 2: [description]",
        "",
        "## Decision",
        "",
        "- Result: GO|CONDITIONAL GO|NO-GO",
        "- Notes: [summary]"
    ) -join [Environment]::NewLine
    if ((Test-Path -LiteralPath $Path) -and (-not $ForceOverwrite)) {
        Write-Host "Skip existing field report (use -ForceOverwrite to replace): $Path" -ForegroundColor Yellow
        return
    }
    Set-Content -Path $Path -Value $content -Encoding UTF8
}

$synologyPath = Join-Path $outputAbs "NAS_FIELD_REPORT_SYNOLOGY_$dateStamp.md"
$qnapPath = Join-Path $outputAbs "NAS_FIELD_REPORT_QNAP_$dateStamp.md"
$asustorPath = Join-Path $outputAbs "NAS_FIELD_REPORT_ASUSTOR_$dateStamp.md"
$truenasPath = Join-Path $outputAbs "NAS_FIELD_REPORT_TRUENAS_$dateStamp.md"
$aggregatorPath = Join-Path $outputAbs "NAS_FIELD_AGGREGATOR_$dateStamp.md"
$checklistPath = Join-Path $outputAbs "NAS_FIELD_ONE_PAGE_CHECKLIST_$dateStamp.md"

New-FieldTemplate -Path $synologyPath -Platform "Synology DSM" -Artifact "build/ip-css-$Version-synology-[arch].spk" -Device "[model]"
New-FieldTemplate -Path $qnapPath -Platform "QNAP QTS" -Artifact "build/ip-css-$Version-qnap-[arch].qpkg" -Device "[model]"
New-FieldTemplate -Path $asustorPath -Platform "Asustor ADM" -Artifact "build/ip-css-$Version-asustor-[arch].apk" -Device "[model]"
New-FieldTemplate -Path $truenasPath -Platform "TrueNAS CORE/SCALE" -Artifact "build/truenas-$Version/" -Device "[host/model]"

$checklist = @(
    "# NAS Field Validation One-Page Checklist ($dateStamp)",
    "",
    "- Tester: $Tester",
    "- Version: $Version",
    "",
    "1. Verify artifacts/checksums in build/ and build/checksums/.",
    "2. Run S1-S6 on Synology and fill `NAS_FIELD_REPORT_SYNOLOGY_$dateStamp.md`.",
    "3. Run S1-S6 on QNAP and fill `NAS_FIELD_REPORT_QNAP_$dateStamp.md`.",
    "4. Run S1-S6 on Asustor and fill `NAS_FIELD_REPORT_ASUSTOR_$dateStamp.md`.",
    "5. Run S1-S6 on TrueNAS and fill `NAS_FIELD_REPORT_TRUENAS_$dateStamp.md`.",
    "6. Consolidate final decision in `NAS_FIELD_AGGREGATOR_$dateStamp.md`."
) -join [Environment]::NewLine
if ((Test-Path -LiteralPath $checklistPath) -and (-not $ForceOverwrite)) {
    Write-Host "Skip existing checklist (use -ForceOverwrite to replace): $checklistPath" -ForegroundColor Yellow
} else {
    Set-Content -Path $checklistPath -Value $checklist -Encoding UTF8
}

$aggregator = @(
    "# NAS Field Aggregator ($dateStamp)",
    "",
    "- Tester: $Tester",
    "- Version: $Version",
    "",
    "| Platform | S1 | S2 | S3 | S4 | S5 | S6 | Decision |",
    "|---|---|---|---|---|---|---|---|",
    "| Synology | TBD | TBD | TBD | TBD | TBD | TBD | TBD |",
    "| QNAP | TBD | TBD | TBD | TBD | TBD | TBD | TBD |",
    "| Asustor | TBD | TBD | TBD | TBD | TBD | TBD | TBD |",
    "| TrueNAS | TBD | TBD | TBD | TBD | TBD | TBD | TBD |",
    "",
    "## Final Program Decision",
    "",
    "- Packaging precheck: PASS",
    "- Field validation: GO|CONDITIONAL GO|NO-GO",
    "- Final decision: GO|NO-GO",
    "",
    "## Evidence Files",
    "",
    "- NAS_FIELD_REPORT_SYNOLOGY_$dateStamp.md",
    "- NAS_FIELD_REPORT_QNAP_$dateStamp.md",
    "- NAS_FIELD_REPORT_ASUSTOR_$dateStamp.md",
    "- NAS_FIELD_REPORT_TRUENAS_$dateStamp.md"
) -join [Environment]::NewLine
if ((Test-Path -LiteralPath $aggregatorPath) -and (-not $ForceOverwrite)) {
    Write-Host "Skip existing aggregator (use -ForceOverwrite to replace): $aggregatorPath" -ForegroundColor Yellow
} else {
    Set-Content -Path $aggregatorPath -Value $aggregator -Encoding UTF8
}

Write-Host "Field validation pack generated:" -ForegroundColor Green
Write-Host " - $checklistPath"
Write-Host " - $aggregatorPath"
Write-Host " - $synologyPath"
Write-Host " - $qnapPath"
Write-Host " - $asustorPath"
Write-Host " - $truenasPath"
