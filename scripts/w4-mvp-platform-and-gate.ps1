# W4 (week 4): platform + GO/NO-GO prep - automated build/compile gate.
# Maps to section "1) Build Gate" in docs/planning/RELEASE_GO_NO_GO_CHECKLIST.md (full or partial).
#
# Usage (from repo root):
#   .\scripts\w4-mvp-platform-and-gate.ps1
#   .\scripts\w4-mvp-platform-and-gate.ps1 -Full
#   .\scripts\w4-mvp-platform-and-gate.ps1 -Full -IncludeWeb -IncludeDesktop
#   .\scripts\w4-mvp-platform-and-gate.ps1 -Full -IncludeNas
#   .\scripts\w4-mvp-platform-and-gate.ps1 -RunProfile custom -IncludeAndroidSmoke -AndroidLocalFrameAnalytics
#
# Exit: 0 success, 1 failure.

[Diagnostics.CodeAnalysis.SuppressMessageAttribute('PSAvoidUsingPlainTextForPassword', '')]

param(
    [switch]$ShowHelp,
    [ValidateSet("custom", "local", "staging", "strict")]
    [string]$RunProfile = "custom",
    [switch]$Full,
    [switch]$IncludeWeb,
    [switch]$IncludeDesktop,
    [switch]$IncludeNas,
    [switch]$IncludeAndroidSmoke,
    [switch]$IncludeDesktopSmoke,
    [switch]$IncludeSecurityValidation,
    [switch]$GenerateGoNoGoSummary,
    [switch]$FailOnPartial,
    [switch]$TreatPartialAsSuccess,
    [switch]$RequirePostgresEvidence,
    [switch]$RequireValidPostgresEvidence,
    [string]$PostgresEvidenceDir = "docs\reports",
    [string]$PostgresEvidencePattern = "POSTGRESQL_FINALIZATION_STAGING_REPORT_*.md",
    [string]$ReportOutputDir = "diagnostics\platform-smoke",
    [string]$DesktopSmokeBaseUrl = "",
    [string]$DesktopSmokeUsername = "admin",
    [string]$DesktopSmokePassword = "",
    [string[]]$DesktopSmokePlaylistUrls = @(),
    [ValidateSet("Strict", "MvpCi")]
    [string]$GoNoGoDecisionProfile = "Strict",
    [switch]$StrictPhase1Summary,
    # When set, Android/Desktop smoke scripts require device/long-run for PASS (no -CompileOnly / -CompileAndTestOnly pass-through).
    [switch]$NoPlatformSmokeCompileOnlyGate,
    # Debug Android build with BuildConfig.LOCAL_FRAME_ANALYTICS (Gradle -P + android-video-background-smoke -LocalFrameAnalytics).
    [switch]$AndroidLocalFrameAnalytics
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "W4 MVP platform gate (Gradle + optional web/smokes/summary)"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\w4-mvp-platform-and-gate.ps1 -RunProfile local"
    Write-Host "  .\scripts\w4-mvp-platform-and-gate.ps1 -RunProfile strict"
    Write-Host "  .\scripts\w4-mvp-platform-and-gate.ps1 -RunProfile custom -IncludeWeb -IncludeAndroidSmoke -GenerateGoNoGoSummary"
    Write-Host "  .\scripts\w4-mvp-platform-and-gate.ps1 -Full -IncludeWeb -IncludeDesktop"
    Write-Host "  .\scripts\w4-mvp-platform-and-gate.ps1 -RunProfile local -StrictPhase1Summary"
    Write-Host "  .\scripts\w4-mvp-platform-and-gate.ps1 -RunProfile local -IncludeAndroidSmoke -AndroidLocalFrameAnalytics"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -RunProfile custom|local|staging|strict  Preset bundles (default custom)."
    Write-Host "  -Full  Release-oriented Gradle tasks (Android bundle/release; optional desktop/NAS)."
    Write-Host "  -IncludeWeb -IncludeAndroidSmoke -IncludeDesktopSmoke -IncludeSecurityValidation -GenerateGoNoGoSummary"
    Write-Host "  -FailOnPartial  Treat PARTIAL optional steps as failure."
    Write-Host "  -TreatPartialAsSuccess  Keep PARTIAL in report but exit 0."
    Write-Host "  -RequirePostgresEvidence  Fail if staging postgres evidence report is missing."
    Write-Host "  -RequireValidPostgresEvidence  Validate latest staging postgres report content (no TODO/unchecked)."
    Write-Host "  -GoNoGoDecisionProfile Strict|MvpCi  Passed to Phase1 summary when generated (profiles may override)."
    Write-Host "  -StrictPhase1Summary  Force Strict decision profile for the summary step."
    Write-Host "  -NoPlatformSmokeCompileOnlyGate  Do not pass -CompileOnly/-CompileAndTestOnly to platform smoke scripts (profiles local|staging|strict pass them by default)."
    Write-Host "  -AndroidLocalFrameAnalytics  Pass -Pipcss.localFrameAnalytics=true to android compileDebug + Android smoke (non-Full gate only)."
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Success (or PARTIAL with -TreatPartialAsSuccess)"
    Write-Host "  1  Failure"
    Write-Host "  3  PARTIAL overall when -TreatPartialAsSuccess is not set"
    Write-Host ""
    Write-Host "Output:"
    Write-Host "  diagnostics/platform-smoke/w4-mvp-platform-gate-<run-id>.md"
    Write-Host "  diagnostics/platform-smoke/w4-mvp-platform-gate-<run-id>.json"
    exit 0
}

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$gradlew = Join-Path $ProjectRoot "gradlew.bat"
$resolvedPostgresEvidenceDir = Join-Path $ProjectRoot $PostgresEvidenceDir
$resolvedReportOutputDir = Join-Path $ProjectRoot $ReportOutputDir
$postgresEvidenceValidatorScript = Join-Path $ScriptDir "postgresql-staging-evidence-validate.ps1"
$androidSmokeScript = Join-Path $ScriptDir "android-video-background-smoke.ps1"
$desktopSmokeScript = Join-Path $ScriptDir "desktop-video-event-longrun-smoke.ps1"
$securityValidationScript = Join-Path $ScriptDir "security-field-staging-validation.ps1"
$goNoGoSummaryScript = Join-Path $ScriptDir "generate-phase1-go-no-go-summary.ps1"
$hadPartial = $false
$runId = Get-Date -Format "yyyyMMdd-HHmmss"
$stepResults = New-Object System.Collections.Generic.List[object]

