# RTSP Client Documentation

**Версия:** 1.0  
**Статус:** В разработке  
**Последнее обновление:** 2026-05-25

---

## 📖 Описание

Нативный RTSP клиент для Kotlin Multiplatform проекта IP-CSS. Обеспечивает низкоуровневую интеграцию с нативной C++ библиотекой для работы с видеопотоками IP камер.

### Особенности:

- ✅ Поддержка H.264/H.265 видео
- ✅ Аудио кодеки: AAC, PCMU, PCMA
- ✅ Автоматическое переподключение
- ✅ Низкая задержка (< 500 мс)
- ✅ Поддержка множественных потоков
- ✅ Кросс-платформенная поддержка (Windows, Linux, macOS, Android, iOS)

---

## 🏗️ Архитектура

### Компоненты:

```
┌─────────────────────────────────────────────────────────┐
│                    Kotlin Layer                          │
│  ┌─────────────────────────────────────────────────┐    │
│  │           RtspClient (commonMain)                │    │
│  │  - Обертка высокого уровня                       │    │
│  │  - Управление состоянием                         │    │
│  │  - Callbacks и Flow                              │    │
│  └─────────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────────┐    │
│  │     NativeRtspClient (nativeMain)                │    │
│  │  - FFI биндинги                                  │    │
│  │  - Маппинг типов                                 │    │
│  │  - C callback handlers                           │    │
│  └─────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│                    FFI Layer                             │
│  ┌─────────────────────────────────────────────────┐    │
│  │         cinterop/rtsp_client.def                 │    │
│  │  - Конфигурация C interop                        │    │
│  └─────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│                   Native Layer                           │
│  ┌─────────────────────────────────────────────────┐    │
│  │       video_processing.dll / .so / .dylib        │    │
│  │  - RTSP клиент на C++                            │    │
│  │  - FFmpeg декодирование                          │    │
│  │  - RTP обработка                                 │    │
│  └─────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│                  FFmpeg Dependencies                     │
│  - libavcodec     (декодирование)                        │
│  - libavformat    (форматы)                              │
│  - libavutil      (утилиты)                              │
│  - libswscale     (масштабирование)                      │
│  - libswresample  (ресемплинг аудио)                     │
└─────────────────────────────────────────────────────────┘
```

---

## 🚀 Быстрый старт

### 1. Установка зависимостей

**Windows:**
```powershell
# FFmpeg
choco install ffmpeg

# Проверка
ffmpeg -version
```

**Linux:**
```bash
sudo apt-get install ffmpeg libavcodec-dev libavformat-dev libavutil-dev
```

**macOS:**
```bash
brew install ffmpeg
```

### 2. Сборка нативной библиотеки

**Windows:**
```powershell
.\scripts\build-video-processing-lib.ps1
```

**Linux/macOS:**
```bash
./scripts/build-video-processing-lib.sh
```

### 3. Использование в коде

**Mock режим (для разработки и тестирования):**

```kotlin
// Явное создание mock клиента
val client = MockRtspClient.create()

// Или через factory в mock режиме
RtspClientFactory.setMode(RtspClientFactory.Mode.MOCK)
val client = RtspClientFactory.create()

// Использование как обычный клиент
client.connect("rtsp://192.168.1.100:554/stream")
client.play()
```

**Native режим (требует FFI биндингов):**

```kotlin
// После успешной компиляции FFI
RtspClientFactory.setMode(RtspClientFactory.Mode.NATIVE)
val client = RtspClientFactory.create()

// Или напрямую
val client = createRtspClient()
```

**Автоматическое переключение:**

```kotlin
// Factory вернет native если доступен, иначе mock
val client = RtspClientFactory.create() ?: MockRtspClient.create()
```

**Базовое подключение:**
```kotlin
val client = RtspClient(
    RtspClientConfig(
        url = "rtsp://192.168.1.100:554/stream",
        username = "admin",
        password = "password123",
        timeoutMillis = 10000
    )
)

// Подключение
client.connect()

// Установить callback для видео
client.setVideoFrameCallback { frame ->
    println("Received frame: ${frame.data.size} bytes")
}

// Начать воспроизведение
client.play()

// Остановить
client.stop()

// Отключиться
client.disconnect()
```

