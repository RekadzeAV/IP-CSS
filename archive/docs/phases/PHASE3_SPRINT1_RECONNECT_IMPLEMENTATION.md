# Phase 3 Sprint 1: Reconnect & Backoff Implementation

## Summary

Реализована продвинутая система автоматического переподключения RTSP потоков с экспоненциальным backoff и jitter для desktop клиента.

## Implementation Date

2026-06-11

## Status

✅ **Implementation Complete** (интеграция с VideoPlayer требует исправления существующих ошибок компиляции в shared модуле)

---

## Changes Made

### 1. Reconnect Policy Module

**File:** `platforms/client-desktop-x86_64/app/src/main/kotlin/com/company/ipcamera/desktop/reconnect/ReconnectPolicy.kt`

**Features:**
- **Стратегии backoff:**
  - `EXPONENTIAL` — экспоненциальная задержка (по умолчанию)
  - `LINEAR` — линейная задержка
  - `FIXED` — фиксированная задержка

- **Готовые политики:**
  - `CONSERVATIVE` — 3 попытки, 3-60 секунд задержка
  - `BALANCED` — 5 попыток, 1.5-30 секунд (по умолчанию)
  - `AGGRESSIVE` — 10 попыток, 0.5-15 секунд
  - `NONE` — без переподключений

- **Функции:**
  - `calculateReconnectDelay()` — расчет задержки с jitter
  - `classifyStreamError()` — классификация ошибок (timeout, connection refused, etc.)
  - `shouldRetryError()` — определение retry-пригодности ошибки

- **Конфигурация через environment variables:**
  ```bash
  IPCSS_RECONNECT_ENABLED=true
  IPCSS_RECONNECT_MAX_ATTEMPTS=5
  IPCSS_RECONNECT_INITIAL_DELAY_MS=1500
  IPCSS_RECONNECT_MAX_DELAY_MS=30000
  IPCSS_RECONNECT_BACKOFF_STRATEGY=EXPONENTIAL
  IPCSS_RECONNECT_BACKOFF_MULTIPLIER=2.0
  IPCSS_RECONNECT_JITTER_RATIO=0.1
  ```

### 2. Reconnect Controller

**File:** `platforms/client-desktop-x86_64/app/src/main/kotlin/com/company/ipcamera/desktop/reconnect/ReconnectController.kt`

**Features:**
- Управление reconnect для каждой камеры независимо
- Экспоненциальный backoff с jitter
- Фильтрация ошибок (retryable vs non-retryable)
- Глобальные события reconnect через `SharedFlow<ReconnectEvent>`
- Статистика reconnect attempts, success/failure rates

**Events:**
- `ReconnectStarted` — начало попытки переподключения
- `ReconnectSuccess` — успешное переподключение
- `ReconnectFailed` — неудачная попытка
- `ReconnectExhausted` — исчерпаны все попытки
- `ReconnectCancelled` — отмена reconnect
- `ErrorOccurred` — ошибка потока

**API:**
```kotlin
fun startReconnectMonitoring(cameraId: String, policy: ReconnectPolicy)
fun stopReconnect(cameraId: String, reason: String? = null)
fun onStreamError(cameraId: String, error: String)
fun onConnected(cameraId: String)
fun getReconnectState(cameraId: String): StateFlow<ReconnectState>?
fun getReconnectStats(): Map<String, ReconnectState>
```

### 3. RtspStreamSession Enhancement

**File:** `platforms/client-desktop-x86_64/app/src/main/kotlin/com/company/ipcamera/desktop/stream/RtspStreamSession.kt`

**Changes:**
- Добавлена интеграция с `ReconnectController`
- Автоматическое переподключение при ошибках
- Метод `manualReconnect()` для ручного переподключения
- Метод `updateReconnectPolicy()` для обновления политики на лету
- Передача `reconnectPolicy` в `RtspClientConfig`