if (-not (Test-Path $gradlew)) {
    Write-Host "gradlew.bat not found: $gradlew" -ForegroundColor Red
    exit 1
}

function Set-ProfileDefaults {
    switch ($RunProfile) {
        "local" {
            $script:IncludeWeb = $true
            $script:IncludeAndroidSmoke = $true
            $script:IncludeSecurityValidation = $true
            $script:GenerateGoNoGoSummary = $true
            $script:TreatPartialAsSuccess = $true
            $script:GoNoGoDecisionProfile = "MvpCi"
        }
        "staging" {
            $script:IncludeWeb = $true
            $script:IncludeAndroidSmoke = $true
            $script:IncludeDesktopSmoke = $true
            $script:IncludeSecurityValidation = $true
            $script:GenerateGoNoGoSummary = $true
            $script:RequirePostgresEvidence = $true
            $script:RequireValidPostgresEvidence = $true
            $script:TreatPartialAsSuccess = $true
            $script:GoNoGoDecisionProfile = "MvpCi"
        }
        "strict" {
            $script:IncludeWeb = $true
            $script:IncludeAndroidSmoke = $true
            $script:IncludeDesktopSmoke = $true
            $script:IncludeSecurityValidation = $true
            $script:GenerateGoNoGoSummary = $true
            $script:RequirePostgresEvidence = $true
            $script:RequireValidPostgresEvidence = $true
            $script:FailOnPartial = $true
            $script:TreatPartialAsSuccess = $false
            $script:GoNoGoDecisionProfile = "Strict"
        }
        default { }
    }
}
Set-ProfileDefaults
if ($StrictPhase1Summary) {
    $script:GoNoGoDecisionProfile = "Strict"
}

