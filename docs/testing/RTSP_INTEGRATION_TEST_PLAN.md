# План интеграционных тестов RTSP клиента

**Дата:** 2026-05-25  
**Статус:** Подготовка к тестированию  
**Цель:** Проверка работы RTSP клиента с реальными камерами

---

## 📋 Тестовая среда

### 1. Тестовое оборудование

**Требуется:**
- [ ] IP камера с RTSP (H.264/H.265)
- [ ] Локальная сеть (LAN)
- [ ] Компьютер для тестирования

**Рекомендуемые камеры:**
- Hikvision DS-2CD2xxx
- Dahua IPC-HFWxxxx
- Axis P32xx
- Any generic ONVIF-compatible camera

### 2. Тестовый RTSP сервер (для разработки)

**FFmpeg тестовый сервер:**
```bash
# Генерация тестового видеопотока
ffmpeg -re -f lavfi -i testsrc=duration=0:size=1920x1080:rate=25 \
       -c:v libx264 -preset ultrafast -tune zerolatency \
       -f rtsp rtsp://localhost:8554/live
```

**VLC тестовый сервер:**
```bash
# Стрим тестового паттерна
vlc vlc:// --sout '#rtp{sdp=rtsp://:8554/live}'
```

### 3. Docker контейнер для тестирования

**docker-compose.yml:**
```yaml
version: '3.8'
services:
  rtsp-test-server:
    image: binwiederhier/rtsp-simple-server
    ports:
      - "8554:8554"  # RTSP
      - "8889:8889"  # HTTP API
    environment:
      - RTSP_ADDRESS=0.0.0.0:8554
```

---

## 🧪 Тестовые сценарии

### Сценарий 1: Базовое подключение

**Цель:** Проверить успешное подключение к RTSP серверу

**Шаги:**
1. Создать экземпляр RtspClient
2. Установить конфигурацию с URL камеры
3. Вызвать connect()
4. Проверить статус CONNECTED
5. Вызвать disconnect()

**Ожидаемый результат:**
- Подключение успешно
- Статус меняется: DISCONNECTED → CONNECTING → CONNECTED
- Время подключения < 10 секунд

**Тест:**
```kotlin
@Test
fun testBasicConnection() = runTest {
    val client = RtspClient(
        RtspClientConfig(
            url = "rtsp://192.168.1.100:554/stream",
            username = "admin",
            password = "password123",
            timeoutMillis = 10000
        )
    )
    
    client.connect()
    val status = client.getStatus().first()
    assertEquals(RtspClientStatus.CONNECTED, status)
    
    client.disconnect()
}
```

### Сценарий 2: Воспроизведение видео

**Цель:** Проверить получение видеокадров

**Шаги:**
1. Подключиться к камере
2. Вызвать play()
3. Установить callback для видеокадров
4. Проверить получение кадров
5. Вызвать stop()

**Ожидаемый результат:**
- Кадры приходят с частотой ~25 FPS
- Формат H.264/H.265
- Timestamp монотонно увеличивается

**Тест:**
```kotlin
@Test
fun testVideoPlayback() = runTest {
    val client = RtspClient(RtspClientConfig(url = "rtsp://..."))
    client.connect()
    
    var frameCount = 0
    client.setVideoFrameCallback { frame ->
        frameCount++
        assertTrue(frame.data.isNotEmpty())
        assertTrue(frame.timestamp > 0)
    }
    
    client.play()
    delay(5000)  // 5 секунд
    
    assertTrue(frameCount > 100)  // Минимум 100 кадров за 5 секунд
    client.stop()
}
```

### Сценарий 3: Аудио поток

**Цель:** Проверить работу аудио

**Шаги:**
1. Подключиться с enableAudio = true
2. Вызвать play()
3. Проверить получение аудиокадров

**Ожидаемый результат:**
- Аудиокадры приходят
- Формат AAC/PCMU/PCMA
- Sample rate соответствует камере

**Тест:**
```kotlin
@Test
fun testAudioStream() = runTest {
    val client = RtspClient(
        RtspClientConfig(
            url = "rtsp://...",
            enableAudio = true
        )
    )
    client.connect()
    client.play()
    
    var audioFrames = 0
    client.setAudioFrameCallback { frame ->
        audioFrames++
        assertTrue(frame.data.isNotEmpty())
    }
    
    delay(3000)
    assertTrue(audioFrames > 50)
}
```

