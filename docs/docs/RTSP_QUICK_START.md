# Быстрый старт: Активация RTSP клиента

**Для разработчика:** Это краткое руководство для активации RTSP клиента после установки зависимостей.

---

## ⚠️ Предупреждение: Требуются зависимости

Для компиляции нативной библиотеки необходимо установить:
- **CMake** (≥ 3.15)
- **FFmpeg** (libavformat, libavcodec, libavutil, libswscale)
- **pkg-config**

**macOS:**
```bash
brew install cmake ffmpeg pkg-config
```

**Ubuntu/Debian:**
```bash
sudo apt-get install cmake build-essential pkg-config libavformat-dev libavcodec-dev libavutil-dev libswscale-dev libswresample-dev
```

---

## Шаги активации

### 1. Проверка зависимостей

```bash
cmake --version
pkg-config --exists libavformat && echo "FFmpeg OK" || echo "FFmpeg NOT FOUND"
```

### 2. Компиляция библиотеки

```bash
./scripts/build-native-lib.sh
```

### 3. Проверка компиляции

```bash
# macOS
ls -lh native/video-processing/build/libvideo_processing.dylib

# Linux  
ls -lh native/video-processing/build/libvideo_processing.so
```

### 4. Генерация биндингов

```bash
./gradlew :core:network:compileKotlinNative
# или для конкретной платформы:
./gradlew :core:network:compileKotlinMacosX64
./gradlew :core:network:compileKotlinMacosArm64
./gradlew :core:network:compileKotlinLinuxX64
```

### 5. Активация кода

Откройте файл:
`core/network/src/nativeMain/kotlin/com/company/ipcamera/core/network/rtsp/NativeRtspClient.native.kt`

**Шаг 5.1:** Раскомментируйте импорт (строка 14):
```kotlin
import com.company.ipcamera.core.network.rtsp.rtsp_client.*
```

**Шаг 5.2:** Раскомментируйте реализацию методов (найдите все `// TODO: После компиляции cinterop...`)

**Шаг 5.3:** Раскомментируйте вспомогательные функции в конце файла

**Шаг 5.4:** Реализуйте callbacks (setFrameCallback и setStatusCallback)

### 6. Проверка компиляции

```bash
./gradlew :core:network:compileKotlinNative
```

Если есть ошибки - см. [RTSP_CLIENT_ACTIVATION_GUIDE.md](RTSP_CLIENT_ACTIVATION_GUIDE.md) раздел "Известные проблемы"

---

## Подробные инструкции

- [RTSP_CLIENT_ACTIVATION_GUIDE.md](RTSP_CLIENT_ACTIVATION_GUIDE.md) - Полное руководство
- [RTSP_BUILD_INSTRUCTIONS.md](RTSP_BUILD_INSTRUCTIONS.md) - Детальные инструкции по сборке