**С мониторингом статуса:**
```kotlin
val client = RtspClient(config)

client.getStatus().collect { status ->
    when (status) {
        RtspClientStatus.DISCONNECTED -> println("Disconnected")
        RtspClientStatus.CONNECTING -> println("Connecting...")
        RtspClientStatus.CONNECTED -> println("Connected")
        RtspClientStatus.PLAYING -> println("Playing")
        RtspClientStatus.ERROR -> println("Error!")
    }
}

client.connect()
```

---

## 📚 API Reference

### RtspClientConfig

```kotlin
data class RtspClientConfig(
    val url: String,                    // RTSP URL камеры
    val username: String? = null,       // Имя пользователя (опционально)
    val password: String? = null,       // Пароль (опционально)
    val timeoutMillis: Long = 10000,    // Таймаут подключения (мс)
    val bufferSize: Int = 1024 * 1024,  // Размер буфера (1MB)
    val enableAudio: Boolean = true,    // Включить аудио
    val enableVideo: Boolean = true,    // Включить видео
    val enableMetadata: Boolean = false,// Включить метаданные
    val reconnectEnabled: Boolean = true,    // Автопереподключение
    val reconnectMaxRetries: Int = 5,        // Максимум попыток (0 = бесконечно)
    val reconnectInitialDelayMs: Int = 500,  // Начальная задержка
    val reconnectMaxDelayMs: Int = 10_000,   // Максимальная задержка
    val reconnectBackoffMultiplier: Float = 2.0f  // Множитель задержки
)
```

### Основные методы

| Метод | Описание | Возвращает |
|-------|----------|------------|
| `connect()` | Подключение к RTSP серверу | Unit |
| `disconnect()` | Отключение от сервера | Unit |
| `play()` | Начало воспроизведения | Unit |
| `stop()` | Остановка воспроизведения | Unit |
| `pause()` | Пауза воспроизведения | Unit |
| `getStatus()` | Получение статуса | StateFlow\<RtspClientStatus\> |
| `getStreams()` | Список потоков | List\<RtspStreamInfo\> |
| `setVideoFrameCallback()` | Callback для видео | Unit |
| `setAudioFrameCallback()` | Callback для аудио | Unit |
| `setStatusCallback()` | Callback статуса | Unit |

### Типы данных

```kotlin
enum class RtspClientStatus {
    DISCONNECTED,    // Не подключен
    CONNECTING,      // Подключение в процессе
    CONNECTED,       // Подключен
    PLAYING,         // Воспроизведение
    ERROR            // Ошибка
}

enum class RtspStreamType {
    VIDEO,           // Видео поток
    AUDIO,           // Аудио поток
    METADATA         // Метаданные
}

data class RtspFrame(
    val data: ByteArray,       // Данные кадра
    val timestamp: Long,       // Временная метка (мс)
    val streamType: RtspStreamType,
    val width: Int = 0,        // Ширина (для видео)
    val height: Int = 0        // Высота (для видео)
)

data class RtspStreamInfo(
    val index: Int,
    val type: RtspStreamType,
    val resolution: Resolution?,  // Разрешение
    val fps: Int,                 // Кадров в секунду
    val codec: String,            // Видео кодек
    val audioCodec: String? = null,  // Аудио кодек
    val sampleRate: Int? = null,     // Частота дискретизации
    val channels: Int? = null        // Количество каналов
)
```

---

## 🔧 Конфигурация

### FFI конфигурация

**Файл:** `core/network/src/nativeInterop/cinterop/rtsp_client.def`

```
language = C
headers = rtsp_client.h
headerFilter = rtsp_client.h
package = com.company.ipcamera.core.network.rtsp
compilerOpts = -I${project.rootDir}/native/video-processing/include
linkerOpts = -L${project.rootDir}/native/video-processing/lib/windows/x64 -lvideo_processing
```

