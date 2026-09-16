param(
    [switch]$AsJson,
    [switch]$ShowHelp
)

if ($ShowHelp) {
    Write-Host "Run one-command local smoke for network layer 1.4."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\run-network-layer-1-4-local-smoke.ps1"
    Write-Host "  .\scripts\run-network-layer-1-4-local-smoke.ps1 -AsJson"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0 - all steps passed"
    Write-Host "  1 - at least one step failed"
    exit 0
}

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$runId = Get-Date -Format "yyyyMMdd-HHmmss"

$steps = New-Object System.Collections.Generic.List[object]
$failed = New-Object System.Collections.Generic.List[string]

function Invoke-Step {
    param(
        [string]$Name,
        [scriptblock]$Action
    )
    $startedAt = Get-Date
    try {
        & $Action
        $steps.Add([PSCustomObject]@{
            name = $Name
            status = "PASS"
            startedAt = $startedAt.ToString("s")
            finishedAt = (Get-Date).ToString("s")
            error = ""
        })
    } catch {
        $msg = $_.Exception.Message
        $failed.Add($Name)
        $steps.Add([PSCustomObject]@{
            name = $Name
            status = "FAIL"
            startedAt = $startedAt.ToString("s")
            finishedAt = (Get-Date).ToString("s")
            error = $msg
        })
        throw
    }
}

try {
    Push-Location $projectRoot
    Write-Host "=== network-layer 1.4 local smoke ===" -ForegroundColor Cyan

    Invoke-Step -Name "preflight local-smoke" -Action {
        & .\scripts\check-network-layer-ci-prerequisites.ps1 -RequireLiveChecks -RequireStatusSyncAudit -LocalSmoke
        if ($LASTEXITCODE -ne 0) {
            throw "check-network-layer-ci-prerequisites exited with code $LASTEXITCODE"
        }
    }

    Invoke-Step -Name "status-sync strict audit" -Action {
        & .\scripts\sync-network-layer-1-4-status.ps1 -FailOnLegacyConflicts
        if ($LASTEXITCODE -ne 0) {
            throw "sync-network-layer-1-4-status exited with code $LASTEXITCODE"
        }
    }

    Invoke-Step -Name "aggregate decision report" -Action {
        & .\scripts\show-latest-network-layer-1-4-decision.ps1 -WriteReport
        if ($LASTEXITCODE -ne 0) {
            throw "show-latest-network-layer-1-4-decision exited with code $LASTEXITCODE"
        }
    }
}
catch {
    # status is reflected in summary below
}
finally {
    Pop-Location
}

$status = if ($failed.Count -eq 0) { "PASS" } else { "FAIL" }
$summary = [PSCustomObject]@{
    runId = $runId
    generatedAt = (Get-Date).ToString("s")
    status = $status
    steps = $steps.ToArray()
    failedSteps = $failed.ToArray()
}

if ($AsJson) {
    $summary | ConvertTo-Json -Depth 6
} else {
    Write-Host "Local smoke status: $status" -ForegroundColor $(if ($status -eq "PASS") { "Green" } else { "Red" })
}

if ($failed.Count -gt 0) {
    exit 1
}
exit 0
param(
    [switch]$AsJson,
    [switch]$ShowHelp
)

if ($ShowHelp) {
    Write-Host "Run one-command local smoke for network layer 1.4."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\run-network-layer-1-4-local-smoke.ps1"
    Write-Host "  .\scripts\run-network-layer-1-4-local-smoke.ps1 -AsJson"
    Write-Host ""
    Write-Host "Steps:"
    Write-Host "  1) check-network-layer-ci-prerequisites.ps1 -RequireLiveChecks -RequireStatusSyncAudit -LocalSmoke"
    Write-Host "  2) sync-network-layer-1-4-status.ps1 -FailOnLegacyConflicts"
    Write-Host "  3) show-latest-network-layer-1-4-decision.ps1 -WriteReport"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0 - all steps passed"
    Write-Host "  1 - at least one step failed"
    exit 0
}

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$runId = Get-Date -Format "yyyyMMdd-HHmmss"

$steps = New-Object System.Collections.Generic.List[object]
$failed = New-Object System.Collections.Generic.List[string]

function Invoke-Step {
    param(
        [string]$Name,
        [scriptblock]$Action
    )

    $startedAt = Get-Date
    try {
        & $Action
        $steps.Add([PSCustomObject]@{
            name = $Name
            status = "PASS"
            startedAt = $startedAt.ToString("s")
            finishedAt = (Get-Date).ToString("s")
            error = ""
        })
    } catch {
        $msg = $_.Exception.Message
        $failed.Add($Name)
        $steps.Add([PSCustomObject]@{
            name = $Name
            status = "FAIL"
            startedAt = $startedAt.ToString("s")
            finishedAt = (Get-Date).ToString("s")
            error = $msg
        })
        throw
    }
}

try {
    Push-Location $projectRoot

    Write-Host "=== network-layer 1.4 local smoke ===" -ForegroundColor Cyan

    Invoke-Step -Name "preflight local-smoke" -Action {
        & .\scripts\check-network-layer-ci-prerequisites.ps1 -RequireLiveChecks -RequireStatusSyncAudit -LocalSmoke
        if ($LASTEXITCODE -ne 0) {
            throw "check-network-layer-ci-prerequisites exited with code $LASTEXITCODE"
        }
    }

    Invoke-Step -Name "status-sync strict audit" -Action {
        & .\scripts\sync-network-layer-1-4-status.ps1 -FailOnLegacyConflicts
        if ($LASTEXITCODE -ne 0) {
            throw "sync-network-layer-1-4-status exited with code $LASTEXITCODE"
        }
    }

    Invoke-Step -Name "aggregate decision report" -Action {
        & .\scripts\show-latest-network-layer-1-4-decision.ps1 -WriteReport
        if ($LASTEXITCODE -ne 0) {
            throw "show-latest-network-layer-1-4-decision exited with code $LASTEXITCODE"
        }
    }
}
catch {
    # rethrow after summary is prepared below
}
finally {
    Pop-Location
}

$status = if ($failed.Count -eq 0) { "PASS" } else { "FAIL" }
$summary = [PSCustomObject]@{
    runId = $runId
    generatedAt = (Get-Date).ToString("s")
    status = $status
    steps = $steps.ToArray()
    failedSteps = $failed.ToArray()
}

if ($AsJson) {
    $summary | ConvertTo-Json -Depth 6
} else {
    Write-Host "Local smoke status: $status" -ForegroundColor $(if ($status -eq "PASS") { "Green" } else { "Red" })
}

if ($failed.Count -gt 0) {
    exit 1
}
exit 0
