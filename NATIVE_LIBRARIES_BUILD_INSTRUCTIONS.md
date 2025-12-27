# Инструкции по сборке нативных библиотек

## Обзор

Этот документ описывает процесс сборки нативных библиотек для всех поддерживаемых платформ:
- **Desktop**: Linux, macOS, Windows
- **Android**: armeabi-v7a, arm64-v8a, x86, x86_64
- **iOS**: arm64, x64 (симулятор), simulator-arm64

## Быстрый старт

### Все платформы (текущая ОС)
```bash
# Linux/macOS
./scripts/build-all-native-libs.sh all

# Windows
.\scripts\build-all-native-libs.ps1
```

### Конкретная платформа
```bash
# Linux
./scripts/build-all-native-libs.sh linux

# macOS
./scripts/build-all-native-libs.sh macos

# Windows
.\scripts\build-all-native-libs.ps1

# Android (все архитектуры)
./scripts/build-android-native-libs.sh all

# iOS (все архитектуры, только на macOS)
./scripts/build-ios-native-libs.sh all
```

## Требования

### Общие требования
- **CMake 3.15+** - система сборки
- **C++ компилятор**:
  - Linux: GCC или Clang
  - macOS: Xcode Command Line Tools
  - Windows: MinGW-w64 или Visual Studio Build Tools

### Android
- **Android NDK** (установлен через Android Studio SDK Manager)
- Переменная окружения `ANDROID_NDK_HOME` или `NDK_HOME` должна указывать на NDK
- Или NDK должен быть в стандартном месте: `~/Android/Sdk/ndk/`

### iOS
- **macOS** с установленным Xcode
- **Xcode Command Line Tools**: `xcode-select --install`

### Опциональные зависимости
- **FFmpeg** - для RTSP и обработки видео (можно отключить через `ENABLE_FFMPEG=OFF`)
- **OpenCV** - для обработки изображений (можно отключить через `ENABLE_OPENCV=OFF`)
- **TensorFlow Lite** - для AI аналитики (можно отключить через `ENABLE_TENSORFLOW=OFF`)

## Детальные инструкции

### 1. Desktop платформы

#### Linux
```bash
./scripts/build-all-native-libs.sh linux
```

Библиотеки будут созданы в:
- `native/video-processing/lib/linux/x64/libvideo_processing.so`
- `native/analytics/lib/linux/x64/libanalytics.so`
- `native/codecs/lib/linux/x64/libcodecs.so`

#### macOS
```bash
./scripts/build-all-native-libs.sh macos
```

Библиотеки будут созданы в:
- `native/video-processing/lib/macos/arm64/libvideo_processing.dylib`
- `native/video-processing/lib/macos/x64/libvideo_processing.dylib`
- (аналогично для analytics и codecs)

#### Windows
```powershell
.\scripts\build-all-native-libs.ps1
```

Библиотеки будут созданы в:
- `native\video-processing\lib\windows\x64\video_processing.dll`
- `native\analytics\lib\windows\x64\analytics.dll`
- `native\codecs\lib\windows\x64\codecs.dll`

### 2. Android

#### Настройка NDK

**Linux/macOS:**
```bash
# Установка через Android Studio SDK Manager, затем:
export ANDROID_NDK_HOME="$HOME/Android/Sdk/ndk/<version>"
```

**Windows:**
```powershell
# Установка через Android Studio SDK Manager, затем:
$env:ANDROID_NDK_HOME = "$env:LOCALAPPDATA\Android\Sdk\ndk\<version>"
```

#### Сборка для всех архитектур
```bash
./scripts/build-android-native-libs.sh all
```

#### Сборка для конкретной архитектуры
```bash
# ARM 32-bit
./scripts/build-android-native-libs.sh armeabi-v7a

# ARM 64-bit
./scripts/build-android-native-libs.sh arm64-v8a

# x86
./scripts/build-android-native-libs.sh x86

# x86_64
./scripts/build-android-native-libs.sh x86_64
```

**Windows:**
```powershell
.\scripts\build-android-native-libs.ps1 all
.\scripts\build-android-native-libs.ps1 arm64-v8a
```

Библиотеки будут созданы в:
- `native/video-processing/lib/android/armeabi-v7a/libvideo_processing.so`
- `native/video-processing/lib/android/arm64-v8a/libvideo_processing.so`
- (аналогично для других архитектур и библиотек)

#### Интеграция в Android проект

Библиотеки автоматически подхватываются из `native/video-processing/lib/android/` благодаря конфигурации в `android/app/build.gradle.kts`:

```kotlin
sourceSets {
    getByName("main") {
        jniLibs.srcDirs("src/main/jniLibs", "../native/video-processing/lib/android")
    }
}
```

