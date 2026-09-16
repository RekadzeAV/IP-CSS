# Сборка нативной библиотеки RTSP клиента

**Дата создания:** 26 January 2026
**Версия:** 1.0

## Обзор

Нативная библиотека `video_processing` содержит реализацию RTSP клиента на C++ и должна быть собрана для каждой целевой платформы перед использованием RTSP клиента в Kotlin коде.

## Быстрый старт

### Windows

```powershell
# Использование готового скрипта
.\scripts\build-rtsp-native-libs.ps1 windows x64 Release

# Или для конкретной библиотеки
.\scripts\build-video-processing-lib.ps1
```

### Linux/macOS

```bash
# Использование готового скрипта
./scripts/build-video-processing-lib.sh linux x64
./scripts/build-video-processing-lib.sh macos arm64

# Или для всех платформ
./scripts/build-all-native-libs.sh
```

## Требования

### Общие требования

- **CMake 3.15+** - система сборки
- **C++17 компилятор:**
  - Windows: Visual Studio 2019+ или MinGW-w64
  - Linux: g++ 7+ или clang++ 7+
  - macOS: Xcode Command Line Tools (clang++)
  - Android: Android NDK r21+
  - iOS: Xcode 12+

### Зависимости

#### FFmpeg (рекомендуется)

FFmpeg используется для декодирования видео и улучшенной работы RTSP клиента. Без FFmpeg библиотека будет работать в упрощенном режиме.

**Windows:**
```powershell
# Через Chocolatey
choco install ffmpeg

# Или скачать с https://ffmpeg.org/download.html
# Распаковать в C:\ffmpeg
$env:FFMPEG_DIR = "C:\ffmpeg"
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt-get update
sudo apt-get install libavformat-dev libavcodec-dev libavutil-dev libswscale-dev libswresample-dev
```

**Linux (Fedora/RHEL):**
```bash
sudo dnf install ffmpeg-devel
```

**macOS:**
```bash
brew install ffmpeg
```

#### OpenCV (опционально)

OpenCV используется для дополнительной обработки изображений. Не требуется для базовой работы RTSP клиента.

## Сборка для разных платформ

### Windows x64

#### С Visual Studio

```powershell
cd native\video-processing
mkdir build\windows-x64 -Force
cd build\windows-x64

cmake ..\.. `
    -G "Visual Studio 17 2022" `
    -A x64 `
    -DCMAKE_BUILD_TYPE=Release `
    -DENABLE_FFMPEG=ON `
    -DENABLE_OPENCV=OFF

cmake --build . --config Release --parallel

# Копируем библиотеку
mkdir ..\..\lib\windows\x64 -Force
copy Release\video_processing.dll ..\..\lib\windows\x64\
```

#### С MinGW

```powershell
cd native\video-processing
mkdir build\windows-mingw -Force
cd build\windows-mingw

cmake ..\.. `
    -G "MinGW Makefiles" `
    -DCMAKE_BUILD_TYPE=Release `
    -DENABLE_FFMPEG=ON `
    -DENABLE_OPENCV=OFF

cmake --build . --config Release -j4

# Копируем библиотеку
mkdir ..\..\lib\windows\x64 -Force
copy libvideo_processing.dll ..\..\lib\windows\x64\
```

### Linux x64

```bash
cd native/video-processing
mkdir -p build/linux-x64
cd build/linux-x64

cmake ../.. \
    -DCMAKE_BUILD_TYPE=Release \
    -DENABLE_FFMPEG=ON \
    -DENABLE_OPENCV=OFF

cmake --build . --config Release -j$(nproc)

# Копируем библиотеку
mkdir -p ../../lib/linux/x64
cp libvideo_processing.so ../../lib/linux/x64/
```

### macOS (ARM64 и x64)

#### ARM64 (Apple Silicon)

```bash
cd native/video-processing
mkdir -p build/macos-arm64
cd build/macos-arm64

cmake ../.. \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_OSX_ARCHITECTURES="arm64" \
    -DCMAKE_OSX_DEPLOYMENT_TARGET="11.0" \
    -DENABLE_FFMPEG=ON \
    -DENABLE_OPENCV=OFF

