# Скрипт для скачивания моделей оценки плотности толпы
# Использование: .\scripts\download-crowd-density-models.ps1 [model_name]
# Пример: .\scripts\download-crowd-density-models.ps1 csrnet

param(
    [switch]$ShowHelp,
    [string]$ModelName = "csrnet"
)

if ($ShowHelp) {
    Write-Host "Download CSRNet crowd-density model assets into data/models/crowd-analysis."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\download-crowd-density-models.ps1 -ShowHelp"
    Write-Host "  .\scripts\download-crowd-density-models.ps1 [[-ModelName] <name>]"
    Write-Host ""
    Write-Host "Exit codes: 0 (prints manual CSRNet setup; no bundled auto-download)."
    exit 0
}

$ModelsDir = "data/models/crowd-analysis"
$ErrorActionPreference = "Stop"

# Создаем папку если не существует
if (-not (Test-Path $ModelsDir)) {
    New-Item -ItemType Directory -Path $ModelsDir -Force | Out-Null
    Write-Host "Создана папка: $ModelsDir" -ForegroundColor Green
}

# Функция для скачивания CSRNet
function Download-CSRNet {
    param([string]$OutputDir)

    Write-Host "Скачивание CSRNet модели..." -ForegroundColor Yellow

    # Проверяем наличие Python
    $python = Get-Command python -ErrorAction SilentlyContinue
    if (-not $python) {
        Write-Host "Python не найден. Используем альтернативный метод..." -ForegroundColor Yellow

        Write-Host "`nДля скачивания CSRNet выполните следующие команды:" -ForegroundColor Cyan
        Write-Host "1. Установите зависимости: pip install torch torchvision" -ForegroundColor Cyan
        Write-Host "2. Клонируйте репозиторий: git clone https://github.com/leeyeehoo/CSRNet-pytorch.git" -ForegroundColor Cyan
        Write-Host "3. Скачайте предобученную модель с: https://github.com/leeyeehoo/CSRNet-pytorch/releases" -ForegroundColor Cyan
        Write-Host "4. Конвертируйте в ONNX и переместите в папку $OutputDir" -ForegroundColor Cyan

        return $false
    }

    # Создаем временный Python скрипт
    $scriptContent = @"
import torch
import torch.onnx
import os
import sys

output_dir = "$OutputDir"

try:
    print("Загрузка CSRNet модели...")

    # Пытаемся скачать предобученную модель
    # CSRNet обычно требует обучения или скачивания весов
    print("CSRNet требует предобученные веса.")
    print("Альтернативный метод:")
    print("1. Скачайте веса с: https://github.com/leeyeehoo/CSRNet-pytorch/releases")
    print("2. Используйте скрипт конвертации в ONNX")
    print("3. Переместите модель в папку: $output_dir")

    # Для простоты используем YOLOv8 с подсчетом как альтернативу
    print("")
    print("Рекомендуется использовать YOLOv8 для подсчета людей:")
    print("python -c \"from ultralytics import YOLO; model = YOLO('yolov8n.pt'); model.export(format='onnx')\"")

    sys.exit(1)
except Exception as e:
    print(f"Ошибка: {e}")
    sys.exit(1)
"@

    $tempScript = [System.IO.Path]::GetTempFileName() + ".py"
    $scriptContent | Out-File -FilePath $tempScript -Encoding UTF8

    try {
        $result = & python $tempScript 2>&1
        Write-Host $result
        return $false  # CSRNet требует ручной настройки
    } finally {
        Remove-Item $tempScript -ErrorAction SilentlyContinue
    }
}

# Основная логика
$modelFile = Join-Path $ModelsDir "$ModelName.onnx"

if (Test-Path $modelFile) {
    Write-Host "Модель $ModelName уже существует: $modelFile" -ForegroundColor Green
    $overwrite = Read-Host "Перезаписать? (y/N)"
    if ($overwrite -ne "y" -and $overwrite -ne "Y") {
        Write-Host "Пропущено." -ForegroundColor Yellow
        exit 0
    }
}

Write-Host "`n=== Скачивание модели оценки плотности толпы ===" -ForegroundColor Cyan
Write-Host "Модель: $ModelName" -ForegroundColor Cyan
Write-Host "Папка: $ModelsDir" -ForegroundColor Cyan
Write-Host ""
Write-Host "Примечание: CSRNet требует ручной настройки." -ForegroundColor Yellow
Write-Host "Рекомендуется использовать YOLOv8 для подсчета людей." -ForegroundColor Yellow
Write-Host ""

Write-Host "Инструкции по установке CSRNet:" -ForegroundColor Cyan
Write-Host "1. git clone https://github.com/leeyeehoo/CSRNet-pytorch.git" -ForegroundColor Cyan
Write-Host "2. Скачайте веса модели с репозитория" -ForegroundColor Cyan
Write-Host "3. Конвертируйте в ONNX формат" -ForegroundColor Cyan
Write-Host "4. Переместите в папку: $ModelsDir" -ForegroundColor Cyan
Write-Host ""
Write-Host "Альтернатива: Используйте YOLOv8 для подсчета людей (детекция + подсчет)" -ForegroundColor Green
