# Native Video Processing Library

Нативная C++ библиотека для обработки видео, включая RTSP клиент.

## Зависимости

### Требуемые зависимости:

- **FFmpeg** (обязательно):
  - libavformat
  - libavcodec
  - libavutil
  - libswscale
  - libswresample

### Опциональные зависимости:

- **OpenCV** (для обработки изображений):
  - opencv4 (core, imgproc)

## Установка зависимостей

### Ubuntu/Debian:

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
    libswresample-dev \
    libopencv-dev
```

### macOS:

```bash
brew install cmake ffmpeg opencv pkg-config
```

### Fedora/RHEL:

```bash
sudo dnf install -y \
    cmake \
    gcc-c++ \
    pkgconfig \
    ffmpeg-devel \
    opencv-devel
```

## Компиляция

### Автоматическая сборка (рекомендуется):

```bash
# Из корневой директории проекта
./scripts/build-native-lib.sh [platform]

# Платформы: linux, macos, all
# По умолчанию: all (определяется автоматически)
```

### Ручная сборка:

```bash
cd native/video-processing
mkdir build && cd build

# Конфигурация
cmake .. -DCMAKE_BUILD_TYPE=Release

# Сборка
cmake --build . --config Release

# Библиотека будет создана в build/
```

### Проверка экспорта символов:

После сборки проверьте, что символы правильно экспортированы:

**Linux:**
```bash
nm -D build/libvideo_processing.so | grep rtsp_client
```

**macOS:**
```bash
nm -gU build/libvideo_processing.dylib | grep rtsp_client
```

Должны быть видны символы:
- `rtsp_client_create`
- `rtsp_client_connect`
- `rtsp_client_play`
- `rtsp_client_stop`
- `rtsp_client_pause`
- и другие...

## Структура

```
native/video-processing/
├── CMakeLists.txt          # Конфигурация сборки
├── include/                # Заголовочные файлы
│   ├── rtsp_client.h      # RTSP клиент API
│   ├── video_decoder.h    # Декодер видео
│   ├── video_encoder.h    # Кодировщик видео
│   └── ...
├── src/                   # Исходный код
│   ├── rtsp_client.cpp    # Реализация RTSP клиента
│   └── ...
└── lib/                   # Скомпилированные библиотеки (создается после сборки)
    ├── linux/
    └── macos/
```

## Использование

Библиотека используется через Kotlin/Native cinterop биндинги. См.:
- [RTSP_CLIENT_INTEGRATION.md](../../docs/RTSP_CLIENT_INTEGRATION.md)
- [RTSP_CLIENT_IMPLEMENTATION_STATUS.md](../../docs/RTSP_CLIENT_IMPLEMENTATION_STATUS.md)

## Известные проблемы

1. **FFmpeg не найден:**
   - Убедитесь, что FFmpeg установлен и доступен через pkg-config
   - Проверьте: `pkg-config --exists libavformat && echo "OK"`

2. **Символы не экспортируются:**
   - Проверьте, что библиотека собрана как SHARED (динамическая)
   - Проверьте visibility настройки в CMakeLists.txt

3. **Сборка не работает на macOS:**
   - Убедитесь, что архитектура правильная (x86_64 или arm64)
   - Проверьте Xcode Command Line Tools: `xcode-select --install`

## Отладка

Для отладки используйте Debug сборку:

```bash
cmake .. -DCMAKE_BUILD_TYPE=Debug
cmake --build . --config Debug
```

Для включения детального логирования, раскомментируйте соответствующие строки в исходном коде.

## Связанные документы

- [RTSP_CLIENT.md](../../docs/RTSP_CLIENT.md) - Общее описание RTSP клиента
- [RTSP_CLIENT_INTEGRATION.md](../../docs/RTSP_CLIENT_INTEGRATION.md) - Руководство по интеграции
- [RTSP_CLIENT_IMPLEMENTATION_STATUS.md](../../docs/RTSP_CLIENT_IMPLEMENTATION_STATUS.md) - Статус реализации

