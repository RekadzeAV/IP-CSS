[Diagnostics.CodeAnalysis.SuppressMessageAttribute('PSAvoidUsingPlainTextForPassword', '')]
# Profile-aware release gate for video E2E evidence.
param(
    [switch]$ShowHelp,
    [switch]$RunNetworkSmoke,
    [switch]$RunLongRunMatrix,
    [string]$NetworkSmokeConfigPath = "config\test-cameras.local.json",
    [string]$MatrixConfigPath = "config\video-runtime-matrix.local.json",
    [string]$OutputDir = "release-build\test",
    [string]$ReportFileName = "video-e2e-go-no-go-report.md",
    [string]$AcceptanceProfilePath = "config\video-e2e-acceptance-profile.local.json",
    [double]$MinNetworkLayerPct = 80.0,
    [double]$MinLongRunScenarioPassPct = 80.0,
    [double]$MinLongRunChecksPassPct = 95.0,
    [double]$MaxEvidenceAgeHours = 24.0
)

if ($ShowHelp) {
    Write-Host "Video E2E Go/No-Go gate (evidence + profile-aware release decision)"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\video-e2e-go-no-go.ps1"
    Write-Host "  .\scripts\video-e2e-go-no-go.ps1 -RunNetworkSmoke -RunLongRunMatrix"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -AcceptanceProfilePath  config/video-e2e-acceptance-profile.local.json (repo-relative ok)"
    Write-Host "  -OutputDir  -ReportFileName  Where to write the markdown report"
    Write-Host "  -NetworkSmokeConfigPath  -MatrixConfigPath  Evidence sources when smoke/matrix flags set"
    Write-Host "  -MinNetworkLayerPct  -MinLongRunScenarioPassPct  -MinLongRunChecksPassPct  -MaxEvidenceAgeHours"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Profile-aware release decision is GO"
    Write-Host "  1  Profile-aware release decision is NO-GO"
    exit 0
}

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Resolve-ProjectPath {
    param([string]$PathValue)
    $scriptDir = $PSScriptRoot
    $projectRoot = Split-Path -Parent $scriptDir
    if ([System.IO.Path]::IsPathRooted($PathValue)) { return $PathValue }
    return (Join-Path $projectRoot $PathValue)
}

function Add-Check {
    param(
        [System.Collections.Generic.List[object]]$List,
        [string]$Id,
        [string]$Control,
        [string]$State,
        [string]$Evidence
    )
    $List.Add([PSCustomObject]@{
        Id = $Id
        Control = $Control
        State = $State
        Evidence = $Evidence
    }) | Out-Null
}

function Get-LatestDirectory {
    param([string]$RootPath)
    if (-not (Test-Path -LiteralPath $RootPath)) { return $null }
    return Get-ChildItem -Path $RootPath -Directory -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
}

function Get-NetworkSmokeMetrics {
    param([string]$DiagnosticsRoot)
    $latestRunDir = Get-LatestDirectory -RootPath $DiagnosticsRoot
    if ($null -eq $latestRunDir) { return $null }

    $summaryPath = Join-Path $latestRunDir.FullName "summary.json"
    if (-not (Test-Path -LiteralPath $summaryPath)) { return $null }

    $summary = Get-Content -LiteralPath $summaryPath -Raw | ConvertFrom-Json
    return [PSCustomObject]@{
        NetworkLayerPct = [double]$summary.totals.networkLayerPct
        RtspPct = [double]$summary.totals.rtspPct
        HttpPct = [double]$summary.totals.httpPct
        OnvifPct = [double]$summary.totals.onvifPct
        MediaEventsPct = [double]$summary.totals.mediaEventsPct
        PullPointPct = [double]$summary.totals.pullPointPct
        SummaryPath = $summaryPath
        SummaryLastWrite = (Get-Item -LiteralPath $summaryPath).LastWriteTime
    }
}