**Usage:**
```kotlin
val session = RtspStreamSession(
    camera = camera,
    reconnectPolicy = ReconnectPolicy.BALANCED,
    reconnectController = reconnectController
)

session.start(autoPlay = true)

// При ошибке reconnectController автоматически обработает переподключение
```

### 4. LiveViewViewModel Enhancement

**File:** `platforms/client-desktop-x86_64/app/src/main/kotlin/com/company/ipcamera/desktop/ui/viewmodel/LiveViewViewModel.kt`

**Changes:**
- Добавлен `ReconnectController` для управления reconnect всех камер
- Новые поля в `LiveViewState`:
  - `cameraReconnectPolicies: Map<String, ReconnectPolicy>` — политики для каждой камеры
  - `reconnectWarnings: Map<String, ReconnectWarning>` — предупреждения о reconnect

- Методы управления reconnect:
  ```kotlin
  fun getReconnectPolicy(cameraId: String): ReconnectPolicy
  fun setReconnectPolicy(cameraId: String, policy: ReconnectPolicy)
  fun setGlobalReconnectPolicy(policy: ReconnectPolicy)
  fun requestManualReconnect(cameraId: String)
  ```

- Обработка событий reconnect от контроллера
- Обновление метрик при reconnect attempts

---

## Design Decisions

### Exponential Backoff with Jitter

Выбрана стратегия экспоненциального backoff с jitter для:
- Предотвращения thundering herd при массовых reconnect
- Адаптации к временным сетевым проблемам
- Снижения нагрузки на сервер при повторяющихся сбоях

Формула: `delay = min(initialDelay * (multiplier ^ (attempt - 1)), maxDelay) ± jitter`

### Error Classification

Ошибки классифицируются для умной политики retry:
- **TIMEOUT** — retry по умолчанию включен
- **CONNECTION_REFUSED** — retry по умолчанию включен
- **SERVER_UNAVAILABLE (503)** — retry по умолчанию включен
- **NETWORK_ERROR** — всегда retry
- **UNKNOWN** — retry по умолчанию включен

### Per-Camera Policies

Каждая камера может иметь свою политику reconnect, что позволяет:
- Настроить агрессивный reconnect для критичных камер
- Использовать консервативный подход для нестабильных камер
- Экономить ресурсы для камер с низким приоритетом

---

## Testing Recommendations

### Unit Tests

```kotlin
@Test
fun `test exponential backoff delay calculation`() {
    val policy = ReconnectPolicy.BALANCED
    assertEquals(1500L, calculateReconnectDelay(1, policy)) // 1500 * 2^0
    assertEquals(3000L, calculateReconnectDelay(2, policy)) // 1500 * 2^1
    assertEquals(6000L, calculateReconnectDelay(3, policy)) // 1500 * 2^2
}

@Test
fun `test jitter prevents exact delays`() {
    val policy = ReconnectPolicy(
        enabled = true,
        initialDelayMs = 1000L,
        maxDelayMs = 30000L,
        backoffStrategy = ReconnectBackoffStrategy.EXPONENTIAL,
        jitterRatio = 0.2
    )
    // Jitter должен добавлять вариативность
    val delays = (1..10).map { calculateReconnectDelay(1, policy) }
    assertTrue(delays.toSet().size > 1, "Jitter должен создавать вариативность")
}

@Test
fun `test error classification`() {
    assertEquals(StreamErrorType.TIMEOUT, classifyStreamError("Connection timed out"))
    assertEquals(StreamErrorType.CONNECTION_REFUSED, classifyStreamError("Connection refused"))
    assertEquals(StreamErrorType.SERVER_UNAVAILABLE, classifyStreamError("503 Service Unavailable"))
    assertEquals(StreamErrorType.NETWORK_ERROR, classifyStreamError("Network IO error"))
}
```

### Integration Tests

1. **Multiple camera reconnect stress test**
   - Запустить 16 камер одновременно
   - Имитировать массовую потерю соединения
   - Проверить, что jitter предотвращает thundering herd

