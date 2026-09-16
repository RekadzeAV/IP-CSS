#!/bin/bash
# Скрипт для скачивания YOLO моделей
# Использование: ./scripts/download-yolo-models.sh [model_name]
# Пример: ./scripts/download-yolo-models.sh yolov8n

MODEL_NAME=${1:-yolov8n}
MODELS_DIR="data/models"

# Создаем папку если не существует
mkdir -p "$MODELS_DIR"

# Функция для скачивания модели через Python
download_yolo_model() {
    local model_name=$1
    local output_dir=$2

    echo "Скачивание модели $model_name..."

    # Проверяем наличие Python
    if ! command -v python3 &> /dev/null && ! command -v python &> /dev/null; then
        echo "Python не найден. Используем альтернативный метод..."
        echo ""
        echo "Для скачивания модели выполните следующие команды:"
        echo "1. Установите ultralytics: pip install ultralytics"
        echo "2. Экспортируйте модель в ONNX:"
        echo "   python3 -c \"from ultralytics import YOLO; model = YOLO('$model_name.pt'); model.export(format='onnx')\""
        echo "3. Переместите файл $model_name.onnx в папку $output_dir"
        return 1
    fi

    # Определяем команду Python
    PYTHON_CMD="python3"
    if ! command -v python3 &> /dev/null; then
        PYTHON_CMD="python"
    fi

    # Создаем временный Python скрипт
    TEMP_SCRIPT=$(mktemp)
    cat > "$TEMP_SCRIPT" << EOF
from ultralytics import YOLO
import os
import sys

model_name = "$model_name"
output_dir = "$output_dir"

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
EOF

    # Выполняем скрипт
    $PYTHON_CMD "$TEMP_SCRIPT"
    local result=$?
    rm -f "$TEMP_SCRIPT"

    return $result
}

# Основная логика
MODEL_FILE="$MODELS_DIR/$MODEL_NAME.onnx"

if [ -f "$MODEL_FILE" ]; then
    echo "Модель $MODEL_NAME уже существует: $MODEL_FILE"
    read -p "Перезаписать? (y/N): " overwrite
    if [ "$overwrite" != "y" ] && [ "$overwrite" != "Y" ]; then
        echo "Пропущено."
        exit 0
    fi
fi

echo ""
echo "=== Скачивание YOLO модели ==="
echo "Модель: $MODEL_NAME"
echo "Папка: $MODELS_DIR"
echo ""

if download_yolo_model "$MODEL_NAME" "$MODELS_DIR"; then
    echo ""
    echo "Модель успешно скачана!"
    echo "Путь: $MODEL_FILE"
else
    echo ""
    echo "Не удалось скачать модель автоматически."
    echo "Следуйте инструкциям выше для ручного скачивания."
    exit 1
fi