### Сценарий 4: Переподключение

**Цель:** Проверить автоматическое переподключение

**Шаги:**
1. Подключиться к камере
2. Имитировать разрыв сети
3. Проверить автоматическое переподключение
4. Проверить восстановление потока

**Ожидаемый результат:**
- Переподключение в течение 5-10 секунд
- Минимальная потеря кадров
- Статус: CONNECTED → DISCONNECTED → CONNECTED

**Тест:**
```kotlin
@Test
fun testAutoReconnect() = runTest {
    val client = RtspClient(
        RtspClientConfig(
            url = "rtsp://...",
            reconnectEnabled = true,
            reconnectMaxRetries = 5,
            reconnectInitialDelayMs = 1000
        )
    )
    
    client.connect()
    client.play()
    
    // Имитировать разрыв
    // (в реальном тесте - отключить сеть)
    
    val statuses = mutableListOf<RtspClientStatus>()
    client.getStatus().collect { statuses.add(it) }
    
    // Проверить последовательность статусов
    assertTrue(statuses.contains(RtspClientStatus.DISCONNECTED))
    assertTrue(statuses.contains(RtspClientStatus.CONNECTED))
}
```

### Сценарий 5: Обработка ошибок

**Цель:** Проверить обработку различных ошибок

**Тесты:**
1. Неправильный URL
2. Неправильные учетные данные
3. Камера недоступна
4. Таймаут подключения
5. Недостаточно ресурсов

**Ожидаемый результат:**
- Ошибки корректно обрабатываются
- Статус меняется на ERROR
- Сообщение об ошибке доступно
- Возможность повторной попытки

**Тест:**
```kotlin
@Test
fun testInvalidCredentials() = runTest {
    val client = RtspClient(
        RtspClientConfig(
            url = "rtsp://192.168.1.100:554/stream",
            username = "wrong_user",
            password = "wrong_pass"
        )
    )
    
    client.connect()
    
    val status = client.getStatus().first { it == RtspClientStatus.ERROR }
    assertEquals(RtspClientStatus.ERROR, status)
}
```

### Сценарий 6: Пауза/Возобновление

**Цель:** Проверить работу паузы

**Шаги:**
1. Подключиться и начать воспроизведение
2. Вызвать pause()
3. Проверить остановку кадров
4. Вызвать play()
5. Проверить возобновление

**Ожидаемый результат:**
- Кадры прекращают приходить при паузе
- Воспроизведение возобновляется с текущей позиции
- Минимальная задержка при возобновлении

**Тест:**
```kotlin
@Test
fun testPauseResume() = runTest {
    val client = RtspClient(RtspClientConfig(url = "rtsp://..."))
    client.connect()
    client.play()
    
    var frameCountBefore = 0
    client.setVideoFrameCallback { frameCountBefore++ }
    
    delay(2000)
    val framesAtPause = frameCountBefore
    
    client.pause()
    delay(2000)
    
    assertTrue(frameCountBefore == framesAtPause)  // Кадры не приходят
    
    client.play()
    delay(2000)
    
    assertTrue(frameCountBefore > framesAtPause)  // Кадры снова приходят
}
```

### Сценарий 7: Множественные потоки

**Цель:** Проверить работу с несколькими потоками

**Шаги:**
1. Подключиться к камере с main и sub stream
2. Проверить получение информации о потоках
3. Подключиться к обоим потокам

**Ожидаемый результат:**
- Доступны оба потока
- Разрешение соответствует настройкам
- Sub stream имеет меньшее разрешение

**Тест:**
```kotlin
@Test
fun testMultipleStreams() = runTest {
    val client = RtspClient(RtspClientConfig(url = "rtsp://..."))
    client.connect()
    
    val streams = client.getStreams()
    assertTrue(streams.size >= 2)
    
    val mainStream = streams.first { it.type == RtspStreamType.VIDEO }
    assertEquals(Resolution(1920, 1080), mainStream.resolution)
    
    val subStream = streams.last { it.type == RtspStreamType.VIDEO }
    assertTrue(subStream.resolution!!.width < 1920)
}
```

### Сценарий 8: Производительность

**Цель:** Измерить производительность

