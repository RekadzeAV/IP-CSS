[CmdletBinding()]
param(
    [switch]$ShowHelp,
    [string]$Date = (Get-Date -Format "yyyy-MM-dd")
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Sync release status lines across key docs from current reports."
    Write-Host "Usage: .\scripts\sync-release-status-docs.ps1 -Date 2026-04-27"
    Write-Host "Sources:"
    Write-Host "  - docs/reports/NAS_FIELD_AGGREGATOR_<date>.md"
    Write-Host "  - release-build/test/video-e2e-go-no-go-report.md"
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
    return $Content
}

function Get-MatchValue {
    param([string]$Content, [string]$Pattern, [string]$DefaultValue)
    $m = [regex]::Match($Content, $Pattern)
    if ($m.Success) { return $m.Groups[1].Value.Trim() }
    return $DefaultValue
}

$aggPath = Resolve-ProjectPath -PathValue "docs/reports/NAS_FIELD_AGGREGATOR_$Date.md"
$gatePath = Resolve-ProjectPath -PathValue "release-build/test/video-e2e-go-no-go-report.md"
$currentStatusPath = Resolve-ProjectPath -PathValue "docs/status/CURRENT_STATUS.md"
$projectStatusPath = Resolve-ProjectPath -PathValue "docs/status/PROJECT_STATUS.md"
$docsReadmePath = Resolve-ProjectPath -PathValue "docs/README.md"
$syncReportPath = Resolve-ProjectPath -PathValue "docs/reports/DOCUMENTATION_STATUS_SYNC_2026-04-27.md"

if (-not (Test-Path -LiteralPath $aggPath)) { throw "Aggregator not found: $aggPath" }
if (-not (Test-Path -LiteralPath $gatePath)) { throw "Gate report not found: $gatePath" }

$agg = Get-Content -LiteralPath $aggPath -Raw
$gate = Get-Content -LiteralPath $gatePath -Raw

$fieldValidation = Get-MatchValue -Content $agg -Pattern "(?m)^- Field validation:\s*(.+?)\s*$" -DefaultValue "PENDING"
$programFinal = Get-MatchValue -Content $agg -Pattern "(?m)^- Final decision:\s*(.+?)\s*$" -DefaultValue "NO-GO"
$strictDecision = Get-MatchValue -Content $gate -Pattern "(?m)^- Release decision:\s*\*\*(.+?)\*\*" -DefaultValue "NO-GO"
$profileDecision = Get-MatchValue -Content $gate -Pattern "(?m)^- Release decision \(profile-aware\):\s*\*\*(.+?)\*\*" -DefaultValue "NO-GO"
$runtimeDecision = Get-MatchValue -Content $gate -Pattern "(?m)^- Runtime decision \(1\.8\.A/1\.8\.B/1\.8\.C\):\s*\*\*(.+?)\*\*" -DefaultValue "NO-GO"

$snapshotLine = "strict `"$strictDecision`", profile-aware `"$profileDecision`", runtime decision `"$runtimeDecision`""

foreach ($path in @($currentStatusPath, $projectStatusPath, $docsReadmePath, $syncReportPath)) {
    if (-not (Test-Path -LiteralPath $path)) { continue }
    $content = Get-Content -LiteralPath $path -Raw

    # Append/refresh a dedicated auto-sync marker line.
    if ($content -match "(?m)^- Auto-synced gate snapshot:") {
        $content = Update-LineValue -Content $content -Prefix "- Auto-synced gate snapshot: " -Value "$snapshotLine; field validation `"$fieldValidation`"; final decision `"$programFinal`"."
    } else {
        $content = $content.TrimEnd() + [Environment]::NewLine + "- Auto-synced gate snapshot: $snapshotLine; field validation `"$fieldValidation`"; final decision `"$programFinal`"." + [Environment]::NewLine
    }

    Set-Content -LiteralPath $path -Value $content -Encoding UTF8
}

Write-Host "Synced docs status snapshot from:" -ForegroundColor Green
Write-Host " - $aggPath"
Write-Host " - $gatePath"
Write-Host "Field validation: $fieldValidation" -ForegroundColor Cyan
Write-Host "Final decision: $programFinal" -ForegroundColor Cyan
Write-Host "Gate snapshot: $snapshotLine" -ForegroundColor Cyan
exit 0

