#!/bin/bash
# Скрипт для генерации compile_commands.json для C++ линтера
# Использование: ./scripts/generate-compile-commands.sh

set -e

echo "Генерация compile_commands.json для C++ проекта..."

NATIVE_DIR="$(cd "$(dirname "$0")/.." && pwd)/native"
BUILD_DIR="$NATIVE_DIR/build"

# Создаем директорию build если её нет
mkdir -p "$BUILD_DIR"

cd "$NATIVE_DIR"

# Генерируем compile_commands.json через CMake
echo "Запуск CMake для генерации compile_commands.json..."

# Запускаем CMake конфигурацию
cmake -B build -DCMAKE_EXPORT_COMPILE_COMMANDS=ON

if [ $? -eq 0 ]; then
    # Копируем compile_commands.json в корень native директории
    COMPILE_COMMANDS="$BUILD_DIR/compile_commands.json"
    TARGET_FILE="$NATIVE_DIR/compile_commands.json"

    if [ -f "$COMPILE_COMMANDS" ]; then
        cp "$COMPILE_COMMANDS" "$TARGET_FILE"
        echo "✓ compile_commands.json успешно создан в $TARGET_FILE"
    else
        echo "⚠ compile_commands.json не найден в $COMPILE_COMMANDS"
        echo "Попытка создания базового compile_commands.json..."

        # Создаем базовый compile_commands.json
        cat > "$TARGET_FILE" << EOF
[
  {
    "directory": "$NATIVE_DIR/video-processing",
    "command": "clang++ -std=c++17 -I$NATIVE_DIR/video-processing/include -I$NATIVE_DIR/video-processing/src -c",
    "file": "$NATIVE_DIR/video-processing/src/rtsp_client.cpp"
  }
]
EOF
        echo "✓ Базовый compile_commands.json создан"
    fi
else
    echo "✗ Ошибка при выполнении CMake"
    exit 1
fi

echo "Готово!"
