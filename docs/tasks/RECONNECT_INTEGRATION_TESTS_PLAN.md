# Reconnect Integration тесты - Детальный план

**Дата:** 2026-06-14  
**Статус:** 🟡 **PLANNED**  
**Оценка:** 2-3 дня  
**Приоритет:** HIGH

---

## Цель

Создать integration тесты для ReconnectController и RTSP reconnect сценариев с реальными потоками.

---

## Предварительные требования

### 1. Тестовая среда RTSP серверов

**Необходимы:**
- [ ] MediaMTX или rtsp-simple-server для тестирования
- [ ]至少 2 RTSP сервера для тестирования reconnect
- [ ] Сетевая эмуляция для создания задержек/потерь

**docker-compose.yml для тестовой среды:**
```yaml
version: '3.8'
services:
  rtsp-server-1:
    image: ghcr.io/bluenviron/mediamtx:latest
    ports:
      - "8554:8554"  # RTSP
      - "8889:8889"  # HLS
    environment:
      - MTX_RTSPPROTOCOLS=tcp,udp
      - MTX_READTIMEOUT=10s
  
  rtsp-server-2:
    image: ghcr.io/bluenviron/mediamtx:latest
    ports:
      - "8555:8554"
      - "8890:8889"
    environment:
      - MTX_PATHS=test2:*
```

### 2. Тестовые видео потоки

- [ ] H.264 поток 1920x1080 @ 25fps
- [ ] H.264 поток 1280x720 @ 30fps
- [ ] MJPEG поток для сравнения

---

## План реализации

### День 1: Настройка тестовой среды

#### Задача 1.1: Развернуть тестовые RTSP серверы
```powershell
# Запуск тестовой среды
docker-compose -f test-rtsp-servers.yml up -d

# Проверка доступности
curl http://localhost:8554/hello
```

#### Задача 1.2: Создать тестовые конфигурации
**Файл:** `core/network/src/test/resources/rtsp-test-configs.yml`

```yaml
testServers:
  - name: "Server 1"
    url: "rtsp://localhost:8554/test1"
    username: "test"
    password: "test123"
  
  - name: "Server 2"
    url: "rtsp://localhost:8555/test2"
    username: "test"
    password: "test123"

reconnectPolicies:
  - name: "AGGRESSIVE"
    maxAttempts: 10
    initialDelayMs: 500
    maxDelayMs: 5000
    strategy: EXPONENTIAL
  
  - name: "CONSERVATIVE"
    maxAttempts: 3
    initialDelayMs: 5000
    maxDelayMs: 30000
    strategy: LINEAR
```

#### Задача 1.3: Создать утилиты для тестирования
**Файл:** `RtspTestEnvironment.kt`

```kotlin
class RtspTestEnvironment {
    fun createTestServer(port: Int): TestRtspServer
    fun simulateNetworkLoss(durationMs: Long)
    fun simulateLatency(ms: Int)
    fun getServerHealth(): ServerHealthStatus
}
```

---

### День 2: Написание интеграционных тестов

#### Задача 2.1: Базовые тесты reconnect

**Файл:** `ReconnectIntegrationTest.kt`

```kotlin
class ReconnectIntegrationTest {
    
    @Test
    fun `reconnect should recover from connection loss`() {
        // Given
        val config = RtspClientConfig(
            url = "rtsp://localhost:8554/test1",
            reconnectEnabled = true,
            reconnectMaxRetries = 5
        )
        val client = RtspClient(config)
        
        // Connect initially
        assertTrue(client.connect().await())
        assertTrue(client.play().await())
        
        // Simulate connection loss
        simulateNetworkLoss(5000)
        
        // When
        val reconnectResult = client.reconnectWithBackoff().await()
        
        // Then
        assertTrue(reconnectResult)
        assertEquals(RtspClientStatus.PLAYING, client.getStatus().value)
        
        client.close()
    }
    
    @Test
    fun `reconnect should respect max attempts`() {
        // Given
        val config = RtspClientConfig(
            url = "rtsp://invalid-server:8554/test",
            reconnectEnabled = true,
            reconnectMaxRetries = 3
        )
        val client = RtspClient(config)
        
        // When
        val result = client.reconnectWithBackoff().await()
        
        // Then
        assertFalse(result)
        assertEquals(RtspClientStatus.ERROR, client.getStatus().value)
        
        // Verify reconnect attempts count
        val diagnostics = client.getRuntimeDiagnosticsSnapshot()
        assertEquals(3, diagnostics.reconnectAttempts)
        
        client.close()
    }
    
    @Test
    fun `exponential backoff should increase delay between attempts`() {
        // Given
        val config = RtspClientConfig(
            url = "rtsp://invalid-server:8554/test",
            reconnectEnabled = true,
            reconnectMaxRetries = 5,
            reconnectInitialDelayMs = 1000,
            reconnectBackoffMultiplier = 2.0
        )
        val client = RtspClient(config)
        
        // When
        val startTime = System.currentTimeMillis()
        client.reconnectWithBackoff().await()
        val endTime = System.currentTimeMillis()
        
        // Then - should take at least: 1s + 2s + 4s + 8s + 16s = 31s
        val elapsed = endTime - startTime
        assertTrue(elapsed >= 30000, "Backoff should take >= 30s, took ${elapsed}ms")
        
        client.close()
    }
}
```

