[CmdletBinding()]
param(
    [string]$BaseUrl = "http://localhost:8080",
    [int]$Attempts = 3,
    [int]$InitialBackoffSeconds = 10,
    [int]$MaxDiscoverFailures = 0,
    [int]$MaxAuthFailures = 5,
    [int]$MaxEventStatisticsFailures = 0,
    [int]$KeepReports = 20,
    [string]$WebhookUrl = "",
    [int]$WebhookTimeoutSeconds = 15,
    [switch]$FailOnWebhookError,
    [string]$WebhookTextPrefix = "[IP-CSS Gate]",
    [switch]$WebhookIncludeAttemptsTable,
    [switch]$TelegramBotFormat,
    [string]$TelegramChatId = "",
    [switch]$ExportToReleaseBuild,
    [switch]$ExportWithTimestampSubdir,
    [switch]$FailOnExportError,
    [int]$ExportKeepReports = 20,
    [int]$ExportKeepRuns = 20
)

$ErrorActionPreference = "Stop"

if ($Attempts -lt 1) { $Attempts = 1 }
if ($InitialBackoffSeconds -lt 1) { $InitialBackoffSeconds = 1 }
if ($KeepReports -lt 1) { $KeepReports = 1 }
if ($WebhookTimeoutSeconds -lt 1) { $WebhookTimeoutSeconds = 1 }
if ($ExportKeepReports -lt 1) { $ExportKeepReports = 1 }
if ($ExportKeepRuns -lt 1) { $ExportKeepRuns = 1 }
if ($TelegramBotFormat -and [string]::IsNullOrWhiteSpace($TelegramChatId)) {
    throw "TelegramChatId is required when -TelegramBotFormat is enabled."
}

$results = @()
$success = $false
$lastReport = ""

