# Lighthouse Performance Audit Script
# Запускает аудит производительности для веб-интерфейса

param(
    [switch]$Mobile,
    [switch]$CI,
    [string]$OutputDir = "./lighthouse-report"
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Lighthouse Performance Audit" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Проверка Node.js
Write-Host "Проверка Node.js..." -ForegroundColor Yellow
try {
    $nodeVersion = node --version
    Write-Host "Node.js: $nodeVersion" -ForegroundColor Green
} catch {
    Write-Host "Ошибка: Node.js не найден" -ForegroundColor Red
    exit 1
}

# Проверка установки lighthouse
Write-Host "Проверка Lighthouse..." -ForegroundColor Yellow
try {
    $lighthouseVersion = npx lighthouse --version
    Write-Host "Lighthouse: $lighthouseVersion" -ForegroundColor Green
} catch {
    Write-Host "Установка Lighthouse..." -ForegroundColor Yellow
    npx lighthouse --help | Out-Null
}

# Создание директории для отчётов
if (!(Test-Path $OutputDir)) {
    Write-Host "Создание директории отчётов..." -ForegroundColor Yellow
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}

# Сборка проекта
Write-Host ""
Write-Host "Сборка проекта..." -ForegroundColor Yellow
Set-Location "server/web"
npm run build
if ($LASTEXITCODE -ne 0) {
    Write-Host "Ошибка сборки проекта" -ForegroundColor Red
    exit 1
}

# Запуск production сервера в фоне
Write-Host ""
Write-Host "Запуск production сервера..." -ForegroundColor Yellow
$serverProcess = Start-Process -FilePath "node" -ArgumentList "server/web/.next/standalone/server.js" -PassThru -NoNewWindow
Write-Host "Сервер запущен с PID: $($serverProcess.Id)" -ForegroundColor Green

# Ожидание запуска сервера
Write-Host "Ожидание запуска сервера (10 секунд)..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# Определение параметров Lighthouse
$chromeFlags = "--headless --no-sandbox --disable-setuid-sandbox"
$onlyCategories = "--only-categories=performance,accessibility,best-practices,seo"

if ($Mobile) {
    Write-Host "Режим: Mobile" -ForegroundColor Cyan
    $preset = "--preset=perf"
    $formFactor = "--form-factor=mobile"
    $onlyCategories = "--only-categories=performance,accessibility"
} else {
    Write-Host "Режим: Desktop" -ForegroundColor Cyan
    $preset = ""
    $formFactor = ""
}

if ($CI) {
    Write-Host "Режим: CI (только JSON)" -ForegroundColor Cyan
    $outputFormat = "--output=json"
    $outputPath = "--output-path=./lighthouse-report/report.json"
    $onlyCategories = "--only-categories=performance,accessibility,best-practices"
} else {
    Write-Host "Режим: Full (HTML + JSON)" -ForegroundColor Cyan
    $outputFormat = "--output=html --output=json"
    $outputPath = "--output-path=./lighthouse-report/report.html"
}

# Запуск Lighthouse
Write-Host ""
Write-Host "Запуск Lighthouse..." -ForegroundColor Yellow
Write-Host "URL: http://localhost:3000" -ForegroundColor Cyan

$arguments = "http://localhost:3000 $preset $formFactor $chromeFlags $onlyCategories $outputFormat $outputPath"
Write-Host "Команда: npx lighthouse $arguments" -ForegroundColor Gray

npx lighthouse http://localhost:3000 `
    $preset `
    $formFactor `
    --chrome-flags="$chromeFlags" `
    $onlyCategories `
    $outputFormat `
    $outputPath

$LighthouseExitCode = $LASTEXITCODE

# Остановка сервера
Write-Host ""
Write-Host "Остановка сервера..." -ForegroundColor Yellow
Stop-Process -Id $serverProcess.Id -Force -ErrorAction SilentlyContinue
Write-Host "Сервер остановлен" -ForegroundColor Green

# Вывод результатов
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Результаты" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

if ($LighthouseExitCode -eq 0) {
    Write-Host "Lighthouse завершён успешно!" -ForegroundColor Green
    
    if ($CI) {
        Write-Host "JSON отчёт: $OutputDir/report.json" -ForegroundColor Green
        
        # Чтение и вывод основных метрик
        if (Test-Path "$OutputDir/report.json") {
            $report = Get-Content "$OutputDir/report.json" | ConvertFrom-Json
            $score = $report.categories.performance.score * 100
            Write-Host "Performance Score: $([math]::Round($score, 1))%" -ForegroundColor $(if ($score -ge 90) { "Green" } elseif ($score -ge 50) { "Yellow" } else { "Red" })
            
            $metrics = $report.audits | Where-Object { $_.id -in @('first-contentful-paint', 'largest-contentful-paint', 'total-blocking-time', 'cumulative-layout-shift', 'speed-index') }
            Write-Host ""
            Write-Host "Основные метрики:" -ForegroundColor Yellow
            $metrics | ForEach-Object {
                $value = [math]::Round($_.numericValue, 0)
                Write-Host "  $($_.id): $value ms" -ForegroundColor Gray
            }
        }
    } else {
        Write-Host "Отчёты сохранены в: $OutputDir" -ForegroundColor Green
        Write-Host "  - report.html (HTML)" -ForegroundColor Gray
        Write-Host "  - report.json (JSON)" -ForegroundColor Gray
        
        # Открытие HTML отчёта
        Write-Host ""
        Write-Host "Открытие HTML отчёта..." -ForegroundColor Yellow
        Start-Process "lighthouse-report/report.html"
    }
} else {
    Write-Host "Lighthouse завершён с ошибкой (код: $LighthouseExitCode)" -ForegroundColor Red
}

Write-Host ""
Write-Host "Готово!" -ForegroundColor Cyan
