# Руководство по сборке нативной библиотеки video_processing

## Быстрый старт

### Linux/macOS

```bash
# Сборка для текущей платформы
./scripts/build-all-platforms.sh Release

# Или для конкретной платформы
./scripts/build-native-lib.sh linux x64 Release
./scripts/build-native-lib.sh macos arm64 Release
```

### Windows

```powershell
# Сборка для Windows x64
.\scripts\build-native-lib.ps1 x64 Release
```

### Android

```bash
# Установите переменную окружения ANDROID_NDK
export ANDROID_NDK=/path/to/android-ndk-r21e

# Сборка для разных архитектур
./scripts/build-android.sh arm64-v8a Release
./scripts/build-android.sh armeabi-v7a Release
./scripts/build-android.sh x86 Release
./scripts/build-android.sh x86_64 Release
```

### iOS

```bash
# Сборка для iOS
./scripts/build-ios.sh arm64 Release          # Устройство
./scripts/build-ios.sh x64 Release            # Симулятор (Intel)
./scripts/build-ios.sh simulator-arm64 Release # Симулятор (Apple Silicon)
```

## Требования

### Общие
- **CMake 3.15+**
- **C++17 компилятор:**
  - Linux: g++ 7+ или clang++ 7+
  - macOS: Xcode Command Line Tools (clang++)
  - Windows: Visual Studio 2019+ или MinGW-w64
  - Android: Android NDK r21+
  - iOS: Xcode 12+

### Зависимости

#### FFmpeg (обязательно)

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

**Windows:**
```powershell
# Через Chocolatey
choco install ffmpeg

# Или скачать с https://ffmpeg.org/download.html
# Распаковать в C:\ffmpeg и установить переменную окружения:
$env:FFMPEG_DIR = "C:\ffmpeg"
```

**Android:**
FFmpeg должен быть собран для Android и размещен в `${ANDROID_NDK}/sources/third_party/ffmpeg/`

**iOS:**
FFmpeg должен быть собран для iOS и размещен в `third_party/ffmpeg/ios/`

#### OpenCV (опционально)

**Linux:**
```bash
sudo apt-get install libopencv-dev
```

**macOS:**
```bash
brew install opencv
```

**Windows:**
Скачать с https://opencv.org/releases/ и установить переменную `OPENCV_DIR`

## Структура выходных файлов

После сборки библиотеки будут находиться в:

```
native/video-processing/lib/
├── linux/
│   └── x64/
│       └── libvideo_processing.so
├── macos/
│   ├── x64/
│   │   └── libvideo_processing.dylib
│   └── arm64/
│       └── libvideo_processing.dylib
├── windows/
│   └── x64/
│       └── video_processing.dll
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

## Ручная сборка через CMake

Если скрипты не работают, можно собрать вручную:

### Linux

```bash
cd native/video-processing
mkdir -p build/linux/x64 && cd build/linux/x64

cmake ../../.. \
    -DCMAKE_BUILD_TYPE=Release \
    -DENABLE_FFMPEG=ON \
    -DENABLE_OPENCV=ON

cmake --build . --config Release -j$(nproc)

# Копируем библиотеку
mkdir -p ../../../lib/linux/x64
cp libvideo_processing.so ../../../lib/linux/x64/
```

### macOS

```bash
cd native/video-processing
mkdir -p build/macos/arm64 && cd build/macos/arm64

cmake ../../.. \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_OSX_ARCHITECTURES="arm64" \
    -DCMAKE_OSX_DEPLOYMENT_TARGET="11.0" \
    -DENABLE_FFMPEG=ON \
    -DENABLE_OPENCV=ON

cmake --build . --config Release

# Копируем библиотеку
mkdir -p ../../../lib/macos/arm64
cp libvideo_processing.dylib ../../../lib/macos/arm64/
```

### Windows (MSVC)

```powershell
cd native\video-processing
mkdir build\windows\x64 -Force
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
copy Release\video_processing.dll ..\..\..\lib\windows\x64\
```

### Windows (MinGW)

```powershell
cd native\video-processing
mkdir build\windows\mingw -Force
cd build\windows\mingw

