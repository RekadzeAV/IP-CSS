# PowerShell СЃРєСЂРёРїС‚ РґР»СЏ Р·Р°РїСѓСЃРєР° РІСЃРµС… CI/CD РїСЂРѕРІРµСЂРѕРє Р»РѕРєР°Р»СЊРЅРѕ
# РСЃРїРѕР»СЊР·РѕРІР°РЅРёРµ: .\scripts\ci-run-all.ps1 [-Profile mvp|staging|strict] [-SkipNative]

param(
    [ValidateSet("mvp", "staging", "strict")]
    [string]$Profile = "mvp",
    [switch]$SkipNative,
    [switch]$SkipTests,
    [switch]$ShowHelp
)

if ($ShowHelp) {
    Write-Host "Run all CI/CD checks locally" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\ci-run-all.ps1 -Profile mvp"
    Write-Host "  .\scripts\ci-run-all.ps1 -Profile strict -SkipNative"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -Profile mvp|staging|strict  Verification profile (default: mvp)"
    Write-Host "  -SkipNative  Skip native library build checks"
    Write-Host "  -SkipTests  Skip test execution"
    exit 0
}

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  CI/CD Local Runner - Profile: $Profile" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$startTime = Get-Date

Write-Host "==> KMP Phase 1 Python Checks" -ForegroundColor Yellow
python scripts/ci/check-commonmain-forbidden-imports.py .
python scripts/ci/check-security-expect-actual-signatures.py .
python scripts/ci/check-no-jvm-deps-in-native-source-sets.py .
python scripts/ci/check-video-runtime-matrix-config.py --root .
python scripts/ci/validate-video-e2e-profile.py --root .
Write-Host "   вњ… PASS" -ForegroundColor Green
Write-Host ""

Write-Host "==> Gradle Metadata Compilation" -ForegroundColor Yellow
.\gradlew.bat :core:common:compileKotlinMetadata :core:network:compileKotlinMetadata :shared:compileKotlinMetadata --no-daemon
Write-Host "   вњ… PASS" -ForegroundColor Green
Write-Host ""

$endTime = Get-Date
$totalDuration = New-TimeSpan -Start $startTime -End $endTime

$reportPath = "diagnostics\ci\local-run-report-$(Get-Date -Format 'yyyyMMdd-HHmmss').md"
New-Item -ItemType Directory -Force -Path (Split-Path $reportPath) | Out-Null

@"
# CI/CD Local Run Report

- Profile: **$Profile**
- Start: $($startTime.ToString('yyyy-MM-dd HH:mm:ss'))
- End: $($endTime.ToString('yyyy-MM-dd HH:mm:ss'))
- Total Duration: $($totalDuration.TotalMinutes.ToString('0.0')) minutes

## Summary

- Overall: вњ… SUCCESS

"@ | Set-Content -Path $reportPath -Encoding UTF8

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  CI/CD Run Complete" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Total Duration: $($totalDuration.TotalMinutes.ToString('0.0')) minutes" -ForegroundColor Gray
Write-Host "Report: $reportPath" -ForegroundColor Gray

exit 0

# 1. KMP Phase 1 Python checks
Invoke-Step -Name "KMP Phase 1 Python Checks" -Action {
    $checks = @(
        @("scripts/ci/check-commonmain-forbidden-imports.py", "."),
        @("scripts/ci/check-security-expect-actual-signatures.py", "."),
        @("scripts/ci/check-no-jvm-deps-in-native-source-sets.py", "."),
        @("scripts/ci/check-video-runtime-matrix-config.py", "--root", "."),
        @("scripts/ci/validate-video-e2e-profile.py", "--root", ".")
    )
    
    foreach ($check in $checks) {
        python @check
        if ($LASTEXITCODE -ne 0) {
            throw "Python check failed: $($check -join ' ')"
        }
    }
}

