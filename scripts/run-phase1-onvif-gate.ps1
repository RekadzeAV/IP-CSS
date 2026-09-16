param(
    [switch]$ShowHelp,
    [string]$ApiBase = "http://localhost:8080",
    [string]$AdminUser = "admin",
    [int]$CameraIndex = 0,
    [int]$WaitSeconds = 15,
    [bool]$EnableWebSocketCheck = $true,
    [int]$MaxEventWaitSeconds = 30,
    [int]$EventPollIntervalSeconds = 2,
    [int]$PollIntervalMs = 5000,
    [int]$PullTimeoutMs = 500,
    [int]$LatencyBufferMs = 10000,
    [string]$WebSocketUrl = "",
    [bool]$AllowApiUnavailable = $true,
    [switch]$NoPrompt,
    [switch]$RequireOnvifEvidence,
    [switch]$FailOnNoGo,
    [switch]$FailOnConditional,
    [switch]$SkipArtifactsValidation,
    [switch]$ShowLatestOnRunError,
    [switch]$AsJson = $false
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "ONVIF Phase 1 gate (secure acceptance + latest decision + artifact validation)"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\run-phase1-onvif-gate.ps1 -ApiBase http://localhost:8080 -AdminUser admin"
    Write-Host "  .\scripts\run-phase1-onvif-gate.ps1 -NoPrompt -RequireOnvifEvidence -FailOnNoGo -FailOnConditional"
    Write-Host "  .\scripts\run-phase1-onvif-gate.ps1 -AsJson -ShowLatestOnRunError"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -SkipArtifactsValidation  Skip validate-phase1-onvif-artifacts.ps1."
    Write-Host "  -ShowLatestOnRunError  On acceptance failure, still emit latest decision/JSON path logic."
    Write-Host "  -AsJson  Print combined JSON (run + decision + artifacts validation)."
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Success path"
    Write-Host "  1  Acceptance run failed (or propagated from a child script)"
    Write-Host "  2/3  From decision viewer when used with -FailOnNoGo / -FailOnConditional (inner scripts)"
    Write-Host "  1/2  From validate-phase1-onvif-artifacts.ps1 when not skipped"
    exit 0
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$secureRun = Join-Path $scriptDir "run-phase1-onvif-acceptance-secure.ps1"
$showLatest = Join-Path $scriptDir "show-latest-phase1-onvif-decision.ps1"
$validateArtifacts = Join-Path $scriptDir "validate-phase1-onvif-artifacts.ps1"

if (-not (Test-Path $secureRun)) { throw "Script not found: $secureRun" }
if (-not (Test-Path $showLatest)) { throw "Script not found: $showLatest" }
if (-not (Test-Path $validateArtifacts)) { throw "Script not found: $validateArtifacts" }

$runFailed = $false
$runErrorMessage = $null
try {
    & $secureRun `
        -ApiBase $ApiBase `
        -AdminUser $AdminUser `
        -CameraIndex $CameraIndex `
        -WaitSeconds $WaitSeconds `
        -EnableWebSocketCheck:$EnableWebSocketCheck `
        -MaxEventWaitSeconds $MaxEventWaitSeconds `
        -EventPollIntervalSeconds $EventPollIntervalSeconds `
        -PollIntervalMs $PollIntervalMs `
        -PullTimeoutMs $PullTimeoutMs `
        -LatencyBufferMs $LatencyBufferMs `
        -WebSocketUrl $WebSocketUrl `
        -AllowApiUnavailable:$AllowApiUnavailable `
        -NoPrompt:$NoPrompt `
        -RequireOnvifEvidence:$RequireOnvifEvidence `
        -FailOnNoGo:$FailOnNoGo `
        -FailOnConditional:$FailOnConditional
} catch {
    $runFailed = $true
    $runErrorMessage = $_.Exception.Message
    if (-not $ShowLatestOnRunError.IsPresent) {
        throw
    }
    if (-not $AsJson.IsPresent) {
        Write-Host ("Acceptance run failed: {0}" -f $runErrorMessage) -ForegroundColor Yellow
        Write-Host "Showing latest known decision/artifacts..." -ForegroundColor Yellow
    }
}

if ($AsJson) {
    $psExe = if ($PSVersionTable.PSEdition -eq "Core") { "pwsh" } else { "powershell" }
    $decisionJson = & $psExe -NoProfile -File $showLatest -AsJson
    $decisionObj = $decisionJson | ConvertFrom-Json

    $validationObj = $null
    $validationExitCode = $null
    if (-not $SkipArtifactsValidation.IsPresent) {
        $validationJson = & $psExe -NoProfile -File $validateArtifacts -AsJson
        $validationExitCode = $LASTEXITCODE
        if ($validationJson) {
            $validationObj = $validationJson | ConvertFrom-Json
        }
    }

    $combined = [ordered]@{
        run = @{
            failed = $runFailed
            error = $runErrorMessage
            showLatestOnRunError = $ShowLatestOnRunError.IsPresent
        }
        decision = $decisionObj
        artifactsValidation = $validationObj
        artifactsValidationExitCode = $validationExitCode
    }
    $combined | ConvertTo-Json -Depth 12
} else {
    Write-Host ""
    & $showLatest
    if (-not $SkipArtifactsValidation.IsPresent) {
        & $validateArtifacts
        Write-Host ""
    }
}

if ($runFailed) {
    exit 1
}
