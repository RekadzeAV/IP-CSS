# Run VideoDecoder JVM demo tests (simulator; no real cameras).
# Usage: .\scripts\run-demo-tests.ps1

param(
    [switch]$ShowHelp
)

if ($ShowHelp) {
    Write-Host "Run Gradle VideoDecoderDemoTest (simulator-based)."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\run-demo-tests.ps1 -ShowHelp"
    Write-Host "  .\scripts\run-demo-tests.ps1"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  Non-zero  Propagated from gradlew.bat"
    exit 0
}

Write-Host "=== VideoDecoder Demo Tests ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "These tests use a simulator and don't require real cameras" -ForegroundColor Yellow
Write-Host ""

# Р—Р°РїСѓСЃРє РґРµРјРѕ-С‚РµСЃС‚РѕРІ
Write-Host "Running demo tests..." -ForegroundColor Green
& .\gradlew.bat :core:network:jvmTest --tests "com.company.ipcamera.core.network.video.VideoDecoderDemoTest" --info

Write-Host ""
Write-Host "=== Demo Tests Complete ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "To run tests with real cameras:" -ForegroundColor Yellow
Write-Host "  1. Configure camera URLs in .test-env"
Write-Host "  2. Run: . .\scripts\setup-test-environment.ps1"
Write-Host "  3. Run: .\scripts\test-video-decoder.ps1"