# Automated gate: compile+assemble (Android) / compile+JVM tests (Desktop) without failing overall when hardware/long-run is absent.
$usePlatformSmokeCompileOnlyGate = ($RunProfile -in @("local", "staging", "strict")) -and (-not $NoPlatformSmokeCompileOnlyGate)

$gradleAndroidDebugExtras = @()
if ($AndroidLocalFrameAnalytics) {
    $gradleAndroidDebugExtras = @("-Pipcss.localFrameAnalytics=true")
}

function Invoke-Gradle {
    param(
        [string[]]$Tasks,
        [string[]]$LeadingGradleArgs = @()
    )
    $allArgs = [System.Collections.Generic.List[string]]::new()
    foreach ($a in $LeadingGradleArgs) {
        $null = $allArgs.Add($a)
    }
    foreach ($t in $Tasks) {
        $null = $allArgs.Add($t)
    }
    $line = ($allArgs | ForEach-Object { $_ }) -join " "
    Write-Host "==> gradlew $line" -ForegroundColor Cyan
    & $gradlew @allArgs --no-daemon
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle failed: $line (exit $LASTEXITCODE)"
    }
}

function Test-PostgresEvidence {
    if (-not (Test-Path $resolvedPostgresEvidenceDir)) {
        if ($RequirePostgresEvidence) {
            throw "PostgreSQL evidence directory not found: $resolvedPostgresEvidenceDir"
        }
        Write-Host "PostgreSQL evidence directory not found: $resolvedPostgresEvidenceDir (warning only)" -ForegroundColor Yellow
        return
    }

    $latestReport = Get-ChildItem -Path $resolvedPostgresEvidenceDir -File -Filter $PostgresEvidencePattern -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTimeUtc -Descending |
        Select-Object -First 1

    if ($null -eq $latestReport) {
        $message = "PostgreSQL staging evidence not found ($PostgresEvidencePattern in $resolvedPostgresEvidenceDir)."
        if ($RequirePostgresEvidence) {
            throw $message
        }
        Write-Host "$message Warning only." -ForegroundColor Yellow
        return
    }

    $ageHours = [math]::Round(((Get-Date).ToUniversalTime() - $latestReport.LastWriteTimeUtc).TotalHours, 1)
    Write-Host ("==> PostgreSQL evidence: {0} (age {1}h)" -f $latestReport.FullName, $ageHours) -ForegroundColor Cyan

    if ($RequireValidPostgresEvidence) {
        if (-not (Test-Path $postgresEvidenceValidatorScript)) {
            throw "PostgreSQL evidence validator script not found: $postgresEvidenceValidatorScript"
        }
        Write-Host "==> Validating PostgreSQL staging evidence content" -ForegroundColor Cyan
        & $postgresEvidenceValidatorScript -ReportPath $latestReport.FullName
        if ($LASTEXITCODE -ne 0) {
            throw "PostgreSQL staging evidence validation failed for: $($latestReport.FullName)"
        }
    }
}

function Add-StepResult {
    param(
        [string]$Step,
        [string]$Status,
        [string]$Details = ""
    )
    $stepResults.Add([PSCustomObject]@{
        Step = $Step
        Status = $Status
        Details = $Details
    }) | Out-Null
}

