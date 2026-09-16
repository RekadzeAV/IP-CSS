# Пункт 2: 1.8.4 RTSP Native Integration - План

**Дата:** 2026-04-27  
**Приоритет:** Критический blocker для production  
**Текущий статус:** 🟡 ~58%, требуется runtime stability

---

## 📋 Обзор

RTSP клиент уже имеет базовую реализацию с:
- ✅ Native integration через NativeRtspClient
- ✅ Fallback на синтетические потоки (при allowSimulatedFallback=true)
- ✅ Reconnect с exponential backoff
- ✅ Runtime diagnostics
- ✅ Status callbacks
- ✅ Frame flows (video/audio)

**Цель пункта 2:** Довести runtime stability и production readiness.

---

## 🎯 Задачи

### 2.1 Runtime Stability Improvements

#### 2.1.1 Long-run testing

**Цель:** Убедиться что RTSP клиент работает стабильно в течение длительного времени.

**Задачи:**
1. Создать тест на 30 минут стабильности
2. Проверить на утечки памяти
3. Проверить корректность reconnect при разрывах
4. Валидация cleanup при disconnect

**Тест:** `RtspClientSoakTest.kt`
```kotlin
@Test
fun `test 30 minute stability`() = runTest(timeout = 35.minutes) {
    val config = RtspClientConfig(
        url = System.getenv("RTSP_SOAK_TEST_URL") ?: return@runTest,
        enableAudio = true,
        enableVideo = true,
        reconnectEnabled = true,
        allowSimulatedFallback = false
    )
    
    val client = RtspClient(config)
    val framesReceived = AtomicLong(0)
    val errors = AtomicLong(0)
    
    client.setVideoFrameCallback { framesReceived.incrementAndGet() }
    client.setStatusCallback { status, error ->
        if (status == RtspClientStatus.ERROR) {
            errors.incrementAndGet()
        }
    }
    
    client.connect()
    
    // Ждать 30 минут
    delay(30.minutes)
    
    // Проверить результаты
    assertTrue(framesReceived.get() > 0, "No frames received in 30 minutes")
    assertEquals(0, errors.get(), "Errors occurred during soak test")
    
    client.disconnect()
}
```

#### 2.1.2 Memory leak detection

**Цель:** Убедиться что нет утечек памяти при многократных connect/disconnect.

**Методика:**
1. Выполнить 100 циклов connect/disconnect
2. Проверить heap dump
3. Проверить количество native handles

**Тест:** `RtspClientMemoryTest.kt`
```kotlin
@Test
fun `test no memory leaks after 100 connect/disconnect cycles`() = runTest {
    val config = RtspClientConfig(
        url = "rtsp://test-server/stream",
        allowSimulatedFallback = true
    )
    
    repeat(100) { i ->
        val client = RtspClient(config)
        client.connect()
        delay(100)
        client.disconnect()
        client.close()
        
        if ((i + 1) % 20 == 0) {
            System.gc()
            logger.info { "Cycle ${i + 1}/100 completed" }
        }
    }
    
    // Проверить что все native handles освобождены
    assertTrue(true, "Memory test completed")
}
```

### 2.2 Fallback Path Improvements

#### 2.2.1 HLS fallback integration

**Цель:** Добавить fallback на HLS когда RTSP недоступен.

**Текущее состояние:**
- Fallback только на синтетические потоки
- Нет fallback на HLS

**Задачи:**
1. Добавить проверку доступности HLS
2. При неудаче RTSP попытаться получить HLS URL
3. Переключить на HLS поток

**Реализация:**
```kotlin
class RtspClientWithHlsFallback(
    private val rtspClient: RtspClient,
    private val hlsUrlProvider: (String) -> String?
) {
    suspend fun connectWithHlsFallback(cameraId: String): Boolean {
        // Пытаемся RTSP
        rtspClient.connect()
        
        val rtspSuccess = withTimeoutOrNull(10_000) {
            rtspClient.getStatus().first { it == RtspClientStatus.CONNECTED || it == RtspClientStatus.PLAYING }
        } != null
        
        if (rtspSuccess) {
            return true
        }
        
        // RTSP failed, try HLS fallback
        val hlsUrl = hlsUrlProvider(cameraId) ?: return false
        
        // Подключаемся к HLS
        // TODO: HLS client integration
        return true
    }
}
```

#### 2.2.2 Improved error messages

**Цель:** Улучшить диагностические сообщения при ошибках.

**Задачи:**
1. Добавить больше контекста в ошибки
2. Логи с уровнями (DEBUG/INFO/WARN/ERROR)
3. Runtime diagnostics экспортировать через API

