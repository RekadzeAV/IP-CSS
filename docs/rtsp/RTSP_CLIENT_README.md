# RTSP Client for IP-CSS

Нативный RTSP клиент для работы с IP-камерами в Kotlin Multiplatform проекте.

## 📋 Особенности

- ✅ **Кроссплатформенность**: Windows, Linux, macOS
- ✅ **Нативная производительность**: C++ библиотека с FFmpeg
- ✅ **Kotlin Multiplatform**: Единый API для всех платформ
- ✅ **Поддержка кодеков**: H.264, H.265, AAC, G.711
- ✅ **Автоматическое переподключение**: Exponential backoff с jitter
- ✅ **TCP/UDP транспорт**: Поддержка обоих режимов
- ✅ **Digest Authentication**: Полная поддержка аутентификации

## 🚀 Быстрый старт

### 1. Установка

```kotlin
// В build.gradle.kts
implementation("com.company.ipcamera:core-network:1.8.4")
```

### 2. Базовое использование

```kotlin
import com.company.ipcamera.core.network.*

// Создаем конфигурацию
val config = RtspClientConfig(
    url = "rtsp://192.168.1.100:554/stream",
    username = "admin",
    password = "password123",
    timeoutMillis = 10000,
    enableVideo = true,
    enableAudio = true,
    reconnectEnabled = true,
    reconnectMaxRetries = 5,
    reconnectInitialDelayMs = 1000,
    reconnectMaxDelayMs = 30000
)

// Создаем клиент
val client = RtspClient(config)

// Подключаемся
client.connect()

// Слушаем статус
client.getStatus().collect { status ->
    when (status) {
        RtspClientStatus.CONNECTED -> println("Подключено!")
        RtspClientStatus.PLAYING -> println("Воспроизведение начато")
        RtspClientStatus.ERROR -> println("Ошибка: ${client.getRuntimeDiagnostics().value.lastError}")
        else -> {}
    }
}

// Начинаем воспроизведение
client.play()

// Получаем видеокадры
client.getVideoFrames().collect { frame ->
    // frame.data - H.264/H.265 NAL units
    // frame.width, frame.height - размеры кадра
    // frame.timestamp - временная метка
    processVideoFrame(frame)
}

// Останавливаем
client.stop()
client.disconnect()
client.close()
```

## 📚 API Reference

### RtspClientConfig

Параметры конфигурации RTSP клиента:

| Параметр | Тип | По умолчанию | Описание |
|----------|-----|--------------|----------|
| `url` | String | - | RTSP URL камеры |
| `username` | String? | null | Имя пользователя |
| `password` | String? | null | Пароль |
| `timeoutMillis` | Int | 10000 | Таймаут подключения (мс) |
| `enableVideo` | Boolean | true | Включить видео |
| `enableAudio` | Boolean | true | Включить аудио |
| `reconnectEnabled` | Boolean | false | Автоматическое переподключение |
| `reconnectMaxRetries` | Int | 0 | Макс. попыток переподключения |
| `reconnectInitialDelayMs` | Int | 1000 | Начальная задержка (мс) |
| `reconnectMaxDelayMs` | Int | 30000 | Макс. задержка (мс) |
| `reconnectBackoffMultiplier` | Double | 2.0 | Множитель задержки |
| `allowSimulatedFallback` | Boolean | false | Разрешить симуляцию при отсутствии native |

### Основные методы

#### Подключение

```kotlin
suspend fun connect(): Unit
```
Подключиться к RTSP серверу. Асинхронная операция.

```kotlin
suspend fun reconnectWithBackoff(
    maxAttempts: Int = config.reconnectMaxRetries,
    initialDelayMs: Long = config.reconnectInitialDelayMs.toLong(),
    maxDelayMs: Long = config.reconnectMaxDelayMs.toLong(),
    backoffMultiplier: Double = config.reconnectBackoffMultiplier.toDouble()
): Boolean
```
Попытаться переподключиться с экспоненциальным backoff.

