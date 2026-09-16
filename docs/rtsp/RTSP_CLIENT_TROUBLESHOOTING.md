# RTSP Client Troubleshooting Guide

Руководство по устранению проблем с RTSP клиентом.

## 🚨 Распространенные проблемы

### 1. Ошибка подключения

#### Проблема: `Connection timeout`

**Симптомы:**
```
Status: ERROR
Last error: Connection timeout after 10000ms
```

**Возможные причины:**
1. Камера недоступна по сети
2. Неправильный RTSP URL
3. Порт 554 заблокирован брандмауэром
4. Камера требует аутентификацию

**Решение:**

1. **Проверьте доступность камеры:**
   ```bash
   # Ping камеры
   ping 192.168.1.100
   
   # Проверка порта
   telnet 192.168.1.100 554
   
   # Или с помощью ffmpeg
   ffprobe rtsp://192.168.1.100:554/stream
   ```

2. **Проверьте RTSP URL:**
   - Правильный формат: `rtsp://[user:pass@]host:port/path`
   - Примеры для разных камер:
     ```
     Hikvision: rtsp://admin:password@192.168.1.64:554/Streaming/Channels/101
     Dahua: rtsp://admin:password@192.168.1.108:554/cam/realmonitor?channel=1&subtype=0
     Reolink: rtsp://admin:password@192.168.1.202:554/h264Preview_01_main
     Axis: rtsp://192.168.1.90/axis-media/media.amp
     ```

3. **Проверьте брандмауэр:**
   ```powershell
   # Windows: открыть порт 554
   New-NetFirewallRule -DisplayName "RTSP Port" -Direction Inbound -LocalPort 554 -Protocol TCP -Action Allow
   ```

4. **Проверьте аутентификацию:**
   ```kotlin
   val config = RtspClientConfig(
       url = "rtsp://192.168.1.100:554/stream",
       username = "admin",
       password = "correct_password"
   )
   ```

---

### 2. Ошибка "Native library not found"

#### Проблема: UnsatisfiedLinkError

**Симптомы:**
```
java.lang.UnsatisfiedLinkError: no video_processing in java.library.path
```

**Причины:**
- Нативная библиотека не найдена в CLASSPATH
- Неправильная платформа (например, Linux библиотека на Windows)

**Решение:**

1. **Убедитесь, что библиотека собрана:**
   ```powershell
   # Проверить наличие DLL
   ls native/video-processing/lib/windows/x64/video_processing.dll
   
   # Или Linux
   ls native/video-processing/lib/linux/x64/libvideo_processing.so
   ```

2. **Скопируйте библиотеку в правильное место:**
   ```
   Windows: build/classes/kotlin/jvm/main/video_processing.dll
   Linux:   build/classes/kotlin/jvm/main/libvideo_processing.so
   ```

3. **Или добавьте в java.library.path:**
   ```bash
   java -Djava.library.path=./native/video-processing/lib/windows/x64 -jar app.jar
   ```

4. **Проверьте архитектуру:**
   - Убедитесь, что библиотека соответствует платформе (x64 vs x86)
   - Windows x64 требует 64-битную JDK

---

### 3. Нет видеокадров

#### Проблема: Поток подключен, но кадров нет

**Симптомы:**
```
Status: PLAYING
Video frames: 0
```

**Возможные причины:**
1. Камера не отправляет видео
2. Неподдерживаемый кодек
3. Проблемы с RTP

**Решение:**

1. **Проверьте кодек камеры:**
   ```bash
   ffprobe rtsp://camera/stream
   ```
   Поддерживаемые кодеки: H.264, H.265

2. **Измените субпоток (если доступен):**
   ```kotlin
   // Основной поток (высокое качество)
   val url = "rtsp://camera/Streaming/Channels/101"
   
   // Субпоток (низкое качество, меньше задержка)
   val url = "rtsp://camera/Streaming/Channels/102"
   ```

3. **Проверьте настройки RTP:**
   ```kotlin
   val config = RtspClientConfig(
       url = "rtsp://...",
       // Попробовать TCP транспорт
       transportMode = RTSP_TRANSPORT_TCP
   )
   ```

4. **Перезагрузите камеру:**
   - Иногда камера "зависает" и перестает отправлять потоки

---

### 4. Высокая задержка

#### Проблема: Задержка > 2 секунд

**Причины:**
1. Использование UDP вместо TCP
2. Большой буфер на камере
3. Проблемы сети

**Решение:**

1. **Используйте TCP транспорт:**
   ```kotlin
   val config = RtspClientConfig(
       url = "rtsp://...",
       transportMode = RTSP_TRANSPORT_TCP // Вместо UDP
   )
   ```

2. **Настройте камеру на низкую задержку:**
   - Отключите VBR/CRF
   - Уменьшите GOP size
   - Включите "Low Latency" режим

3. **Уменьшите буферы:**
   ```kotlin
   // В C++ коде (rtsp_client.cpp)
   client->bufferSize = 64 * 1024; // Вместо 1MB
   ```

4. **Проверьте сеть:**
   ```bash
   # Тест скорости и задержки
   ping -n 100 192.168.1.100
   
   # Проверка потери пакетов
   pathping 192.168.1.100
   ```

---

### 5. Автоматическое переподключение не работает

#### Проблема: После обрыва соединения нет реконнекта

**Причины:**
- `reconnectEnabled = false` в конфигурации
- Достижен лимит попыток

**Решение:**

1. **Включите переподключение:**
   ```kotlin
   val config = RtspClientConfig(
       url = "rtsp://...",
       reconnectEnabled = true,
       reconnectMaxRetries = 5,
       reconnectInitialDelayMs = 1000,
       reconnectMaxDelayMs = 30000
   )
   ```