function Write-W4Report {
    param(
        [string]$OverallStatus,
        [string]$Message
    )
    if (-not (Test-Path $resolvedReportOutputDir)) {
        New-Item -ItemType Directory -Path $resolvedReportOutputDir -Force | Out-Null
    }

    $mdPath = Join-Path $resolvedReportOutputDir ("w4-mvp-platform-gate-{0}.md" -f $runId)
    $jsonPath = Join-Path $resolvedReportOutputDir ("w4-mvp-platform-gate-{0}.json" -f $runId)

    $md = @(
        "# W4 MVP Platform Gate Report",
        "",
        "- Run ID: $runId",
        "- Run profile: **$RunProfile**",
        "- Full: **$([bool]$Full)**",
        "- Platform smoke compile-only gate: **$usePlatformSmokeCompileOnlyGate**",
        "- Android local frame analytics: **$([bool]$AndroidLocalFrameAnalytics)**",
        "- Overall: **$OverallStatus**",
        "- Message: $Message",
        "",
        "| Step | Status | Details |",
        "|---|---|---|"
    )
    foreach ($s in $stepResults) {
        $details = ([string]$s.Details).Replace("|", "/")
        $md += "| $($s.Step) | $($s.Status) | $details |"
    }
    $md -join [Environment]::NewLine | Set-Content -Path $mdPath -Encoding UTF8

    $stepsArray = @($stepResults | ForEach-Object {
        [PSCustomObject]@{
            step = $_.Step
            status = $_.Status
            details = $_.Details
        }
    })
    $reportObject = [PSCustomObject]@{
        runId = $runId
        runProfile = $RunProfile
        full = [bool]$Full
        platformSmokeCompileOnlyGate = [bool]$usePlatformSmokeCompileOnlyGate
        androidLocalFrameAnalytics = [bool]$AndroidLocalFrameAnalytics
        timestampUtc = [DateTime]::UtcNow.ToString("o")
        overall = $OverallStatus
        message = $Message
        steps = $stepsArray
    }
    $reportObject | ConvertTo-Json -Depth 8 | Set-Content -Path $jsonPath -Encoding UTF8

    Write-Host ("W4 report: {0}" -f $mdPath) -ForegroundColor Green
    Write-Host ("W4 json: {0}" -f $jsonPath) -ForegroundColor Green
}

function Invoke-OptionalScript {
    param(
        [string]$ScriptPath,
        [string]$Title,
        [hashtable]$Arguments = @{},
        [int[]]$NonFailExitCodes = @()
    )

    if (-not (Test-Path $ScriptPath)) {
        throw "$Title script not found: $ScriptPath"
    }

    $maxAttempts = 2
    for ($attempt = 1; $attempt -le $maxAttempts; $attempt++) {
        Write-Host ("==> {0} (attempt {1}/{2})" -f $Title, $attempt, $maxAttempts) -ForegroundColor Cyan
        & $ScriptPath @Arguments
        $exitCode = $LASTEXITCODE
        if ($exitCode -eq 0) {
            Add-StepResult -Step $Title -Status "PASS"
            return
        }

        if ($NonFailExitCodes -contains $exitCode) {
            $script:hadPartial = $true
            Add-StepResult -Step $Title -Status "PARTIAL" -Details ("non-fatal exit code {0}" -f $exitCode)
            Write-Host "$Title returned non-fatal exit code $exitCode." -ForegroundColor Yellow
            return
        }

        if ($exitCode -eq 3) {
            $script:hadPartial = $true
            Add-StepResult -Step $Title -Status "PARTIAL" -Details "exit code 3"
            if ($FailOnPartial) {
                throw "$Title returned PARTIAL (exit 3) and -FailOnPartial is set."
            }
            Write-Host "$Title returned PARTIAL (exit 3)." -ForegroundColor Yellow
            return
        }

        if ($attempt -lt $maxAttempts) {
            Write-Host "$Title failed (exit $exitCode). Retrying..." -ForegroundColor Yellow
        } else {
            throw "$Title failed (exit $exitCode)"
        }
    }
}

