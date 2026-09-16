[CmdletBinding()]
param(
    [switch]$ShowHelp,
    [ValidateSet("local", "ci")]
    [string]$RunProfile = "local",
    [string]$ReportDir = "diagnostics\phase1-e2e-smoke",
    [string]$AcceptanceMatrixDir = "docs\reports",
    [switch]$SkipWebTypecheck
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Phase1 minimal critical E2E smoke runner"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\phase1-critical-e2e-smoke.ps1"
    Write-Host "  .\scripts\phase1-critical-e2e-smoke.ps1 -RunProfile ci"
    Write-Host "  .\scripts\phase1-critical-e2e-smoke.ps1 -SkipWebTypecheck"
    Write-Host ""
    Write-Host "Checks:"
    Write-Host "  1) shared desktop tests: DB migration + recording integration"
    Write-Host "  2) server API build"
    Write-Host "  3) web TypeScript compile (optional)"
    Write-Host ""
    Write-Host "Output:"
    Write-Host "  diagnostics/phase1-e2e-smoke/phase1-critical-e2e-smoke-<run-id>.md|json"
    Write-Host "  docs/reports/PHASE1_MINIMAL_ACCEPTANCE_MATRIX_<run-id>.md|json"
    exit 0
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$gradlew = Join-Path $projectRoot "gradlew.bat"
$resolvedReportDir = Join-Path $projectRoot $ReportDir
$resolvedAcceptanceMatrixDir = Join-Path $projectRoot $AcceptanceMatrixDir
$runId = Get-Date -Format "yyyyMMdd-HHmmss"
$steps = New-Object System.Collections.Generic.List[object]

function Add-Step {
    param(
        [string]$Name,
        [string]$Status,
        [string]$Details = ""
    )
    $steps.Add([PSCustomObject]@{
        name = $Name
        status = $Status
        details = $Details
    }) | Out-Null
}

function Invoke-GradleStep {
    param(
        [string]$Name,
        [string[]]$TaskArgs
    )
    Write-Host ("==> gradlew {0}" -f ($TaskArgs -join " ")) -ForegroundColor Cyan
    & $gradlew @TaskArgs --no-daemon
    if ($LASTEXITCODE -ne 0) {
        throw "$Name failed with exit $LASTEXITCODE"
    }
    Add-Step -Name $Name -Status "PASS"
}

function Write-Report {
    param(
        [string]$Overall,
        [string]$Message
    )
    if (-not (Test-Path $resolvedReportDir)) {
        New-Item -ItemType Directory -Path $resolvedReportDir -Force | Out-Null
    }
    $mdPath = Join-Path $resolvedReportDir ("phase1-critical-e2e-smoke-{0}.md" -f $runId)
    $jsonPath = Join-Path $resolvedReportDir ("phase1-critical-e2e-smoke-{0}.json" -f $runId)

    $md = @(
        "# Phase1 Minimal Critical E2E Smoke",
        "",
        "- Run ID: $runId",
        "- Profile: **$RunProfile**",
        "- Overall: **$Overall**",
        "- Message: $Message",
        "",
        "| Step | Status | Details |",
        "|---|---|---|"
    )
    foreach ($step in $steps) {
        $details = ([string]$step.details).Replace("|", "/")
        $md += "| $($step.name) | $($step.status) | $details |"
    }
    $md -join [Environment]::NewLine | Set-Content -Path $mdPath -Encoding UTF8

    $stepRows = @($steps | ForEach-Object {
        [PSCustomObject]@{
            name = [string]$_.name
            status = [string]$_.status
            details = [string]$_.details
        }
    })

    $reportObj = [PSCustomObject]@{
        runId = $runId
        profile = $RunProfile
        timestampUtc = [DateTime]::UtcNow.ToString("o")
        overall = $Overall
        message = $Message
        steps = $stepRows
    }
    $reportObj | ConvertTo-Json -Depth 8 | Set-Content -Path $jsonPath -Encoding UTF8

    Write-Host ("Report: {0}" -f $mdPath) -ForegroundColor Green
    Write-Host ("Json: {0}" -f $jsonPath) -ForegroundColor Green
}

function Write-AcceptanceMatrix {
    param(
        [string]$Overall
    )
    if (-not (Test-Path $resolvedAcceptanceMatrixDir)) {
        New-Item -ItemType Directory -Path $resolvedAcceptanceMatrixDir -Force | Out-Null
    }
    $matrixMdPath = Join-Path $resolvedAcceptanceMatrixDir ("PHASE1_MINIMAL_ACCEPTANCE_MATRIX_{0}.md" -f $runId)
    $matrixJsonPath = Join-Path $resolvedAcceptanceMatrixDir ("PHASE1_MINIMAL_ACCEPTANCE_MATRIX_{0}.json" -f $runId)

    $step103 = ($steps | Where-Object { $_.name -eq "shared-db-migration-and-recording-tests" } | Select-Object -First 1)
    $status103 = if ($null -ne $step103 -and $null -ne $step103.status) { [string]$step103.status } else { "UNKNOWN" }

    $rows = @(
        [PSCustomObject]@{
            taskId = "1.10.3"
            scenario = "DB migration + recording core integration"
            status = $status103
            evidence = "diagnostics/phase1-e2e-smoke/phase1-critical-e2e-smoke-$runId.md"
            dod = "migration + recording integration checks are green"
        },
        [PSCustomObject]@{
            taskId = "1.10.4"
            scenario = "Minimal critical E2E smoke (server + shared + optional web)"
            status = $Overall
            evidence = "diagnostics/phase1-e2e-smoke/phase1-critical-e2e-smoke-$runId.md"
            dod = "single command produces reproducible smoke evidence"
        }
    )

    $md = @(
        "# Phase1 Minimal Acceptance Matrix",
        "",
        "- Run ID: $runId",
        "- Generated by: scripts/phase1-critical-e2e-smoke.ps1",
        "- Overall smoke: **$Overall**",
        "",
        "| Task ID | Scenario | Status | Evidence | DoD |",
        "|---|---|---|---|---|"
    )
    foreach ($r in $rows) {
        $md += "| $($r.taskId) | $($r.scenario) | $($r.status) | $($r.evidence) | $($r.dod) |"
    }
    $md -join [Environment]::NewLine | Set-Content -Path $matrixMdPath -Encoding UTF8
    [PSCustomObject]@{
        runId = $runId
        generatedAtUtc = [DateTime]::UtcNow.ToString("o")
        overall = $Overall
        rows = $rows
    } | ConvertTo-Json -Depth 8 | Set-Content -Path $matrixJsonPath -Encoding UTF8

    Write-Host ("Acceptance matrix: {0}" -f $matrixMdPath) -ForegroundColor Green
    Write-Host ("Acceptance matrix json: {0}" -f $matrixJsonPath) -ForegroundColor Green
}

try {
    if (-not (Test-Path $gradlew)) {
        throw "gradlew.bat not found: $gradlew"
    }

    Invoke-GradleStep -Name "shared-db-migration-and-recording-tests" -TaskArgs @(
        ":shared:desktopTest",
        "--tests",
        "*MigrationManagerIntegrationTest*",
        "--tests",
        "*RecordingRepositorySqlDelightIntegrationTest*"
    )
    Invoke-GradleStep -Name "server-api-build" -TaskArgs @(":server:api:build")

    if (-not $SkipWebTypecheck) {
        $webDir = Join-Path $projectRoot "server\web"
        if (-not (Test-Path $webDir)) {
            throw "server\web not found"
        }
        Push-Location $webDir
        try {
            Write-Host "==> npx tsc --noEmit" -ForegroundColor Cyan
            npx tsc --noEmit
            if ($LASTEXITCODE -ne 0) {
                throw "web-tsc failed with exit $LASTEXITCODE"
            }
            Add-Step -Name "web-tsc" -Status "PASS"
        }
        finally {
            Pop-Location
        }
    } else {
        Add-Step -Name "web-tsc" -Status "SKIP" -Details "skipped by flag"
    }

    Write-Report -Overall "SUCCESS" -Message "All enabled minimal critical E2E checks passed."
    Write-AcceptanceMatrix -Overall "SUCCESS"
    exit 0
}
catch {
    $errorDetails = "{0} :: {1}" -f $_.Exception.GetType().FullName, $_.Exception.Message
    Add-Step -Name "exception" -Status "FAIL" -Details $errorDetails
    Write-Report -Overall "FAIL" -Message $errorDetails
    Write-AcceptanceMatrix -Overall "FAIL"
    Write-Host ("Phase1 critical E2E smoke failed: {0}" -f $errorDetails) -ForegroundColor Red
    if ($_.ScriptStackTrace) {
        Write-Host $_.ScriptStackTrace -ForegroundColor DarkGray
    }
    exit 1
}
