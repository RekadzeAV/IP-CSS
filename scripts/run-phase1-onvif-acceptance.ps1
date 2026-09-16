param(
    [switch]$ShowHelp,
    [string]$ApiBase = "http://localhost:8080",
    [string]$AdminUser = "admin",
    [Alias("AdminPassword")]
    [string]$AdminInput = "",
    [System.Security.SecureString]$AdminPasswordSecure = $null,
    [int]$CameraIndex = 0,
    [int]$WaitSeconds = 15,
    [switch]$EnableWebSocketCheck = $false,
    [int]$MaxEventWaitSeconds = 30,
    [int]$EventPollIntervalSeconds = 2,
    [int]$PollIntervalMs = 5000,
    [int]$PullTimeoutMs = 500,
    [int]$LatencyBufferMs = 10000,
    [string]$WebSocketUrl = "",
    [object]$AllowApiUnavailable = $true,
    [switch]$RequireOnvifEvidence,
    [switch]$FailOnNoGo,
    [switch]$FailOnConditional
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Phase 1 ONVIF acceptance (unit + UI + resilience + evidence report)"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\run-phase1-onvif-acceptance.ps1 -ApiBase http://localhost:8080 -AdminUser admin -AdminPassword <pwd>"
    Write-Host "  .\scripts\run-phase1-onvif-acceptance.ps1 -ApiBase http://localhost:8080 -AdminUser admin -AdminPassword <pwd> -EnableWebSocketCheck -FailOnNoGo -FailOnConditional"
    Write-Host "  .\scripts\run-phase1-onvif-acceptance.ps1 -AllowApiUnavailable `$true"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -AdminPassword / -AdminPasswordSecure / env IPCSS_ADMIN_PASSWORD"
    Write-Host "  -AllowApiUnavailable  Soft-mode when API/camera unavailable."
    Write-Host "  -RequireOnvifEvidence  Elevate incomplete 1.4.3 to NO_GO."
    Write-Host "  -FailOnNoGo  Exit 2 on NO_GO; -FailOnConditional  Exit 3 on CONDITIONAL."
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Completed (decision may still be CONDITIONAL in output)"
    Write-Host "  2  With -FailOnNoGo when decision is NO_GO"
    Write-Host "  3  With -FailOnConditional when decision is CONDITIONAL"
    Write-Host ""
    Write-Host "Output:"
    Write-Host "  diagnostics/onvif-events/phase1-onvif-acceptance-<run-id>.json"
    Write-Host "  docs/reports/ONVIF_PHASE1_EVIDENCE_REPORT_<date>.md"
    exit 0
}

function Resolve-PlainPassword([object]$value) {
    if ($null -eq $value) { return $null }
    if ($value -is [System.Security.SecureString]) {
        $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($value)
        try {
            return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
        } finally {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
        }
    }
    return [string]$value
}

function Resolve-Bool([object]$value, [bool]$defaultValue = $false) {
    if ($null -eq $value) { return $defaultValue }
    if ($value -is [bool]) { return [bool]$value }
    $text = ([string]$value).Trim().ToLowerInvariant()
    if ([string]::IsNullOrWhiteSpace($text)) { return $defaultValue }
    switch ($text) {
        "1" { return $true }
        "0" { return $false }
        "true" { return $true }
        "false" { return $false }
        "$true" { return $true }
        "$false" { return $false }
        default { return $defaultValue }
    }
}

function Invoke-Step([string]$name, [scriptblock]$scriptBlock) {
    Write-Host ""
    Write-Host "=== $name ===" -ForegroundColor Cyan
    try {
        & $scriptBlock | Out-Host
        Write-Host ("{0}: OK" -f $name) -ForegroundColor Green
        return $true
    } catch {
        Write-Host ("{0}: FAILED -> {1}" -f $name, $_.Exception.Message) -ForegroundColor Yellow
        return $false
    }
}