try {
    Add-StepResult -Step "profile" -Status "PASS" -Details (
        "profile={0}; androidLocalFrameAnalytics={1}" -f $RunProfile, ([bool]$AndroidLocalFrameAnalytics)
    )
    Invoke-Gradle @(":server:api:build")
    Add-StepResult -Step "server-api-build" -Status "PASS"

    if ($Full) {
        Invoke-Gradle @(":android:app:assembleRelease")
        Add-StepResult -Step "android-assemble-release" -Status "PASS"
        Invoke-Gradle @(":android:app:bundleRelease")
        Add-StepResult -Step "android-bundle-release" -Status "PASS"
        if ($IncludeDesktop) {
            Invoke-Gradle @(":platforms:client-desktop-x86_64:app:packageReleaseDistributionForCurrentOS")
            Invoke-Gradle @(":platforms:client-desktop-arm:app:packageReleaseDistributionForCurrentOS")
            Add-StepResult -Step "desktop-release-packages" -Status "PASS"
        }
        if ($IncludeNas) {
            Invoke-Gradle @("buildNasPackages")
            Add-StepResult -Step "nas-packages" -Status "PASS"
        }
    }
    else {
        Invoke-Gradle @(":android:app:compileDebugKotlin") -LeadingGradleArgs $gradleAndroidDebugExtras
        Add-StepResult -Step "android-compile-debug" -Status "PASS"
    }

    if ($IncludeWeb) {
        $webDir = Join-Path $ProjectRoot "server\web"
        if (-not (Test-Path $webDir)) {
            throw "server\web not found"
        }
        Push-Location $webDir
        try {
            Write-Host "==> npx tsc --noEmit (server\web)" -ForegroundColor Cyan
            npx tsc --noEmit
            if ($LASTEXITCODE -ne 0) {
                throw "tsc failed (exit $LASTEXITCODE)"
            }
            Add-StepResult -Step "web-tsc" -Status "PASS"
        }
        finally {
            Pop-Location
        }
    }

    Test-PostgresEvidence
    Add-StepResult -Step "postgres-evidence" -Status "PASS"

    if ($IncludeAndroidSmoke) {
        $androidArgs = @{}
        if ($usePlatformSmokeCompileOnlyGate) {
            $androidArgs["CompileOnly"] = $true
        }
        if ($AndroidLocalFrameAnalytics) {
            $androidArgs["LocalFrameAnalytics"] = $true
        }
        Invoke-OptionalScript -ScriptPath $androidSmokeScript -Title "Android video/background smoke" -Arguments $androidArgs
    }

    if ($IncludeDesktopSmoke) {
        $desktopArgs = @{}
        if (-not [string]::IsNullOrWhiteSpace($DesktopSmokeBaseUrl)) {
            $desktopArgs["BaseUrl"] = $DesktopSmokeBaseUrl
        }
        if (-not [string]::IsNullOrWhiteSpace($DesktopSmokeUsername)) {
            $desktopArgs["Username"] = $DesktopSmokeUsername
        }
        if (-not [string]::IsNullOrWhiteSpace($DesktopSmokePassword)) {
            $desktopArgs["Password"] = $DesktopSmokePassword
        }
        if ($DesktopSmokePlaylistUrls.Count -gt 0) {
            $desktopArgs["PlaylistUrls"] = $DesktopSmokePlaylistUrls
        }
        if ($usePlatformSmokeCompileOnlyGate) {
            $desktopArgs["CompileAndTestOnly"] = $true
        }
        Invoke-OptionalScript -ScriptPath $desktopSmokeScript -Title "Desktop video/event smoke" -Arguments $desktopArgs
    }

    if ($IncludeSecurityValidation) {
        Invoke-OptionalScript -ScriptPath $securityValidationScript -Title "Security field/staging validation"
    }

    if ($GenerateGoNoGoSummary) {
        Invoke-OptionalScript -ScriptPath $goNoGoSummaryScript -Title "Phase1 Go/No-Go summary" -Arguments @{
            DecisionProfile = $GoNoGoDecisionProfile
        } -NonFailExitCodes @(2, 3)
    }

    Write-Host ""
    if ($hadPartial) {
        Write-Host "W4 gate: PARTIAL SUCCESS" -ForegroundColor Yellow
        Write-W4Report -OverallStatus "PARTIAL" -Message "Some optional checks produced partial signals."
    } else {
        Write-Host "W4 gate: SUCCESS" -ForegroundColor Green
        Write-W4Report -OverallStatus "SUCCESS" -Message "All enabled checks passed."
    }
    if (-not $Full) {
        Write-Host "Hint: full release gate: .\scripts\w4-mvp-platform-and-gate.ps1 -Full" -ForegroundColor DarkGray
    }
    if ($hadPartial -and -not $TreatPartialAsSuccess) { exit 3 }
    exit 0
}
catch {
    Add-StepResult -Step "w4-exception" -Status "FAIL" -Details $_.Exception.Message
    Write-W4Report -OverallStatus "FAIL" -Message $_.Exception.Message
    Write-Host ""
    Write-Host "W4 gate: FAILED - $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
