# Compile :core:network:compileKotlinDesktop only (lighter path before running VideoDecoderDemoTest in IDE).
# Usage: .\scripts\run-demo-tests-simple.ps1

param(
    [switch]$ShowHelp
)

if ($ShowHelp) {
    Write-Host "Compile desktop Kotlin for core:network (no full test run). Use IDE or run-demo-tests.ps1 for JVM tests."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\run-demo-tests-simple.ps1 -ShowHelp"
    Write-Host "  .\scripts\run-demo-tests-simple.ps1"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  compileKotlinDesktop succeeded"
    Write-Host "  1  Gradle compile failed"
    exit 0
}

Write-Host "=== VideoDecoder Demo Tests (Simple) ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Note: These tests require JavaCV/FFmpeg dependencies" -ForegroundColor Yellow
Write-Host ""

# РџРѕРїС‹С‚РєР° РєРѕРјРїРёР»СЏС†РёРё С‚РµСЃС‚РѕРІ
Write-Host "Compiling tests..." -ForegroundColor Green
& .\gradlew.bat :core:network:compileKotlinDesktop 2>&1 | Out-Null

if ($LASTEXITCODE -ne 0) {
    Write-Host "Compilation failed. Trying alternative approach..." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "To run tests manually:" -ForegroundColor Cyan
    Write-Host "  1. Open the project in IntelliJ IDEA" -ForegroundColor White
    Write-Host "  2. Navigate to: core/network/src/jvmTest/.../VideoDecoderDemoTest.kt" -ForegroundColor White
    Write-Host "  3. Right-click and select 'Run VideoDecoderDemoTest'" -ForegroundColor White
    Write-Host ""
    Write-Host "Or configure test environment for real cameras:" -ForegroundColor Cyan
    Write-Host "  . .\scripts\setup-test-environment.ps1" -ForegroundColor White
    exit 1
}

Write-Host "Compilation successful!" -ForegroundColor Green
Write-Host ""
Write-Host "Note: For full test execution, use IntelliJ IDEA or configure JUnit runner" -ForegroundColor Yellow
