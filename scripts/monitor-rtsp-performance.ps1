#!/usr/bin/env pwsh
# Monitor RTSP Performance During Long-Run Tests
# Собирает метрики производительности в реальном времени

param(
    [string]$ProcessName = "java",
    [int]$IntervalSeconds = 60,
    [string]$OutputFile = "logs/rtsp-performance-metrics.json",
    [int]$DurationMinutes = 0  # 0 = бесконечно
)

$LogDirectory = Split-Path -Parent $OutputFile
if (-not (Test-Path $LogDirectory)) {
    New-Item -ItemType Directory -Path $LogDirectory -Force | Out-Null
}

$metrics = @{
    StartTime = (Get-Date).ToString('o')
    Samples = @()
    Alerts = @()
}

Write-Host "📊 RTSP Performance Monitor" -ForegroundColor Cyan
Write-Host "Process: $ProcessName"
Write-Host "Interval: $IntervalSeconds seconds"
Write-Host "Output: $OutputFile"
if ($DurationMinutes -gt 0) {
    Write-Host "Duration: $DurationMinutes minutes"
}
Write-Host "Press Ctrl+C to stop" -ForegroundColor Yellow
Write-Host ""

$startTime = Get-Date
$sampleCount = 0

try {
    while ($true) {
        if ($DurationMinutes -gt 0) {
            $elapsed = (Get-Date) - $startTime
            if ($elapsed.TotalMinutes -ge $DurationMinutes) {
                Write-Host "`n⏹️  Duration limit reached" -ForegroundColor Yellow
                break
            }
        }

        # Сбор метрик процесса
        $processes = Get-Process | Where-Object { $_.ProcessName -like "*$ProcessName*" }
        
        foreach ($process in $processes) {
            $sample = @{
                Timestamp = (Get-Date).ToString('o')
                ProcessId = $process.Id
                ProcessName = $process.ProcessName
                WorkingSetMB = [math]::Round($process.WorkingSet / 1MB, 2)
                PrivateMemoryMB = [math]::Round($process.PrivateMemorySize / 1MB, 2)
                CPUPercent = [math]::Round($process.CPU, 2)
                ThreadCount = $process.ThreadCount
                HandleCount = $process.HandleCount
            }
            
            $metrics.Samples += $sample
            $sampleCount++
            
            Write-Host "[$($sample.Timestamp)] PID $($process.Id): Memory=$($sample.WorkingSetMB)MB, CPU=$($sample.CPUPercent)%, Threads=$($sample.ThreadCount)"
            
            # Проверка на memory leak (>50% growth)
            if ($metrics.Samples.Count -gt 5) {
                $recentSamples = $metrics.Samples[-5..-1]
                $memoryValues = $recentSamples | ForEach-Object { $_.WorkingSetMB }
                $memoryGrowth = (($memoryValues[-1] - $memoryValues[0]) / $memoryValues[0]) * 100
                
                if ($memoryGrowth -gt 50) {
                    $alert = @{
                        Timestamp = (Get-Date).ToString('o')
                        Type = "MemoryLeak"
                        Severity = "HIGH"
                        Message = "Memory growth detected: +$([math]::Round($memoryGrowth, 1))% over 5 samples"
                        CurrentMemoryMB = $memoryValues[-1]
                    }
                    $metrics.Alerts += $alert
                    Write-Host "⚠️  ALERT: $($alert.Message)" -ForegroundColor Red
                }
                
                # Проверка на high CPU
                if ($sample.CPUPercent -gt 80) {
                    $alert = @{
                        Timestamp = (Get-Date).ToString('o')
                        Type = "HighCPU"
                        Severity = "MEDIUM"
                        Message = "High CPU usage detected: $($sample.CPUPercent)%"
                        CurrentCPU = $sample.CPUPercent
                    }
                    $metrics.Alerts += $alert
                    Write-Host "⚠️  ALERT: $($alert.Message)" -ForegroundColor Yellow
                }
            }
        }
        
        # Сохранение метрик каждые 10 минут
        if ($sampleCount % 10 -eq 0) {
            $metrics | ConvertTo-Json -Depth 10 | Out-File -FilePath $OutputFile -Encoding UTF8
        }
        
        Start-Sleep -Seconds $IntervalSeconds
    }
}
catch {
    Write-Host "`n⚠️  Monitor stopped: $($_.Exception.Message)" -ForegroundColor Yellow
}
finally {
    # Финальное сохранение
    $metrics.EndTime = (Get-Date).ToString('o')
    $metrics.TotalSamples = $metrics.Samples.Count
    $metrics.TotalAlerts = $metrics.Alerts.Count
    
    $metrics | ConvertTo-Json -Depth 10 | Out-File -FilePath $OutputFile -Encoding UTF8
    
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  Performance Monitor Summary" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "Total samples: $($metrics.TotalSamples)" -ForegroundColor Green
    Write-Host "Total alerts: $($metrics.TotalAlerts)" -ForegroundColor $(if ($metrics.TotalAlerts -eq 0) { "Green" } else { "Red" })
    Write-Host "Output file: $OutputFile" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Cyan
}
