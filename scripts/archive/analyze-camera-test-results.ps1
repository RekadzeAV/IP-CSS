# РЎРєСЂРёРїС‚ РґР»СЏ Р°РЅР°Р»РёР·Р° СЂРµР·СѓР»СЊС‚Р°С‚РѕРІ С‚РµСЃС‚РёСЂРѕРІР°РЅРёСЏ РЅР° СЂРµР°Р»СЊРЅС‹С… РєР°РјРµСЂР°С…
# РСЃРїРѕР»СЊР·РѕРІР°РЅРёРµ: .\scripts\analyze-camera-test-results.ps1 -ResultsDir <dir>

param(
    [string]$ResultsDir = "test-results",
    [switch]$GenerateReport = $true,
    [switch]$Help = $false,
    [switch]$ShowHelp
)

$ErrorActionPreference = "Stop"

if ($Help -or $ShowHelp) {
    Write-Host "Summarize *_results.json under a test-results folder; optional HTML report."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\analyze-camera-test-results.ps1 -ShowHelp"
    Write-Host '  .\scripts\analyze-camera-test-results.ps1 [-ResultsDir <relative-or-name>] [-GenerateReport:$false]'
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -ResultsDir, -d   Folder under repo root (default test-results)"
    Write-Host "  -GenerateReport   Write analysis_report.html when true (default)"
    Write-Host "  -Help, -h         Show help"
    Write-Host "  -ShowHelp         Same as -Help"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Results found and processed"
    Write-Host "  1  Missing directory or no *_results.json"
    exit 0
}

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$ResultsPath = Join-Path $ProjectRoot $ResultsDir

if (-not (Test-Path $ResultsPath)) {
    Write-Host "[FAIL] Results directory not found: $ResultsPath" -ForegroundColor Red
    exit 1
}

Write-Host "=== Analyzing Camera Test Results ===" -ForegroundColor Cyan
Write-Host ""

# РџРѕРёСЃРє JSON С„Р°Р№Р»РѕРІ СЃ СЂРµР·СѓР»СЊС‚Р°С‚Р°РјРё
$ResultFiles = Get-ChildItem -Path $ResultsPath -Filter "*_results.json" -Recurse

if ($ResultFiles.Count -eq 0) {
    Write-Host "[FAIL] No result files found in $ResultsPath" -ForegroundColor Red
    exit 1
}

Write-Host "Found $($ResultFiles.Count) result file(s)" -ForegroundColor Green
Write-Host ""

$Summary = @{
    totalCameras = $ResultFiles.Count
    totalFrames = 0
    decodedFrames = 0
    failedFrames = 0
    averageFPS = 0.0
    averageDecodeTime = 0.0
    cameras = @()
}

foreach ($file in $ResultFiles) {
    Write-Host "Analyzing: $($file.Name)" -ForegroundColor Yellow

    $result = Get-Content $file.FullName | ConvertFrom-Json

    $cameraSummary = @{
        id = $result.cameraId
        name = $result.cameraName
        totalFrames = $result.metrics.totalFrames
        decodedFrames = $result.metrics.decodedFrames
        failedFrames = $result.metrics.failedFrames
        averageFPS = $result.metrics.averageFPS
        averageDecodeTime = $result.metrics.averageDecodeTime
        errors = $result.errors.Count
    }

    $Summary.cameras += $cameraSummary
    $Summary.totalFrames += $result.metrics.totalFrames
    $Summary.decodedFrames += $result.metrics.decodedFrames
    $Summary.failedFrames += $result.metrics.failedFrames

    Write-Host "  Total frames: $($result.metrics.totalFrames)" -ForegroundColor Gray
    Write-Host "  Decoded: $($result.metrics.decodedFrames)" -ForegroundColor Green
    Write-Host "  Failed: $($result.metrics.failedFrames)" -ForegroundColor $(if ($result.metrics.failedFrames -gt 0) { "Red" } else { "Gray" })
    Write-Host "  Average FPS: $($result.metrics.averageFPS)" -ForegroundColor Gray
    Write-Host "  Average decode time: $($result.metrics.averageDecodeTime)ms" -ForegroundColor Gray
    Write-Host ""
}