function Get-LongRunMatrixMetrics {
    param([string]$DiagnosticsRoot)
    if (-not (Test-Path -LiteralPath $DiagnosticsRoot)) { return $null }
    $runDirs = Get-ChildItem -Path $DiagnosticsRoot -Directory -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending
    if ($null -eq $runDirs -or $runDirs.Count -eq 0) { return $null }

    $summaryPath = $null
    foreach ($dir in $runDirs) {
        $candidate = Join-Path $dir.FullName "video-runtime-matrix-summary.md"
        if (Test-Path -LiteralPath $candidate) {
            $summaryPath = $candidate
            break
        }
    }
    if ([string]::IsNullOrWhiteSpace($summaryPath)) { return $null }

    $content = Get-Content -LiteralPath $summaryPath -Raw
    $scenarioPass = [regex]::Match($content, 'Passed scenarios:\s*\*\*(\d+)\s*/\s*(\d+)\*\*', 'IgnoreCase')
    if (-not $scenarioPass.Success) { return $null }

    $passedScenarios = [int]$scenarioPass.Groups[1].Value
    $totalScenarios = [int]$scenarioPass.Groups[2].Value
    $scenarioPassPct = if ($totalScenarios -gt 0) {
        [math]::Round(($passedScenarios * 100.0) / $totalScenarios, 1)
    } else {
        0.0
    }

    # Aggregate checks across per-scenario reports.
    $runDir = Split-Path -Parent $summaryPath
    $scenarioReports = Get-ChildItem -Path $runDir -Recurse -File -Filter "video-longrun-report-*.md" -ErrorAction SilentlyContinue
    $checksTotal = 0
    $checksOk = 0
    foreach ($rep in $scenarioReports) {
        $repText = Get-Content -LiteralPath $rep.FullName -Raw
        $m = [regex]::Match($repText, 'Passed checks:\s*\*\*(\d+)\s*/\s*(\d+)\*\*', 'IgnoreCase')
        if ($m.Success) {
            $checksOk += [int]$m.Groups[1].Value
            $checksTotal += [int]$m.Groups[2].Value
        }
    }
    $checksPassPct = if ($checksTotal -gt 0) {
        [math]::Round(($checksOk * 100.0) / $checksTotal, 1)
    } else {
        0.0
    }

    return [PSCustomObject]@{
        PassedScenarios = $passedScenarios
        TotalScenarios = $totalScenarios
        ScenarioPassPct = $scenarioPassPct
        ChecksOk = $checksOk
        ChecksTotal = $checksTotal
        ChecksPassPct = $checksPassPct
        SummaryPath = $summaryPath
        SummaryLastWrite = (Get-Item -LiteralPath $summaryPath).LastWriteTime
    }
}

function Get-RecordingWsAcceptanceMetrics {
    param([string]$DiagnosticsRoot)
    if (-not (Test-Path -LiteralPath $DiagnosticsRoot)) { return $null }

    $latestJson = Get-ChildItem -Path $DiagnosticsRoot -Recurse -File -Filter "recording-ws-lifecycle-acceptance-*.json" -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if ($null -eq $latestJson) { return $null }

    $payload = Get-Content -LiteralPath $latestJson.FullName -Raw | ConvertFrom-Json
    return [PSCustomObject]@{
        Overall = [string]$payload.overall
        Environment = [string]$payload.environment
        JsonPath = $latestJson.FullName
        LastWrite = $latestJson.LastWriteTime
    }
}

$networkScript = Resolve-ProjectPath -PathValue "scripts\network-layer-smoke-test.ps1"
$matrixScript = Resolve-ProjectPath -PathValue "scripts\video-runtime-platform-matrix.ps1"
$networkConfig = Resolve-ProjectPath -PathValue $NetworkSmokeConfigPath
$matrixConfig = Resolve-ProjectPath -PathValue $MatrixConfigPath
$acceptanceProfileResolved = Resolve-ProjectPath -PathValue $AcceptanceProfilePath

