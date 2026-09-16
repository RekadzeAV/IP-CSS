# Стратегия RTSP Client - Решение

**Дата:** 11 June 2026  
**Решение:** Fix Current Implementation  
**Причина:** Live555 недоступен через стандартные источники

---

## Анализ вариантов

### Вариант 1: Live555 Migration ❌ ОТМЕНЕН

**Проблемы:**
- ❌ Live555 репозиторий недоступен на GitHub
- ❌ Официальный сайт (live555.com) возвращает 404
- ❌ Требуется ручной download и сложная настройка
- ❌ Лицензия LGPL требует дополнительных согласований

**Время оценки:** 5-7 дней (вместо 3-4)

---

### Вариант 2: Fix Current Implementation ✅ ВЫБРАН

**Преимущества:**
- ✅ Все исходные коды уже есть в проекте
- ✅ Нет внешних зависимостей
- ✅ Быстрая итерация и отладка
- ✅ Полный контроль над реализацией
- ✅ Лицензионная чистота

**Задачи:**
1. Добавить детальное логирование
2. Исправить URL formatting в RTSP REQUEST
3. Добавить thread synchronization
4. Graceful error handling вместо crash
5. Улучшить timeout и retry logic

**Время оценки:** 1-2 дня

---

## План исправления (Variant 2)

### Задача 1: Детальное логирование

**Файл:** `rtsp_client.cpp`

**Изменения:**
- Добавить логирование на каждом этапе подключения
- Логировать все RTSP запросы/ответы
- Логировать socket errors с детальными кодами
- Добавить логирование в RTP thread

**Реализация:**

```cpp
// В начале rtsp_client_connect()
LOGI("=== RTSP Connection Started ===");
LOGI("URL: %s", url);
LOGI("Host: %s, Port: %d", rtspUrl.host.c_str(), rtspUrl.port);
LOGI("Username: %s", username ? username : "(none)");

// При создании socket
LOGI("Creating TCP socket...");
// После connect
if (sock == INVALID_SOCKET) {
    int error = WSAGetLastError();
    LOGE("Socket creation failed: WSAGetLastError() = %d", error);
} else {
    LOGI("TCP socket created successfully: %d", sock);
}

// При парсинге SDP
LOGI("Parsing SDP response (%d bytes)", response.size());
```

---

### Задача 2: Исправить URL formatting

**Проблема:** MediaMTX получает `invalid URL (/test)`

**Корректный формат:**
```
DESCRIBE rtsp://127.0.0.1:8554/test RTSP/1.0
```

**Ошибка в коде:**
```cpp
// Строка ~2823 в rtsp_client.cpp
request << method << " " << url << " RTSP/1.0\r\n";
// где url = "/test" вместо "rtsp://127.0.0.1:8554/test"
```

**Исправление:**
```cpp
// Функция для получения полного RTSP URL
static std::string get_full_rtsp_url(const RtspUrl& url, const std::string& path) {
    std::ostringstream fullUrl;
    fullUrl << "rtsp://" << url.host << ":" << url.port << "/" << path;
    return fullUrl.str();
}

// В send_rtsp_request()
std::string fullUrl = get_full_rtsp_url(rtspUrl, controlPath);
request << method << " " << fullUrl << " RTSP/1.0\r\n";
```

---

### Задача 3: Thread Synchronization

**Проблема:** RTP thread запускается до завершения handshake

**Решение:**
```cpp
// Добавить флаги синхронизации
struct RTSPClient {
    std::mutex mutex;
    std::condition_variable connectCv;
    std::atomic<bool> handshakeComplete;  // NEW
    std::atomic<bool> rtpThreadReady;     // NEW
    
    // ... остальные поля
};

// В rtsp_client_connect()
handshakeComplete = false;
// ... после DESCRIBE/SETUP/PLAY
handshakeComplete = true;
connectCv.notify_all();

// В receive_rtp_thread()
while (!shouldStop && !playing) {
    // Ждем завершения handshake
    {
        std::unique_lock<std::mutex> lock(mutex);
        connectCv.wait_for(lock, std::chrono::seconds(5), 
            [this] { return handshakeComplete || shouldStop; });
    }
    
    if (!handshakeComplete || shouldStop) {
        break; // Выходим если handshake не завершен
    }
    
    // Продолжаем прием RTP
}
```

---

### Задача 4: Graceful Error Handling

**Проблема:** Crash в RTP thread при ошибках

**Решение:**
```cpp
// Вместо crash - graceful shutdown
static void receive_rtp_thread(RTSPClient* client) {
    if (!client) return;
    
    try {
        // ... основной цикл
    } catch (const std::exception& e) {
        LOGE("RTP thread exception: %s", e.what());
        client->status = RTSP_STATUS_ERROR;
        if (client->statusCallback) {
            client->statusCallback(RTSP_STATUS_ERROR, e.what(), client->statusUserData);
        }
    } catch (...) {
        LOGE("RTP thread unknown exception");
        client->status = RTSP_STATUS_ERROR;
    }
    
    // Cleanup
    client->shouldStop = true;
}

// В rtsp_client_connect()
// Проверка перед запуском RTP thread
if (!start_rtp_thread(client)) {
    LOGE("Failed to start RTP thread, aborting connection");
    rtsp_client_disconnect(client);
    return false;
}
```

---

### Задача 5: Timeout и Retry Logic