cmake --build . --config Release

# Копируем библиотеку
mkdir -p ../../lib/macos/arm64
cp libvideo_processing.dylib ../../lib/macos/arm64/
```

#### x64 (Intel)

```bash
cd native/video-processing
mkdir -p build/macos-x64
cd build/macos-x64

cmake ../.. \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_OSX_ARCHITECTURES="x86_64" \
    -DCMAKE_OSX_DEPLOYMENT_TARGET="11.0" \
    -DENABLE_FFMPEG=ON \
    -DENABLE_OPENCV=OFF

cmake --build . --config Release

# Копируем библиотеку
mkdir -p ../../lib/macos/x64
cp libvideo_processing.dylib ../../lib/macos/x64/
```

### Android

Для Android требуется Android NDK и FFmpeg, собранный для Android.

```bash
export ANDROID_NDK=/path/to/android-ndk-r21e

cd native/video-processing
mkdir -p build/android/arm64-v8a
cd build/android/arm64-v8a

cmake ../.. \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI=arm64-v8a \
    -DANDROID_PLATFORM=android-21 \
    -DENABLE_FFMPEG=ON \
    -DENABLE_OPENCV=OFF

cmake --build . --config Release

# Копируем библиотеку
mkdir -p ../../../lib/android/arm64-v8a
cp libvideo_processing.so ../../../lib/android/arm64-v8a/
```

Повторите для других архитектур:
- `armeabi-v7a` (ARM 32-bit)
- `x86` (32-bit)
- `x86_64` (64-bit)

### iOS

Для iOS требуется Xcode и FFmpeg, собранный для iOS.

```bash
cd native/video-processing
mkdir -p build/ios/arm64
cd build/ios/arm64

cmake ../.. \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_SYSTEM_NAME=iOS \
    -DCMAKE_OSX_ARCHITECTURES="arm64" \
    -DCMAKE_OSX_DEPLOYMENT_TARGET="11.0" \
    -DCMAKE_XCODE_ATTRIBUTE_ONLY_ACTIVE_ARCH=NO \
    -DENABLE_FFMPEG=ON \
    -DENABLE_OPENCV=OFF

cmake --build . --config Release

# Копируем библиотеку
mkdir -p ../../../lib/ios/arm64
cp libvideo_processing.a ../../../lib/ios/arm64/
```

Для симулятора используйте `x64` или `simulator-arm64` архитектуру.

## Структура выходных файлов

После сборки библиотеки должны находиться в:

```
native/video-processing/lib/
├── windows/
│   └── x64/
│       ├── video_processing.dll
│       └── video_processing.lib (для MSVC)
├── linux/
│   └── x64/
│       └── libvideo_processing.so
├── macos/
│   ├── x64/
│   │   └── libvideo_processing.dylib
│   └── arm64/
│       └── libvideo_processing.dylib
├── android/
│   ├── arm64-v8a/
│   │   └── libvideo_processing.so
│   ├── armeabi-v7a/
│   │   └── libvideo_processing.so
│   ├── x86/
│   │   └── libvideo_processing.so
│   └── x86_64/
│       └── libvideo_processing.so
└── ios/
    ├── arm64/
    │   └── libvideo_processing.a
    ├── x64/
    │   └── libvideo_processing.a
    └── simulator-arm64/
        └── libvideo_processing.a