2. **Проверьте статус реконнекта:**
   ```kotlin
   val diagnostics = client.getRuntimeDiagnostics().value
   println("Попытки реконнекта: ${diagnostics.reconnectAttempts}")
   println("Успешные реконнекты: ${diagnostics.reconnectSuccesses}")
   println("Неудачные реконнекты: ${diagnostics.reconnectFailures}")
   ```

3. **Ручной реконнект:**
   ```kotlin
   if (status.value == RtspClientStatus.ERROR) {
       val success = client.reconnectWithBackoff()
       if (!success) {
           logger.error("Не удалось переподключиться")
       }
   }
   ```

---

### 6. Ошибка декодирования аудио

#### Проблема: Видео работает, аудио нет

**Симптомы:**
```
Audio frames: 0
Codec: AAC (но нет звука)
```

**Причины:**
1. Аудиокодек не поддерживается
2. FFmpeg собран без аудио поддержки

**Решение:**

1. **Проверьте аудиокодек камеры:**
   ```bash
   ffprobe rtsp://camera/stream 2>&1 | findstr Audio
   ```
   Поддерживаемые: AAC, G.711 (PCMU/PCMA)

2. **Включите аудио в конфигурации:**
   ```kotlin
   val config = RtspClientConfig(
       enableAudio = true
   )
   ```

3. **Пересоберите библиотеку с аудио поддержкой:**
   ```bash
   # Linux с FFmpeg 7+
   docker build -f Dockerfile.linux --build-arg ENABLE_AUDIO=ON -t rtsp-builder .
   ```

---

### 7. Утечка памяти

#### Проблема: Память растет со временем

**Симптомы:**
- Heap usage постоянно увеличивается
- GC не освобождает память

**Причины:**
- Не закрыт клиент (`.close()` не вызван)
- Callback-и создают циклические ссылки

**Решение:**

1. **Всегда закрывайте клиент:**
   ```kotlin
   val client = RtspClient(config)
   try {
       client.connect()
       client.play()
       // работа...
   } finally {
       client.disconnect()
       client.close() // Обязательно!
   }
   ```

2. **Используйте try-with-resources (Kotlin):**
   ```kotlin
   client.use {
       it.connect()
       it.play()
   } // автоматически закроется
   ```

3. **Очистите callback-и:**
   ```kotlin
   client.setVideoFrameCallback(null)
   client.setStatusCallback(null)
   ```

---

## 🔍 Диагностика

### Включить детальное логирование

```kotlin
// Установите уровень логирования
System.setProperty("ch.qos.logback.classic.Logger", "DEBUG")

// Или в logback.xml
<configuration>
    <logger name="com.company.ipcamera.core.network" level="DEBUG"/>
</configuration>
```

### Получить диагностическую информацию

```kotlin
val diagnostics = client.getRuntimeDiagnostics().value
println("Статус: ${diagnostics.status}")
println("Попытки подключения: ${diagnostics.connectAttempts}")
println("Успешные подключения: ${diagnostics.connectSuccesses}")
println("Ошибки подключения: ${diagnostics.connectFailures}")
println("Попытки реконнекта: ${diagnostics.reconnectAttempts}")
println("Последняя ошибка: ${diagnostics.lastError}")
println("Время работы: ${diagnostics.uptimeMs} мс")
```

### Проверка нативной библиотеки

```bash
# Windows
dumpbin /exports video_processing.dll | findstr rtsp_client

# Linux
nm -D libvideo_processing.so | grep rtsp_client

# Должны быть видны символы:
# rtsp_client_create
# rtsp_client_connect
# rtsp_client_play
# rtsp_client_destroy
```

---

## 🛠️ Инструменты отладки

### VLC Media Player

Проверка потока вне приложения:
```bash
vlc rtsp://admin:password@192.168.1.100:554/stream
```

### ffprobe/ffmpeg

Детальная информация о потоке:
```bash
ffprobe -v verbose -print_format json rtsp://camera/stream
```

Просмотр потока в реальном времени:
```bash
ffmpeg -i rtsp://camera/stream -c copy -f null -
```

### Wireshark

Анализ сетевого трафика:
```bash
# Фильтр RTSP
wireshark -k -Y "rtsp or rtp"

# Экспорт RTP потоков
File -> Export Objects -> RTP
```

### RTSP Testing Tools

- **rtsp-simple-server** (mediamtx) — тестовый RTSP сервер
- **GStreamer** — проверка потоков
  ```bash
  gst-launch-1.0 -v rtspsrc location=rtsp://camera/stream ! rtph264depay ! avdec_h264 ! fakesink
  ```

---

## 📞 Получение помощи

### Перед обращением за поддержкой

Соберите следующую информацию:

1. **Версии:**
   - Версия библиотеки (например, 1.8.4)
   - Версия Kotlin (например, 1.9.23)
   - Версия FFmpeg (например, 6.1)
   - ОС и архитектура (например, Windows 11 x64)

2. **Конфигурация:**
   ```kotlin
   println(config) // Выведите конфигурацию
   ```

3. **Логи:**
   - Full logs от запуска до ошибки
   - Diagnostic snapshot

4. **Сеть:**
   - ping тест к камере
   - telnet проверка порта

5. **Воспроизведение:**
   - Шаги для воспроизведения проблемы
   - Минимальный пример кода

### Контакты

- **GitHub Issues:** Откройте issue с тегами `bug` и `troubleshooting`
- **Документация:** См. [RTSP_CLIENT_README.md](RTSP_CLIENT_README.md)
- **Примеры:** См. [RTSP_CLIENT_EXAMPLES.md](RTSP_CLIENT_EXAMPLES.md)

---

**Версия guide:** 1.0  
**Последнее обновление:** 24 мая 2026
