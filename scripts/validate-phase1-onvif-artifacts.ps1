param(
    [switch]$ShowHelp,
    [string]$DiagnosticsDir = "diagnostics\onvif-events",
    [string]$AcceptanceJsonPath = "",
    [switch]$AsJson = $false
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Validate Phase 1 ONVIF artifacts consistency"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\validate-phase1-onvif-artifacts.ps1"
    Write-Host "  .\scripts\validate-phase1-onvif-artifacts.ps1 -AsJson"
    Write-Host "  .\scripts\validate-phase1-onvif-artifacts.ps1 -AcceptanceJsonPath diagnostics/onvif-events/phase1-onvif-acceptance-<run-id>.json"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -DiagnosticsDir  Scan for latest phase1-onvif-acceptance-*.json when -AcceptanceJsonPath omitted."
    Write-Host "  -AcceptanceJsonPath  Absolute or repo-relative path to a specific acceptance json."
    Write-Host "  -AsJson  Machine-readable result to stdout."
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  PASS"
    Write-Host "  1  WARN"
    Write-Host "  2  FAIL"
    exit 0
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$diagRoot = Join-Path $projectRoot $DiagnosticsDir

function Resolve-AcceptanceJson {
    if ($AcceptanceJsonPath) {
        if (Test-Path $AcceptanceJsonPath) { return (Get-Item -LiteralPath $AcceptanceJsonPath).FullName }
        $candidate = Join-Path $projectRoot $AcceptanceJsonPath
        if (Test-Path $candidate) { return (Get-Item -LiteralPath $candidate).FullName }
        throw "Acceptance json not found: $AcceptanceJsonPath"
    }

    if (-not (Test-Path $diagRoot)) {
        throw "Diagnostics directory not found: $diagRoot"
    }

    $latest = Get-ChildItem -Path $diagRoot -Filter "phase1-onvif-acceptance-*.json" -File |
        Sort-Object LastWriteTimeUtc -Descending |
        Select-Object -First 1
    if (-not $latest) {
        throw "No phase1-onvif-acceptance json found in $diagRoot"
    }
    return $latest.FullName
}

$issues = New-Object System.Collections.Generic.List[string]
$warnings = New-Object System.Collections.Generic.List[string]
$acceptancePath = Resolve-AcceptanceJson
$data = Get-Content -LiteralPath $acceptancePath -Raw | ConvertFrom-Json

if (-not $data.runId) { $issues.Add("Missing runId.") }
if (-not $data.statuses) { $issues.Add("Missing statuses block.") }
if (-not $data.decision) { $issues.Add("Missing decision block.") }
if (-not $data.artifacts) { $issues.Add("Missing artifacts block.") }

if ($data.artifacts.acceptanceJson) {
    $selfPath = [string]$data.artifacts.acceptanceJson
    if (-not (Test-Path $selfPath)) {
        $issues.Add("artifacts.acceptanceJson path does not exist: $selfPath")
    } elseif ((Get-Item -LiteralPath $selfPath).FullName -ne (Get-Item -LiteralPath $acceptancePath).FullName) {
        $issues.Add("artifacts.acceptanceJson does not reference current acceptance json.")
    }
} else {
    $warnings.Add("artifacts.acceptanceJson is not set.")
}

if ($data.artifacts.evidenceReport -and -not (Test-Path ([string]$data.artifacts.evidenceReport))) {
    $warnings.Add("artifacts.evidenceReport path does not exist: $($data.artifacts.evidenceReport)")
}
if ($data.artifacts.latestOnvifSummary -and -not (Test-Path ([string]$data.artifacts.latestOnvifSummary))) {
    $warnings.Add("artifacts.latestOnvifSummary path does not exist: $($data.artifacts.latestOnvifSummary)")
}

$requireOnvifEvidence = $false
if ($data.policy -and $null -ne $data.policy.requireOnvifEvidence) {
    $requireOnvifEvidence = [bool]$data.policy.requireOnvifEvidence
} else {
    $warnings.Add("policy.requireOnvifEvidence is missing; assumed false.")
}

$onvifStatus = [string]$data.statuses.onvif143
$goNoGo = [string]$data.decision.goNoGo

if ($requireOnvifEvidence -and ($onvifStatus -eq "PARTIAL" -or $onvifStatus -eq "NOT_RUN") -and $goNoGo -ne "NO_GO") {
    $issues.Add("Policy requires ONVIF evidence, but decision is not NO_GO for onvif143=$onvifStatus.")
}
if (-not $requireOnvifEvidence -and ($onvifStatus -eq "PARTIAL" -or $onvifStatus -eq "NOT_RUN") -and $goNoGo -eq "GO") {
    $issues.Add("Soft policy with incomplete ONVIF evidence cannot have GO decision.")
}

if ($data.diagnostics) {
    $sourceJson = [string]$data.diagnostics.sourceJson
    if ($sourceJson -and (Test-Path $sourceJson)) {
        if ((Get-Item -LiteralPath $sourceJson).FullName -ne (Get-Item -LiteralPath $acceptancePath).FullName) {
            $issues.Add("diagnostics.sourceJson does not match current acceptance json.")
        }
    } elseif ($sourceJson) {
        $issues.Add("diagnostics.sourceJson path does not exist: $sourceJson")
    } else {
        $warnings.Add("diagnostics.sourceJson is empty.")
    }
} else {
    $warnings.Add("diagnostics block is missing.")
}

$status = if ($issues.Count -gt 0) { "FAIL" } elseif ($warnings.Count -gt 0) { "WARN" } else { "PASS" }

$result = [ordered]@{
    status = $status
    acceptanceJson = $acceptancePath
    issues = $issues
    warnings = $warnings
}

if ($AsJson) {
    $result | ConvertTo-Json -Depth 8
} else {
    Write-Host "Phase1 ONVIF artifacts validation: $status" -ForegroundColor $(if ($status -eq "PASS") { "Green" } elseif ($status -eq "WARN") { "Yellow" } else { "Red" })
    Write-Host "Acceptance json: $acceptancePath"
    if ($issues.Count -gt 0) {
        Write-Host ""
        Write-Host "Issues:" -ForegroundColor Red
        $issues | ForEach-Object { Write-Host "- $_" -ForegroundColor Red }
    }
    if ($warnings.Count -gt 0) {
        Write-Host ""
        Write-Host "Warnings:" -ForegroundColor Yellow
        $warnings | ForEach-Object { Write-Host "- $_" -ForegroundColor Yellow }
    }
}

if ($status -eq "FAIL") { exit 2 }
if ($status -eq "WARN") { exit 1 }
exit 0
