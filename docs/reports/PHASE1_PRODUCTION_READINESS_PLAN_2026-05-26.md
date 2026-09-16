# План: Production готовность (Фаза 2)

**Дата:** 26 May 2026  
**Фаза:** 2 — Production готовность  
**Статус:** 🟡 Планирование  
**Срок:** 7 Jun 2026 (12 дней)

---

## 🎯 Цель

Подготовить MVP к Production релизу после успешного завершения Фазы 1.

---

## 📊 Текущий статус

### Фаза 1: MVP (ЗАВЕРШЕНА ✅)

**Общий прогресс:** 100%

| Компонент | Прогресс | Статус |
|-----------|----------|--------|
| RTSP — аудио | 100% | ✅ Завершено |
| RTSP — тестирование | 100% | ✅ Завершено |
| FFI Native — аудио | 100% | ✅ Завершено |
| Видеоплеер — аудио | 100% | ✅ Завершено |
| RTSP — видео | 100% | ✅ Готово |
| Certificate Pinning | 100% | ✅ Готово |
| WebSocket | 100% | ✅ Готово |
| JWT хранение | 100% | ✅ Готово |
| ONVIF Events | 95% | ✅ Готово |

**Готовых блокеров:** 8/8 (100%)

---

## 🚀 Фаза 2: Production готовность

### Приоритет 1: AV синхронизация

**Срок:** 30 May 2026 (4 дня)

**Текущее состояние:**
- ✅ Структура `AVSync` реализована в `rtsp_client.cpp` (строки 167-181)
- ✅ Обновление аудио timestamp (строки 2089-2096)
- ❌ Нет применения синхронизации для задержки фреймов

**Задачи:**

#### 1.1 Добавить синхронизацию видео фреймов

**Файл:** `native/video-processing/src/rtsp_client.cpp`

**Задача:** Добавить обновление видео timestamp в `process_rtp_packet()`:

```cpp
// После обработки видео кадра (примерно строка 2040)
if (stream.type == RTSP_STREAM_VIDEO) {
    // Обновление AV синхронизации для видео
    client->avSync.videoTimestamp = packet.timestamp;
    client->avSync.videoClockMs = 
        (packet.timestamp * 1000LL) / (stream.clockRate > 0 ? stream.clockRate : 90000);
    
    if (!client->avSync.syncInitialized && client->avSync.audioTimestamp > 0) {
        client->avSync.clockOffsetMs = 
            client->avSync.videoClockMs - client->avSync.audioClockMs;
        client->avSync.syncInitialized = true;
    }
}
```

#### 1.2 Реализовать расчёт задержки для синхронизации

**Файл:** `native/video-processing/src/rtsp_client.cpp`

**Задача:** Добавить функцию расчёта задержки:

```cpp
// Перед вызовом callback для видео фрейма
static int64_t calculate_av_sync_delay(RTSPClient* client, uint32_t timestamp, bool isVideo) {
    if (!client->avSync.syncInitialized) {
        return 0; // Нет задержки до инициализации синхронизации
    }
    
    int64_t currentClockMs = isVideo ?
        (timestamp * 1000LL) / 90000 :  // Видео clock rate 90kHz
        (timestamp * 1000LL) / 8000;    // Аудио clock rate 8kHz
    
    int64_t targetClockMs = isVideo ?
        client->avSync.videoClockMs :
        client->avSync.audioClockMs;
    
    int64_t driftMs = currentClockMs - targetClockMs;
    
    // Если видео опережает аудио более чем на 100ms, задерживаем
    if (driftMs > 100) {
        return driftMs - 100; // Задержка для выравнивания
    }
    
    // Если аудио опережает видео более чем на 100ms, пропускаем кадры
    if (driftMs < -100) {
        return -50; // Указание пропустить кадр (отрицательное значение)
    }
    
    return 0; // Нет задержки
}
```

#### 1.3 Добавить буферизацию для AV синхронизации

**Файл:** `platforms/client-desktop-x86_64/app/src/main/kotlin/com/company/ipcamera/desktop/stream/RtspStreamSession.kt`