#### Воспроизведение

```kotlin
suspend fun play(): Unit
```
Начать воспроизведение потоков.

```kotlin
suspend fun pause(): Unit
```
Приостановить воспроизведение.

```kotlin
suspend fun stop(): Unit
```
Остановить воспроизведение.

#### Управление сессией

```kotlin
suspend fun disconnect(): Unit
```
Отключиться от сервера.

```kotlin
fun close(): Unit
```
Освободить все ресурсы.

#### Получение данных

```kotlin
fun getStatus(): StateFlow<RtspClientStatus>
```
Получить текущий статус подключения.

```kotlin
fun getVideoFrames(): SharedFlow<RtspFrame>
```
Получить поток видеокадров.

```kotlin
fun getAudioFrames(): SharedFlow<RtspFrame>
```
Получить поток аудиокадров.

```kotlin
fun getStreams(): List<RtspStreamInfo>
```
Получить список доступных потоков.

```kotlin
fun getRuntimeDiagnostics(): StateFlow<RtspRuntimeDiagnostics>
```
Получить диагностическую информацию.

### Callback-и

```kotlin
fun setVideoFrameCallback(callback: RtspFrameCallback?)
fun setAudioFrameCallback(callback: RtspFrameCallback?)
fun setStatusCallback(callback: RtspStatusCallback?)
```

Пример:
```kotlin
client.setStatusCallback { status, message ->
    when (status) {
        RtspClientStatus.CONNECTED -> logger.info("Connected")
        RtspClientStatus.ERROR -> logger.error("Error: $message")
        else -> {}
    }
}
```

## 🔧 Сборка нативных библиотек

### Требования

- **Windows**: Visual Studio 2022, CMake 3.20+, FFmpeg 6.0+
- **Linux**: GCC 11+, CMake 3.20+, FFmpeg 6.0+
- **macOS**: Xcode 14+, CMake 3.20+, FFmpeg 6.0+

### Автоматическая сборка (рекомендуется)

Используйте GitHub Actions workflow:

```bash
# Запустить сборку вручную
gh workflow run rtsp-native-build.yml
```

### Ручная сборка

#### Windows

```powershell
cd native/video-processing
mkdir build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release -DJAVA_HOME="$env:JAVA_HOME"
cmake --build . --config Release
```

#### Linux

```bash
cd native/video-processing
docker build -f Dockerfile.linux -t rtsp-builder .
docker create --name rtsp-build rtsp-builder
docker cp rtsp-build:/build/build/lib/linux/x64/libvideo_processing.so ./lib/linux/x64/
docker rm rtsp-build
```

#### macOS

```bash
cd native/video-processing
mkdir build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
cmake --build .
```

## 🧪 Тестирование

### Unit-тесты

```bash
# Все тесты
./gradlew :core:network:desktopTest

# Конкретный тест
./gradlew :core:network:desktopTest --tests "*RtspClientNativeMockTest*"
```

### Интеграционные тесты

```bash
# С RTSP сервером
export TEST_RTSP_URL=rtsp://localhost:8554/test
export TEST_RTSP_USERNAME=admin
export TEST_RTSP_PASSWORD=password123

./gradlew :core:network:desktopTest --tests "*RtspRealStreamTest*"
```

### Запуск RTSP сервера для тестов

```bash
# mediamtx (rtsp-simple-server)
docker run -d --name rtsp-test -p 8554:8554 bluenviron/mediamtx:latest

# Создать тестовый поток
ffmpeg -re -i test.mp4 -c copy -f rtsp rtsp://localhost:8554/test
```

## ⚠️ Известные проблемы и решения

### Ошибка: "Native library not found"

**Решение:** Убедитесь, что библиотека `video_processing.dll` (Windows) или `libvideo_processing.so` (Linux) находится в CLASSPATH или директории системы.

### Ошибка: "Connection timeout"

