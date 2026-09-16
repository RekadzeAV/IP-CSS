# РЎРєСЂРёРїС‚ РґР»СЏ РјРѕРЅРёС‚РѕСЂРёРЅРіР° РїСЂРѕРёР·РІРѕРґРёС‚РµР»СЊРЅРѕСЃС‚Рё С‚РµСЃС‚РѕРІ
# РСЃРїРѕР»СЊР·РѕРІР°РЅРёРµ: .\scripts\monitor-test-performance.ps1 [--duration <seconds>]

param(
    [int]$Duration = 60,
    [switch]$Help = $false,
    [switch]$ShowHelp
)

$ErrorActionPreference = "Stop"

if ($Help -or $ShowHelp) {
    Write-Host "Sample CPU/memory while native video_processing_tests.exe runs (--integration); writes CSV in test/build."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\monitor-test-performance.ps1 -ShowHelp"
    Write-Host "  .\scripts\monitor-test-performance.ps1 [-Duration <sec>]"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -Duration, -d  Seconds to poll (default 60)"
    Write-Host "  -Help, -h      Show help"
    Write-Host "  -ShowHelp      Same as -Help"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Monitoring finished (process may be stopped)"
    Write-Host "  1  Test executable missing"
    exit 0
}

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$TestBuildDir = Join-Path $ProjectRoot "native\video-processing\test\build"
$TestExecutable = Join-Path $TestBuildDir "video_processing_tests.exe"

if (-not (Test-Path $TestExecutable)) {
    Write-Host "[FAIL] Test executable not found: $TestExecutable" -ForegroundColor Red
    Write-Host "   Please build tests first" -ForegroundColor Yellow
    exit 1
}

Write-Host "=== Performance Monitoring ===" -ForegroundColor Cyan
Write-Host "Duration: $Duration seconds" -ForegroundColor Yellow
Write-Host ""

# РЎРѕР·РґР°РЅРёРµ С„Р°Р№Р»Р° РґР»СЏ РјРµС‚СЂРёРє
$MetricsFile = Join-Path $TestBuildDir "performance_metrics_$(Get-Date -Format 'yyyyMMdd_HHmmss').csv"
"Timestamp,CPU%,MemoryMB,FPS,FramesDecoded" | Out-File -FilePath $MetricsFile -Encoding UTF8

Write-Host "Starting performance monitoring..." -ForegroundColor Yellow
Write-Host "Metrics will be saved to: $MetricsFile" -ForegroundColor Gray
Write-Host ""

$StartTime = Get-Date
$EndTime = $StartTime.AddSeconds($Duration)

# Р—Р°РїСѓСЃРє С‚РµСЃС‚РѕРІ РІ С„РѕРЅРµ
$TestProcess = Start-Process -FilePath $TestExecutable -ArgumentList "--integration" -PassThru -NoNewWindow

try {
    $FrameCount = 0
    $LastFrameCount = 0

    while ((Get-Date) -lt $EndTime) {
        $CurrentTime = Get-Date
        $Elapsed = ($CurrentTime - $StartTime).TotalSeconds

        # РџРѕР»СѓС‡РµРЅРёРµ РјРµС‚СЂРёРє РїСЂРѕС†РµСЃСЃР°
        $Process = Get-Process -Id $TestProcess.Id -ErrorAction SilentlyContinue
        if ($Process) {
            $CpuPercent = $Process.CPU
            $MemoryMB = [math]::Round($Process.WorkingSet64 / 1MB, 2)

            # Р Р°СЃС‡РµС‚ FPS (СѓРїСЂРѕС‰РµРЅРЅС‹Р№)
            $FPS = if ($Elapsed -gt 0) { [math]::Round($FrameCount / $Elapsed, 2) } else { 0 }

            # Р—Р°РїРёСЃСЊ РјРµС‚СЂРёРє
            "$Elapsed,$CpuPercent,$MemoryMB,$FPS,$FrameCount" | Out-File -FilePath $MetricsFile -Append -Encoding UTF8

            Write-Host "[$([math]::Round($Elapsed, 1))s] CPU: $CpuPercent% | Memory: $MemoryMB MB | FPS: $FPS | Frames: $FrameCount" -ForegroundColor Gray
        }

        Start-Sleep -Seconds 1
    }
} finally {
    # РћСЃС‚Р°РЅРѕРІРєР° РїСЂРѕС†РµСЃСЃР°
    if (-not $TestProcess.HasExited) {
        Stop-Process -Id $TestProcess.Id -Force -ErrorAction SilentlyContinue
    }
}

Write-Host ""
Write-Host "=== Monitoring Complete ===" -ForegroundColor Green
Write-Host "Metrics saved to: $MetricsFile" -ForegroundColor Cyan
Write-Host ""
Write-Host "To analyze metrics, use:" -ForegroundColor Yellow
Write-Host "  Import-Csv $MetricsFile | Format-Table" -ForegroundColor White
