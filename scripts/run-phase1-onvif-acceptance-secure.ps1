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
    [switch]$FailOnConditional
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Phase 1 ONVIF acceptance (secure password; calls run-phase1-onvif-acceptance.ps1)"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\run-phase1-onvif-acceptance-secure.ps1 -ApiBase http://localhost:8080 -AdminUser admin"
    Write-Host "  .\scripts\run-phase1-onvif-acceptance-secure.ps1 -ApiBase http://localhost:8080 -AdminUser admin -NoPrompt"
    Write-Host "  .\scripts\run-phase1-onvif-acceptance-secure.ps1 -EnableWebSocketCheck -RequireOnvifEvidence -FailOnNoGo -FailOnConditional"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  Interactive: prompts for admin password as SecureString."
    Write-Host "  -NoPrompt  Requires IPCSS_ADMIN_PASSWORD (non-interactive/CI)."
    Write-Host "  Same gate flags as inner script (see run-phase1-onvif-acceptance.ps1 -ShowHelp)."
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  Same as run-phase1-onvif-acceptance.ps1 (0 / 2 / 3) or 1 on setup errors."
    exit 0
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$runScript = Join-Path $scriptDir "run-phase1-onvif-acceptance.ps1"

if (-not (Test-Path $runScript)) {
    throw "Script not found: $runScript"
}

$adminPasswordSecure = $null
if ($env:IPCSS_ADMIN_PASSWORD) {
    Write-Host "Using admin password from IPCSS_ADMIN_PASSWORD environment variable." -ForegroundColor DarkGray
    $adminPasswordSecure = ConvertTo-SecureString -String $env:IPCSS_ADMIN_PASSWORD -AsPlainText -Force
} else {
    if ($NoPrompt.IsPresent) {
        throw "NoPrompt mode enabled, but IPCSS_ADMIN_PASSWORD is not set. Set it before running, e.g. `$env:IPCSS_ADMIN_PASSWORD = '<pwd>' (session) or setx IPCSS_ADMIN_PASSWORD '<pwd>' (persistent)."
    }
    $adminPasswordSecure = Read-Host "Enter API admin password" -AsSecureString
}

& $runScript `
    -ApiBase $ApiBase `
    -AdminUser $AdminUser `
    -AdminPasswordSecure $adminPasswordSecure `
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
    -RequireOnvifEvidence:$RequireOnvifEvidence `
    -FailOnNoGo:$FailOnNoGo `
    -FailOnConditional:$FailOnConditional