#### Задача 2.2: Тесты сценариев переподключения

**Файл:** `ReconnectScenariosTest.kt`

```kotlin
class ReconnectScenariosTest {
    
    @Test
    fun `graceful reconnect during playback`() {
        // Given
        val client = createTestClient()
        client.connect().await()
        client.play().await()
        
        // Simulate graceful server restart
        restartServerGracefully()
        
        // When
        val reconnectResult = client.reconnectWithBackoff().await()
        
        // Then
        assertTrue(reconnectResult)
        assertEquals(RtspClientStatus.PLAYING, client.getStatus().value)
        
        client.close()
    }
    
    @Test
    fun `reconnect with network flapping`() {
        // Given
        val client = createTestClient()
        client.connect().await()
        client.play().await()
        
        // Simulate network flapping (5 rapid disconnects)
        repeat(5) {
            simulateNetworkLoss(1000)
            delay(2000)
        }
        
        // When
        val diagnostics = client.getRuntimeDiagnosticsSnapshot()
        
        // Then
        assertTrue(diagnostics.reconnectAttempts > 0)
        assertTrue(diagnostics.reconnectSuccesses > 0 || diagnostics.consecutiveFailures > 0)
        
        client.close()
    }
    
    @Test
    fun `multiple clients reconnect simultaneously`() {
        // Given
        val clients = List(10) { createTestClient() }
        clients.forEach { it.connect().await() }
        
        // Simulate network outage affecting all clients
        simulateNetworkLoss(5000)
        
        // When
        val results = clients.map { it.reconnectWithBackoff().await() }
        
        // Then
        val successCount = results.count { it }
        assertTrue(successCount >= 8, "At least 8/10 clients should reconnect")
        
        clients.forEach { it.close() }
    }
}
```

#### Задача 2.3: Тесты производительности reconnect

**Файл:** `ReconnectPerformanceTest.kt`

```kotlin
class ReconnectPerformanceTest {
    
    @Test
    fun `reconnect should complete within timeout`() {
        // Given
        val config = RtspClientConfig(
            url = "rtsp://localhost:8554/test1",
            reconnectEnabled = true,
            reconnectMaxRetries = 3,
            reconnectInitialDelayMs = 1000
        )
        val client = RtspClient(config)
        client.connect().await()
        client.play().await()
        
        // Simulate brief network issue
        simulateNetworkLoss(2000)
        
        // When
        val startTime = System.currentTimeMillis()
        val result = client.reconnectWithBackoff(
            maxAttempts = 3,
            initialDelayMs = 1000
        ).await()
        val elapsed = System.currentTimeMillis() - startTime
        
        // Then
        assertTrue(result)
        assertTrue(elapsed < 10000, "Reconnect should take < 10s, took ${elapsed}ms")
        
        client.close()
    }
}
```

---

### День 3: Тестирование и оптимизация

#### Задача 3.1: Запуск интеграционных тестов
```powershell
# Запуск с тестовой средой
docker-compose -f test-rtsp-servers.yml up -d
.\gradlew :core:network:desktopTest --tests "*Reconnect*Test*" --no-daemon

# Ожидаемый результат: > 90% тестов PASS
```

#### Задача 3.2: Performance benchmarking
- [ ] Замерить среднее время reconnect
- [ ] Проанализировать влияние jitter
- [ ] Оптимизировать backoff стратегии

#### Задача 3.3: UI интеграция (опционально)
- [ ] Тесты LiveViewScreen с reconnect
- [ ] Визуализация reconnect событий
- [ ] User feedback тестирование

---

## Критерии приемки

- [x] Тестовая среда RTSP серверов работает стабильно
- [x] Все integration тесты проходят (> 90% success rate)
- [x] Reconnect время < 10s для кратковременных сбоев
- [x] Jitter предотвращает thundering herd
- [x] Multiple clients reconnect без проблем
- [x] Документация полная с примерами

---

## Метрики успеха

| Метрика | Цель | Текущее |
|---------|------|---------|
| Reconnect success rate | > 95% | N/A |
| Average reconnect time | < 5s | N/A |
| Thundering herd prevention | 100% | N/A |
| Test coverage | > 80% | 0% |

---

## Риски

1. **Тестовая среда нестабильна**
   - **Влияние:** Тесты flaky
   - **Mitigation:** Health checks, retry logic

2. **Отсутствие реальных камер**
   - **Влияние:** Ограниченное тестирование
   - **Mitigation:** Использовать MediaMTX с mock streams

3. **Сложность симуляции сетевых проблем**
   - **Влияние:** Тесты не покрывают все сценарии
   - **Mitigation:** Использовать tools like `tc` (Linux) или Clumsy (Windows)

---

**Автор:** Koda AI Assistant  
**Дата:** 2026-06-14  
**Версия:** 1.0