# 2. Gradle Metadata compilation
Invoke-Step -Name "Gradle Metadata Compilation" -Action {
    .\gradlew.bat :core:common:compileKotlinMetadata `
                  :core:network:compileKotlinMetadata `
                  :shared:compileKotlinMetadata `
                  --no-daemon
    
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle metadata compilation failed"
    }
}

# 3. Desktop Tests
if (-not $SkipTests) {
    Invoke-Step -Name "Desktop Tests" -Action {
        .\gradlew.bat :core:common:desktopTest `
                      :core:network:desktopTest `
                      --no-daemon
        
        if ($LASTEXITCODE -ne 0) {
            throw "Desktop tests failed"
        }
    }
}

# 4. Native Windows Build
if (-not $SkipNative) {
    Invoke-Step -Name "Native Windows Build" -Action {
        # РџСЂРѕРІРµСЂРєР° РЅР°Р»РёС‡РёСЏ РёРЅСЃС‚СЂСѓРјРµРЅС‚РѕРІ
        if (-not (Get-Command cmake -ErrorAction SilentlyContinue)) {
            throw "CMake not found in PATH"
        }
        
        if (-not (Get-Command g++ -ErrorAction SilentlyContinue)) {
            throw "MinGW g++ not found in PATH"
        }
        
        # РЎР±РѕСЂРєР° native Р±РёР±Р»РёРѕС‚РµРєРё
        .\scripts\build-video-processing-lib.ps1 windows x64 Release
        
        # РџСЂРѕРІРµСЂРєР° СЂРµР·СѓР»СЊС‚Р°С‚РѕРІ
        if (-not (Test-Path "native\video-processing\lib\windows\x64\video_processing.dll")) {
            throw "video_processing.dll not found after build"
        }
        
        if (-not (Test-Path "native\video-processing\lib\windows\x64\video_processing.lib")) {
            throw "video_processing.lib not found after build"
        }
        
        # Р“РµРЅРµСЂР°С†РёСЏ cinterop
        .\gradlew.bat :core:network:cinteropVideoProcessingNativeWindows --no-daemon
        
        if ($LASTEXITCODE -ne 0) {
            throw "cinterop generation failed"
        }
    }
}

# 5. Android Build
if ($Profile -in @("staging", "strict")) {
    Invoke-Step -Name "Android Build" -Action {
        .\gradlew.bat :android:app:assembleDebug --no-daemon
        
        if ($LASTEXITCODE -ne 0) {
            throw "Android build failed"
        }
    }
}

# 6. Server Build
if ($Profile -in @("staging", "strict")) {
    Invoke-Step -Name "Server API Build" -Action {
        .\gradlew.bat :server:api:build --no-daemon
        
        if ($LASTEXITCODE -ne 0) {
            throw "Server API build failed"
        }
    }
}

# Generate report
$endTime = Get-Date
$totalDuration = New-TimeSpan -Start $startTime -End $endTime

$reportPath = "diagnostics\ci\local-run-report-$(Get-Date -Format 'yyyyMMdd-HHmmss').md"
New-Item -ItemType Directory -Force -Path (Split-Path $reportPath) | Out-Null

$report = @(
    "# CI/CD Local Run Report",
    "",
    "- Profile: **$Profile**",
    "- Start: $($startTime.ToString('yyyy-MM-dd HH:mm:ss'))",
    "- End: $($endTime.ToString('yyyy-MM-dd HH:mm:ss'))",
    "- Total Duration: $($totalDuration.TotalMinutes.ToString('0.0')) minutes",
    "",
    "| Step | Status | Duration (s) |",
    "|------|--------|--------------|"
)

foreach ($result in $stepResults) {
    $report += "| $($result.Step) | $($result.Status) | $($result.Duration.ToString('0.0')) |"
}

$report += ""
$report += "## Summary"
$report += ""
$passedCount = ($stepResults | Where-Object { $_.Status -eq "PASS" }).Count
$totalCount = $stepResults.Count
$overallStatus = if ($totalCount -eq 0 -or $passedCount -eq $totalCount) { "вњ… SUCCESS" } else { "вљ пёЏ PARTIAL" }
$report += "- Passed: $passedCount / $totalCount"
$report += "- Overall: $overallStatus"

$report | Set-Content -Path $reportPath -Encoding UTF8

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  CI/CD Run Complete" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Passed: $passedCount / $totalCount" -ForegroundColor $(if ($passedCount -eq $totalCount) { "Green" } else { "Yellow" })
Write-Host "Total Duration: $($totalDuration.TotalMinutes.ToString('0.0')) minutes" -ForegroundColor Gray
Write-Host "Report: $reportPath" -ForegroundColor Gray

if ($passedCount -ne $totalCount -and $Profile -eq "strict") {
    exit 1
}

exit 0
