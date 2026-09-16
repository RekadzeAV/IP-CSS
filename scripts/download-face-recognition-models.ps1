# Скрипт для скачивания моделей распознавания лиц
# Использование: .\scripts\download-face-recognition-models.ps1 [model_name]
# Пример: .\scripts\download-face-recognition-models.ps1 insightface

param(
    [switch]$ShowHelp,
    [string]$ModelName = "insightface"
)

if ($ShowHelp) {
    Write-Host "Download InsightFace-related assets into data/models/face-recognition (Python when available)."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\download-face-recognition-models.ps1 -ShowHelp"
    Write-Host "  .\scripts\download-face-recognition-models.ps1 [[-ModelName] <name>]"
    Write-Host ""
    Write-Host "Exit codes: 0 success or skipped; 1 if automatic download failed (manual steps printed)."
    exit 0
}

$ModelsDir = "data/models/face-recognition"
$ErrorActionPreference = "Stop"

# Создаем папку если не существует
if (-not (Test-Path $ModelsDir)) {
    New-Item -ItemType Directory -Path $ModelsDir -Force | Out-Null
    Write-Host "Создана папка: $ModelsDir" -ForegroundColor Green
}

# Функция для скачивания модели InsightFace
function Download-InsightFace {
    param([string]$OutputDir)

    Write-Host "Скачивание InsightFace модели..." -ForegroundColor Yellow

    # Проверяем наличие Python
    $python = Get-Command python -ErrorAction SilentlyContinue
    if (-not $python) {
        Write-Host "Python не найден. Используем альтернативный метод..." -ForegroundColor Yellow

        Write-Host "`nДля скачивания InsightFace выполните следующие команды:" -ForegroundColor Cyan
        Write-Host "1. Установите insightface: pip install insightface" -ForegroundColor Cyan
        Write-Host "2. Экспортируйте модель в ONNX:" -ForegroundColor Cyan
        Write-Host "   python -c `"import insightface; app = insightface.app.FaceAnalysis(); app.prepare(ctx_id=-1, det_size=(640, 640)); model = app.models[0]; model.export('$OutputDir/insightface.onnx')`"" -ForegroundColor Cyan

        return $false
    }

    # Создаем временный Python скрипт
    $scriptContent = @"
import insightface
import onnxruntime as ort
import os
import sys

output_dir = "$OutputDir"

try:
    print("Инициализация InsightFace...")
    app = insightface.app.FaceAnalysis(providers=['CPUExecutionProvider'])
    app.prepare(ctx_id=-1, det_size=(640, 640))

    # Получаем модель детекции лиц
    model = app.models[0]

    output_path = os.path.join(output_dir, "insightface.onnx")
    print(f"Экспорт модели в ONNX: {output_path}")

    # Экспортируем модель
    if hasattr(model, 'export'):
        model.export(output_path)
        print(f"Модель успешно сохранена: {output_path}")
        sys.exit(0)
    else:
        print("Модель не поддерживает экспорт. Используйте альтернативный метод.")
        print("Скачайте модель вручную с: https://github.com/deepinsight/insightface")
        sys.exit(1)
except Exception as e:
    print(f"Ошибка при загрузке модели: {e}")
    print("Альтернативный метод:")
    print("1. pip install insightface")
    print("2. Скачайте модели с: https://github.com/deepinsight/insightface/releases")
    print("3. Распакуйте в папку: $output_dir")
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

# Функция для скачивания RetinaFace
function Download-RetinaFace {
    param([string]$OutputDir)

    Write-Host "Скачивание RetinaFace модели..." -ForegroundColor Yellow

    # RetinaFace можно скачать напрямую
    $modelUrl = "https://github.com/deepinsight/insightface/releases/download/v0.7/retinaface_r50_v1.onnx"
    $outputPath = Join-Path $OutputDir "retinaface.onnx"

    try {
        Write-Host "Скачивание с GitHub..." -ForegroundColor Yellow
        Invoke-WebRequest -Uri $modelUrl -OutFile $outputPath -UseBasicParsing
        Write-Host "Модель успешно скачана: $outputPath" -ForegroundColor Green
        return $true
    } catch {
        Write-Host "Ошибка при скачивании: $_" -ForegroundColor Red
        Write-Host "Скачайте модель вручную с: $modelUrl" -ForegroundColor Yellow
        return $false
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

Write-Host "`n=== Скачивание модели распознавания лиц ===" -ForegroundColor Cyan
Write-Host "Модель: $ModelName" -ForegroundColor Cyan
Write-Host "Папка: $ModelsDir" -ForegroundColor Cyan
Write-Host ""

$success = $false
switch ($ModelName.ToLower()) {
    "insightface" {
        $success = Download-InsightFace -OutputDir $ModelsDir
    }
    "retinaface" {
        $success = Download-RetinaFace -OutputDir $ModelsDir
    }
    default {
        Write-Host "Неизвестная модель: $ModelName" -ForegroundColor Red
        Write-Host "Доступные модели: insightface, retinaface" -ForegroundColor Yellow
        exit 1
    }
}

if ($success) {
    Write-Host "`nМодель успешно скачана!" -ForegroundColor Green
    Write-Host "Путь: $modelFile" -ForegroundColor Green
} else {
    Write-Host "`nНе удалось скачать модель автоматически." -ForegroundColor Red
    Write-Host "Следуйте инструкциям выше для ручного скачивания." -ForegroundColor Yellow
    exit 1
}