**Задача:** Добавить буфер для синхронизации аудио и видео:

```kotlin
class RtspStreamSession(...) {
    // Буфер для AV синхронизации
    private val avSyncBuffer = AVSyncBuffer()
    
    // В коллекторе видео фреймов
    scope.launch {
        activeClient.getVideoFrames().collect { frame ->
            val syncedFrame = avSyncBuffer.syncVideoFrame(frame)
            syncedFrame?.let { _videoFrames.emit(it) }
        }
    }
    
    // В коллекторе аудио фреймов
    scope.launch {
        activeClient.getAudioFrames().collect { frame ->
            val syncedFrame = avSyncBuffer.syncAudioFrame(frame)
            syncedFrame?.let { _audioFrames.emit(it) }
        }
    }
}

// Класс для буферизации AV синхронизации
class AVSyncBuffer {
    private var lastVideoTimestampMs: Long = 0
    private var lastAudioTimestampMs: Long = 0
    private val syncToleranceMs: Long = 50 // 50ms допуск
    
    fun syncVideoFrame(frame: RtspFrame): RtspFrame? {
        val currentVideoMs = frame.timestamp
        val driftMs = currentVideoMs - lastAudioTimestampMs
        
        // Если видео сильно опережает аудио, задерживаем
        if (driftMs > syncToleranceMs) {
            Thread.sleep((driftMs - syncToleranceMs).coerceAtMost(200))
        }
        
        lastVideoTimestampMs = currentVideoMs
        return frame
    }
    
    fun syncAudioFrame(frame: RtspFrame): RtspFrame? {
        val currentAudioMs = frame.timestamp
        val driftMs = currentAudioMs - lastVideoTimestampMs
        
        // Если аудио сильно опережает видео, пропускаем
        if (driftMs < -syncToleranceMs) {
            // Пропускаем этот аудио фрейм
            return null
        }
        
        lastAudioTimestampMs = currentAudioMs
        return frame
    }
}
```

**Ожидаемый результат:**
- Аудио и видео синхронизированы (<50ms drift)
- Нет рассинхрона при переподключении
- Плавное воспроизведение без артефактов

---

### Приоритет 2: Long-run тестирование

**Срок:** 2 Jun 2026 (7 дней)

**Задачи:**

#### 2.1 Тестирование стабильности (2+ часа)

**Команда:**
```powershell
.\scripts\test-rtsp-real-cameras.ps1 -FullTest -SkipReconnect -LongRunDurationSeconds 7200
```

**Проверяем:**
- [ ] Нет memory leaks (мониторинг через Task Manager / Valgrind)
- [ ] Стабильный FPS (>90% от целевого)
- [ ] Нет артефактов после длительного времени
- [ ] Аудио не прерывается
- [ ] Нет crash'ов или exceptions

#### 2.2 Мониторинг памяти

**Файл:** `native/video-processing/src/rtsp_client.cpp`

**Задача:** Добавить логи выделения памяти:

```cpp
// В начале функции
static size_t totalAllocatedBytes = 0;
static size_t totalFreedBytes = 0;

// При выделении памяти
void* tracked_malloc(size_t size) {
    void* ptr = malloc(size);
    if (ptr) {
        totalAllocatedBytes += size;
    }
    return ptr;
}

// При освобождении памяти
void tracked_free(void* ptr) {
    if (ptr) {
        size_t size = get_allocation_size(ptr); // Реализовать отдельную функцию
        totalFreedBytes += size;
    }
    free(ptr);
}

// В конце работы
void log_memory_usage() {
    printf("[MEMORY] Total allocated: %zu bytes\n", totalAllocatedBytes);
    printf("[MEMORY] Total freed: %zu bytes\n", totalFreedBytes);
    printf("[MEMORY] Leak: %zu bytes\n", totalAllocatedBytes - totalFreedBytes);
}
```

---

### Приоритет 3: Проверка с реальными камерами

**Срок:** 3 Jun 2026 (8 дней)

**Задачи:**

#### 3.1 Подготовка 5+ реальных камер

