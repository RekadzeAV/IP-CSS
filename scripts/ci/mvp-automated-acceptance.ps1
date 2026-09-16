# Автоматическая приёмка Фазы 1 (MVP) — см. docs/automation/MVP_PHASE1_AUTOMATED_ACCEPTANCE.md
# Exit: 0 success, non-zero failure.

param(
    [switch]$ShowHelp,
    [switch]$SkipVideoGate,
    [switch]$GeneratePhase1Summary,
    [ValidateSet("Strict", "MvpCi")]
    [string]$Phase1SummaryProfile = "MvpCi"
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $ScriptDir)
$gradlew = Join-Path $ProjectRoot "gradlew.bat"
$videoGateScript = Join-Path $ProjectRoot "scripts\video-e2e-go-no-go.ps1"
$phase1SummaryScript = Join-Path $ProjectRoot "scripts\generate-phase1-go-no-go-summary.ps1"

if ($ShowHelp) {
    Write-Host "MVP Phase 1 automated acceptance (Gradle + web + optional video gate + optional summary)"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\ci\mvp-automated-acceptance.ps1"
    Write-Host "  .\scripts\ci\mvp-automated-acceptance.ps1 -SkipVideoGate"
    Write-Host "  .\scripts\ci\mvp-automated-acceptance.ps1 -GeneratePhase1Summary -Phase1SummaryProfile Strict"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -SkipVideoGate  Skip scripts/video-e2e-go-no-go.ps1."
    Write-Host "  -GeneratePhase1Summary  Run scripts/generate-phase1-go-no-go-summary.ps1 after other steps."
    Write-Host "  -Phase1SummaryProfile Strict|MvpCi  Passed to the summary script (default MvpCi)."
    Write-Host ""
    Write-Host "Steps:"
    Write-Host "  - :shared:desktopTest, :core:network:desktopTest, :server:api:test"
    Write-Host "  - server/web: npm ci (with retry), npm run build, npm test"
    Write-Host "  - video gate unless -SkipVideoGate (on GitHub Actions uses config/video-e2e-acceptance-profile.mvp-ci.json when present)"
    Write-Host "  - optional Phase1 summary when -GeneratePhase1Summary (включает последний w4-mvp-platform-gate-*.json при наличии — контекст в отчёте, см. generate-phase1-go-no-go-summary.ps1 -ShowHelp)"
    Write-Host "  - env MVP_VIDEO_ACCEPTANCE_PROFILE: optional path to override video gate acceptance profile"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Success"
    Write-Host "  Non-zero  First failing step; with -GeneratePhase1Summary: exit 2 (NO_GO) always fails; exit 3 (CONDITIONAL) fails only when -Phase1SummaryProfile Strict"
    exit 0
}

if (-not (Test-Path $gradlew)) {
    Write-Host "gradlew.bat not found: $gradlew" -ForegroundColor Red
    exit 1
}

function Invoke-NpmCiWithRetry {
    param(
        [string]$WorkingDir,
        [int]$MaxAttempts = 2
    )

    for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
        if ($attempt -gt 1) {
            Write-Host "npm ci retry #${attempt}: cleaning node_modules..." -ForegroundColor Yellow
            $nodeModulesPath = Join-Path $WorkingDir "node_modules"
            if (Test-Path $nodeModulesPath) {
                Remove-Item -Recurse -Force $nodeModulesPath -ErrorAction SilentlyContinue
            }
        }

        npm ci --include=dev
        if ($LASTEXITCODE -eq 0) {
            return
        }
    }

    throw "npm ci failed after $MaxAttempts attempts"
}

Write-Host "==> mvp-automated-acceptance: Gradle (shared + core:network desktopTest + server api test)" -ForegroundColor Cyan
& $gradlew ":shared:desktopTest" ":core:network:desktopTest" ":server:api:test" --no-daemon
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$webDir = Join-Path $ProjectRoot "server\web"
if (-not (Test-Path $webDir)) {
    Write-Host "server\web not found" -ForegroundColor Red
    exit 1
}

Push-Location $webDir
try {
    Write-Host "==> mvp-automated-acceptance: server/web (build + jest)" -ForegroundColor Cyan
    Invoke-NpmCiWithRetry -WorkingDir $webDir
    npm run build
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    npm test
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}
finally {
    Pop-Location
}

if (-not $SkipVideoGate) {
    if (-not (Test-Path $videoGateScript)) {
        Write-Host "video-e2e-go-no-go.ps1 not found: $videoGateScript" -ForegroundColor Red
        exit 1
    }

    Write-Host "==> mvp-automated-acceptance: video e2e gate (profile-aware)" -ForegroundColor Cyan
    $acceptanceProfileForGate = $null
    if (-not [string]::IsNullOrWhiteSpace($env:MVP_VIDEO_ACCEPTANCE_PROFILE)) {
        $acceptanceProfileForGate = $env:MVP_VIDEO_ACCEPTANCE_PROFILE
    } elseif ($env:GITHUB_ACTIONS -eq "true") {
        $ciProf = Join-Path $ProjectRoot "config\video-e2e-acceptance-profile.mvp-ci.json"
        if (Test-Path -LiteralPath $ciProf) {
            $acceptanceProfileForGate = $ciProf
        }
    }
    if ($null -ne $acceptanceProfileForGate) {
        Write-Host "  (AcceptanceProfilePath: $acceptanceProfileForGate)" -ForegroundColor DarkGray
        & $videoGateScript -AcceptanceProfilePath $acceptanceProfileForGate
    } else {
        & $videoGateScript
    }
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

if ($GeneratePhase1Summary) {
    if (-not (Test-Path $phase1SummaryScript)) {
        Write-Host "generate-phase1-go-no-go-summary.ps1 not found: $phase1SummaryScript" -ForegroundColor Red
        exit 1
    }
    Write-Host "==> mvp-automated-acceptance: Phase1 go/no-go summary (-DecisionProfile $Phase1SummaryProfile)" -ForegroundColor Cyan
    & $phase1SummaryScript -DecisionProfile $Phase1SummaryProfile
    $summaryExit = $LASTEXITCODE
    if ($summaryExit -eq 2) { exit 2 }
    if ($summaryExit -eq 3) {
        if ($Phase1SummaryProfile -eq "Strict") { exit 3 }
        Write-Host "Phase1 summary: CONDITIONAL (exit 3); not failing acceptance (profile MvpCi)." -ForegroundColor Yellow
    } elseif ($summaryExit -ne 0) {
        exit $summaryExit
    }
}

Write-Host "==> mvp-automated-acceptance: SUCCESS" -ForegroundColor Green
exit 0