$networkDiagnosticsRoot = Resolve-ProjectPath -PathValue "diagnostics\network-smoke"
$matrixDiagnosticsRoot = Resolve-ProjectPath -PathValue "diagnostics\video-longrun-matrix"
$recordingWsDiagnosticsRoot = Resolve-ProjectPath -PathValue "diagnostics\recording-ws-acceptance"
$resolvedOutputDir = Resolve-ProjectPath -PathValue $OutputDir
if (-not (Test-Path -LiteralPath $resolvedOutputDir)) {
    New-Item -ItemType Directory -Path $resolvedOutputDir -Force | Out-Null
}

if ($RunNetworkSmoke) {
    if (-not (Test-Path -LiteralPath $networkConfig)) {
        throw "Network smoke config not found: $networkConfig"
    }
    & $networkScript -ConfigPath $networkConfig -DiagnosticMode $true
    $networkExitVar = Get-Variable -Name LASTEXITCODE -Scope Global -ErrorAction SilentlyContinue
    $networkExitCode = if ($null -ne $networkExitVar) { [int]$networkExitVar.Value } else { 0 }
    if ($networkExitCode -ne 0) {
        Write-Host "Network smoke exited with non-zero code: $networkExitCode" -ForegroundColor Yellow
    }
}

if ($RunLongRunMatrix) {
    if (-not (Test-Path -LiteralPath $matrixConfig)) {
        throw "Matrix config not found: $matrixConfig"
    }
    & $matrixScript -ConfigPath $matrixConfig
    $matrixExitVar = Get-Variable -Name LASTEXITCODE -Scope Global -ErrorAction SilentlyContinue
    $matrixExitCode = if ($null -ne $matrixExitVar) { [int]$matrixExitVar.Value } else { 0 }
    if ($matrixExitCode -ne 0) {
        Write-Host "Long-run matrix exited with non-zero code: $matrixExitCode" -ForegroundColor Yellow
    }
}

