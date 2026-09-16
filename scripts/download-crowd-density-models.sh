#!/bin/bash
# Скрипт для скачивания моделей оценки плотности толпы
# Использование: ./scripts/download-crowd-density-models.sh [model_name]
# Пример: ./scripts/download-crowd-density-models.sh csrnet

MODEL_NAME=${1:-csrnet}
MODELS_DIR="data/models/crowd-analysis"

# Создаем папку если не существует
mkdir -p "$MODELS_DIR"

echo ""
echo "=== Скачивание модели оценки плотности толпы ==="
echo "Модель: $MODEL_NAME"
echo "Папка: $MODELS_DIR"
echo ""
echo "Примечание: CSRNet требует ручной настройки."
echo "Рекомендуется использовать YOLOv8 для подсчета людей."
echo ""

echo "Инструкции по установке CSRNet:"
echo "1. git clone https://github.com/leeyeehoo/CSRNet-pytorch.git"
echo "2. Скачайте веса модели с репозитория"
echo "3. Конвертируйте в ONNX формат"
echo "4. Переместите в папку: $MODELS_DIR"
echo ""
echo "Альтернатива: Используйте YOLOv8 для подсчета людей (детекция + подсчет)"