**Метрики:**
- Время подключения
- Задержка видео (latency)
- FPS стабильность
- Потребление памяти
- Потребление CPU

**Тест:**
```kotlin
@Test
fun testPerformance() = runTest {
    val client = RtspClient(RtspClientConfig(url = "rtsp://..."))
    
    // Измерить время подключения
    val connectTime = measureTime { client.connect() }
    assertTrue(connectTime.inWholeMilliseconds < 10000)
    
    client.play()
    
    // Измерить FPS
    var frameCount = 0
    val startFrameTime = Clock.System.now().toEpochMilliseconds()
    
    client.setVideoFrameCallback { frameCount++ }
    
    delay(10000)  // 10 секунд
    
    val endFrameTime = Clock.System.now().toEpochMilliseconds()
    val fps = frameCount * 1000.0 / (endFrameTime - startFrameTime)
    
    assertTrue(fps >= 20)  // Минимум 20 FPS
}
```

### Сценарий 9: Длительное воспроизведение

**Цель:** Проверить стабильность при длительной работе

**Шаги:**
1. Подключиться к камере
2. Воспроизводить 1+ час
3. Мониторить стабильность

**Ожидаемый результат:**
- Нет утечек памяти
- FPS остается стабильным
- Нет внезапных разрывов
- Автоматическое восстановление при сбоях

**Тест:**
```kotlin
@Test
fun testLongRunning() = runTest {
    val client = RtspClient(RtspClientConfig(url = "rtsp://..."))
    client.connect()
    client.play()
    
    var totalFrames = 0
    client.setVideoFrameCallback { totalFrames++ }
    
    delay(3600000)  // 1 час
    
    assertTrue(totalFrames > 72000)  // Минимум 20 FPS * 3600 секунд
}
```

### Сценарий 10: Разные кодеки

**Цель:** Проверить поддержку различных кодеков

**Кодеки для тестирования:**
- H.264 (Baseline, Main, High)
- H.265/HEVC
- MJPEG
- AAC (audio)
- G.711 PCMU/PCMA (audio)

**Тест:**
```kotlin
@Test
fun testH264Codec() = runTest {
    val client = RtspClient(RtspClientConfig(url = "rtsp://h264-camera/..."))
    client.connect()
    
    val streams = client.getStreams()
    val videoStream = streams.first { it.type == RtspStreamType.VIDEO }
    
    assertEquals("H.264", videoStream.codec)
}
```

---

## 📊 Метрики тестирования

### Ключевые показатели:

| Метрика | Цель | Измерение |
|---------|------|-----------|
| Время подключения | < 10 сек | stopwatch |
| Задержка видео | < 500 мс | timestamp diff |
| FPS стабильность | > 95% | frame count / time |
| Потребление памяти | < 200 MB | Runtime.getRuntime() |
| CPU использование | < 30% | ManagementFactory |
| Время восстановления | < 5 сек | status change |
| Утечки памяти | 0 | heap dump |

---

## 🐛 Известные проблемы

### Требуется тестирование:
1. [ ] H.265 декодирование
2. [ ] Аудио PCMU/PCMA
3. [ ] Автоматическое переподключение
4. [ ] Длительное воспроизведение (>1 час)
5. [ ] Множественные камеры одновременно

---

## ✅ Критерии успешного тестирования

**Обязательные:**
- [ ] Все базовые сценарии пройдены (1-7)
- [ ] Время подключения < 10 секунд
- [ ] FPS > 20 при 25 FPS потоке
- [ ] Нет критических ошибок в логах
- [ ] Автоматическое восстановление работает

**Желаемые:**
- [ ] Длительное тестирование 1+ час без проблем
- [ ] Все кодеки работают
- [ ] Потребление памяти < 200 MB
- [ ] CPU использование < 30%

---

## 📁 Тестовые данные

### RTSP URL для тестирования:

**Публичные тестовые потоки:**
```
rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mp4
rtsp://184.72.239.149/vod/mp4://BigBuckBunny_115k.mov
rtsp://media-1.isnpltv.com:8554/
```

**Локальные тестовые серверы:**
```
rtsp://localhost:8554/live
rtsp://127.0.0.1:8554/test
```

---

**Следующий шаг:** Запуск тестов после завершения FFI компиляции  
**Ожидаемое время:** 5-7 дней для полного тестирования
