[CmdletBinding()]
param(
    [switch]$ShowHelp,
    [string]$ReportDir = "diagnostics\recording-ws-acceptance",
    [string]$EnvironmentName = "local",
    [string]$CameraId = "",
    [string]$RecordingId = "",
    [ValidateSet("PASS", "FAIL", "NOT_RUN")]
    [string]$StartedEvent = "NOT_RUN",
    [ValidateSet("PASS", "FAIL", "NOT_RUN")]
    [string]$PausedEvent = "NOT_RUN",
    [ValidateSet("PASS", "FAIL", "NOT_RUN")]
    [string]$ResumedEvent = "NOT_RUN",
    [ValidateSet("PASS", "FAIL", "NOT_RUN")]
    [string]$StoppedEvent = "NOT_RUN",
    [ValidateSet("PASS", "FAIL", "NOT_RUN")]
    [string]$ChannelIsolation = "NOT_RUN",
    [string]$WsLogPath = "",
    [string]$Notes = ""
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Recording WS lifecycle acceptance evidence recorder"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\recording-ws-lifecycle-acceptance-evidence.ps1"
    Write-Host "  .\scripts\recording-ws-lifecycle-acceptance-evidence.ps1 -EnvironmentName staging -CameraId cam-1 -RecordingId rec-1 -StartedEvent PASS -PausedEvent PASS -ResumedEvent PASS -StoppedEvent PASS -ChannelIsolation PASS -WsLogPath diagnostics\ws\recordings.log"
    Write-Host ""
    Write-Host "Output:"
    Write-Host "  diagnostics/recording-ws-acceptance/recording-ws-lifecycle-acceptance-<run-id>.md|json"
    Write-Host ""
    Write-Host "Decision logic:"
    Write-Host "  PASS    all events + channel isolation are PASS"
    Write-Host "  FAIL    any event/isolation is FAIL"
    Write-Host "  PARTIAL otherwise (NOT_RUN / mixed incomplete)"
    exit 0
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$resolvedReportDir = Join-Path $projectRoot $ReportDir
$runId = Get-Date -Format "yyyyMMdd-HHmmss"

function Resolve-Overall {
    param(
        [string[]]$States
    )
    if ($States -contains "FAIL") { return "FAIL" }
    if (($States | Where-Object { $_ -eq "PASS" }).Count -eq $States.Count) { return "PASS" }
    return "PARTIAL"
}

$states = @(
    $StartedEvent,
    $PausedEvent,
    $ResumedEvent,
    $StoppedEvent,
    $ChannelIsolation
)
$overall = Resolve-Overall -States $states

if (-not (Test-Path -LiteralPath $resolvedReportDir)) {
    New-Item -ItemType Directory -Path $resolvedReportDir -Force | Out-Null
}

$mdPath = Join-Path $resolvedReportDir ("recording-ws-lifecycle-acceptance-{0}.md" -f $runId)
$jsonPath = Join-Path $resolvedReportDir ("recording-ws-lifecycle-acceptance-{0}.json" -f $runId)

$rows = @(
    [PSCustomObject]@{ check = "recording_started"; state = $StartedEvent },
    [PSCustomObject]@{ check = "recording_paused"; state = $PausedEvent },
    [PSCustomObject]@{ check = "recording_resumed"; state = $ResumedEvent },
    [PSCustomObject]@{ check = "recording_stopped"; state = $StoppedEvent },
    [PSCustomObject]@{ check = "channel_isolation"; state = $ChannelIsolation }
)

$md = @(
    "# Recording WS Lifecycle Acceptance Evidence",
    "",
    "- Run ID: $runId",
    "- Environment: **$EnvironmentName**",
    "- Overall: **$overall**",
    "- Camera ID: $CameraId",
    "- Recording ID: $RecordingId",
    "- WS Log Path: $WsLogPath",
    "- Notes: $Notes",
    "",
    "| Check | State |",
    "|---|---|"
)
foreach ($r in $rows) {
    $md += "| $($r.check) | $($r.state) |"
}
$md -join [Environment]::NewLine | Set-Content -Path $mdPath -Encoding UTF8

$report = [PSCustomObject]@{
    runId = $runId
    timestampUtc = [DateTime]::UtcNow.ToString("o")
    environment = $EnvironmentName
    overall = $overall
    cameraId = $CameraId
    recordingId = $RecordingId
    wsLogPath = $WsLogPath
    notes = $Notes
    checks = @($rows)
    artifacts = [PSCustomObject]@{
        markdown = $mdPath
        json = $jsonPath
    }
}
$report | ConvertTo-Json -Depth 8 | Set-Content -Path $jsonPath -Encoding UTF8

Write-Host ("Evidence markdown: {0}" -f $mdPath) -ForegroundColor Green
Write-Host ("Evidence json:     {0}" -f $jsonPath) -ForegroundColor Green
Write-Host ("Overall: {0}" -f $overall) -ForegroundColor Cyan

if ($overall -eq "FAIL") { exit 1 }
exit 0
