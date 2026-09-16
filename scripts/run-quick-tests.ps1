# Quick RTSP Tests Runner (PowerShell)
# Runs only JVM desktop tests without native compilation
# Usage: .\run-quick-tests.ps1 [-Verbose]

param(
    [switch]$Verbose
)

# Colors
function Write-Color {
    param([string]$Text, [string]$Color)
    $original = $Host.UI.RawUI.ForegroundColor
    $Host.UI.RawUI.ForegroundColor = $Color
    Write-Host $Text
    $Host.UI.RawUI.ForegroundColor = $original
}

Write-Color "==========================================" -Color Cyan
Write-Color "IP-CSS Quick Test Runner (PowerShell)" -Color Cyan
Write-Color "==========================================" -Color Cyan
Write-Host ""
Write-Host "Running JVM desktop tests only (skipping native compilation)"
Write-Host ""

# Check if native library exists
$LibPath = "native/video-processing/lib/windows/x64/video_processing.dll"
if (-not (Test-Path $LibPath)) {
    Write-Color "⚠ Native library not found at: $LibPath" -Color Yellow
    Write-Color "Building native library first..." -Color Yellow
    
    if (Test-Path "native/video-processing/build.ps1") {
        & ".\native\video-processing\build.ps1"
    }
}

# Gradle command with optimization flags
$GradleArgs = @(
    ":core:network:desktopTest",
    "-Dipcss.skipNativeTargets=true",
    "--parallel",
    "--no-daemon",
    "--max-workers=4"
)

if ($Verbose) {
    $GradleArgs += "--info"
    $GradleArgs += "--stacktrace"
}

Write-Color "Executing: ./gradlew $($GradleArgs -join ' ')" -Color Green
Write-Host ""

# Run tests
$Result = Start-Process -FilePath ".\gradlew" -ArgumentList $GradleArgs -Wait -PassThru

# Check results
if ($Result.ExitCode -eq 0) {
    Write-Host ""
    Write-Color "==========================================" -Color Green
    Write-Color "✓ All tests passed!" -Color Green
    Write-Color "==========================================" -Color Green
    exit 0
} else {
    Write-Host ""
    Write-Color "==========================================" -Color Red
    Write-Color "✗ Some tests failed" -Color Red
    Write-Color "==========================================" -Color Red
    exit 1
}
