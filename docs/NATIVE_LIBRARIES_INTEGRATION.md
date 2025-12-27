# Интеграция нативных библиотек (C++)

## Обзор

Проект использует нативные C++ библиотеки для обработки видео, RTSP стриминга и AI аналитики. Эти библиотеки не управляются через Gradle, а требуют установки на системе разработчика и настройки через CMake.

## Критически необходимые библиотеки

### 1. FFmpeg

**Назначение:** RTSP клиент, декодирование/кодирование видео

**Установка:**

#### Windows
```powershell
# Через Chocolatey
choco install ffmpeg

# Или скачать с https://ffmpeg.org/download.html
# Распаковать в C:\ffmpeg
```

#### Linux (Ubuntu/Debian)
```bash
sudo apt-get update
sudo apt-get install ffmpeg libavformat-dev libavcodec-dev libavutil-dev libswscale-dev libswresample-dev
```

#### macOS
```bash
brew install ffmpeg
```

**CMake конфигурация:**
- Уже настроено в `native/CMakeLists.txt` и `native/video-processing/CMakeLists.txt`
- CMake автоматически найдет FFmpeg через `pkg-config` или `find_package`

**Проверка установки:**
```bash
ffmpeg -version
```

### 2. OpenCV

**Назначение:** Обработка изображений, детекция движения

**Установка:**

#### Windows
```powershell
# Скачать с https://opencv.org/releases/
# Распаковать в C:\opencv
# Установить переменную окружения OPENCV_DIR=C:\opencv\build
```

#### Linux (Ubuntu/Debian)
```bash
sudo apt-get install libopencv-dev
```

#### macOS
```bash
brew install opencv
```

**CMake конфигурация:**
- Уже настроено в `native/CMakeLists.txt`
- CMake автоматически найдет OpenCV через `find_package(OpenCV)`

**Проверка установки:**
```bash
pkg-config --modversion opencv4
```

### 3. TensorFlow Lite (опционально)

**Назначение:** AI/ML inference для детекции объектов, распознавания лиц

**Установка:**

#### Android
Добавлено в `gradle/libs.versions.toml`:
```kotlin
// Раскомментировать в shared/build.gradle.kts при необходимости
implementation(libs.tensorflow.lite)
implementation(libs.tensorflow.lite.gpu)
implementation(libs.tensorflow.lite.support)
```

#### C++ (Desktop/Linux)
```bash
# Скачать TensorFlow Lite C++ API
# https://www.tensorflow.org/lite/guide/build_cmake
git clone https://github.com/tensorflow/tensorflow.git
cd tensorflow
./tensorflow/lite/tools/cmake/download_dependencies.sh
mkdir tflite_build && cd tflite_build
cmake ../tensorflow/lite/c
make -j8
```

**CMake конфигурация:**
- Уже настроено в `native/analytics/CMakeLists.txt`
- Требуется указать путь к TensorFlow Lite через переменную `TFLITE_INCLUDE_DIR`

### 4. Live555 (опционально, альтернатива FFmpeg для RTSP)

**Назначение:** Специализированная RTSP библиотека

**Установка:**
```bash
# Скачать с http://www.live555.com/liveMedia/
# Или через git
git clone https://github.com/rgaufman/live555.git
cd live555
./genMakefiles linux
make
```

**Интеграция в CMake:**
Добавить в `native/video-processing/CMakeLists.txt`:
```cmake
include(FetchContent)
FetchContent_Declare(
    live555
    GIT_REPOSITORY https://github.com/rgaufman/live555.git
    GIT_TAG master
)
FetchContent_MakeAvailable(live555)
```

## Сборка нативных библиотек

### Windows
```powershell
cd native
mkdir build
cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
cmake --build . --config Release
```

### Linux
```bash
cd native
mkdir build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
make -j$(nproc)
```

### macOS
```bash
cd native
mkdir build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
make -j$(sysctl -n hw.ncpu)
```

## Переменные окружения (опционально)

Если библиотеки установлены в нестандартных местах:

```bash
# Windows
set FFMPEG_DIR=C:\ffmpeg
set OPENCV_DIR=C:\opencv\build

# Linux/macOS
export FFMPEG_DIR=/usr/local
export OPENCV_DIR=/usr/local
export TFLITE_INCLUDE_DIR=/path/to/tensorflow/lite/c
```

## Проверка интеграции

После установки всех библиотек, проверьте сборку:

```bash
# Проверить что CMake находит все библиотеки
cd native
mkdir build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Debug

# Должны увидеть сообщения:
# - FFmpeg found via pkg-config (или найден вручную)
# - OpenCV version: 4.x.x
# - TensorFlow Lite found: /path/to/tflite (если установлен)
```

## Android специфичные библиотеки

### OpenCV для Android
```kotlin
// В shared/build.gradle.kts (androidMain)
implementation(libs.opencv.android)
```

### TensorFlow Lite для Android
```kotlin
// В shared/build.gradle.kts (androidMain)
implementation(libs.tensorflow.lite)
implementation(libs.tensorflow.lite.gpu) // Для GPU ускорения
implementation(libs.tensorflow.lite.support)
```

**Примечание:** Эти зависимости закомментированы в `shared/build.gradle.kts` и должны быть раскомментированы при необходимости.

## iOS специфичные библиотеки

Для iOS используются встроенные фреймворки:
- **VideoToolbox** - декодирование видео (встроен)
- **AVFoundation** - работа с медиа (встроен)
- **CoreML** - ML inference (встроен, альтернатива TensorFlow Lite)

## Troubleshooting

### FFmpeg не найден
1. Проверьте установку: `ffmpeg -version`
2. Убедитесь что `pkg-config` может найти FFmpeg: `pkg-config --modversion libavformat`
3. Установите переменную окружения `FFMPEG_DIR` если нужно

### OpenCV не найден
1. Проверьте установку: `pkg-config --modversion opencv4`
2. Убедитесь что OpenCV установлен с модулями `core` и `imgproc`
3. Для Windows установите `OPENCV_DIR`

### TensorFlow Lite не найден
1. Убедитесь что скачали C++ API
2. Установите переменную `TFLITE_INCLUDE_DIR`
3. Для Android используйте Gradle зависимости (уже настроено)

## Рекомендации

1. **Для разработки:** Используйте предкомпилированные версии библиотек
2. **Для production:** Рассмотрите статическую линковку для уменьшения размера
3. **Для CI/CD:** Используйте Docker образы с предустановленными библиотеками
4. **Для Android:** Используйте Gradle зависимости вместо нативных библиотек где возможно

## Ссылки

- [FFmpeg Documentation](https://ffmpeg.org/documentation.html)
- [OpenCV Documentation](https://docs.opencv.org/)
- [TensorFlow Lite](https://www.tensorflow.org/lite)
- [Live555](http://www.live555.com/liveMedia/)
- [CMake Documentation](https://cmake.org/documentation/)


