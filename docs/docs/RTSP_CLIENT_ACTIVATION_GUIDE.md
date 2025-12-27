# Руководство по активации RTSP клиента

**Дата создания:** Декабрь 2025  
**Версия:** 1.0

## Обзор

Это руководство описывает шаги, необходимые для активации RTSP клиента после установки зависимостей и компиляции библиотеки.

---

## Предварительные требования

Перед активацией RTSP клиента необходимо:

1. ✅ Установить зависимости (FFmpeg, CMake, pkg-config)
2. ✅ Скомпилировать нативную библиотеку
3. ✅ Сгенерировать cinterop биндинги
4. ✅ Проверить экспорт символов

Подробные инструкции: [RTSP_BUILD_INSTRUCTIONS.md](RTSP_BUILD_INSTRUCTIONS.md)

---

## Шаг 1: Установка зависимостей

### macOS

```bash
# Установка Homebrew (если не установлен)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Установка зависимостей
brew install cmake ffmpeg pkg-config

# Проверка установки
cmake --version
pkg-config --exists libavformat && echo "FFmpeg OK" || echo "FFmpeg NOT FOUND"
```

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

---

## Шаг 2: Компиляция нативной библиотеки

```bash
# Из корневой директории проекта
./scripts/build-native-lib.sh

# Или вручную:
cd native/video-processing
mkdir -p build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
cmake --build . --config Release
```

**Проверка успешной компиляции:**

```bash
# Linux
ls -lh native/video-processing/build/libvideo_processing.so

# macOS
ls -lh native/video-processing/build/libvideo_processing.dylib
```

---

## Шаг 3: Проверка экспорта символов

**Linux:**
```bash
nm -D native/video-processing/build/libvideo_processing.so | grep rtsp_client
```

**macOS:**
```bash
nm -gU native/video-processing/build/libvideo_processing.dylib | grep rtsp_client
```

Должны быть видны символы:
- `rtsp_client_create` (или `_rtsp_client_create` на macOS)
- `rtsp_client_connect`
- `rtsp_client_play`
- `rtsp_client_stop`
- `rtsp_client_pause`
- и другие...

---

## Шаг 4: Копирование библиотеки в правильную директорию

```bash
# Создать директории
mkdir -p native/video-processing/lib/linux
mkdir -p native/video-processing/lib/macos

# Копировать библиотеки (Linux)
cp native/video-processing/build/libvideo_processing.so native/video-processing/lib/linux/ 2>/dev/null || true

# Копировать библиотеки (macOS)
cp native/video-processing/build/libvideo_processing.dylib native/video-processing/lib/macos/ 2>/dev/null || true
```

> **Примечание:** Скрипт `build-native-lib.sh` делает это автоматически.

---

## Шаг 5: Генерация cinterop биндингов

После компиляции библиотеки, скомпилируйте Kotlin/Native проект:

```bash
# Сборка для всех native платформ
./gradlew :core:network:compileKotlinNative

# Или для конкретных платформ:
./gradlew :core:network:compileKotlinLinuxX64
./gradlew :core:network:compileKotlinMacosX64
./gradlew :core:network:compileKotlinMacosArm64
```

**Проверка генерации биндингов:**

После успешной компиляции, биндинги будут сгенерированы в:
```
core/network/build/bin/native/.../klib/.../com/company/ipcamera/core/network/rtsp/rtsp_client/
```

Проверьте наличие файлов:
- `rtsp_client.klib`
- Сгенерированные Kotlin файлы с типами

---

## Шаг 6: Активация кода в NativeRtspClient.native.kt

После успешной генерации cinterop биндингов:

### 6.1 Раскомментировать импорты

Найти строку:
```kotlin
// import com.company.ipcamera.core.network.rtsp.rtsp_client.*
```

Изменить на:
```kotlin
import com.company.ipcamera.core.network.rtsp.rtsp_client.*
```

### 6.2 Раскомментировать реализацию методов

Для каждого метода с комментариями TODO, раскомментировать реализацию. Например:

**create():**
```kotlin
actual fun create(): NativeRtspClientHandle {
    val client = rtsp_client_create()
    return client?.rawValue?.toLong() ?: 0L
}
```

