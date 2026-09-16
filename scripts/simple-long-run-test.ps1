# Simple Long-Run Test
# Базовое тестирование стабильности

param(
    [int]$DurationSeconds = 300,
    [string]$CameraName = "Emulator_AAC"
)

Write-Host "Starting simple long-run test..." -ForegroundColor Green
Write-Host "Duration: $DurationSeconds seconds"
Write-Host "Camera: $CameraName"
Write-Host ""

$startTime = Get-Date
$endTime = $startTime.AddSeconds($DurationSeconds)
$iteration = 0
$successes = 0
$failures = 0

try {
    while ((Get-Date) -lt $endTime) {
        $iteration++
        $elapsed = (New-TimeSpan -Start $startTime -End (Get-Date)).TotalSeconds
        $progress = [math]::Round(($elapsed / $DurationSeconds) * 100, 1)
        
        # Простая проверка подключения
        $tcpTest = Test-NetConnection -ComputerName "127.0.0.1" -Port 8554 -WarningAction SilentlyContinue -InformationLevel Quiet
        
        if ($tcpTest) {
            $successes++
            Write-Host "✓ Iteration $iteration ($([math]::Round($progress, 1))%)" -ForegroundColor Green
        } else {
            $failures++
            Write-Host "✗ Iteration $iteration ($([math]::Round($progress, 1))%)" -ForegroundColor Red
        }
        
        Start-Sleep -Seconds 1
    }
    
    Write-Host ""
    Write-Host "Test completed!" -ForegroundColor Green
    Write-Host "Successes: $successes"
    Write-Host "Failures: $failures"
    Write-Host "Success rate: $([math]::Round(($successes / $iteration) * 100, 2))%"
    
} catch {
    Write-Host "Test interrupted: $($_.Exception.Message)" -ForegroundColor Yellow
}
