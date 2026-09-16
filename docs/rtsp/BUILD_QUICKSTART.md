# Быстрый старт: Сборка нативной библиотеки RTSP клиента

**Дата:** Январь 2026

## Минимальные требования

1. **CMake 3.15+** - [Скачать](https://cmake.org/download/)
2. **C++ компилятор:**
   - Windows: Visual Studio 2019+ или MinGW-w64
   - Linux: g++ или clang++
   - macOS: Xcode Command Line Tools
3. **FFmpeg** (опционально, но рекомендуется)

## Быстрая сборка

### Windows

```powershell
# 1. Установите зависимости (если еще не установлены)
#    - CMake: https://cmake.org/download/
#    - Visual Studio Build Tools или MinGW-w64
#    - FFmpeg (опционально): choco install ffmpeg

# 2. Запустите сборку
.\scripts\build-rtsp-native-libs.ps1 windows x64 Release

# 3. Проверьте результат
Test-Path native\video-processing\lib\windows\x64\video_processing.dll
```

### Linux

```bash
# 1. Установите зависимости
sudo apt-get update
sudo apt-get install build-essential cmake libavformat-dev libavcodec-dev libavutil-dev libswscale-dev libswresample-dev

# 2. Запустите сборку
./scripts/build-video-processing-lib.sh linux x64

# 3. Проверьте результат
ls -lh native/video-processing/lib/linux/x64/libvideo_processing.so
```

### macOS

```bash
# 1. Установите зависимости
brew install cmake ffmpeg

# 2. Запустите сборку
./scripts/build-video-processing-lib.sh macos arm64

# 3. Проверьте результат
ls -lh native/video-processing/lib/macos/arm64/libvideo_processing.dylib
```

## Что дальше?

После успешной сборки:

1. **Соберите Kotlin модуль:**
   ```bash
   ./gradlew :core:network:build
   ```

2. **Используйте RTSP клиент:**
   См. [RTSP_CLIENT_EXAMPLES.md](RTSP_CLIENT_EXAMPLES.md)

## Проблемы?

См. полное руководство: [NATIVE_LIBRARY_BUILD.md](NATIVE_LIBRARY_BUILD.md)