**connect():**
```kotlin
actual suspend fun connect(
    handle: NativeRtspClientHandle,
    url: String,
    username: String?,
    password: String?,
    timeoutMs: Int
): Boolean = withContext(Dispatchers.Default) {
    val client = handleToPointer(handle) ?: return@withContext false
    memScoped {
        val urlPtr = url.cstr.ptr
        val usernamePtr = username?.cstr?.ptr
        val passwordPtr = password?.cstr?.ptr
        return@withContext rtsp_client_connect(client, urlPtr, usernamePtr, passwordPtr, timeoutMs.convert())
    }
}
```

И так далее для всех методов.

### 6.3 Раскомментировать вспомогательные функции

В конце файла раскомментировать все вспомогательные функции конвертации:
- `handleToPointer()`
- `convertNativeStatus()`
- `convertNativeStreamType()`
- `convertStreamType()`
- `convertNativeFrame()` (для callbacks)

### 6.4 Реализовать callbacks

Реализовать методы `setFrameCallback()` и `setStatusCallback()` с использованием StableRef для thread-safety.

---

## Шаг 7: Проверка компиляции

После активации кода, проверьте компиляцию:

```bash
./gradlew :core:network:compileKotlinNative --info
```

Если есть ошибки:
1. Проверьте правильность имен сгенерированных типов
2. Убедитесь, что библиотека скопирована в правильную директорию
3. Проверьте пути в .def файле

---

## Шаг 8: Тестирование

### Базовый тест

```kotlin
val config = RtspClientConfig(
    url = "rtsp://test-camera:554/stream",
    username = "admin",
    password = "password"
)
val client = RtspClient(config)

// Подключение
client.connect()

// Проверка статуса
val status = client.getStatus().value
println("Status: $status")

// Воспроизведение
client.play()

// Ожидание кадров
client.getVideoFrames().collect { frame ->
    println("Received frame: ${frame.width}x${frame.height}")
}

// Остановка
client.stop()
client.disconnect()
client.close()
```

---

## Известные проблемы и решения

### Проблема: "Unresolved reference: rtsp_client_*"

**Причина:** cinterop биндинги не сгенерированы или не импортированы.

**Решение:**
1. Проверьте, что вы раскомментировали импорт
2. Запустите компиляцию снова: `./gradlew :core:network:compileKotlinNative`
3. Проверьте наличие сгенерированных файлов в `build/bin/`

### Проблема: "Library not found: video_processing"

**Причина:** Библиотека не найдена в указанном пути.

**Решение:**
1. Проверьте, что библиотека скопирована в `native/video-processing/lib/`
2. Проверьте пути в `.def` файле
3. Для macOS может потребоваться установить DYLD_LIBRARY_PATH (только для разработки)

### Проблема: Callbacks не вызываются

**Причина:** Неправильная реализация StableRef или callbacks не установлены.

**Решение:**
1. Убедитесь, что callbacks установлены перед вызовом `connect()`
2. Проверьте, что StableRef создается и правильно передается
3. Убедитесь, что callbacks не освобождаются преждевременно

---

## Следующие шаги после активации

1. ✅ Протестировать базовую функциональность (connect/play/stop)
2. ✅ Протестировать с реальной RTSP камерой
3. ✅ Проверить работу callbacks
4. ✅ Реализовать Android платформу (JNI)
5. ✅ Реализовать iOS платформу (cinterop)
6. ✅ Добавить unit тесты

---

## Связанные документы

- [RTSP_BUILD_INSTRUCTIONS.md](RTSP_BUILD_INSTRUCTIONS.md) - Инструкции по сборке
- [RTSP_CLIENT_IMPLEMENTATION_STATUS.md](RTSP_CLIENT_IMPLEMENTATION_STATUS.md) - Статус реализации
- [RTSP_CLIENT_INTEGRATION.md](RTSP_CLIENT_INTEGRATION.md) - Руководство по интеграции
- [RTSP_CLIENT.md](RTSP_CLIENT.md) - Общее описание RTSP клиента

---

**Последнее обновление:** Декабрь 2025

