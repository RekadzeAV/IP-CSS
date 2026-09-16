# Ожидание завершения сборки и автоматическое продолжение

param(
    [int]$MaxWaitMinutes = 60,
    [int]$CheckIntervalSeconds = 300
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  ОЖИДАНИЕ СБОРКИ И АВТОМАТИЧЕСКОЕ ПРОДОЛЖЕНИЕ" -ForegroundColor Cyan
Write-Host "  Максимум ожидания: $MaxWaitMinutes минут" -ForegroundColor Cyan
Write-Host "  Интервал проверки: $($CheckIntervalSeconds) секунд" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$startTime = Get-Date
$maxWaitSeconds = $MaxWaitMinutes * 60

while ((New-TimeSpan -Start $startTime).TotalSeconds -lt $maxWaitSeconds) {
    Write-Host "[$((New-TimeSpan -Start $startTime).TotalMinutes.ToString('0')) мин] Проверка статуса..." -ForegroundColor Gray
    
    $javaProcesses = Get-Process java -ErrorAction SilentlyContinue
    
    if ($javaProcesses.Count -eq 0) {
        Write-Host "`n✅ Сборка завершена!" -ForegroundColor Green
        
        if (Test-Path "build") {
            Write-Host "Папка build создана" -ForegroundColor Green
            $buildDirs = Get-ChildItem "build" -Directory
            Write-Host "Созданные директории: $($buildDirs.Count)" -ForegroundColor Gray
            $buildDirs | Select-Object Name | Format-Table
            
            Write-Host "`n========== ЗАПУСК КОНТРОЛЛЕРА ВЫПОЛНЕНИЯ ==========" -ForegroundColor Cyan
            & ".\scripts\phase1-automation-controller.ps1"
            exit $LASTEXITCODE
        } else {
            Write-Host "❌ Сборка завершена, но папка build не создана" -ForegroundColor Red
            Write-Host "Проверка логов..." -ForegroundColor Yellow
            
            $logs = Get-ChildItem -Recurse -Filter "*.log" -ErrorAction SilentlyContinue | Select-Object -First 5
            if ($logs.Count -gt 0) {
                $logs | Select-Object FullName | Format-Table
            }
            
            exit 1
        }
    } else {
        $totalCPU = ($javaProcesses | Measure-Object -Property CPU -Sum).Sum
        $totalMem = ($javaProcesses | Measure-Object -Property WorkingSet -Sum).Sum
        
        Write-Host "  Процессов Java: $($javaProcesses.Count)" -ForegroundColor Yellow
        Write-Host "  CPU: $([math]::Round($totalCPU, 1)) сек" -ForegroundColor Gray
        Write-Host "  Память: $([math]::Round($totalMem/1MB, 0)) MB" -ForegroundColor Gray
        
        Start-Sleep -Seconds $CheckIntervalSeconds
    }
}

Write-Host "`n❌ Превышено максимальное время ожидания ($MaxWaitMinutes минут)" -ForegroundColor Red
exit 1