function Resolve-CommandPath([string]$preferredWindows, [string]$fallback) {
    if ($IsWindows -and (Test-Path $preferredWindows)) { return $preferredWindows }
    return $fallback
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$allowApiUnavailableValue = Resolve-Bool $AllowApiUnavailable $true
$resolvedAdminPassword = $null
if ($AdminPasswordSecure) {
    $resolvedAdminPassword = Resolve-PlainPassword $AdminPasswordSecure
}
if (-not $resolvedAdminPassword -and $AdminInput) {
    $resolvedAdminPassword = Resolve-PlainPassword $AdminInput
}
if (-not $resolvedAdminPassword) { $resolvedAdminPassword = $env:IPCSS_ADMIN_PASSWORD }
$diagRoot = Join-Path $projectRoot "diagnostics\onvif-events"
$evidenceReportPath = Join-Path $projectRoot ("docs\reports\ONVIF_PHASE1_EVIDENCE_REPORT_{0}.md" -f (Get-Date -Format "yyyy-MM-dd"))
$runId = Get-Date -Format "yyyyMMdd-HHmmss"
$jsonSummaryPath = Join-Path $diagRoot ("phase1-onvif-acceptance-{0}.json" -f $runId)
$gradleCmd = Resolve-CommandPath -preferredWindows (Join-Path $projectRoot "gradlew.bat") -fallback (Join-Path $projectRoot "gradlew")
$npmCmd = Resolve-CommandPath -preferredWindows "npm.cmd" -fallback "npm"
$webRoot = Join-Path $projectRoot "server\web"
$configValidator = Join-Path $scriptDir "validate-onvif-test-camera-config.ps1"

$preflightStatus = "PASS"
$preflightDetails = "Not executed."
if (Test-Path $configValidator) {
    $preflightOk = Invoke-Step "ONVIF config preflight" {
        $global:LASTEXITCODE = 0
        & $configValidator
        if ($global:LASTEXITCODE -eq 2) { throw "Config validation FAIL (exit 2)." }
    }
    if ($preflightOk) {
        $preflightStatus = "PASS"
        $preflightDetails = "Config validation PASS."
    } else {
        $preflightStatus = "FAIL"
        $preflightDetails = "Config validation failed."
    }
} else {
    $preflightStatus = "NOT_RUN"
    $preflightDetails = "Config validator script not found."
}

$unit141Ok = Invoke-Step "1.4.1 server unit tests" {
    $global:LASTEXITCODE = 0
    & $gradleCmd :server:api:test `
        --tests "com.company.ipcamera.server.service.OnvifEventMapperTest" `
        --tests "com.company.ipcamera.server.service.OnvifEventSubscriptionServiceTest" `
        --tests "com.company.ipcamera.server.service.CameraEventMonitoringServiceJvmTest"
    if ($global:LASTEXITCODE -and $global:LASTEXITCODE -ne 0) { throw "Gradle tests failed with exit code $global:LASTEXITCODE" }
}

$uiRegressionOk = Invoke-Step "UI realtime regression test" {
    $global:LASTEXITCODE = 0
    & $npmCmd --prefix $webRoot test -- --runInBand src/store/slices/eventsSlice.test.ts src/hooks/useWebSocket.test.tsx
    if ($global:LASTEXITCODE -and $global:LASTEXITCODE -ne 0) { throw "npm test failed with exit code $global:LASTEXITCODE" }
}

$onvif143Status = "NOT_RUN"
$onvifNote = "ONVIF scenario not executed."
$phase1WsStatus = "NOT_RUN"
$phase1LatencyStatus = "NOT_MEASURED"
$phase2WsStatus = "NOT_RUN"
$phase2LatencyStatus = "NOT_MEASURED"
$latestPhase1Path = $null
$latestPhase2Path = $null
if ($resolvedAdminPassword) {
    $onvifOk = Invoke-Step "1.4.3 ONVIF resilience scenario" {
        $global:LASTEXITCODE = 0
        & "$scriptDir\onvif-events-resilience-verification.ps1" `
            -ApiBase $ApiBase `
            -AdminUser $AdminUser `
            -AdminPassword $resolvedAdminPassword `
            -CameraIndex $CameraIndex `
            -WaitSeconds $WaitSeconds `
            -EnableWebSocketCheck:$EnableWebSocketCheck `
            -MaxEventWaitSeconds $MaxEventWaitSeconds `
            -EventPollIntervalSeconds $EventPollIntervalSeconds `
            -PollIntervalMs $PollIntervalMs `
            -PullTimeoutMs $PullTimeoutMs `
            -LatencyBufferMs $LatencyBufferMs `
            -WebSocketUrl $WebSocketUrl `
            -AllowApiUnavailable:$allowApiUnavailableValue `
            -GenerateEvidenceReport:$false
        if ($global:LASTEXITCODE -and $global:LASTEXITCODE -ne 0) { throw "Resilience script failed with exit code $global:LASTEXITCODE" }
    }
    if ($onvifOk) {
        $latestSummary = Get-ChildItem -Path $diagRoot -Filter "*summary*.md" -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTimeUtc -Descending | Select-Object -First 1
        $latestPhase1Json = Get-ChildItem -Path $diagRoot -Filter "*phase1.json" -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTimeUtc -Descending | Select-Object -First 1
        $latestPhase2Json = Get-ChildItem -Path $diagRoot -Filter "*phase2.json" -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTimeUtc -Descending | Select-Object -First 1
        $latestPhase1Path = if ($latestPhase1Json) { $latestPhase1Json.FullName } else { $null }
        $latestPhase2Path = if ($latestPhase2Json) { $latestPhase2Json.FullName } else { $null }
        if ($latestPhase1Json) {
            $phase1Obj = Get-Content $latestPhase1Json.FullName -Raw | ConvertFrom-Json
            if ($phase1Obj.websocketStatus) { $phase1WsStatus = [string]$phase1Obj.websocketStatus }
            if ($phase1Obj.latencyStatus) { $phase1LatencyStatus = [string]$phase1Obj.latencyStatus }
        }
        if ($latestPhase2Json) {
            $phase2Obj = Get-Content $latestPhase2Json.FullName -Raw | ConvertFrom-Json
            if ($phase2Obj.websocketStatus) { $phase2WsStatus = [string]$phase2Obj.websocketStatus }
            if ($phase2Obj.latencyStatus) { $phase2LatencyStatus = [string]$phase2Obj.latencyStatus }
        }

        if ($latestSummary) {
            $content = Get-Content $latestSummary.FullName -Raw
            if ($content -match "Status:\s+PASS") { $onvif143Status = "PASS" }
            elseif ($content -match "Status:\s+PARTIAL") { $onvif143Status = "PARTIAL" }
            elseif ($content -match "Status:\s+NOT_RUN") { $onvif143Status = "NOT_RUN" }
            else { $onvif143Status = "PARTIAL" }
            if ($onvif143Status -eq "PASS") {
                if ($EnableWebSocketCheck -and ($phase1WsStatus -ne "PASS" -or $phase2WsStatus -ne "PASS")) {
                    $onvif143Status = "PARTIAL"
                    $onvifNote = "Resilience summary PASS but websocket check not fully passed (phase1=$phase1WsStatus, phase2=$phase2WsStatus)."
                } elseif ($phase1LatencyStatus -eq "FAIL" -or $phase2LatencyStatus -eq "FAIL") {
                    $onvif143Status = "PARTIAL"
                    $onvifNote = "Resilience summary PASS but latency gate failed (phase1=$phase1LatencyStatus, phase2=$phase2LatencyStatus)."
                } else {
                    $onvifNote = "From resilience summary: $($latestSummary.Name); ws=($phase1WsStatus,$phase2WsStatus), latency=($phase1LatencyStatus,$phase2LatencyStatus)"
                }
            } else {
                $onvifNote = "From resilience summary: $($latestSummary.Name); ws=($phase1WsStatus,$phase2WsStatus), latency=($phase1LatencyStatus,$phase2LatencyStatus)"
            }
        } else {
            $onvif143Status = "PARTIAL"
            $onvifNote = "Resilience script finished, summary artifact not found."
        }
    } else {
        $onvif143Status = "PARTIAL"
        $onvifNote = "Resilience script step failed."
    }
} else {
    $onvif143Status = "NOT_RUN"
    $onvifNote = "Admin password missing; skipped ONVIF camera verification."
}

$unit141Status = if ($unit141Ok) { "PASS" } else { "FAIL" }
$uiStatus = if ($uiRegressionOk) { "PASS" } else { "FAIL" }
$notes = "run-phase1-onvif-acceptance: $onvifNote"

Write-Host ""
Write-Host "=== Generating consolidated evidence report ===" -ForegroundColor Cyan
& "$scriptDir\generate-phase1-onvif-evidence-report.ps1" `
    -Onvif143Status $onvif143Status `
    -Unit141Status $unit141Status `
    -UiRegressionStatus $uiStatus `
    -Notes $notes
if ($LASTEXITCODE -ne 0) { throw "Failed to generate evidence report." }

Write-Host ""
Write-Host "Done. Statuses: 1.4.3=$onvif143Status, 1.4.1=$unit141Status, ui-regression=$uiStatus" -ForegroundColor Green
if (Test-Path $evidenceReportPath) {
    Write-Host ("Evidence report: {0}" -f $evidenceReportPath) -ForegroundColor Green
}
$latestRunSummary = Get-ChildItem -Path $diagRoot -Filter "*summary*.md" -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTimeUtc -Descending | Select-Object -First 1
if ($latestRunSummary) {
    Write-Host ("Latest ONVIF resilience summary: {0}" -f $latestRunSummary.FullName) -ForegroundColor Green
}

$overall = "PASS"
if ($preflightStatus -eq "FAIL" -or $onvif143Status -eq "FAIL" -or $unit141Status -eq "FAIL" -or $uiStatus -eq "FAIL") {
    $overall = "FAIL"
} elseif ($preflightStatus -eq "NOT_RUN" -or $onvif143Status -eq "PARTIAL" -or $onvif143Status -eq "NOT_RUN") {
    $overall = "PARTIAL"
}

$goNoGo = "GO"
$goNoGoReason = "All required checks passed."
if ($preflightStatus -eq "FAIL" -or $unit141Status -eq "FAIL" -or $uiStatus -eq "FAIL" -or $onvif143Status -eq "FAIL") {
    $goNoGo = "NO_GO"
    $goNoGoReason = "Critical block failed (preflight/unit141/uiRegression/onvif143)."
} elseif ($RequireOnvifEvidence.IsPresent -and ($onvif143Status -eq "PARTIAL" -or $onvif143Status -eq "NOT_RUN")) {
    $goNoGo = "NO_GO"
    $goNoGoReason = "ONVIF evidence is required, but 1.4.3 did not reach PASS."
} elseif ($onvif143Status -eq "PARTIAL" -or $onvif143Status -eq "NOT_RUN") {
    $goNoGo = "CONDITIONAL"
    $goNoGoReason = "Core tests passed but ONVIF camera evidence is incomplete."
    if ($onvifNote -match "Admin password missing") {
        $goNoGoReason = "Core tests passed, but ONVIF verification skipped because admin password is missing."
    } elseif ($onvifNote -match "websocket check not fully passed") {
        $goNoGoReason = "Core tests passed, but ONVIF WebSocket realtime validation failed for one or more phases."
    } elseif ($onvifNote -match "latency gate failed") {
        $goNoGoReason = "Core tests passed, but ONVIF latency gate failed for one or more phases."
    } elseif ($onvif143Status -eq "NOT_RUN") {
        $goNoGoReason = "Core tests passed, but ONVIF verification was not executed."
    }
}

if (-not (Test-Path $diagRoot)) {
    New-Item -ItemType Directory -Path $diagRoot -Force | Out-Null
}
$summaryObj = [ordered]@{
    runId = $runId
    timestampUtc = [DateTime]::UtcNow.ToString("o")
    apiBase = $ApiBase
    cameraIndex = $CameraIndex
    statuses = @{
        overall = $overall
        preflight = $preflightStatus
        onvif143 = $onvif143Status
        unit141 = $unit141Status
        uiRegression = $uiStatus
    }
    decision = @{
        goNoGo = $goNoGo
        reason = $goNoGoReason
    }
    policy = @{
        requireOnvifEvidence = $RequireOnvifEvidence.IsPresent
    }
    artifacts = @{
        acceptanceJson = $jsonSummaryPath
        evidenceReport = $(if (Test-Path $evidenceReportPath) { $evidenceReportPath } else { $null })
        latestOnvifSummary = $(if ($latestRunSummary) { $latestRunSummary.FullName } else { $null })
    }
    diagnostics = @{
        sourceJson = $jsonSummaryPath
        websocket = @{
            phase1 = $phase1WsStatus
            phase2 = $phase2WsStatus
        }
        latency = @{
            phase1 = $phase1LatencyStatus
            phase2 = $phase2LatencyStatus
        }
        onvifPhaseJson = @{
            phase1 = $latestPhase1Path
            phase2 = $latestPhase2Path
        }
    }
    notes = $notes
    preflight = @{
        status = $preflightStatus
        details = $preflightDetails
    }
}
$summaryObj | ConvertTo-Json -Depth 8 | Out-File -FilePath $jsonSummaryPath -Encoding UTF8
Write-Host ("Machine summary: {0}" -f $jsonSummaryPath) -ForegroundColor Green
Write-Host ("Gate decision: {0} ({1})" -f $goNoGo, $goNoGoReason) -ForegroundColor Green
Write-Host ("Gate policy: requireOnvifEvidence={0}" -f $RequireOnvifEvidence.IsPresent) -ForegroundColor Green

if ($goNoGo -eq "NO_GO" -and $FailOnNoGo.IsPresent) {
    Write-Host "Exiting with code 2 due to NO_GO decision." -ForegroundColor Red
    exit 2
}
if ($goNoGo -eq "CONDITIONAL" -and $FailOnConditional.IsPresent) {
    Write-Host "Exiting with code 3 due to CONDITIONAL decision." -ForegroundColor Yellow
    exit 3
}
