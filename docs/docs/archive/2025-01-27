# Инструкции по сборке RTSP клиента

**Дата создания:** Декабрь 2025  
**Версия:** 1.0

## Обзор

Это руководство описывает процесс сборки и интеграции RTSP клиента для проекта IP-CSS.

---

## Шаг 1: Установка зависимостей

### Ubuntu/Debian

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
    libswresample-dev
```

### macOS

```bash
# Установка Homebrew (если не установлен)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Установка зависимостей
brew install cmake ffmpeg pkg-config
```

### Проверка установки

```bash
# Проверка CMake
cmake --version

# Проверка FFmpeg
pkg-config --exists libavformat && echo "FFmpeg OK" || echo "FFmpeg NOT FOUND"

# Проверка версии FFmpeg
pkg-config --modversion libavformat
```

---

## Шаг 2: Компиляция нативной библиотеки

### Автоматическая сборка (рекомендуется)

```bash
# Из корневой директории проекта
./scripts/build-native-lib.sh

# Для конкретной платформы:
./scripts/build-native-lib.sh linux
./scripts/build-native-lib.sh macos
```

Скрипт автоматически:
- Проверяет зависимости
- Создает необходимые директории
- Компилирует библиотеку
- Копирует результат в `native/video-processing/lib/`

### Ручная сборка

```bash
cd native/video-processing
mkdir -p build && cd build

# Конфигурация
cmake .. -DCMAKE_BUILD_TYPE=Release

# Сборка
cmake --build . --config Release -j$(nproc)

# Библиотека будет создана в:
# - Linux: build/libvideo_processing.so
# - macOS: build/libvideo_processing.dylib
```

---

## Шаг 3: Проверка экспорта символов

После сборки необходимо убедиться, что все символы правильно экспортированы:

### Linux

```bash
cd native/video-processing/build
nm -D libvideo_processing.so | grep rtsp_client
```

Ожидаемый вывод:
```
00000000000xxxxx T rtsp_client_connect
00000000000xxxxx T rtsp_client_create
00000000000xxxxx T rtsp_client_destroy
00000000000xxxxx T rtsp_client_disconnect
00000000000xxxxx T rtsp_client_get_status
00000000000xxxxx T rtsp_client_get_stream_count
00000000000xxxxx T rtsp_client_get_stream_info
00000000000xxxxx T rtsp_client_get_stream_type
00000000000xxxxx T rtsp_client_pause
00000000000xxxxx T rtsp_client_play
00000000000xxxxx T rtsp_client_set_frame_callback
00000000000xxxxx T rtsp_client_set_status_callback
00000000000xxxxx T rtsp_client_stop
```

### macOS

```bash
cd native/video-processing/build
nm -gU libvideo_processing.dylib | grep rtsp_client
```

Должны быть видны те же символы с префиксом `_` (например, `_rtsp_client_create`).

---

## Шаг 4: Настройка путей к библиотеке

Убедитесь, что библиотека находится в правильной директории:

```bash
# Создать директории для библиотек
mkdir -p native/video-processing/lib/linux
mkdir -p native/video-processing/lib/macos

# Скопировать библиотеки (если еще не скопированы скриптом)
cp native/video-processing/build/libvideo_processing.so native/video-processing/lib/linux/ 2>/dev/null || true
cp native/video-processing/build/libvideo_processing.dylib native/video-processing/lib/macos/ 2>/dev/null || true
```

---

## Шаг 5: Генерация cinterop биндингов

После компиляции нативной библиотеки, скомпилируйте Kotlin/Native проект для генерации cinterop биндингов:

```bash
# Сборка для всех native платформ
./gradlew :core:network:compileKotlinNative

# Или для конкретных платформ:
./gradlew :core:network:compileKotlinLinuxX64
./gradlew :core:network:compileKotlinMacosX64
./gradlew :core:network:compileKotlinMacosArm64
```

После успешной компиляции, cinterop сгенерирует биндинги в:
- `core/network/build/bin/native/.../klib/.../com/company/ipcamera/core/network/rtsp/rtsp_client/`

---

## Шаг 6: Активация реализации

После генерации cinterop биндингов:

1. **Раскомментируйте импорты** в `NativeRtspClient.native.kt`:
   ```kotlin
   // Было:
   // import com.company.ipcamera.core.network.rtsp.rtsp_client.*
   
   // Стало:
   import com.company.ipcamera.core.network.rtsp.rtsp_client.*
   ```

2. **Раскомментируйте реализацию методов** в `NativeRtspClient.native.kt`

3. **Реализуйте вспомогательные функции конвертации** (они уже подготовлены в комментариях)

---

## Шаг 7: Тестирование

### Базовое тестирование подключения

```bash
# Запуск тестов (после реализации)
./gradlew :core:network:test
```

### Тестирование с реальной RTSP камерой

Создайте простой тест:

```kotlin
val config = RtspClientConfig(
    url = "rtsp://your-camera-ip:554/stream",
    username = "admin",
    password = "password"
)
val client = RtspClient(config)

// Подключение
client.connect()

// Воспроизведение
client.play()

// Ожидание кадров
client.getVideoFrames().collect { frame ->
    println("Received frame: ${frame.width}x${frame.height}")
}

// Остановка
client.stop()
client.disconnect()
```

---

## Решение проблем

### Проблема: FFmpeg не найден

**Решение:**
```bash
# Проверить установку
pkg-config --exists libavformat || echo "FFmpeg not found"

# Установить FFmpeg
# Ubuntu/Debian:
sudo apt-get install libavformat-dev libavcodec-dev libavutil-dev libswscale-dev

# macOS:
brew install ffmpeg
```

### Проблема: Символы не экспортируются

**Решение:**
1. Убедитесь, что библиотека собрана как SHARED:
   ```cmake
   add_library(video_processing SHARED ...)
   ```

2. Проверьте visibility настройки в CMakeLists.txt

3. Для Linux добавьте:
   ```cmake
   set_target_properties(video_processing PROPERTIES
       LINK_FLAGS "-Wl,--export-dynamic"
   )
   ```

### Проблема: cinterop не генерирует биндинги

**Решение:**
1. Проверьте .def файл: `core/network/src/nativeInterop/cinterop/rtsp_client.def`
2. Убедитесь, что путь к заголовочным файлам правильный
3. Проверьте ошибки компиляции:
   ```bash
   ./gradlew :core:network:compileKotlinNative --info
   ```

### Проблема: Ошибки линковки

**Решение:**
1. Убедитесь, что библиотека находится в правильной директории
2. Проверьте linkerOpts в .def файле
3. Для macOS добавьте путь к библиотеке в DYLD_LIBRARY_PATH (только для разработки)

---

## Следующие шаги

После успешной сборки и активации реализации:

1. ✅ Протестируйте базовую функциональность (connect/play/stop)
2. ✅ Реализуйте Android платформу (JNI)
3. ✅ Реализуйте iOS платформу (cinterop)
4. ✅ Добавьте unit тесты
5. ✅ Интегрируйте с видеоплеером

---

## Связанные документы

- [RTSP_CLIENT.md](RTSP_CLIENT.md) - Общее описание RTSP клиента
- [RTSP_CLIENT_INTEGRATION.md](RTSP_CLIENT_INTEGRATION.md) - Руководство по интеграции
- [RTSP_CLIENT_IMPLEMENTATION_STATUS.md](RTSP_CLIENT_IMPLEMENTATION_STATUS.md) - Статус реализации
- [native/video-processing/README.md](../native/video-processing/README.md) - README нативной библиотеки

---

**Последнее обновление:** Декабрь 2025

