# Инструкция по сборке нативной библиотеки для VideoDecoder

## Обзор

Нативная библиотека `video_processing` используется для декодирования H.264/H.265 на Native платформах (Linux, macOS, Windows) через FFmpeg.

## Требования

1. **FFmpeg 6.0+** должен быть установлен в системе
2. **CMake 3.15+**
3. **C++ компилятор** (GCC/Clang/MSVC)

## Установка FFmpeg

### Linux (Ubuntu/Debian)
```bash
sudo apt-get update
sudo apt-get install ffmpeg libavcodec-dev libavformat-dev libavutil-dev libswscale-dev libswresample-dev
```

### macOS
```bash
brew install ffmpeg
```

### Windows
1. Скачайте FFmpeg с https://ffmpeg.org/download.html
2. Распакуйте в `C:\ffmpeg`
3. Установите переменную окружения:
   ```powershell
   $env:FFMPEG_DIR = "C:\ffmpeg"
   ```

## Сборка библиотеки

### Linux x64
```bash
cd native/video-processing
mkdir -p build/linux-x64
cd build/linux-x64
cmake ../.. -DCMAKE_BUILD_TYPE=Release -DENABLE_FFMPEG=ON
cmake --build . --config Release
```

Библиотека будет в: `native/video-processing/lib/linux/x64/libvideo_processing.so`

### macOS x64
```bash
cd native/video-processing
mkdir -p build/macos-x64
cd build/macos-x64
cmake ../.. -DCMAKE_BUILD_TYPE=Release -DENABLE_FFMPEG=ON
cmake --build . --config Release
```

Библиотека будет в: `native/video-processing/lib/macos/x64/libvideo_processing.dylib`

### macOS arm64
```bash
cd native/video-processing
mkdir -p build/macos-arm64
cd build/macos-arm64
cmake ../.. -DCMAKE_BUILD_TYPE=Release -DENABLE_FFMPEG=ON -DCMAKE_OSX_ARCHITECTURES=arm64
cmake --build . --config Release
```

Библиотека будет в: `native/video-processing/lib/macos/arm64/libvideo_processing.dylib`

### Windows x64
```powershell
cd native/video-processing
mkdir build\windows-x64
cd build\windows-x64
cmake ..\.. -DCMAKE_BUILD_TYPE=Release -DENABLE_FFMPEG=ON -G "Visual Studio 17 2022" -A x64
cmake --build . --config Release
```

Или с MinGW:
```bash
cd native/video-processing
mkdir -p build/windows-x64
cd build/windows-x64
cmake ../.. -DCMAKE_BUILD_TYPE=Release -DENABLE_FFMPEG=ON -G "MinGW Makefiles"
cmake --build . --config Release
```

Библиотека будет в: `native/video-processing/lib/windows/x64/video_processing.dll`

## Проверка сборки

После сборки проверьте наличие библиотеки:

```bash
# Linux
ls native/video-processing/lib/linux/x64/libvideo_processing.so

# macOS
ls native/video-processing/lib/macos/x64/libvideo_processing.dylib
ls native/video-processing/lib/macos/arm64/libvideo_processing.dylib

# Windows
ls native/video-processing/lib/windows/x64/video_processing.dll
```

## Генерация cinterop биндингов

После сборки библиотеки сгенерируйте Kotlin биндинги:

```bash
# Для Linux
./gradlew :core:network:generateCInteropVideoProcessingNativeLinux

# Для macOS x64
./gradlew :core:network:generateCInteropVideoProcessingNativeMacosX64

# Для macOS arm64
./gradlew :core:network:generateCInteropVideoProcessingNativeMacosArm64

# Для Windows
./gradlew :core:network:generateCInteropVideoProcessingNativeWindows
```

## Активация VideoDecoder.native.kt

После генерации биндингов:

1. Откройте `core/network/src/nativeMain/kotlin/com/company/ipcamera/core/network/video/VideoDecoder.native.kt`
2. Раскомментируйте импорты cinterop
3. Раскомментируйте код создания и использования декодера
4. Соберите проект: `./gradlew :core:network:build`

## Устранение проблем

### FFmpeg не найден
- Убедитесь, что FFmpeg установлен
- Установите переменную окружения `FFMPEG_DIR` если FFmpeg в нестандартном месте
- Для Linux/macOS убедитесь, что `pkg-config` может найти FFmpeg

### Ошибки линковки
- Проверьте, что библиотека собрана и находится в правильной директории
- Проверьте пути в `linkerOpts` в `build.gradle.kts`

### Ошибки компиляции cinterop
- Убедитесь, что заголовочные файлы доступны
- Проверьте пути в `compilerOpts` в `build.gradle.kts`

## Статус

- ✅ Нативная библиотека улучшена (поддержка SPS/PPS)
- ✅ CMake конфигурация проверена
- ✅ Cinterop конфигурация настроена
- ⏳ Требуется сборка библиотеки для каждой платформы
- ⏳ Требуется генерация cinterop биндингов
- ⏳ Требуется активация VideoDecoder.native.kt