# Р Р°СЃС‡РµС‚ СЃСЂРµРґРЅРёС… Р·РЅР°С‡РµРЅРёР№
if ($Summary.cameras.Count -gt 0) {
    $Summary.averageFPS = ($Summary.cameras | Measure-Object -Property averageFPS -Average).Average
    $Summary.averageDecodeTime = ($Summary.cameras | Measure-Object -Property averageDecodeTime -Average).Average
}

# Р’С‹РІРѕРґ СЃРІРѕРґРєРё
Write-Host "=== Summary ===" -ForegroundColor Cyan
Write-Host "Total cameras tested: $($Summary.totalCameras)" -ForegroundColor White
Write-Host "Total frames: $($Summary.totalFrames)" -ForegroundColor White
Write-Host "Decoded frames: $($Summary.decodedFrames)" -ForegroundColor Green
Write-Host "Failed frames: $($Summary.failedFrames)" -ForegroundColor $(if ($Summary.failedFrames -gt 0) { "Red" } else { "Gray" })
Write-Host "Average FPS: $([math]::Round($Summary.averageFPS, 2))" -ForegroundColor White
Write-Host "Average decode time: $([math]::Round($Summary.averageDecodeTime, 2))ms" -ForegroundColor White
Write-Host ""

# Р“РµРЅРµСЂР°С†РёСЏ РѕС‚С‡РµС‚Р°
if ($GenerateReport) {
    $ReportFile = Join-Path $ResultsPath "analysis_report.html"

    $html = @"
<!DOCTYPE html>
<html>
<head>
    <title>Camera Test Results Analysis</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        h1 { color: #333; }
        table { border-collapse: collapse; width: 100%; margin: 20px 0; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #4CAF50; color: white; }
        tr:nth-child(even) { background-color: #f2f2f2; }
        .success { color: green; }
        .error { color: red; }
    </style>
</head>
<body>
    <h1>Camera Test Results Analysis</h1>
    <h2>Summary</h2>
    <ul>
        <li>Total cameras: $($Summary.totalCameras)</li>
        <li>Total frames: $($Summary.totalFrames)</li>
        <li>Decoded frames: $($Summary.decodedFrames)</li>
        <li>Failed frames: $($Summary.failedFrames)</li>
        <li>Average FPS: $([math]::Round($Summary.averageFPS, 2))</li>
        <li>Average decode time: $([math]::Round($Summary.averageDecodeTime, 2))ms</li>
    </ul>
    <h2>Per-Camera Results</h2>
    <table>
        <tr>
            <th>Camera ID</th>
            <th>Name</th>
            <th>Total Frames</th>
            <th>Decoded</th>
            <th>Failed</th>
            <th>FPS</th>
            <th>Decode Time (ms)</th>
            <th>Errors</th>
        </tr>
"@

    foreach ($camera in $Summary.cameras) {
        $html += @"
        <tr>
            <td>$($camera.id)</td>
            <td>$($camera.name)</td>
            <td>$($camera.totalFrames)</td>
            <td class="success">$($camera.decodedFrames)</td>
            <td class="error">$($camera.failedFrames)</td>
            <td>$([math]::Round($camera.averageFPS, 2))</td>
            <td>$([math]::Round($camera.averageDecodeTime, 2))</td>
            <td>$($camera.errors)</td>
        </tr>
"@
    }

    $html += @"
    </table>
    <p>Generated: $(Get-Date)</p>
</body>
</html>
"@

    Set-Content -Path $ReportFile -Value $html
    Write-Host "вњ… Report generated: $ReportFile" -ForegroundColor Green
}

Write-Host "=== Analysis Complete ===" -ForegroundColor Green
