#!/bin/bash
# Скрипт для скачивания моделей распознавания лиц
# Использование: ./scripts/download-face-recognition-models.sh [model_name]
# Пример: ./scripts/download-face-recognition-models.sh insightface

MODEL_NAME=${1:-insightface}
MODELS_DIR="data/models/face-recognition"

# Создаем папку если не существует
mkdir -p "$MODELS_DIR"

# Функция для скачивания RetinaFace
download_retinaface() {
    local output_dir=$1
    local model_url="https://github.com/deepinsight/insightface/releases/download/v0.7/retinaface_r50_v1.onnx"
    local output_path="$output_dir/retinaface.onnx"

    echo "Скачивание RetinaFace модели..."

    if curl -L -o "$output_path" "$model_url"; then
        echo "Модель успешно скачана: $output_path"
        return 0
    else
        echo "Ошибка при скачивании. Скачайте модель вручную с: $model_url"
        return 1
    fi
}

# Функция для скачивания InsightFace через Python
download_insightface() {
    local output_dir=$1

    echo "Скачивание InsightFace модели..."

    # Проверяем наличие Python
    if ! command -v python3 &> /dev/null && ! command -v python &> /dev/null; then
        echo "Python не найден."
        echo ""
        echo "Для скачивания InsightFace выполните следующие команды:"
        echo "1. Установите insightface: pip install insightface"
        echo "2. Скачайте модели с: https://github.com/deepinsight/insightface/releases"
        echo "3. Распакуйте в папку: $output_dir"
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
import insightface
import os
import sys

output_dir = "$output_dir"

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
echo "=== Скачивание модели распознавания лиц ==="
echo "Модель: $MODEL_NAME"
echo "Папка: $MODELS_DIR"
echo ""

case "$MODEL_NAME" in
    insightface)
        download_insightface "$MODELS_DIR"
        ;;
    retinaface)
        download_retinaface "$MODELS_DIR"
        ;;
    *)
        echo "Неизвестная модель: $MODEL_NAME"
        echo "Доступные модели: insightface, retinaface"
        exit 1
        ;;
esac

if [ $? -eq 0 ]; then
    echo ""
    echo "Модель успешно скачана!"
    echo "Путь: $MODEL_FILE"
else
    echo ""
    echo "Не удалось скачать модель автоматически."
    echo "Следуйте инструкциям выше для ручного скачивания."
    exit 1
fi
