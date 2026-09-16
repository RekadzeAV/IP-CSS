# Руководство по сборке библиотеки аналитики

## Обзор

Это руководство описывает процесс компиляции нативной библиотеки `analytics` для всех целевых платформ и интеграции с Kotlin через FFI/JNI.

## Требования

### Общие зависимости
- CMake 3.15 или выше
- C++ компилятор (GCC, Clang, MSVC)
- OpenCV (опционально, для обработки изображений)
- TensorFlow Lite (опционально, для детекции объектов)
- Tesseract OCR (опционально, для ANPR)

### Платформо-специфичные требования

#### Linux
```bash
# Ubuntu/Debian
sudo apt-get install cmake build-essential libopencv-dev libtesseract-dev

# Fedora/RHEL
sudo dnf install cmake gcc-c++ opencv-devel tesseract-devel
```

#### macOS
```bash
brew install cmake opencv tesseract
```

#### Windows
- Установите CMake из https://cmake.org/download/
- Установите MinGW-w64 или Visual Studio Build Tools
- Установите OpenCV и Tesseract вручную или через vcpkg

#### Android
- Android NDK (r21 или выше)
- Установите через Android Studio SDK Manager или скачайте отдельно

## Сборка библиотеки

### Linux (x64)

```bash
cd native/analytics
mkdir -p build/linux && cd build/linux
cmake ../.. \
    -DCMAKE_BUILD_TYPE=Release \
    -DENABLE_OPENCV=ON \
    -DENABLE_TENSORFLOW=ON \
    -DENABLE_TESSERACT=ON
cmake --build . --config Release

# Библиотека будет создана в:
# native/analytics/lib/linux/x64/libanalytics.so
```

### macOS (x64 и arm64)

```bash
cd native/analytics
mkdir -p build/macos && cd build/macos
cmake ../.. \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_OSX_ARCHITECTURES="x86_64;arm64" \
    -DENABLE_OPENCV=ON \
    -DENABLE_TENSORFLOW=ON \
    -DENABLE_TESSERACT=ON
cmake --build . --config Release

# Библиотеки будут созданы в:
# native/analytics/lib/macos/x64/libanalytics.dylib
# native/analytics/lib/macos/arm64/libanalytics.dylib
```

### Windows (x64)

```powershell
cd native\analytics
mkdir build\windows -Force
cd build\windows
cmake ..\.. `
    -DCMAKE_BUILD_TYPE=Release `
    -DENABLE_OPENCV=ON `
    -DENABLE_TENSORFLOW=ON `
    -DENABLE_TESSERACT=ON `
    -G "MinGW Makefiles"
cmake --build . --config Release

# Библиотека будет создана в:
# native\analytics\lib\windows\x64\analytics.dll
```

### Android

Для каждой архитектуры (armeabi-v7a, arm64-v8a, x86, x86_64):

```bash
# Установите переменную окружения
export ANDROID_NDK_HOME=/path/to/android/ndk

# Используйте скрипт сборки
./scripts/build-android-native-libs.sh all

# Или для конкретной архитектуры:
./scripts/build-android-native-libs.sh arm64-v8a
```

Библиотеки будут созданы в:
- `native/analytics/lib/android/armeabi-v7a/libanalytics.so`
- `native/analytics/lib/android/arm64-v8a/libanalytics.so`
- `native/analytics/lib/android/x86/libanalytics.so`
- `native/analytics/lib/android/x86_64/libanalytics.so`

## Сборка с JNI поддержкой

Для Android и JVM платформ требуется сборка с JNI поддержкой:

```bash
cd native/analytics
mkdir -p build/jni && cd build/jni
cmake ../.. \
    -DCMAKE_BUILD_TYPE=Release \
    -DBUILD_JNI_LIBRARY=ON \
    -DENABLE_OPENCV=ON \
    -DENABLE_TENSORFLOW=ON \
    -DENABLE_TESSERACT=ON

# Для Android также требуется указать toolchain:
cmake ../.. \
    -DCMAKE_BUILD_TYPE=Release \
    -DBUILD_JNI_LIBRARY=ON \
    -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake \
    -DANDROID_ABI=arm64-v8a \
    -DANDROID_PLATFORM=android-21
```

## Использование скриптов сборки

### Linux/macOS

```bash
# Сборка для текущей платформы
./scripts/build-all-native-libs.sh

# Сборка для конкретной платформы
./scripts/build-all-native-libs.sh linux
./scripts/build-all-native-libs.sh macos
./scripts/build-all-native-libs.sh windows
./scripts/build-all-native-libs.sh android
```

### Windows (PowerShell)

```powershell
.\scripts\build-all-native-libs.ps1
```

## Интеграция с Android

После сборки библиотеки для Android:

1. Скопируйте библиотеки в проект:
```bash
cp native/analytics/lib/android/arm64-v8a/libanalytics.so \
   android/app/src/main/jniLibs/arm64-v8a/
cp native/analytics/lib/android/armeabi-v7a/libanalytics.so \
   android/app/src/main/jniLibs/armeabi-v7a/
# и т.д. для других архитектур
```

2. Убедитесь, что библиотека загружается в `NativeAnalytics.android.kt`:
```kotlin
System.loadLibrary("analytics")
```

## Интеграция с JVM (Desktop)

После сборки библиотеки для вашей платформы:

1. Убедитесь, что библиотека находится в системном пути или укажите путь в `NativeAnalytics.jvm.kt`
2. Библиотека будет автоматически загружена при первом использовании

## Проверка сборки

После сборки проверьте наличие библиотек:

```bash
# Linux
ls -lh native/analytics/lib/linux/x64/

# macOS
ls -lh native/analytics/lib/macos/arm64/
ls -lh native/analytics/lib/macos/x64/

# Windows
dir native\analytics\lib\windows\x64\

# Android
ls -lh native/analytics/lib/android/arm64-v8a/
```

## Устранение проблем

### Ошибка: OpenCV not found
```bash
# Установите OpenCV или отключите его:
cmake .. -DENABLE_OPENCV=OFF
```

### Ошибка: TensorFlow Lite not found
```bash
# Установите TensorFlow Lite или отключите его:
cmake .. -DENABLE_TENSORFLOW=OFF
```

### Ошибка: Tesseract not found
```bash
# Установите Tesseract или отключите его:
cmake .. -DENABLE_TESSERACT=OFF
```

### Ошибка при загрузке библиотеки в Android
- Убедитесь, что библиотека находится в правильной директории `jniLibs`
- Проверьте архитектуру устройства и соответствие библиотеки
- Проверьте логи Android Studio для деталей ошибки

## Следующие шаги

После успешной сборки библиотеки:
1. Интегрируйте `NativeAnalytics` с `AnalyticsService` в модуле `shared`
2. Протестируйте функциональность на целевых платформах
3. Добавьте unit-тесты для FFI интеграции

## Связанные документы

- [ANALYTICS_FFI_INTEGRATION.md](../archive/docs-deprecated-2026-09-04/ANALYTICS_FFI_INTEGRATION.md) - Документация по FFI интеграции
- [AI_ANALYTICS.md](AI_ANALYTICS.md) - Общая документация по AI-аналитике