cmake ..\..\.. `
    -G "MinGW Makefiles" `
    -DCMAKE_BUILD_TYPE=Release `
    -DCMAKE_C_COMPILER=x86_64-w64-mingw32-gcc `
    -DCMAKE_CXX_COMPILER=x86_64-w64-mingw32-g++ `
    -DENABLE_FFMPEG=ON `
    -DENABLE_OPENCV=ON

cmake --build . --config Release

# Копируем библиотеку
mkdir ..\..\..\lib\windows\x64 -Force
copy libvideo_processing.dll ..\..\..\lib\windows\x64\
```

### Android

```bash
export ANDROID_NDK=/path/to/android-ndk-r21e

cd native/video-processing
mkdir -p build/android/arm64-v8a && cd build/android/arm64-v8a

cmake ../../.. \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI=arm64-v8a \
    -DANDROID_PLATFORM=android-21 \
    -DENABLE_FFMPEG=ON \
    -DENABLE_OPENCV=ON

cmake --build . --config Release

# Копируем библиотеку
mkdir -p ../../../lib/android/arm64-v8a
cp libvideo_processing.so ../../../lib/android/arm64-v8a/
```

### iOS

```bash
cd native/video-processing
mkdir -p build/ios/arm64 && cd build/ios/arm64

cmake ../../.. \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_SYSTEM_NAME=iOS \
    -DCMAKE_OSX_ARCHITECTURES="arm64" \
    -DCMAKE_OSX_DEPLOYMENT_TARGET="11.0" \
    -DCMAKE_XCODE_ATTRIBUTE_ONLY_ACTIVE_ARCH=NO \
    -DENABLE_FFMPEG=ON \
    -DENABLE_OPENCV=ON

cmake --build . --config Release

# Копируем библиотеку
mkdir -p ../../../lib/ios/arm64
cp libvideo_processing.a ../../../lib/ios/arm64/
```

## Переменные окружения

### FFmpeg

Если FFmpeg установлен в нестандартном месте:

```bash
# Linux/macOS
export FFMPEG_DIR=/path/to/ffmpeg
export PKG_CONFIG_PATH=/path/to/ffmpeg/lib/pkgconfig:$PKG_CONFIG_PATH

# Windows
$env:FFMPEG_DIR = "C:\ffmpeg"
```

### OpenCV

```bash
# Linux/macOS
export OpenCV_DIR=/path/to/opencv

# Windows
$env:OpenCV_DIR = "C:\opencv"
```

### Android

```bash
export ANDROID_NDK=/path/to/android-ndk-r21e
export ANDROID_ABI=arm64-v8a  # или armeabi-v7a, x86, x86_64
```

## Устранение проблем

### FFmpeg не найден

1. Убедитесь, что FFmpeg установлен
2. Установите переменную окружения `FFMPEG_DIR`
3. Для Linux/macOS убедитесь, что `pkg-config` может найти FFmpeg:
   ```bash
   pkg-config --modversion libavformat
   ```

### OpenCV не найден

OpenCV опционален. Если не установлен, библиотека соберется без него:

```bash
cmake .. -DENABLE_OPENCV=OFF
```

### Ошибки компиляции

1. Убедитесь, что компилятор поддерживает C++17
2. Проверьте версию CMake (должна быть 3.15+)
3. Убедитесь, что все зависимости установлены

### Android: библиотека не собирается

1. Убедитесь, что `ANDROID_NDK` установлен правильно
2. Проверьте версию NDK (рекомендуется r21+)
3. Убедитесь, что FFmpeg собран для Android

### iOS: библиотека не собирается

1. Убедитесь, что Xcode установлен
2. Проверьте версию Xcode (должна быть 12+)
3. Убедитесь, что FFmpeg собран для iOS

## Проверка результата

После сборки проверьте наличие библиотеки:

```bash
# Linux/macOS
ls -lh native/video-processing/lib/*/*/libvideo_processing.*

# Windows
Get-ChildItem -Recurse native\video-processing\lib\*\*\video_processing.*
```

## Следующие шаги

После успешной сборки библиотеки:

1. Убедитесь, что библиотека находится в правильной директории
2. Соберите Kotlin модуль: `./gradlew :core:network:build`
3. Протестируйте интеграцию с RTSP клиентом

## Поддержка

Если возникли проблемы:
1. Проверьте логи сборки
2. Убедитесь, что все зависимости установлены
3. Проверьте версии инструментов (CMake, компилятор)
4. См. раздел "Устранение проблем" выше