**Улучшения:**
```cpp
// Увеличить таймауты
#define RTSP_CONNECT_TIMEOUT_MS 10000
#define RTSP_READ_TIMEOUT_MS 5000
#define RTSP_WRITE_TIMEOUT_MS 5000

// Добавить retry для DESCRIBE
int maxRetries = 3;
for (int i = 0; i < maxRetries; i++) {
    if (send_describe_request(sock, url)) {
        break; // Успех
    }
    if (i < maxRetries - 1) {
        LOGW("DESCRIBE failed, retry %d/%d", i + 1, maxRetries);
        std::this_thread::sleep_for(std::chrono::milliseconds(1000));
    }
}
```

---

## Ожидаемые результаты

### До исправления:
```
[INFO] RTSPClientJNI: Connecting to RTSP: rtsp://127.0.0.1:8554/test
[ERROR] RTSPClientJNI: Connection failed
MediaMTX: "invalid URL (/test)"
→ Crash в RTP thread
```

### После исправления:
```
[INFO] === RTSP Connection Started ===
[INFO] URL: rtsp://127.0.0.1:8554/test
[INFO] Host: 127.0.0.1, Port: 8554
[INFO] Creating TCP socket...
[INFO] TCP socket created successfully: 156
[INFO] Connecting to 127.0.0.1:8554...
[INFO] TCP connection established
[INFO] Sending DESCRIBE rtsp://127.0.0.1:8554/test RTSP/1.0
[INFO] Received DESCRIBE response: 200 OK
[INFO] Parsing SDP response (1024 bytes)
[INFO] Found video stream: H264, 1920x1080, 25fps
[INFO] Sending SETUP...
[INFO] Sending PLAY...
[INFO] Handshake complete, starting RTP thread
[INFO] RTP thread started successfully
[INFO] Connected and playing
```

---

## Тестирование

### Unit Tests
```bash
.\gradlew.bat :core:network:test --tests "*RtspClientTest*"
```

**Ожидаемый результат:** 100% PASSED

### Integration Tests
```bash
.\gradlew.bat :core:network:desktopTest --tests "*NativeRtspClientLiveFrameTest*"
```

**Ожидаемый результат:**
- testStreamDiscoveryAfterConnect() → **PASSED**
- testVideoFrameReception() → **PASSED**
- testAudioFrameReception() → **PASSED**

### Manual Testing
```powershell
# Запуск тестового клиента
.\release-build\test\rtsp_test_client.exe rtsp://127.0.0.1:8554/test
```

---

## Временная шкала

| Задача | Время | Статус |
|--------|-------|--------|
| Детальное логирование | 2 часа | ⏸️ Pending |
| Исправить URL formatting | 1 час | ⏸️ Pending |
| Thread synchronization | 2 часа | ⏸️ Pending |
| Graceful error handling | 2 часа | ⏸️ Pending |
| Timeout и retry logic | 1 час | ⏸️ Pending |
| Тестирование | 2 часа | ⏸️ Pending |
| **Итого** | **10 часов** | |

---

## Риск-менеджмент

| Риск | Вероятность | Влияние | Митигация |
|------|-------------|---------|-----------|
| Не все тесты пройдут | Средняя | Среднее | Фиксация текущего состояния, частичное решение |
| Regression в других функциях | Низкая | Среднее | Полное покрытие unit тестами |
| MediaMTX специфичные проблемы | Средняя | Низкое | Тестирование с другими RTSP серверами |

---

## Заключение

**Рекомендуемая стратегия:** Fix Current Implementation

**Обоснование:**
1. Live555 недоступен через стандартные источники
2. Текущая реализация имеет понятные и исправимые проблемы
3. Быстрая итерация и контроль
4. Нет дополнительных лицензионных требований

**Следующие шаги:**
1. Начать с добавления детального логирования
2. Исправить URL formatting
3. Добавить thread synchronization
4. Протестировать с MediaMTX
5. Обновить документацию

---

**Создан:** 11 June 2026  
**Автор:** Koda AI Assistant  
**Статус:** Ready for Implementation

---

## Implementation Results (2026-06-11)

### ✅ COMPLETED

**URL Formatting Fixed:** All RTSP requests now use full RTSP URLs.

**MediaMTX Verification:**
```
BEFORE: invalid path name: can't begin with a slash (/test)
AFTER:  no stream is available on path 'test'
```

The error change confirms URL formatting is now correct.

**Files Modified:**
1. `native/video-processing/src/rtsp_client.cpp` - All URL formatting fixed
2. `native/video-processing/lib/windows/x64/video_processing.dll` - Updated build

**Functions Fixed:**
- `rtsp_client_connect()` - OPTIONS, DESCRIBE use full URL
- `rtsp_client_connect()` - SETUP controlUrl uses full URL
- `rtsp_client_play()` - PLAY uses full URL
- `rtsp_client_pause()` - PAUSE uses full URL
- `rtsp_client_stop()` - PAUSE uses full URL
- `rtsp_client_disconnect()` - TEARDOWN uses full URL

### ⏸️ PENDING

- Logging macros (removed due to Windows SDK conflicts)
- Thread synchronization (separate task)
- Integration testing with live RTSP stream

### 📊 Test Status

```
NativeRtspClientLiveFrameTest: SKIPPED (no stream published)
MediaMTX: "no stream is available on path 'test'"
```

**Conclusion:** URL formatting issue RESOLVED. Integration testing requires publishing a stream to MediaMTX.

---

**Last Updated:** 11 June 2026  
**Status:** Phase 2 RTSP Connect Issue - RESOLVED ✅