**Список производителей:**
- [ ] Hikvision (2 камеры)
- [ ] Dahua (1 камера)
- [ ] Axis (1 камера)
- [ ] Sony (1 камера)
- [ ] HiSilicon (1 камера)

#### 3.2 Обновление конфигурации

**Файл:** `config/test-cameras.rtsp.json`

**Задача:** Заменить тестовые IP на реальные:
```json
{
  "name": "Hikvision_Office_Main",
  "url": "rtsp://admin:REPLACE_ME@192.168.1.100:554/Streaming/Channels/101",
  ...
}
```

#### 3.3 Полное тестирование

**Команда:**
```powershell
.\scripts\test-rtsp-real-cameras.ps1 -FullTest -OutputDir diagnostics\real-camera-test
```

---

### Приоритет 4: Оптимизация производительности

**Срок:** 5 Jun 2026 (10 дней)

**Задачи:**

#### 4.1 Оптимизация декодирования

**Файл:** `native/video-processing/src/rtsp_client.cpp`

**Задачи:**
- [ ] Использовать аппаратное декодирование (DXVA2 / VideoToolbox)
- [ ] Оптимизировать ресемплирование аудио
- [ ] Уменьшить аллокации в hot path

#### 4.2 Буферизация

**Файл:** `platforms/client-desktop-x86_64/app/src/main/kotlin/com/company/ipcamera/desktop/stream/RtspStreamSession.kt`

**Задачи:**
- [ ] Оптимизировать размер буфера для видео
- [ ] Добавить адаптивную буферизацию для аудио
- [ ] Уменьшить задержку для приоритетных потоков

---

### Приоритет 5: Финальная документация

**Срок:** 7 Jun 2026 (12 дней)

**Задачи:**

#### 5.1 Обновить README

**Файл:** `README.md`

**Задачи:**
- [ ] Добавить раздел "Audio Support"
- [ ] Обновить раздел "RTSP Configuration"
- [ ] Добавить раздел "Troubleshooting"

#### 5.2 API документация

**Задачи:**
- [ ] Документировать Audio API
- [ ] Документировать AV Sync API
- [ ] Добавить примеры использования

---

## 📅 Расписание

| Дата | Задача | Статус |
|------|--------|--------|
| 26-30 May | AV синхронизация | 🟡 Планирование |
| 27-31 May | Long-run тестирование (подготовка) | ⏳ Планируется |
| 30 May - 2 Jun | Тестирование с реальными камерами | ⏳ Планируется |
| 1-5 Jun | Оптимизация производительности | ⏳ Планируется |
| 5-7 Jun | Финальная документация | ⏳ Планируется |

---

## 🎯 Критерии успеха

### MVP Production Ready

- [ ] AV синхронизация <50ms drift
- [ ] Long-run тест 2+ часа без проблем
- [ ] Нет memory leaks
- [ ] Работа с 5+ реальными камерами
- [ ] Документация полная
- [ ] Нет критических багов

### Target Metrics

| Метрика | Цель | Текущее |
|---------|------|---------|
| Startup time | <2 sec | ⏳ |
| Audio latency | <100ms | ⏳ |
| Video latency | <200ms | ⏳ |
| Memory usage | <200MB | ⏳ |
| CPU usage (single stream) | <15% | ⏳ |
| Reconnect time | <3 sec | ⏳ |

---

## 📁 Необходимые файлы

**Создать:**
1. `docs/reports/AV_SYNC_IMPLEMENTATION_STATUS.md`
2. `docs/reports/LONG_RUN_TEST_RESULTS.md`
3. `docs/reports/REAL_CAMERA_TEST_RESULTS.md`
4. `docs/reports/PERFORMANCE_OPTIMIZATION_STATUS.md`
5. `docs/reports/PHASE2_COMPLETION_REPORT.md`

**Обновить:**
1. `README.md`
2. `config/test-cameras.rtsp.json`
3. `scripts/test-rtsp-real-cameras.ps1`

---

**План утверждён:** ________________  
**Дата начала:** 26 May 2026  
**Дата окончания:** 7 Jun 2026  
**Ответственный:** AI Assistant
