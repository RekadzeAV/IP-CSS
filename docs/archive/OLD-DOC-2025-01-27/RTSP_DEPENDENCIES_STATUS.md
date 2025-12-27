# Статус зависимостей для RTSP клиента

**Дата проверки:** Декабрь 2025  
**Статус:** ⚠️ Зависимости не установлены

---

## Требуемые зависимости

### CMake
- **Требуется:** ≥ 3.15
- **Статус:** ❌ Не установлен
- **Установка (macOS):** `brew install cmake`
- **Установка (Ubuntu/Debian):** `sudo apt-get install cmake`

### FFmpeg
- **Требуется:** libavformat, libavcodec, libavutil, libswscale, libswresample
- **Статус:** ❌ Не установлен
- **Установка (macOS):** `brew install ffmpeg`
- **Установка (Ubuntu/Debian):** `sudo apt-get install libavformat-dev libavcodec-dev libavutil-dev libswscale-dev libswresample-dev`

### pkg-config
- **Требуется:** для обнаружения FFmpeg
- **Статус:** ❌ Не установлен
- **Установка (macOS):** `brew install pkg-config`
- **Установка (Ubuntu/Debian):** `sudo apt-get install pkg-config`

---

## Команды для установки

### macOS (Homebrew)

```bash
# Установка Homebrew (если не установлен)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Установка зависимостей
brew install cmake ffmpeg pkg-config

# Проверка
cmake --version
pkg-config --exists libavformat && echo "FFmpeg OK" || echo "FFmpeg NOT FOUND"
```

### Ubuntu/Debian

```bash
sudo apt-get update
sudo apt-get install -y \
    cmake \
    build-essential \
    pkg-config \
    libavformat-dev \
    libavcodec-dev \
    libavutil-dev \
    libswscale-dev \
    libswresample-dev

# Проверка
cmake --version
pkg-config --exists libavformat && echo "FFmpeg OK" || echo "FFmpeg NOT FOUND"
```

---

## Что делать после установки

1. **Проверить установку:**
   ```bash
   cmake --version
   pkg-config --exists libavformat && echo "OK" || echo "NOT FOUND"
   ```

2. **Скомпилировать библиотеку:**
   ```bash
   ./scripts/build-native-lib.sh
   ```

3. **Следовать инструкциям:**
   - [RTSP_QUICK_START.md](docs/RTSP_QUICK_START.md) - Быстрый старт
   - [RTSP_CLIENT_ACTIVATION_GUIDE.md](docs/RTSP_CLIENT_ACTIVATION_GUIDE.md) - Полное руководство

---

## ⚠️ Важно

Установка зависимостей требует:
- Доступ к интернету (для загрузки)
- Права администратора (для установки системных пакетов)
- Интерактивного подтверждения (для некоторых установщиков)

**После установки зависимостей** можно продолжить с компиляции библиотеки и активации кода.

---

**Последнее обновление:** Декабрь 2025