### Поддерживаемые платформы

| Платформа | Статус | Библиотека |
|-----------|--------|------------|
| Windows x64 | ✅ Готово | video_processing.dll |
| Linux x64 | ⏳ В планах | libvideo_processing.so |
| macOS x64 | ⏳ В планах | libvideo_processing.dylib |
| macOS ARM64 | ⏳ В планах | libvideo_processing.dylib |
| Android ARM64 | ⏳ В планах | libvideo_processing.so |
| iOS x64 | ⏳ В планах | libvideo_processing.framework |
| iOS ARM64 | ⏳ В планах | libvideo_processing.framework |

---

## 🧪 Тестирование

### Запуск тестов

```bash
# Unit тесты
./gradlew :core:network:test

# Интеграционные тесты
./gradlew :core:network:integrationTest

# Проверка библиотеки
.\scripts\test-rtsp-library.ps1
```

### Тестовые сценарии

См. [`docs/testing/RTSP_INTEGRATION_TEST_PLAN.md`](../testing/RTSP_INTEGRATION_TEST_PLAN.md)

---

## 🐛 Устранение проблем

### Проблема: "Native RTSP unavailable"

**Причина:** Нативная библиотека не найдена или не загружена

**Решение:**
1. Проверить существование библиотеки:
   ```powershell
   Test-Path "native\video-processing\lib\windows\x64\video_processing.dll"
   ```

2. Пересобрать библиотеку:
   ```powershell
   .\scripts\build-video-processing-lib.ps1
   ```

3. Проверить зависимости FFmpeg:
   ```powershell
   .\scripts\test-rtsp-library.ps1
   ```

### Проблема: "Connection timeout"

**Причины:**
- Камера недоступна в сети
- Неправильный RTSP URL
- Брандмауэр блокирует подключение

**Решение:**
1. Проверить доступность камеры:
   ```bash
   ping 192.168.1.100
   ```

2. Проверить RTSP URL:
   ```bash
   ffplay rtsp://192.168.1.100:554/stream
   ```

3. Проверить порт (по умолчанию 554):
   ```powershell
   Test-NetConnection 192.168.1.100 -Port 554
   ```

### Проблема: "No video frames"

**Причины:**
- Неправильный кодек
- Камера не поддерживает выбранный формат
- Недостаточно ресурсов

**Решение:**
1. Проверить поддерживаемые кодеки камеры
2. Убедиться, что H.264/H.265 поддерживается FFmpeg
3. Проверить потребление памяти и CPU

---

## 📊 Метрики производительности

### Ожидаемые показатели:

| Метрика | Значение |
|---------|----------|
| Время подключения | < 10 секунд |
| Задержка видео | < 500 мс |
| FPS стабильность | > 95% |
| Потребление памяти | < 200 MB |
| CPU использование | < 30% |

---

## 🔗 Ссылки

### Документация:
- [RTSP Protocol](https://www.ietf.org/rfc/rfc2326.txt)
- [FFmpeg Documentation](https://ffmpeg.org/documentation.html)
- [Kotlin/Native FFI](https://kotlinlang.org/docs/native-ffi.html)

### Исходный код:
- [RTSP Client Implementation](../../native/video-processing/src/rtsp_client.cpp)
- Kotlin Wrapper *(утерян/в архиве)*
- [FFI Configuration](../../core/network/src/nativeInterop/cinterop/rtsp_client.def)

### Тестирование:
- [Integration Test Plan](../testing/RTSP_INTEGRATION_TEST_PLAN.md)
- [Test Scripts](../../scripts/test-rtsp-library.ps1)

---

## 📝 История версий

### 1.0.0 (2026-05-25)
- ✅ Базовая реализация RTSP клиента
- ✅ Поддержка H.264/H.265
- ✅ Автоматическое переподключение
- ✅ FFI интеграция
- ✅ Тестовые скрипты

---

**Поддерживается:** NLP-Core-Team  
**Контакты:** team@company.com
