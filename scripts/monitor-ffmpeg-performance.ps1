# Скрипт мониторинга производительности FFmpeg
# Отслеживает использование CPU, памяти и производительность записи

param(
    [int]$Duration = 60,  # Длительность мониторинга в секундах
    [string]$OutputFile = "ffmpeg-performance-report.txt"
)

$ErrorActionPreference = "Continue"

function Write-Info {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor Green
}

function Write-Warn {
    param([string]$Message)
    Write-Host "[WARN] $Message" -ForegroundColor Yellow
}

function Write-Error {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

# Проверка наличия FFmpeg
if (-not (Get-Command ffmpeg -ErrorAction SilentlyContinue)) {
    Write-Error "FFmpeg не установлен. Установите FFmpeg перед запуском мониторинга."
    Write-Info "Используйте: .\scripts\install-ffmpeg.ps1"
    exit 1
}

Write-Info "Мониторинг производительности FFmpeg"
Write-Info "Длительность: $Duration секунд"
Write-Info "Отчет будет сохранен в: $OutputFile"

# Создаем отчет
$report = @"
Отчет о производительности FFmpeg
==================================
Дата: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
Длительность мониторинга: $Duration секунд

"@

# Информация о системе
Write-Info "Сбор информации о системе..."
$cpuInfo = Get-WmiObject Win32_Processor | Select-Object Name, NumberOfCores, NumberOfLogicalProcessors
$memoryInfo = Get-WmiObject Win32_ComputerSystem | Select-Object TotalPhysicalMemory

$report += @"

Информация о системе:
---------------------
Процессор: $($cpuInfo.Name)
Ядра: $($cpuInfo.NumberOfCores)
Логические процессоры: $($cpuInfo.NumberOfLogicalProcessors)
Память: $([math]::Round($memoryInfo.TotalPhysicalMemory / 1GB, 2)) GB

"@

# Информация о FFmpeg
Write-Info "Сбор информации о FFmpeg..."
$ffmpegVersion = & ffmpeg -version 2>&1 | Select-Object -First 1
$report += @"

Информация о FFmpeg:
--------------------
$ffmpegVersion

"@

# Проверка hardware acceleration
Write-Info "Проверка hardware acceleration..."
$encoders = & ffmpeg -encoders 2>&1
$hwAccel = @()

if ($encoders -match "h264_nvenc") {
    $hwAccel += "NVIDIA NVENC (h264_nvenc)"
}
if ($encoders -match "h264_qsv") {
    $hwAccel += "Intel Quick Sync (h264_qsv)"
}
if ($encoders -match "h264_videotoolbox") {
    $hwAccel += "VideoToolbox (h264_videotoolbox)"
}
if ($encoders -match "h264_vaapi") {
    $hwAccel += "VAAPI (h264_vaapi)"
}

if ($hwAccel.Count -gt 0) {
    $report += "Hardware acceleration доступен:`n"
    $hwAccel | ForEach-Object { $report += "  - $_`n" }
    $report += "`n"
} else {
    $report += "Hardware acceleration НЕ доступен (используется программное кодирование)`n`n"
}

# Проверка доступных кодеков
Write-Info "Проверка доступных кодеков..."
$codecs = & ffmpeg -codecs 2>&1

$audioCodecs = @()
if ($codecs -match "DEA.*aac") { $audioCodecs += "AAC" }
if ($codecs -match "DEA.*mp3") { $audioCodecs += "MP3" }
if ($codecs -match "DEA.*pcm") { $audioCodecs += "PCM" }
if ($codecs -match "DEA.*g711") { $audioCodecs += "G.711" }

$videoCodecs = @()
if ($codecs -match "DEV.*h264") { $videoCodecs += "H.264" }
if ($codecs -match "DEV.*h265|DEV.*hevc") { $videoCodecs += "H.265/HEVC" }

$report += @"
Доступные кодеки:
-----------------
Аудио: $($audioCodecs -join ", ")
Видео: $($videoCodecs -join ", ")

"@

# Мониторинг производительности (если есть активные процессы FFmpeg)
Write-Info "Мониторинг процессов FFmpeg..."
$startTime = Get-Date
$samples = @()

for ($i = 0; $i -lt $Duration; $i++) {
    $ffmpegProcesses = Get-Process -Name "ffmpeg" -ErrorAction SilentlyContinue

    if ($ffmpegProcesses) {
        foreach ($proc in $ffmpegProcesses) {
            $sample = [PSCustomObject]@{
                Time = (Get-Date).ToString("HH:mm:ss")
                CPU = $proc.CPU
                MemoryMB = [math]::Round($proc.WorkingSet64 / 1MB, 2)
                Threads = $proc.Threads.Count
            }
            $samples += $sample
        }
    }

    Start-Sleep -Seconds 1

    if (($i % 10) -eq 0) {
        Write-Host "." -NoNewline
    }
}

Write-Host ""

# Анализ результатов
if ($samples.Count -gt 0) {
    $avgCPU = ($samples | Measure-Object -Property CPU -Average).Average
    $maxCPU = ($samples | Measure-Object -Property CPU -Maximum).Maximum
    $avgMemory = ($samples | Measure-Object -Property MemoryMB -Average).Average
    $maxMemory = ($samples | Measure-Object -Property MemoryMB -Maximum).Maximum

    $report += @"

Результаты мониторинга:
------------------------
Количество образцов: $($samples.Count)
Средняя нагрузка CPU: $([math]::Round($avgCPU, 2))%
Максимальная нагрузка CPU: $([math]::Round($maxCPU, 2))%
Среднее использование памяти: $([math]::Round($avgMemory, 2)) MB
Максимальное использование памяти: $([math]::Round($maxMemory, 2)) MB

"@

    # Рекомендации
    $report += "Рекомендации:`n"
    $report += "------------`n"

    if ($maxCPU -gt 80) {
        $report += "⚠ ВНИМАНИЕ: Высокая нагрузка на CPU ($([math]::Round($maxCPU, 2))%)`n"
        $report += "  - Рассмотрите использование hardware acceleration`n"
        $report += "  - Снизьте качество записи (LOW или MEDIUM)`n"
        $report += "  - Уменьшите количество одновременных записей`n"
    } elseif ($maxCPU -gt 50) {
        $report += "ℹ Умеренная нагрузка на CPU ($([math]::Round($maxCPU, 2))%)`n"
        $report += "  - Система работает нормально`n"
    } else {
        $report += "✓ Низкая нагрузка на CPU ($([math]::Round($maxCPU, 2))%)`n"
        $report += "  - Система работает оптимально`n"
    }

    if ($hwAccel.Count -eq 0 -and $maxCPU -gt 50) {
        $report += "`n💡 СОВЕТ: Hardware acceleration не используется`n"
        $report += "  - Установите драйверы GPU для снижения нагрузки на CPU`n"
    }
} else {
    $report += @"

Результаты мониторинга:
------------------------
Активные процессы FFmpeg не обнаружены во время мониторинга.
Запустите запись и повторите мониторинг.

"@
}

$report += @"

Конец отчета
============
"@

# Сохранение отчета
$report | Out-File -FilePath $OutputFile -Encoding UTF8
Write-Info "Отчет сохранен в: $OutputFile"

# Вывод краткой информации
Write-Host "`n" -NoNewline
Write-Info "Краткая сводка:"
Write-Host $report