JNI обертки уже реализованы в:
- `native/video-processing/src/jni/rtsp_client_jni.cpp`
- `core/network/src/androidMain/kotlin/.../NativeRtspClient.android.kt`

### 3. iOS

#### Требования
- macOS с Xcode
- iOS SDK (устанавливается автоматически с Xcode)

#### Сборка для всех архитектур
```bash
./scripts/build-ios-native-libs.sh all
```

#### Сборка для конкретной архитектуры
```bash
# Реальные устройства (ARM64)
./scripts/build-ios-native-libs.sh arm64

# Симулятор x86_64 (старые Mac на Intel)
./scripts/build-ios-native-libs.sh x64

# Симулятор ARM64 (новые Mac на Apple Silicon)
./scripts/build-ios-native-libs.sh simulator-arm64
```

Библиотеки будут созданы в:
- `native/video-processing/lib/ios/arm64/libvideo_processing.a`
- `native/video-processing/lib/ios/x64/libvideo_processing.a`
- `native/video-processing/lib/ios/simulator-arm64/libvideo_processing.a`

#### Интеграция в iOS проект

iOS targets уже настроены в `core/network/build.gradle.kts`:
- `iosX64` - для симулятора x86_64
- `iosArm64` - для реальных устройств
- `iosSimulatorArm64` - для симулятора ARM64

Cinterop автоматически подхватывает библиотеки из соответствующих директорий.

## Проверка сборки

### Проверка наличия библиотек
```bash
# Linux
ls -lh native/video-processing/lib/linux/x64/*.so

# macOS
ls -lh native/video-processing/lib/macos/*/lib*.dylib

# Windows
dir native\video-processing\lib\windows\x64\*.dll

# Android
ls -lh native/video-processing/lib/android/*/lib*.so

# iOS
ls -lh native/video-processing/lib/ios/*/lib*.a
```

### Тестирование интеграции

#### Android
```bash
./gradlew :android:app:assembleDebug
```

Проверьте логи при запуске приложения - библиотека должна загрузиться без ошибок.

#### iOS
```bash
./gradlew :core:network:compileKotlinIosArm64
```

#### Desktop
```bash
./gradlew :core:network:compileKotlinNativeLinuxX64
```

## Устранение проблем

### Android: NDK не найден
```
❌ Android NDK not found. Please set ANDROID_NDK_HOME or NDK_HOME
```

**Решение:**
1. Установите NDK через Android Studio: Tools → SDK Manager → SDK Tools → NDK
2. Установите переменную окружения:
   ```bash
   export ANDROID_NDK_HOME="$HOME/Android/Sdk/ndk/<version>"
   ```

### iOS: SDK не найден
```
❌ iOS SDK not found
```

**Решение:**
```bash
xcode-select --install
sudo xcode-select --switch /Applications/Xcode.app/Contents/Developer
```

### FFmpeg/OpenCV не найдены
Библиотеки будут собраны без этих зависимостей (они отключены по умолчанию для Android/iOS).

Для desktop платформ установите:
```bash
# Ubuntu/Debian
sudo apt-get install libavformat-dev libavcodec-dev libavutil-dev libswscale-dev libopencv-dev

# macOS
brew install ffmpeg opencv
```

### Ошибки компиляции
1. Проверьте версию CMake: `cmake --version` (должна быть >= 3.15)
2. Проверьте компилятор: `gcc --version` или `clang --version`
3. Очистите build директорию: `rm -rf native/build`

## Структура директорий

```
native/
├── video-processing/
│   ├── lib/
│   │   ├── android/
│   │   │   ├── armeabi-v7a/
│   │   │   ├── arm64-v8a/
│   │   │   ├── x86/
│   │   │   └── x86_64/
│   │   ├── ios/
│   │   │   ├── arm64/
│   │   │   ├── x64/
│   │   │   └── simulator-arm64/
│   │   ├── linux/x64/
│   │   ├── macos/arm64/ и macos/x64/
│   │   └── windows/x64/
│   └── src/jni/  # JNI обертки для Android
├── analytics/lib/  # аналогичная структура
└── codecs/lib/    # аналогичная структура
```

## Следующие шаги

После успешной сборки библиотек:

1. **Android**: Библиотеки автоматически подхватываются через `jniLibs.srcDirs`
2. **iOS**: Cinterop автоматически подхватывает библиотеки через `linkerOpts`
3. **Desktop**: Cinterop автоматически подхватывает библиотеки через `linkerOpts`

Для активации нативного декодера см. `BUILD_NATIVE_LIBRARY.md`.

