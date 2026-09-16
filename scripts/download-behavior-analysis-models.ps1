# Скрипт для скачивания моделей анализа поведения
# Использование: .\scripts\download-behavior-analysis-models.ps1 [model_name]
# Пример: .\scripts\download-behavior-analysis-models.ps1 yolov8-pose

param(
    [switch]$ShowHelp,
    [string]$ModelName = "yolov8-pose"
)

if ($ShowHelp) {
    Write-Host "Download YOLOv8-pose style behavior models into data/models/behavior-analysis."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\download-behavior-analysis-models.ps1 -ShowHelp"
    Write-Host "  .\scripts\download-behavior-analysis-models.ps1 [[-ModelName] <name>]"
    Write-Host ""
    Write-Host "Exit codes: 0 success or skipped; 1 if automatic download failed (manual steps printed)."
    exit 0
}

$ModelsDir = "data/models/behavior-analysis"
$ErrorActionPreference = "Stop"

# Создаем папку если не существует
if (-not (Test-Path $ModelsDir)) {
    New-Item -ItemType Directory -Path $ModelsDir -Force | Out-Null
    Write-Host "Создана папка: $ModelsDir" -ForegroundColor Green
}

# Функция для скачивания YOLOv8-Pose
function Download-YOLOv8Pose {
    param([string]$OutputDir)

    Write-Host "Скачивание YOLOv8-Pose модели..." -ForegroundColor Yellow

    # Проверяем наличие Python
    $python = Get-Command python -ErrorAction SilentlyContinue
    if (-not $python) {
        Write-Host "Python не найден. Используем альтернативный метод..." -ForegroundColor Yellow

        Write-Host "`nДля скачивания YOLOv8-Pose выполните следующие команды:" -ForegroundColor Cyan
        Write-Host "1. Установите ultralytics: pip install ultralytics" -ForegroundColor Cyan
        Write-Host "2. Экспортируйте модель в ONNX:" -ForegroundColor Cyan
        Write-Host "   python -c `"from ultralytics import YOLO; model = YOLO('yolov8n-pose.pt'); model.export(format='onnx')`"" -ForegroundColor Cyan
        Write-Host "3. Переместите файл yolov8n-pose.onnx в папку $OutputDir" -ForegroundColor Cyan

        return $false
    }

    # Создаем временный Python скрипт
    $scriptContent = @"
from ultralytics import YOLO
import os
import sys

model_name = "yolov8n-pose"
output_dir = "$OutputDir"

try:
    print(f"Загрузка модели {model_name}...")
    model = YOLO(f"{model_name}.pt")

    output_path = os.path.join(output_dir, f"{model_name}.onnx")
    print(f"Экспорт модели в ONNX: {output_path}")
    model.export(format="onnx", imgsz=640)

    # Перемещаем файл в нужную папку
    exported_file = f"{model_name}.onnx"
    if os.path.exists(exported_file):
        import shutil
        shutil.move(exported_file, output_path)
        print(f"Модель успешно сохранена: {output_path}")
        sys.exit(0)
    else:
        print(f"Ошибка: файл {exported_file} не найден")
        sys.exit(1)
except Exception as e:
    print(f"Ошибка при загрузке модели: {e}")
    sys.exit(1)
"@

    $tempScript = [System.IO.Path]::GetTempFileName() + ".py"
    $scriptContent | Out-File -FilePath $tempScript -Encoding UTF8

    try {
        $result = & python $tempScript 2>&1
        Write-Host $result
        return $LASTEXITCODE -eq 0
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

Write-Host "`n=== Скачивание модели анализа поведения ===" -ForegroundColor Cyan
Write-Host "Модель: $ModelName" -ForegroundColor Cyan
Write-Host "Папка: $ModelsDir" -ForegroundColor Cyan
Write-Host ""

if ($ModelName -eq "yolov8-pose" -or $ModelName -eq "yolov8n-pose") {
    if (Download-YOLOv8Pose -OutputDir $ModelsDir) {
        Write-Host "`nМодель успешно скачана!" -ForegroundColor Green
        Write-Host "Путь: $modelFile" -ForegroundColor Green
    } else {
        Write-Host "`nНе удалось скачать модель автоматически." -ForegroundColor Red
        Write-Host "Следуйте инструкциям выше для ручного скачивания." -ForegroundColor Yellow
        exit 1
    }
} else {
    Write-Host "Неизвестная модель: $ModelName" -ForegroundColor Red
    Write-Host "Доступные модели: yolov8-pose, yolov8n-pose" -ForegroundColor Yellow
    exit 1
}