**Возможные причины:**
1. Камера недоступна по сети
2. Неправильный URL
3. Брандмауэр блокирует порт 554

**Решение:**
```bash
# Проверить доступность
telnet 192.168.1.100 554

# Проверить URL
ffplay rtsp://192.168.1.100:554/stream
```

### Ошибка: "Unsupported codec"

**Решение:** Убедитесь, что камера использует поддерживаемые кодеки (H.264, H.265, AAC, G.711).

### Высокая задержка

**Решение:**
1. Использовать TCP транспорт вместо UDP
2. Уменьшить размер буфера
3. Проверить сетевую конфигурацию

```kotlin
val config = RtspClientConfig(
    url = "rtsp://...",
    // Добавить параметры для низкой задержки
)
```

## 📊 Производительность

### Рекомендации

- **Количество камер**: До 10 одновременно на одном ядре CPU
- **Задержка**: 200-500 мс (зависит от сети и настроек камеры)
- **Использование памяти**: ~50 MB на поток видео
- **Использование CPU**: ~10-20% на поток H.264 1080p

### Мониторинг

```kotlin
val diagnostics = client.getRuntimeDiagnostics().value
println("Подключено: ${diagnostics.connectSuccesses} раз")
println("Ошибки: ${diagnostics.connectFailures}")
println("Последняя ошибка: ${diagnostics.lastError}")
println("Задержка: ${diagnostics.averageLatencyMs} мс")
```

## 🔒 Безопасность

### Аутентификация

Клиент поддерживает:
- **Basic Authentication**
- **Digest Authentication** (рекомендуется)

```kotlin
val config = RtspClientConfig(
    url = "rtsp://camera/stream",
    username = "admin",
    password = "secure_password" // Используйте HTTPS для управления
)
```

### Шифрование

Для RTSP over TLS (RTSPS) используйте `rtsps://` схему URL:

```kotlin
val config = RtspClientConfig(
    url = "rtsps://camera:554/stream"
)
```

## 📝 Частые сценарии использования

### Мультикамерная система

```kotlin
class MultiCameraSystem {
    private val cameras = mutableMapOf<String, RtspClient>()
    
    fun addCamera(id: String, url: String) {
        val client = RtspClient(RtspClientConfig(url = url))
        cameras[id] = client
        
        // Подключиться ко всем камерам
        coroutineScope.launch {
            client.connect()
            client.play()
        }
    }
    
    fun getVideoStream(cameraId: String): SharedFlow<RtspFrame>? {
        return cameras[cameraId]?.getVideoFrames()
    }
}
```

### Автоматическое восстановление при ошибках

```kotlin
suspend fun connectWithRetry(client: RtspClient, maxAttempts: Int = 3) {
    repeat(maxAttempts) { attempt ->
        try {
            client.connect()
            client.play()
            return // Успех
        } catch (e: Exception) {
            logger.warn("Попытка $attempt/${maxAttempts} failed: ${e.message}")
            delay(1000L * (attempt + 1)) // Экспоненциальная задержка
        }
    }
    throw IllegalStateException("Не удалось подключиться после $maxAttempts попыток")
}
```

### Запись видео на диск

```kotlin
suspend fun recordToDisk(client: RtspClient, outputPath: String) {
    val file = File(outputPath)
    val outputStream = file.outputStream()
    
    client.getVideoFrames().collect { frame ->
        // Запись H.264/NAL units в файл
        outputStream.write(frame.data)
    }
    
    outputStream.close()
}
```

## 📖 Дополнительные ресурсы

- [RTSP Protocol Specification](https://www.ietf.org/rfc/rfc2326.txt)
- [FFmpeg Documentation](https://ffmpeg.org/documentation.html)
- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)

## 🤝 Вклад

См. [CONTRIBUTING.md](../../CONTRIBUTING.md) для деталей.

## 📄 Лицензия

См. [LICENSE](../../LICENSE) для деталей.

---

**Версия:** 1.8.4  
**Последнее обновление:** 24 мая 2026