for ($i = 1; $i -le $Attempts; $i++) {
    $timestamp = Get-Date -Format "yyyy-MM-dd_HHmmss"
    $reportPath = "docs/reports/DOCKER_PRE_RELEASE_GATE_REPORT_$timestamp.md"

    powershell -ExecutionPolicy Bypass -File "scripts/server-pre-release-gate.ps1" `
        -BaseUrl $BaseUrl `
        -MaxDiscoverFailures $MaxDiscoverFailures `
        -MaxAuthFailures $MaxAuthFailures `
        -MaxEventStatisticsFailures $MaxEventStatisticsFailures `
        -ReportPath $reportPath | Out-Null

    $exitCode = $LASTEXITCODE
    $status = if ($exitCode -eq 0) { "GO" } else { "NO-GO" }
    $lastReport = $reportPath
    $results += [pscustomobject]@{
        Attempt = $i
        Status = $status
        Report = $reportPath
        ExitCode = $exitCode
        Time = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss zzz")
    }

    if ($exitCode -eq 0) {
        $success = $true
        break
    }

    if ($i -lt $Attempts) {
        $sleepSeconds = [Math]::Min(300, $InitialBackoffSeconds * [Math]::Pow(2, $i - 1))
        Start-Sleep -Seconds ([int]$sleepSeconds)
    }
}

$summaryPath = "docs/reports/DOCKER_PRE_RELEASE_GATE_LAST.md"
$summaryJsonPath = "docs/reports/DOCKER_PRE_RELEASE_GATE_LAST.json"
$summaryLines = @(
    "# Docker Pre-Release Gate (Last Run)",
    "",
    "- Generated at: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')",
    "- Base URL: ``$BaseUrl``",
    "- Attempts configured: $Attempts",
    "- Result: **$(if ($success) { 'GO' } else { 'NO-GO' })**",
    "",
    "## Attempts",
    "",
    "| # | Status | Exit code | Report | Time |",
    "|---|---|---|---|---|"
)

foreach ($r in $results) {
    $summaryLines += "| $($r.Attempt) | $($r.Status) | $($r.ExitCode) | $($r.Report) | $($r.Time) |"
}

$summaryLines -join [Environment]::NewLine | Set-Content -Path $summaryPath -Encoding UTF8

$summaryPayload = [pscustomobject]@{
    generatedAt = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss zzz")
    baseUrl = $BaseUrl
    attemptsConfigured = $Attempts
    result = if ($success) { "GO" } else { "NO-GO" }
    attempts = $results
    lastReport = $lastReport
    markdownSummary = $summaryPath
}

$attemptsShort = ($results | ForEach-Object { "#$($_.Attempt):$($_.Status)(code=$($_.ExitCode))" }) -join "; "
$summaryPayload | Add-Member -NotePropertyName text -NotePropertyValue "$WebhookTextPrefix Result=$($summaryPayload.result); BaseUrl=$BaseUrl; Attempts=$attemptsShort; Report=$lastReport; Summary=$summaryPath"
if ($WebhookIncludeAttemptsTable) {
    $summaryPayload | Add-Member -NotePropertyName attemptsText -NotePropertyValue ($results | ForEach-Object {
        "Attempt $($_.Attempt): status=$($_.Status), exitCode=$($_.ExitCode), report=$($_.Report), time=$($_.Time)"
    })
}
$summaryPayload | ConvertTo-Json -Depth 8 | Set-Content -Path $summaryJsonPath -Encoding UTF8

$exportCompleted = $false
if ($ExportToReleaseBuild -and $success) {
    try {
        $exportArgs = @(
            "-ExecutionPolicy", "Bypass",
            "-File", "scripts/server-export-release-artifacts.ps1",
            "-KeepExportReports", "$ExportKeepReports",
            "-KeepExportRuns", "$ExportKeepRuns"
        )
        if ($ExportWithTimestampSubdir) {
            $exportArgs += "-IncludeTimestampSubdir"
        }
        powershell @exportArgs | Out-Null
        if ($LASTEXITCODE -ne 0) {
            throw "server-export-release-artifacts failed with exit code $LASTEXITCODE"
        }
        $exportCompleted = $true
    } catch {
        if ($FailOnExportError) {
            throw "Export to release-build failed: $($_.Exception.Message)"
        }
    }
}

$webhookDelivered = $false
if (-not [string]::IsNullOrWhiteSpace($WebhookUrl)) {
    try {
        if ($TelegramBotFormat) {
            $bodyObj = [pscustomobject]@{
                chat_id = $TelegramChatId
                text = $summaryPayload.text
            }
        } else {
            $bodyObj = $summaryPayload
        }
        $body = $bodyObj | ConvertTo-Json -Depth 8
        Invoke-RestMethod -Method Post -Uri $WebhookUrl -ContentType "application/json" -Body $body -TimeoutSec $WebhookTimeoutSeconds | Out-Null
        $webhookDelivered = $true
    } catch {
        if ($FailOnWebhookError) {
            throw "Webhook delivery failed: $($_.Exception.Message)"
        }
    }
}

# Rotate old gate reports, keep newest N.
$allReports = Get-ChildItem -Path "docs/reports" -Filter "DOCKER_PRE_RELEASE_GATE_REPORT_*.md" `
    | Sort-Object LastWriteTime -Descending
if ($allReports.Count -gt $KeepReports) {
    $allReports | Select-Object -Skip $KeepReports | Remove-Item -Force
}

Write-Host ""
Write-Host "NIGHTLY GATE RESULT: $(if ($success) { 'GO' } else { 'NO-GO' })" -ForegroundColor $(if ($success) { "Green" } else { "Red" })
Write-Host "Summary: $summaryPath"
Write-Host "Summary JSON: $summaryJsonPath"
Write-Host "Last report: $lastReport"
if ($ExportToReleaseBuild) {
    Write-Host "Export to release-build: $exportCompleted"
}
if (-not [string]::IsNullOrWhiteSpace($WebhookUrl)) {
    Write-Host "Webhook delivered: $webhookDelivered"
}

if (-not $success) {
    exit 1
}

exit 0