```

## Проверка результата

После сборки проверьте наличие библиотеки:

**Windows:**
```powershell
Test-Path native\video-processing\lib\windows\x64\video_processing.dll
Get-Item native\video-processing\lib\windows\x64\video_processing.dll | Select-Object Name, Length
```

**Linux/macOS:**
```bash
ls -lh native/video-processing/lib/*/*/libvideo_processing.*
file native/video-processing/lib/linux/x64/libvideo_processing.so
```

## Переменные окружения

### FFmpeg

Если FFmpeg установлен в нестандартном месте:

**Windows:**
```powershell
$env:FFMPEG_DIR = "C:\ffmpeg"
```

**Linux/macOS:**
```bash
export FFMPEG_DIR=/path/to/ffmpeg
export PKG_CONFIG_PATH=/path/to/ffmpeg/lib/pkgconfig:$PKG_CONFIG_PATH
```

### OpenCV

**Windows:**
```powershell
$env:OpenCV_DIR = "C:\opencv"
```

**Linux/macOS:**
```bash
export OpenCV_DIR=/path/to/opencv
```

### Android

```bash
export ANDROID_NDK=/path/to/android-ndk-r21e
export ANDROID_ABI=arm64-v8a
```

## Устранение проблем

### CMake не находит компилятор

**Windows:**
- Установите Visual Studio Build Tools или MinGW-w64
- Добавьте компилятор в PATH
- Для Visual Studio запустите Developer Command Prompt

**Linux/macOS:**
- Установите build-essential (Linux) или Xcode Command Line Tools (macOS)
- Проверьте: `g++ --version` или `clang++ --version`

### FFmpeg не найден

1. Убедитесь, что FFmpeg установлен
2. Установите переменную окружения `FFMPEG_DIR`
3. Для Linux/macOS убедитесь, что `pkg-config` может найти FFmpeg:
   ```bash
   pkg-config --modversion libavformat
   ```

### Ошибки компиляции

1. Убедитесь, что компилятор поддерживает C++17
2. Проверьте версию CMake (должна быть 3.15+)
3. Убедитесь, что все зависимости установлены
4. Проверьте логи сборки для деталей ошибок

### Android: библиотека не собирается

1. Убедитесь, что `ANDROID_NDK` установлен правильно
2. Проверьте версию NDK (рекомендуется r21+)
3. Убедитесь, что FFmpeg собран для Android
4. Проверьте, что архитектура ABI указана правильно

### iOS: библиотека не собирается

1. Убедитесь, что Xcode установлен
2. Проверьте версию Xcode (должна быть 12+)
3. Убедитесь, что FFmpeg собран для iOS
4. Проверьте архитектуру (arm64 для устройств, x64/simulator-arm64 для симулятора)

## Сборка без FFmpeg

Библиотека может быть собрана без FFmpeg, но с ограниченной функциональностью:

```powershell
# Windows
cmake ..\.. -DENABLE_FFMPEG=OFF -DENABLE_OPENCV=OFF

# Linux/macOS
cmake ../.. -DENABLE_FFMPEG=OFF -DENABLE_OPENCV=OFF
```

В этом режиме RTSP клиент будет работать, но без декодирования видео через FFmpeg.

## Следующие шаги

После успешной сборки библиотеки:

1. **Проверьте библиотеку:**
   ```powershell
   # Windows
   Test-Path native\video-processing\lib\windows\x64\video_processing.dll

   # Linux/macOS
   ls -lh native/video-processing/lib/*/*/libvideo_processing.*
   ```

2. **Соберите Kotlin модуль:**
   ```bash
   ./gradlew :core:network:build
   ```

3. **Протестируйте интеграцию:**
   ```bash
   ./gradlew :core:network:test
   ```

4. **Используйте RTSP клиент:**
   См. [RTSP_CLIENT_EXAMPLES.md](RTSP_CLIENT_EXAMPLES.md) для примеров использования

## Автоматизация сборки

Для автоматической сборки всех платформ используйте скрипты:

**Windows:**
```powershell
.\scripts\build-rtsp-native-libs.ps1 all x64 Release
```

**Linux/macOS:**
```bash
./scripts/build-all-native-libs.sh
```

Эти скрипты автоматически определят доступные компиляторы и соберут библиотеки для всех поддерживаемых платформ.

---

**Связанные документы:**
- [RTSP_NATIVE_INTEGRATION.md](RTSP_NATIVE_INTEGRATION.md) - Интеграция с нативной библиотекой
- [RTSP_CLIENT_EXAMPLES.md](RTSP_CLIENT_EXAMPLES.md) - Примеры использования
- [BUILD.md](../../native/video-processing/BUILD.md) - Общее руководство по сборке