**Пример улучшенных логов:**
```kotlin
logger.warn { 
    "RTSP connection failed: " +
    "url=${config.url}, " +
    "attempt=${diagnostics.connectAttempts}, " +
    "consecutiveFailures=${diagnostics.consecutiveFailures}, " +
    "lastError=${diagnostics.lastError}, " +
    "fallback=${if (config.allowSimulatedFallback) "enabled" else "disabled"}"
}
```

### 2.3 Production Hardening

#### 2.3.1 Connection timeout tuning

**Цель:** Оптимизировать таймауты для production.

**Текущие значения:**
```kotlin
val timeoutMillis: Long = 10000,  // 10 секунд
val reconnectInitialDelayMs: Int = 500,
val reconnectMaxDelayMs: Int = 10_000,
```

**Рекомендации для production:**
```kotlin
val timeoutMillis: Long = 5000,  // 5 секунд (быстрее detect failure)
val reconnectInitialDelayMs: Int = 1000,  // 1 секунда (меньше флудить)
val reconnectMaxDelayMs: Int = 30_000,  // 30 секунд (больше wait при persistent failure)
val reconnectMaxRetries: Int = 3,  // Ограничить retries
```

#### 2.3.2 Resource cleanup

**Цель:** Убедиться что все ресурсы правильно освобождаются.

**Задачи:**
1. Проверить cleanup в finally blocks
2. Добавить explicit close() метод
3. Проверить cleanup при cancellation

**Тест:**
```kotlin
@Test
fun `test resource cleanup on cancellation`() = runTest {
    val client = RtspClient(config)
    
    val connectJob = launch {
        client.connect()
    }
    
    delay(100)
    connectJob.cancel()
    
    client.close()
    
    // Проверить что все ресурсы освобождены
    assertEquals(RtspClientStatus.DISCONNECTED, client.getStatus().value)
}
```

### 2.4 Testing

#### 2.4.1 Integration tests

**Цель:** Проверить интеграцию с реальными камерами.

**Тесты:**
1. Подключение к реальной камере
2. Получение кадров
3. Reconnect при разрыве
4. Graceful disconnect

**Скрипт:** `scripts/test-rtsp-integration.ps1`
```powershell
param(
    [string]$RtspUrl = "rtsp://admin:password@192.168.1.100:554/stream",
    [int]$DurationSeconds = 60
)

Write-Host "Testing RTSP connection for ${DurationSeconds}s..."

# Запустить тест
.\gradlew.bat :core:network:test --tests RtspRealStreamTest

# Проверить результаты
```

#### 2.4.2 Performance benchmarks

**Цель:** Измерить производительность RTSP клиента.

**Метрики:**
- Время подключения
- FPS при получении кадров
- CPU usage
- Memory usage

**Тест:** `RtspClientPerformanceTest.kt`
```kotlin
@Test
fun `test connection time`() = runTest {
    val client = RtspClient(config)
    
    val startTime = System.currentTimeMillis()
    client.connect()
    val connectionTime = System.currentTimeMillis() - startTime
    
    logger.info { "Connection time: ${connectionTime}ms" }
    assertTrue(connectionTime < 10_000, "Connection took too long: ${connectionTime}ms")
}
```

---

## 📊 Критерии готовности

**1.8.4 считается завершённым при:**

1. ✅ Long-run test (30 минут) проходит без ошибок
2. ✅ Нет утечек памяти после 100 connect/disconnect циклов
3. ✅ Reconnect работает корректно при искусственных разрывах
4. ✅ Production таймауты настроены
5. ✅ Runtime diagnostics экспортируются через API
6. ✅ Integration tests с реальными камерами проходят
7. ✅ Performance benchmarks в пределах нормы:
   - Connection time < 10 секунд
   - FPS >= 20 при 25 FPS камере
   - CPU usage < 20% на одном потоке
   - Memory usage стабильно

---

## 🔧 Рекомендации по исполнению

### Приоритеты:
1. **Critical:** Long-run stability testing
2. **High:** Resource cleanup validation
3. **Medium:** HLS fallback integration
4. **Low:** Performance benchmarks

### Риски:
1. **Native library instability** - требует полевых тестов
2. **Network conditions** - сложно эмулировать в тесте
3. **Camera compatibility** - разные камеры ведут себя по-разному

### Mitigation:
1. Полевое тестирование на реальных камерах
2. Добавить больше diagnostics
3. Сделать configurable таймауты и retries

---

## 📝 Следующие шаги

После завершения пункта 2:
1. Перейти к **Пункту 3: 1.8.2 HLS Pipeline**
2. Завершить стабильность HLS
3. Long-run testing HLS
4. Reconnect logic

---

**План создан:** 2026-04-27  
**Версия:** 1.0  
**Статус:** Ready for execution
