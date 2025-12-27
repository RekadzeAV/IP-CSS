# Сборка нативной библиотеки video_processing

## Быстрый старт

### Linux/macOS

```bash
# Сборка для текущей платформы
./scripts/build-video-processing-lib.sh

# Или для конкретной платформы и архитектуры
./scripts/build-video-processing-lib.sh linux x64
./scripts/build-video-processing-lib.sh macos arm64
```

### Windows

```powershell
.\scripts\build-video-processing-lib.ps1
```

## Требования

### Общие
- CMake 3.15 или выше
- C++ компилятор с поддержкой C++17:
  - Linux: g++ или clang++
  - macOS: Xcode Command Line Tools (clang++)
  - Windows: Visual Studio 2019+ или MinGW-w64

### Зависимости

#### FFmpeg (обязательно)
- **Linux (Ubuntu/Debian):**
  ```bash
  sudo apt-get update
  sudo apt-get install libavformat-dev libavcodec-dev libavutil-dev libswscale-dev libswresample-dev
  ```
- **Linux (Fedora/RHEL):**
  ```bash
  sudo dnf install ffmpeg-devel
  ```
- **macOS:**
  ```bash
  brew install ffmpeg
  ```
- **Windows:**
  ```powershell
  choco install ffmpeg
  # Или скачать с https://ffmpeg.org/download.html и распаковать в C:\ffmpeg
  ```

#### OpenCV (опционально)
- **Linux:**
  ```bash
  sudo apt-get install libopencv-dev
  ```
- **macOS:**
  ```bash
  brew install opencv
  ```
- **Windows:**
  Скачать с https://opencv.org/releases/ и установить переменную `OPENCV_DIR`

## Ручная сборка через CMake

Если скрипты не работают, можно собрать вручную:

### Linux

```bash
cd native/video-processing
mkdir -p build/linux && cd build/linux

cmake ../.. \
    -DCMAKE_BUILD_TYPE=Release \
    -DENABLE_FFMPEG=ON \
    -DENABLE_OPENCV=ON

cmake --build . --config Release -j$(nproc)

# Копируем библиотеку
mkdir -p ../../lib/linux/x64
cp libvideo_processing.so ../../lib/linux/x64/
```

### macOS

```bash
cd native/video-processing
mkdir -p build/macos && cd build/macos

cmake ../.. \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_OSX_ARCHITECTURES="x86_64" \
    -DCMAKE_OSX_DEPLOYMENT_TARGET="11.0" \
    -DENABLE_FFMPEG=ON \
    -DENABLE_OPENCV=ON

cmake --build . --config Release

# Копируем библиотеку
mkdir -p ../../lib/macos/x64
cp libvideo_processing.dylib ../../lib/macos/x64/
```

### Windows (MSVC)

```powershell
cd native\video-processing
mkdir build\windows, build\windows\x64 -Force
cd build\windows\x64

cmake ..\..\.. `
    -G "Visual Studio 17 2022" `
    -A x64 `
    -DCMAKE_BUILD_TYPE=Release `
    -DENABLE_FFMPEG=ON `
    -DENABLE_OPENCV=ON

cmake --build . --config Release

# Копируем библиотеку
mkdir ..\..\..\lib\windows\x64 -Force
copy video_processing.dll ..\..\..\lib\windows\x64\
```

### Windows (MinGW)

```powershell
cd native\video-processing
mkdir build\windows, build\windows\mingw -Force
cd build\windows\mingw

cmake ..\..\.. `
    -G "MinGW Makefiles" `
    -DCMAKE_BUILD_TYPE=Release `
    -DENABLE_FFMPEG=ON `
    -DENABLE_OPENCV=ON

cmake --build . --config Release

# Копируем библиотеку
mkdir ..\..\..\lib\windows\x64 -Force
copy libvideo_processing.dll ..\..\..\lib\windows\x64\
```

## Проверка результата

После сборки библиотека должна находиться в:

- **Linux:** `native/video-processing/lib/linux/x64/libvideo_processing.so`
- **macOS x64:** `native/video-processing/lib/macos/x64/libvideo_processing.dylib`
- **macOS arm64:** `native/video-processing/lib/macos/arm64/libvideo_processing.dylib`
- **Windows:** `native/video-processing/lib/windows/x64/video_processing.dll`

Проверка:

```bash
# Linux/macOS
ls -lh native/video-processing/lib/*/*/libvideo_processing.*

# Windows
Get-ChildItem -Recurse native\video-processing\lib\*\*\video_processing.*
```

## Следующие шаги

После успешной сборки библиотеки:

1. Сгенерируйте cinterop биндинги (см. `docs/ACTIVATE_NATIVE_DECODER.md`)
2. Раскомментируйте код в `VideoDecoder.native.kt`
3. Соберите проект: `./gradlew :core:network:build`

## Устранение проблем

### FFmpeg не найден

Если CMake не находит FFmpeg, установите переменные окружения:

```bash
# Linux/macOS
export PKG_CONFIG_PATH=/usr/local/lib/pkgconfig:$PKG_CONFIG_PATH

# Или укажите пути напрямую в CMake
cmake .. -DFFMPEG_ROOT=/path/to/ffmpeg
```

### OpenCV не найден

OpenCV опционален. Если не установлен, библиотека соберется без него:

```bash
cmake .. -DENABLE_OPENCV=OFF
```

### Ошибки компиляции

Убедитесь что:
- Все зависимости установлены
- Версия компилятора поддерживает C++17
- CMake версии 3.15 или выше