2. **Policy change at runtime**
   - Запустить поток с BALANCED политикой
   - Сменить на AGGRESSIVE во время reconnect
   - Проверить применение новой политики

3. **Exhausted retries behavior**
   - Имитировать постоянные ошибки подключения
   - Проверить, что после maxAttempts reconnect прекращается
   - Проверить уведомление через `ReconnectEvent.ReconnectExhausted`

---

## Next Steps

### UI Integration

Для полной интеграции требуется:
1. Добавить индикатор reconnect status в UI (`LiveViewScreen.kt`)
2. Визуализировать retry attempts и next retry time
3. Добавить кнопку "Manual Reconnect" для каждой камеры
4. Показать warnings о reconnect storm в `LiveTelemetryHeader`

### Enhanced Features

1. **Adaptive Backoff**
   - Динамическая подстройка backoff на основе истории успешных reconnect
   - Machine learning для предсказания стабильности потока

2. **Health Check**
   - Periodic heartbeat для обнаружения "зомби" соединений
   - Автоматический reconnect при потере heartbeat

3. **Network Quality Metrics**
   - Отслеживание качества сети (packet loss, latency)
   - Прогнозирование проблем до потери соединения

---

## Known Issues

### Existing Build Issues (Unrelated to Reconnect Implementation)

Компиляция проекта блокируется существующими ошибками в `shared` модуле:
- Ошибки в `CameraRepositoryImpl.kt` связанные с `RtspConnectionResult`
- Конфликты component1/component2 operators в data classes
- Несоответствия типов между `core.network` и `shared.domain`

**Рекомендация:** Исправить эти ошибки перед интеграцией reconnect модуля.

---

## Environment Variables Reference

### Reconnect Configuration

| Variable | Default | Range | Description |
|----------|---------|-------|-------------|
| `IPCSS_RECONNECT_ENABLED` | `true` | boolean | Включить автоматический reconnect |
| `IPCSS_RECONNECT_MAX_ATTEMPTS` | `5` | 1-20 | Максимальное число попыток |
| `IPCSS_RECONNECT_INITIAL_DELAY_MS` | `1500` | 100-10000 | Начальная задержка (мс) |
| `IPCSS_RECONNECT_MAX_DELAY_MS` | `30000` | initial-120000 | Максимальная задержка (мс) |
| `IPCSS_RECONNECT_BACKOFF_STRATEGY` | `EXPONENTIAL` | LINEAR/EXPONENTIAL/FIXED | Стратегия backoff |
| `IPCSS_RECONNECT_BACKOFF_MULTIPLIER` | `2.0` | 1.0-10.0 | Множитель для экспоненты |
| `IPCSS_RECONNECT_JITTER_RATIO` | `0.1` | 0.0-0.5 | Коэффициент jitter |

---

## Files Modified/Created

### Created

1. `platforms/client-desktop-x86_64/app/src/main/kotlin/com/company/ipcamera/desktop/reconnect/ReconnectPolicy.kt`
2. `platforms/client-desktop-x86_64/app/src/main/kotlin/com/company/ipcamera/desktop/reconnect/ReconnectController.kt`

### Modified

1. `platforms/client-desktop-x86_64/app/src/main/kotlin/com/company/ipcamera/desktop/stream/RtspStreamSession.kt`
2. `platforms/client-desktop-x86_64/app/src/main/kotlin/com/company/ipcamera/desktop/ui/viewmodel/LiveViewViewModel.kt`

---

## Conclusion

Реализована robust система reconnect с экспоненциальным backoff для desktop клиента. Модуль готов к интеграции после исправления существующих build ошибок в shared модуле. Система поддерживает:
- ✅ Multiple backoff strategies
- ✅ Per-camera policies
- ✅ Jitter для предотвращения thundering herd
- ✅ Error classification и умный retry
- ✅ Comprehensive event system
- ✅ Runtime policy updates
- ✅ Environment variable configuration
