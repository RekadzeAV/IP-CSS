# Запуск RTSP Long-Run тестирования с проверкой инфраструктуры
# Этот скрипт проверяет готовность системы и запрашивает подтверждение перед запуском длительных тестов

param(
    [string]$RtspUrl = "",
    [int]$DurationMinutes = 60,
    [switch]$FastMode,
    [switch]$WeekendMode,  # Явно указываем что это выходной день
    [switch]$ShowHelp
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir

if ($ShowHelp) {
    Write-Host "RTSP Long-Run Test Runner с проверкой инфраструктуры" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\run-rtsp-test-with-checks.ps1"
    Write-Host "  .\scripts\run-rtsp-test-with-checks.ps1 -RtspUrl 'rtsp://...' -DurationMinutes 120"
    Write-Host "  .\scripts\run-rtsp-test-with-checks.ps1 -FastMode"
    Write-Host "  .\scripts\run-rtsp-test-with-checks.ps1 -WeekendMode"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -RtspUrl         RTSP URL тестовой камеры (обязательно)"
    Write-Host "  -DurationMinutes Длительность теста в минутах (default: 60)"
    Write-Host "  -FastMode        Короткий тест 10 минут для проверки"
    Write-Host "  -WeekendMode     Явно указать что сегодня выходной (разрешает длительные тесты)"
    Write-Host ""
    Write-Host "Примечание:"
    Write-Host "  - Длительные тесты (>2 часов) автоматически разрешены только в выходные дни"
    Write-Host "  - Используйте -WeekendMode для явного указания выходного дня"
    Write-Host "  - Тест проверяет инфраструктуру перед запуском"
    exit 0
}

# Проверка текущего времени и дня недели
$currentTime = Get-Date
$currentDayOfWeek = $currentTime.DayOfWeek
$isWeekend = ($currentDayOfWeek -eq "Saturday") -or ($currentDayOfWeek -eq "Sunday")

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  RTSP Long-Run Test Runner с проверкой инфраструктуры" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Текущее время: $($currentTime.ToString('yyyy-MM-dd HH:mm:ss'))" -ForegroundColor DarkGray
Write-Host "День недели: $currentDayOfWeek" -ForegroundColor DarkGray
Write-Host ""

# Если DurationMinutes > 120 и не выходной, требуем явное подтверждение
$requiresWeekendConfirmation = ($DurationMinutes -gt 120) -and -not $isWeekend -and -not $WeekendMode

if ($requiresWeekendConfirmation) {
    Write-Host ""
    Write-Host "⚠️  ВНИМАНИЕ: Длительный тест ($DurationMinutes минут = $($DurationMinutes/60) часа)" -ForegroundColor Yellow
    Write-Host "     Сегодня НЕ выходной день ($currentDayOfWeek)"
    Write-Host ""
    Write-Host "     Длительные тесты требуют:"
    Write-Host "     - Свободных системных ресурсов"
    Write-Host "     - Стабильной работы инфраструктуры"
    Write-Host "     - Мониторинга в течение всего времени теста"
    Write-Host ""
    Write-Host "     Рекомендуется запускать длительные тесты в выходные дни."
    Write-Host ""
    
    $confirm = Read-Host "    Продолжить всё равно? (y/n)"
    if ($confirm -ne "y" -and $confirm -ne "Y") {
        Write-Host ""
        Write-Host "❌ Тест отменён пользователем" -ForegroundColor Yellow
        Write-Host "   Используйте -WeekendMode если сегодня выходной день" -ForegroundColor DarkGray
        exit 0
    }
    
    Write-Host ""
    Write-Host "✓ Подтверждение получено, продолжаем..." -ForegroundColor Green
}

# Если FastMode, автоматически сокращаем длительность
if ($FastMode) {
    $DurationMinutes = 10
    Write-Host "✓ FAST MODE: Длительность теста = 10 минут" -ForegroundColor Green
}

# Проверка RTSP URL
if ([string]::IsNullOrWhiteSpace($RtspUrl)) {
    Write-Host ""
    Write-Host "❌ RTSP URL не указан" -ForegroundColor Red
    Write-Host "   Используйте параметр -RtspUrl 'rtsp://...'" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  Шаг 1: Проверка инфраструктуры" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

$ProjectRoot = Split-Path -Parent $ScriptDir
$gradlew = Join-Path $ProjectRoot "gradlew.bat"
$dockerCompose = Join-Path $ProjectRoot "docker-compose.yml"

# Проверка наличия файлов
if (-not (Test-Path $gradlew)) {
    Write-Host "❌ gradlew.bat не найден: $gradlew" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path $dockerCompose)) {
    Write-Host "❌ docker-compose.yml не найден: $dockerCompose" -ForegroundColor Red
    exit 1
}

# Проверка Docker
Write-Host "Проверка Docker..." -ForegroundColor DarkGray
try {
    $dockerVersion = docker --version 2>&1
    Write-Host "  ✓ Docker установлен: $dockerVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Docker не установлен или не доступен" -ForegroundColor Red
    exit 1
}

# Проверка Docker Compose
Write-Host "Проверка Docker Compose..." -ForegroundColor DarkGray
try {
    $composeVersion = docker compose version 2>&1
    Write-Host "  ✓ Docker Compose установлен: $composeVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Docker Compose не установлен или не доступен" -ForegroundColor Red
    exit 1
}

# Проверка запущенных контейнеров
Write-Host "Проверка запущенных контейнеров..." -ForegroundColor DarkGray
$runningContainers = docker ps --format "{{.Names}}" 2>&1
$ surveillanceRunning = $runningContainers -contains "ip-camera-surveillance"
$postgresRunning = $runningContainers -contains "surveillance-postgres"
$redisRunning = $runningContainers -contains "surveillance-redis"

if ($surveillanceRunning) {
    Write-Host "  ✓ Контейнер surveillance запущен" -ForegroundColor Green
} else {
    Write-Host "  ⚠ Контейнер surveillance НЕ запущен" -ForegroundColor Yellow
    Write-Host "    Запуск инфраструктуры..." -ForegroundColor DarkGray
    
    # Проверка .env файла
    $envFile = Join-Path $ProjectRoot ".env"
    if (-not (Test-Path $envFile)) {
        Write-Host "    ❌ .env файл не найден" -ForegroundColor Red
        Write-Host "    Скопируйте .env.example в .env и настройте переменные" -ForegroundColor Yellow
        exit 1
    }
    
    # Запуск docker-compose
    Push-Location $ProjectRoot
    try {
        docker compose up -d
        if ($LASTEXITCODE -ne 0) {
            Write-Host "    ❌ Ошибка при запуске контейнеров" -ForegroundColor Red
            exit 1
        }
        Write-Host "    ✓ Контейнеры запущены" -ForegroundColor Green
        Start-Sleep -Seconds 10  # Ожидание запуска сервисов
    } finally {
        Pop-Location
    }
}

if ($postgresRunning) {
    Write-Host "  ✓ PostgreSQL запущен" -ForegroundColor Green
} else {
    Write-Host "  ⚠ PostgreSQL статус неизвестен" -ForegroundColor Yellow
}

if ($redisRunning) {
    Write-Host "  ✓ Redis запущен" -ForegroundColor Green
} else {
    Write-Host "  ⚠ Redis статус неизвестен" -ForegroundColor Yellow
}

# Проверка доступности RTSP камеры (опционально)
Write-Host ""
Write-Host "Проверка доступности RTSP камеры..." -ForegroundColor DarkGray
try {
    # Простая проверка - пробуем подключиться на 5 секунд
    $testUrl = $RtspUrl
    Write-Host "  Тестовое подключение к: $testUrl" -ForegroundColor DarkGray
    
    # Используем timeout для проверки
    $timeout = 5
    $startTime = Get-Date
    
    # Пытаемся открыть TCP соединение на порт RTSP (554)
    $rtspPort = 554
    if ($testUrl -match ":(\d+)/") {
        $rtspPort = [int]$Matches[1]
    }
    
    $ipAddress = $testUrl -replace "rtsp://", "" -replace ":.*", ""
    
    $tcpClient = New-Object System.Net.Sockets.TcpClient
    $asyncResult = $tcpClient.BeginConnect($ipAddress, $rtspPort, $null, $null)
    $wait = $asyncResult.AsyncWaitHandle.WaitOne($timeout * 1000)
    
    if ($wait) {
        $tcpClient.EndConnect($asyncResult)
        $tcpClient.Close()
        Write-Host "  ✓ RTSP камера доступна на порту $rtspPort" -ForegroundColor Green
    } else {
        $tcpClient.Close()
        Write-Host "  ⚠ RTSP камера не отвечает (timeout)" -ForegroundColor Yellow
        Write-Host "    Убедитесь что камера включена и доступна в сети" -ForegroundColor DarkGray
    }
} catch {
    Write-Host "  ⚠ Не удалось проверить RTSP камеру: $($_.Exception.Message)" -ForegroundColor Yellow
    Write-Host "    Продолжаем с предупреждением..." -ForegroundColor DarkGray
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  Шаг 2: Параметры теста" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "RTSP URL:         $RtspUrl" -ForegroundColor White
Write-Host "Длительность:     $DurationMinutes минут" -ForegroundColor White
Write-Host "Fast Mode:        $($FastMode.IsPresent)" -ForegroundColor White
Write-Host "Выходной режим:   $($WeekendMode.IsPresent -or $isWeekend)" -ForegroundColor White
Write-Host "Дата начала:      $($currentTime.ToString('yyyy-MM-dd HH:mm:ss'))" -ForegroundColor White

$expectedEndTime = $currentTime.AddMinutes($DurationMinutes)
Write-Host "Ожидаемое окончание: $($expectedEndTime.ToString('yyyy-MM-dd HH:mm:ss'))" -ForegroundColor White

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  Шаг 3: Запуск теста" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# Создание директории для отчётов
$OutputDir = Join-Path $ProjectRoot "docs\reports\rtsp-stability-tests"
if (!(Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
    Write-Host "✓ Директория для отчётов создана: $OutputDir" -ForegroundColor Green
}

# Запуск теста
$env:RTSP_TEST_URL = $RtspUrl
$env:RTSP_TEST_DURATION_MINUTES = $DurationMinutes
$env:RTSP_TEST_OUTPUT_DIR = $OutputDir

Write-Host "Запуск Gradle теста..." -ForegroundColor DarkGray
Write-Host "Команда: gradlew :shared:desktopTest --tests '*RtspLongRunStabilityTest*'" -ForegroundColor DarkGray
Write-Host ""

& $gradlew ":shared:desktopTest" "--tests" "*RtspLongRunStabilityTest*" "--info" --no-daemon

$exitCode = $LASTEXITCODE

# Очищаем environment переменные
Remove-Item "env:RTSP_TEST_URL" -ErrorAction SilentlyContinue
Remove-Item "env:RTSP_TEST_DURATION_MINUTES" -ErrorAction SilentlyContinue
Remove-Item "env:RTSP_TEST_OUTPUT_DIR" -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  Шаг 4: Результаты" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

if ($exitCode -eq 0) {
    Write-Host "✅ Тест успешно завершен!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Отчеты сохранены в: $OutputDir" -ForegroundColor Green
    
    # Поиск последнего отчета
    $latestReport = Get-ChildItem -Path $OutputDir -Filter "stability-report-*.md" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if ($latestReport) {
        Write-Host "Последний отчет: $($latestReport.Name)" -ForegroundColor DarkGray
        Write-Host "Путь: $($latestReport.FullName)" -ForegroundColor DarkGray
    }
} else {
    Write-Host "❌ Тест завершился с ошибками (exit code: $exitCode)" -ForegroundColor Red
    Write-Host "Проверьте логи выше для деталей" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  Конец" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

exit $exitCode