$checks = New-Object System.Collections.Generic.List[object]
$now = Get-Date
$acceptanceProfileName = "default-strict"
$optionalControlIds = New-Object System.Collections.Generic.HashSet[string]
if (Test-Path -LiteralPath $acceptanceProfileResolved) {
    try {
        $profileJson = Get-Content -LiteralPath $acceptanceProfileResolved -Raw | ConvertFrom-Json
        if ($profileJson.profileName) {
            $acceptanceProfileName = [string]$profileJson.profileName
        }
        if ($profileJson.release.optionalControlIds) {
            foreach ($id in @($profileJson.release.optionalControlIds)) {
                if (-not [string]::IsNullOrWhiteSpace([string]$id)) {
                    [void]$optionalControlIds.Add(([string]$id).Trim())
                }
            }
        }
    } catch {
        Write-Host "Failed to parse acceptance profile, using strict defaults: $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

$networkMetrics = Get-NetworkSmokeMetrics -DiagnosticsRoot $networkDiagnosticsRoot
if ($null -eq $networkMetrics) {
    Add-Check -List $checks -Id "1.8.A" -Control "Network/runtime smoke baseline (RTSP/HTTP/ONVIF/PullPoint)" -State "CONDITIONAL" -Evidence "No diagnostic summary found under diagnostics/network-smoke."
} else {
    $networkAgeHours = [math]::Round(($now - $networkMetrics.SummaryLastWrite).TotalHours, 1)
    if ($networkAgeHours -gt $MaxEvidenceAgeHours) {
        Add-Check -List $checks -Id "1.8.A" -Control "Network/runtime smoke baseline (RTSP/HTTP/ONVIF/PullPoint)" -State "CONDITIONAL" -Evidence "Latest network smoke is stale: age=${networkAgeHours}h > ${MaxEvidenceAgeHours}h. Source: $($networkMetrics.SummaryPath)"
    } else {
        $coreRuntimePass = ($networkMetrics.RtspPct -ge 95.0) -and
            ($networkMetrics.HttpPct -ge 95.0) -and
            ($networkMetrics.OnvifPct -ge 95.0) -and
            ($networkMetrics.MediaEventsPct -ge 70.0)

        if ($coreRuntimePass) {
            Add-Check -List $checks -Id "1.8.A" -Control "Core network/runtime baseline (RTSP/HTTP/ONVIF/Media+Events)" -State "PASS" -Evidence "rtsp=$($networkMetrics.RtspPct)%, http=$($networkMetrics.HttpPct)%, onvif=$($networkMetrics.OnvifPct)%, mediaEvents=$($networkMetrics.MediaEventsPct)% (core thresholds satisfied). Source: $($networkMetrics.SummaryPath)"
        } elseif ($networkMetrics.NetworkLayerPct -ge $MinNetworkLayerPct) {
            Add-Check -List $checks -Id "1.8.A" -Control "Core network/runtime baseline (RTSP/HTTP/ONVIF/Media+Events)" -State "PASS" -Evidence "networkLayerPct=$($networkMetrics.NetworkLayerPct) >= $MinNetworkLayerPct. Source: $($networkMetrics.SummaryPath)"
        } else {
            Add-Check -List $checks -Id "1.8.A" -Control "Core network/runtime baseline (RTSP/HTTP/ONVIF/Media+Events)" -State "FAIL" -Evidence "Core thresholds are not met and networkLayerPct=$($networkMetrics.NetworkLayerPct) < $MinNetworkLayerPct. Source: $($networkMetrics.SummaryPath)"
        }

        if ($networkMetrics.PullPointPct -ge 50.0) {
            Add-Check -List $checks -Id "1.8.A1" -Control "PullPoint compatibility coverage" -State "PASS" -Evidence "pullPointPct=$($networkMetrics.PullPointPct)% >= 50%."
        } elseif ($networkMetrics.PullPointPct -ge 20.0) {
            Add-Check -List $checks -Id "1.8.A1" -Control "PullPoint compatibility coverage" -State "CONDITIONAL" -Evidence "pullPointPct=$($networkMetrics.PullPointPct)% is partial (<50%)."
        } else {
            Add-Check -List $checks -Id "1.8.A1" -Control "PullPoint compatibility coverage" -State "CONDITIONAL" -Evidence "pullPointPct=$($networkMetrics.PullPointPct)% indicates broad camera/vendor limitation or missing config."
        }
    }
}

$matrixMetrics = Get-LongRunMatrixMetrics -DiagnosticsRoot $matrixDiagnosticsRoot
if ($null -eq $matrixMetrics) {
    Add-Check -List $checks -Id "1.8.B" -Control "Long-run matrix scenario pass rate" -State "CONDITIONAL" -Evidence "No matrix summary found under diagnostics/video-longrun-matrix."
    Add-Check -List $checks -Id "1.8.C" -Control "Long-run checks pass rate" -State "CONDITIONAL" -Evidence "Per-scenario long-run reports are unavailable."
} else {
    $matrixAgeHours = [math]::Round(($now - $matrixMetrics.SummaryLastWrite).TotalHours, 1)
    if ($matrixAgeHours -gt $MaxEvidenceAgeHours) {
        Add-Check -List $checks -Id "1.8.B" -Control "Long-run matrix scenario pass rate" -State "CONDITIONAL" -Evidence "Latest long-run matrix is stale: age=${matrixAgeHours}h > ${MaxEvidenceAgeHours}h. Source: $($matrixMetrics.SummaryPath)"
        Add-Check -List $checks -Id "1.8.C" -Control "Long-run checks pass rate" -State "CONDITIONAL" -Evidence "Long-run evidence is stale; run a fresh matrix."
    } else {
        if ($matrixMetrics.ScenarioPassPct -ge $MinLongRunScenarioPassPct) {
            Add-Check -List $checks -Id "1.8.B" -Control "Long-run matrix scenario pass rate" -State "PASS" -Evidence "$($matrixMetrics.PassedScenarios)/$($matrixMetrics.TotalScenarios) ($($matrixMetrics.ScenarioPassPct)%) >= $MinLongRunScenarioPassPct. Source: $($matrixMetrics.SummaryPath)"
        } else {
            Add-Check -List $checks -Id "1.8.B" -Control "Long-run matrix scenario pass rate" -State "FAIL" -Evidence "$($matrixMetrics.PassedScenarios)/$($matrixMetrics.TotalScenarios) ($($matrixMetrics.ScenarioPassPct)%) < $MinLongRunScenarioPassPct. Source: $($matrixMetrics.SummaryPath)"
        }

        if ($matrixMetrics.ChecksTotal -eq 0) {
            Add-Check -List $checks -Id "1.8.C" -Control "Long-run checks pass rate" -State "CONDITIONAL" -Evidence "No parsed check counters in per-scenario reports (ChecksTotal=0)."
        } elseif ($matrixMetrics.ChecksPassPct -ge $MinLongRunChecksPassPct) {
            Add-Check -List $checks -Id "1.8.C" -Control "Long-run checks pass rate" -State "PASS" -Evidence "$($matrixMetrics.ChecksOk)/$($matrixMetrics.ChecksTotal) ($($matrixMetrics.ChecksPassPct)%) >= $MinLongRunChecksPassPct."
        } else {
            Add-Check -List $checks -Id "1.8.C" -Control "Long-run checks pass rate" -State "FAIL" -Evidence "$($matrixMetrics.ChecksOk)/$($matrixMetrics.ChecksTotal) ($($matrixMetrics.ChecksPassPct)%) < $MinLongRunChecksPassPct."
        }
    }
}

$recordingWsMetrics = Get-RecordingWsAcceptanceMetrics -DiagnosticsRoot $recordingWsDiagnosticsRoot
if ($null -eq $recordingWsMetrics) {
    Add-Check -List $checks -Id "1.8.E" -Control "Recording WS lifecycle acceptance evidence" -State "CONDITIONAL" -Evidence "No recording WS acceptance evidence found under diagnostics/recording-ws-acceptance."
} else {
    $wsAgeHours = [math]::Round(($now - $recordingWsMetrics.LastWrite).TotalHours, 1)
    if ($wsAgeHours -gt $MaxEvidenceAgeHours) {
        Add-Check -List $checks -Id "1.8.E" -Control "Recording WS lifecycle acceptance evidence" -State "CONDITIONAL" -Evidence "Latest recording WS evidence is stale: age=${wsAgeHours}h > ${MaxEvidenceAgeHours}h. Source: $($recordingWsMetrics.JsonPath)"
    } elseif ($recordingWsMetrics.Overall -eq "PASS") {
        Add-Check -List $checks -Id "1.8.E" -Control "Recording WS lifecycle acceptance evidence" -State "PASS" -Evidence "recordingWsOverall=PASS (env=$($recordingWsMetrics.Environment)). Source: $($recordingWsMetrics.JsonPath)"
    } elseif ($recordingWsMetrics.Overall -eq "FAIL") {
        Add-Check -List $checks -Id "1.8.E" -Control "Recording WS lifecycle acceptance evidence" -State "FAIL" -Evidence "recordingWsOverall=FAIL (env=$($recordingWsMetrics.Environment)). Source: $($recordingWsMetrics.JsonPath)"
    } else {
        Add-Check -List $checks -Id "1.8.E" -Control "Recording WS lifecycle acceptance evidence" -State "CONDITIONAL" -Evidence "recordingWsOverall=$($recordingWsMetrics.Overall) (env=$($recordingWsMetrics.Environment)). Source: $($recordingWsMetrics.JsonPath)"
    }
}

# Reference weighted readiness from canonical 1.8 breakdown.
$canonicalReadinessPct = 66.0
if ($canonicalReadinessPct -ge 70.0) {
    Add-Check -List $checks -Id "1.8.D" -Control "Canonical weighted readiness floor" -State "PASS" -Evidence "Canonical 1.8 readiness=$canonicalReadinessPct% (target >= 70%)."
} else {
    Add-Check -List $checks -Id "1.8.D" -Control "Canonical weighted readiness floor" -State "CONDITIONAL" -Evidence "Canonical 1.8 readiness=$canonicalReadinessPct% (target >= 70% not met yet)."
}

$failCount = @($checks | Where-Object { $_.State -eq "FAIL" }).Count
$conditionalCount = @($checks | Where-Object { $_.State -eq "CONDITIONAL" }).Count
$goDecision = if ($failCount -eq 0 -and $conditionalCount -eq 0) { "GO" } else { "NO-GO" }

# Profile-aware release decision (optional controls excluded from blockers)
$releaseBlockingChecks = @(
    $checks | Where-Object {
        -not $optionalControlIds.Contains($_.Id)
    }
)
$releaseDecisionProfileAware = if (
    @($releaseBlockingChecks | Where-Object { $_.State -eq "FAIL" }).Count -eq 0 -and
    @($releaseBlockingChecks | Where-Object { $_.State -eq "CONDITIONAL" }).Count -eq 0
) {
    "GO"
} else {
    "NO-GO"
}

# Runtime-focused decision: only blocking controls must be PASS.
$runtimeBlockingIds = @("1.8.A", "1.8.B", "1.8.C")
$runtimeBlockingChecks = @($checks | Where-Object { $runtimeBlockingIds -contains $_.Id })
$runtimeDecision = if (
    @($runtimeBlockingChecks | Where-Object { $_.State -eq "FAIL" }).Count -eq 0 -and
    @($runtimeBlockingChecks | Where-Object { $_.State -eq "CONDITIONAL" }).Count -eq 0
) {
    "GO"
} else {
    "NO-GO"
}

# Runtime readiness score (separate from canonical weighted score).
$runtimeSignals = New-Object System.Collections.Generic.List[double]
if ($null -ne $networkMetrics) { $runtimeSignals.Add([double]$networkMetrics.NetworkLayerPct) | Out-Null }
if ($null -ne $matrixMetrics) { $runtimeSignals.Add([double]$matrixMetrics.ScenarioPassPct) | Out-Null; $runtimeSignals.Add([double]$matrixMetrics.ChecksPassPct) | Out-Null }
$runtimeReadinessPct = if ($runtimeSignals.Count -gt 0) {
    [math]::Round((($runtimeSignals | Measure-Object -Average).Average), 1)
} else {
    0.0
}

$resolvedReportPath = Join-Path $resolvedOutputDir $ReportFileName
$lines = @()
$lines += "# Video E2E Go/No-Go Report"
$lines += ""
$lines += "- Generated: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss zzz")"
$lines += "- Release decision: **$goDecision**"
$lines += "- Release decision (profile-aware): **$releaseDecisionProfileAware**"
$lines += "- Acceptance profile: **$acceptanceProfileName**"
$lines += "- Optional controls in profile: " + $(if ($optionalControlIds.Count -gt 0) { "**$(([string[]]$optionalControlIds | Sort-Object) -join ', ')**" } else { "none" })
$lines += "- Runtime decision (1.8.A/1.8.B/1.8.C): **$runtimeDecision**"
$lines += "- Runtime readiness (signals): **$runtimeReadinessPct%**"
$lines += "- Canonical weighted readiness (1.8): **$canonicalReadinessPct%**"
$lines += ""
$lines += "| ID | Control | State | Evidence |"
$lines += "|---|---|---|---|"
foreach ($c in $checks) {
    $evidence = $c.Evidence.Replace("|", "/")
    $lines += "| $($c.Id) | $($c.Control) | $($c.State) | $evidence |"
}

$lines -join [Environment]::NewLine | Set-Content -LiteralPath $resolvedReportPath -Encoding UTF8
Write-Host "Video E2E decision: $goDecision"
Write-Host "Video E2E profile-aware release decision: $releaseDecisionProfileAware"
Write-Host "Report written to: $resolvedReportPath"

if ($releaseDecisionProfileAware -eq "NO-GO") {
    exit 1
}
exit 0
