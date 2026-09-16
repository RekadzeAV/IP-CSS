# ONVIF Event Integration Service

**Версия:** 1.0
**Дата:** 2026-01-26
**Статус:** ✅ Реализовано

---

## Обзор

`OnvifEventIntegrationService` - сервис для автоматической интеграции ONVIF событий с системой событий приложения. Он автоматически подписывается на события от ONVIF камер, обрабатывает их и сохраняет в `EventRepository`.

## Возможности

- ✅ Автоматическая подписка на события через PullPoint
- ✅ Фоновый мониторинг событий от камер
- ✅ Автоматический маппинг ONVIF событий в события приложения
- ✅ Сохранение событий в EventRepository
- ✅ Поддержка множественных камер
- ✅ Управление жизненным циклом подписок

---

## Сценарии подписки на события (сервер API)

При включённой конфигурации ONVIF Events (`ONVIF_EVENTS_ENABLED=true`, по умолчанию) сервер API ведёт себя следующим образом.

| Сценарий | Поведение |
|----------|-----------|
| **Старт API** | При запуске сервера для каждой камеры с URL `http://` или `https://` создаётся подписка на ONVIF-события (PullPoint). Ошибки по отдельным камерам логируются, не блокируя подписку остальных. |
| **Добавление камеры** | После успешного создания камеры через `POST /api/v1/cameras` сервер автоматически подписывается на события этой камеры (если URL подходит для ONVIF). |
| **Обновление камеры** | После успешного обновления камеры через `PUT /api/v1/cameras/{id}` существующая подписка снимается и при необходимости создаётся заново (переподписка). |
| **Удаление камеры** | После успешного удаления камеры через `DELETE /api/v1/cameras/{id}` или `POST /api/v1/cameras/bulk/delete` сервер отписывается от событий этой камеры. |
| **Перезапуск сервера** | При следующем старте API подписки восстанавливаются по списку камер из репозитория (как в сценарии «Старт API»). |

---

### Детальные сценарии жизненного цикла подписок

Ниже описаны три основных сценария: что происходит при добавлении камеры, при удалении и при перезапуске (восстановление подписок).

#### 1. Добавление камеры → авто-подписка

**Условия:** ONVIF Events включён (`ONVIF_EVENTS_ENABLED=true`), камера имеет URL `http://` или `https://` (подходит для ONVIF).

**Последовательность:**

1. Пользователь создаёт камеру через `POST /api/v1/cameras` (тело: имя, URL, учётные данные и т.д.).
2. `CameraRoutes` вызывает `cameraRepository.addCamera(camera)`; при успехе возвращается `createdCamera`.
3. После успешного сохранения проверяется `onvifEventsConfig.enabled` и `onvifSubscriptionService.supportsOnvifEvents(createdCamera)`.
4. В фоне (`CoroutineScope(Dispatchers.Default).launch`) вызывается `onvifSubscriptionService.subscribeToCameraEvents(createdCamera.id, usePullPoint = onvifEventsConfig.usePullPointByDefault)`.
5. При ошибке подписки пишется лог, ответ клиенту уже отправлен (Created) — подписка не блокирует создание камеры.
6. Дальнейшие события от камеры обрабатываются в `OnvifEventSubscriptionService` (PullPoint) и передаются в `EventService.createEvent` → WebSocket.

**Где в коде:** `server/api/.../routing/CameraRoutes.kt` (post `/cameras`, блок после `addCamera`).

---

#### 2. Удаление камеры → отписка

**Сценарий A — удаление одной камеры**

1. Пользователь удаляет камеру через `DELETE /api/v1/cameras/{id}`.
2. После успешного `cameraRepository.deleteCamera(id)` вызывается `onvifSubscriptionService.unsubscribeFromCameraEvents(id)`.
3. Сервис снимает подписку (Unsubscribe по существующему subscription), очищает внутреннее состояние по этой камере.
4. Ошибка отписки логируется (`logger.warn`), ответ клиенту не отменяется.

**Где в коде:** `CameraRoutes.kt`, обработчик `DELETE /api/v1/cameras/{id}`.

**Сценарий B — массовое удаление**

1. Запрос `POST /api/v1/cameras/bulk/delete` с телом `{ "cameraIds": ["id1", "id2", ...] }`.
2. Для каждого `id` из списка после удаления из репозитория вызывается `onvifSubscriptionService.unsubscribeFromCameraEvents(id)`.
3. Для каждой камеры подписка снимается; ошибки логируются по отдельности.

**Где в коде:** `CameraRoutes.kt`, обработчик bulk delete.

---

#### 3. Перезапуск → восстановление подписок

**Сервер API**

1. При старте приложения в `Application.module()` после настройки маршрутов и DI в фоне запускается:
   - `onvifSubscriptionService.subscribeToAllCamerasAtStartup()`.
2. Метод получает список камер из `CameraRepository.getCameras()` и для каждой камеры с подходящим URL вызывает `subscribeToCameraEvents(...)`.
3. Ошибки по отдельным камерам логируются; подписка остальных не блокируется. В лог пишется итог: «subscribed to X of Y cameras at startup».
4. Таким образом, после любого перезапуска сервера все текущие камеры из БД снова получают подписки на ONVIF-события.

**Где в коде:** `server/api/.../Application.kt` (после `cameraService.startMonitoring()`); логика подписки — `OnvifEventSubscriptionService.subscribeToAllCamerasAtStartup()`.

**Клиенты (Desktop / Android)**

1. **Старт приложения:** при запуске вызывается `CameraEventMonitoringService.initialize()` (Desktop — из `App.kt`, Android — из `MainActivity`). Внутри запрашиваются камеры из `CameraRepository`, для каждой с поддержкой ONVIF запускается `OnvifEventIntegrationService.startMonitoring(...)`.
2. **Периодическое восстановление:** раз в 5 минут (`DEFAULT_ONVIF_RETRY_INTERVAL_MS`) фоновая задача в `CameraEventMonitoringService` вызывает `ensureMonitoringForAllCameras()`: для камер, у которых ещё нет активного мониторинга, снова запускается подписка (восстановление после появления сети или камеры).
3. **Возврат в приложение (Android):** в `MainActivity.onResume()` вызывается `eventMonitoringService.ensureMonitoring()` — однократная попытка подписать все камеры, которые ещё не в мониторинге.

Итого: при перезапуске клиента подписки восстанавливаются при `initialize()`; при временной недоступности сети/камеры — при следующем цикле таймера или при вызове `ensureMonitoring()` (например, при возврате в приложение).

---

### Конфигурация ONVIF Events

Подписки на события ONVIF управляются конфигом приложения. Все параметры задаются **переменными окружения** и загружаются через `OnvifEventsConfig.fromEnvironment()` (сервер API, `server/api/.../config/OnvifEventsConfig.kt`).

| Переменная | Описание | По умолчанию | Допустимые значения |
|------------|----------|--------------|----------------------|
| `ONVIF_EVENTS_ENABLED` | Включить авто-подписки при старте и при add/update камеры | `true` | `true`/`false`, `1`/`0`, `yes`/`no` |
| `ONVIF_SUBSCRIPTION_TIME_SEC` | Время жизни подписки в секундах (для CreatePullPointSubscription и Renew) | `3600` | ≥ 60 |
| `ONVIF_PULL_INTERVAL_MS` | Интервал опроса PullPoint (мс) | `5000` | ≥ 1000 |
| `ONVIF_PULL_TIMEOUT_MS` | Таймаут запроса PullMessages (мс) | `500` | 100–30000 |
| `ONVIF_USE_PULL_POINT` | Использовать PullPoint по умолчанию | `true` | `true`/`false` |

Полное описание переменных и примеры — в [ENVIRONMENT_VARIABLES.md](ENVIRONMENT_VARIABLES.md) (раздел «ONVIF Events»).

---

## Клиенты (Desktop/Android): ошибки и переподписка

На клиенте мониторинг событий ведёт `CameraEventMonitoringService` и `OnvifEventIntegrationService`.

| Ситуация | Поведение |
|----------|-----------|
| **Нет сети / камера недоступна** | Ошибки логируются (`logger.warn`/`logger.error`), приложение не падает. Подписка для этой камеры прекращается после нескольких неудачных попыток в цикле PullPoint. |
| **Переподписка по таймеру** | Каждые 5 минут (значение по умолчанию, `DEFAULT_ONVIF_RETRY_INTERVAL_MS`) сервис проверяет список камер и для тех, у кого нет активного мониторинга, снова вызывает подписку (восстановление после появления сети или камеры). |
| **Возврат в приложение (Android)** | В `MainActivity.onResume()` вызывается `ensureMonitoring()` — однократная попытка подписать все камеры, которые ещё не в мониторинге. |
| **Desktop** | Переподписка только по таймеру (периодическая задача в `CameraEventMonitoringService`). |

Публичный метод `CameraEventMonitoringService.ensureMonitoring()` можно вызывать при возврате в приложение или при восстановлении сети (если платформа это отслеживает).

---

## Полный путь до веб/мобильного UI (события и WebSocket)

Цепочка от ONVIF до отображения в UI проверена и реализована следующим образом.

### Сервер

1. **ONVIF → событие**  
   `OnvifEventSubscriptionService` (или клиентский `OnvifEventIntegrationService`) получает событие от камеры, маппит в доменную модель и вызывает `EventService.createEvent(...)`.

2. **EventService**  
   `createEvent` сохраняет событие через `eventRepository.addEvent(event)` и в корутине вызывает `sendEventViaWebSocket(createdEvent)`.

3. **WebSocket**  
   `WebSocketManager.broadcastEvent(channel = WebSocketChannel.EVENTS, type = "event.created", data = jsonData)` рассылает подписчикам канала `events` сообщение с полями `id`, `cameraId`, `cameraName`, `type`, `severity`, `timestamp`, `description`, `metadata`, `acknowledged` и т.д. Аналогично отправляются `event.acknowledged` и `event.deleted`.

### Веб-клиент (Next.js)

- **Подписка:** В `WebSocketProvider` при `connected` выполняется `subscribe(['cameras', 'events', 'recordings', 'notifications'])`, в т.ч. на канал `events`.
- **Приём:** В `useWebSocket` обрабатываются типы `event.created`, `event.acknowledged`, `event.deleted`; по каналу `events` диспатчатся `addEventFromWebSocket`, `updateEventPartiallyFromWebSocket`, `removeEventFromWebSocket` в `eventsSlice`.
- **Список событий:** Страница `/events` читает `state.events` из Redux; при приходе `event.created` список обновляется в реальном времени.
- **Уведомления:** На странице событий для новых критических (CRITICAL) событий показывается snackbar (см. `app/events/page.tsx`, эффект с `enqueueSnackbar`).

Итог: полный путь **ONVIF → EventService → WebSocket (EVENTS) → веб Redux → список событий и уведомления** на веб-клиенте реализован и работает.

**Фильтры и обновление в реальном времени (веб):**

- На странице `/events` доступны фильтры: **камера**, **тип события**, **уровень важности**, **источник** (ONVIF / система), **статус подтверждения**. При применении фильтров выполняется запрос к API и в Redux сохраняются `filters`; при открытии диалога фильтров значения подтягиваются из Redux.
- События, приходящие по WebSocket (`event.created`), добавляются в список только если соответствуют текущим фильтрам (type, cameraId, severity, acknowledged) — см. `addEventFromWebSocket` в `eventsSlice`. Фильтр по источнику (ONVIF/другое) применяется при отрисовке к уже загруженному списку.
- Есть кнопка **«Сбросить фильтры»** и индикация активных фильтров на кнопке «Фильтры».

### Desktop и Android

- Список событий загружается через **REST** (`GetEventsUseCase` / `EventRepository.getEvents`). Подписка на канал `events` по WebSocket в мобильном и десктопном приложениях не используется — обновление списка только по повторной загрузке (pull). При необходимости в будущем можно добавить подписку на WebSocket и обновление списка в реальном времени по аналогии с веб-клиентом.

---

## Использование

### Базовое использование

```kotlin
import com.company.ipcamera.core.network.onvif.*
import com.company.ipcamera.shared.domain.repository.EventRepository

// Создание сервиса
val eventService = OnvifEventServiceImpl(engine)
val eventRepository: EventRepository = // ... получить из DI
val integrationService = OnvifEventIntegrationService(
    eventService = eventService,
    eventRepository = eventRepository
)

// Запуск мониторинга для камеры
val result = integrationService.startMonitoring(
    cameraId = "camera-1",
    cameraName = "Front Door Camera",
    cameraUrl = "http://192.168.1.100",
    username = "admin",
    password = "password123",
    pullInterval = 5000 // Опрос каждые 5 секунд
)

result.fold(
    onSuccess = {
        println("Event monitoring started for camera-1")
    },
    onFailure = { error ->
        println("Failed to start monitoring: ${error.message}")
    }
)

// Остановка мониторинга
integrationService.stopMonitoring("camera-1")
```

### Мониторинг нескольких камер

```kotlin
// Запуск мониторинга для нескольких камер
val cameras = listOf(
    CameraInfo("camera-1", "Front Door", "http://192.168.1.100", "admin", "pass1"),
    CameraInfo("camera-2", "Back Door", "http://192.168.1.101", "admin", "pass2"),
    CameraInfo("camera-3", "Garage", "http://192.168.1.102", "admin", "pass3")
)

cameras.forEach { camera ->
    integrationService.startMonitoring(
        cameraId = camera.id,
        cameraName = camera.name,
        cameraUrl = camera.url,
        username = camera.username,
        password = camera.password
    )
}

// Получить список камер с активным мониторингом
val monitoredCameras = integrationService.getMonitoredCameras()
println("Monitoring ${monitoredCameras.size} cameras")

// Остановить все мониторинги
integrationService.stopAll()
```

### Проверка статуса мониторинга

```kotlin
// Проверить, активен ли мониторинг для камеры
if (integrationService.isMonitoring("camera-1")) {
    println("Camera-1 is being monitored")
}

// Получить список всех камер с активным мониторингом
val activeCameras = integrationService.getMonitoredCameras()
activeCameras.forEach { cameraId ->
    println("Monitoring: $cameraId")
}
```

---

## Маппинг ONVIF событий

Сервис автоматически маппит ONVIF события в события приложения:

| ONVIF Topic | EventType | EventSeverity |
|-------------|-----------|---------------|
| `tns1:VideoSource/MotionAlarm` | `MOTION_DETECTION` | `INFO` |
| `tns1:RuleEngine/IntrusionDetector` | `OBJECT_DETECTION` | `CRITICAL` |
| `tns1:RuleEngine/LineDetector/Crossed` | `OBJECT_DETECTION` | `WARNING` |
| `tns1:RuleEngine/LoiteringDetector` | `OBJECT_DETECTION` | `CRITICAL` |
| `tns1:Device/TamperDetected` | `OTHER` | `CRITICAL` |
| `tns1:VideoSource/VideoSourceLost` | `CAMERA_OFFLINE` | `WARNING` |
| `tns1:VideoSource/VideoSourceRecovered` | `CAMERA_ONLINE` | `INFO` |
| `tns1:Device/IO/PortState` | `OTHER` | `WARNING` |
| `tns1:VideoSource/VideoSourceConfiguration` | `OTHER` | `INFO` |
| `tns1:System/SystemDateTimeChanged` | `OTHER` | `INFO` |
| `tns1:Analytics/AnalyticsStream` | `OTHER` | `INFO` |
| `tns1:System/*Error*` (системная ошибка) | `SYSTEM_ERROR` | `WARNING` |
| `tns1:Device/StorageFailure`, `*Storage*Full*` | `STORAGE_FULL` | `WARNING` |
| `tns1:Device/HardwareFailure` | `SYSTEM_ERROR` | `WARNING` |

Маппинг реализован в `OnvifEventMapper` (сервер API) и в `OnvifEventIntegrationService` (shared). Константы топиков и хелперы — в `core/network` → `OnvifEventType`.

### Кастомный маппинг

Для изменения логики маппинга можно расширить `OnvifEventIntegrationService`:

```kotlin
class CustomOnvifEventIntegrationService(
    eventService: OnvifEventService,
    eventRepository: EventRepository
) : OnvifEventIntegrationService(eventService, eventRepository) {

    override fun mapOnvifTopicToEventType(topic: String): EventType {
        // Кастомная логика маппинга
        return when {
            topic.contains("CustomEvent") -> EventType.OTHER
            else -> super.mapOnvifTopicToEventType(topic)
        }
    }
}
```

---

## Интеграция с приложением

### Android

```kotlin
class CameraViewModel(
    private val integrationService: OnvifEventIntegrationService,
    private val eventRepository: EventRepository
) : ViewModel() {

    fun startMonitoringCamera(camera: Camera) {
        viewModelScope.launch {
            val result = integrationService.startMonitoring(
                cameraId = camera.id,
                cameraName = camera.name,
                cameraUrl = camera.url,
                username = camera.username,
                password = camera.password
            )

            result.fold(
                onSuccess = {
                    // Показать уведомление об успехе
                },
                onFailure = { error ->
                    // Показать ошибку
                }
            )
        }
    }

    fun observeEvents(cameraId: String) = flow {
        while (true) {
            val events = eventRepository.getEvents(
                cameraId = cameraId,
                limit = 50
            )
            emit(events.data)
            delay(5000) // Обновление каждые 5 секунд
        }
    }
}
```

### iOS

```swift
class CameraService {
    private let integrationService: OnvifEventIntegrationService

    func startMonitoring(camera: Camera) async throws {
        try await integrationService.startMonitoring(
            cameraId: camera.id,
            cameraName: camera.name,
            cameraUrl: camera.url,
            username: camera.username,
            password: camera.password
        )
    }

    func stopMonitoring(cameraId: String) async throws {
        try await integrationService.stopMonitoring(cameraId: cameraId)
    }
}
```

### Desktop/Server

```kotlin
class CameraMonitoringService(
    private val integrationService: OnvifEventIntegrationService,
    private val cameraRepository: CameraRepository
) {

    suspend fun startMonitoringAllCameras() {
        val cameras = cameraRepository.getAllCameras()

        cameras.forEach { camera ->
            if (camera.onvifEnabled) {
                integrationService.startMonitoring(
                    cameraId = camera.id,
                    cameraName = camera.name,
                    cameraUrl = camera.url,
                    username = camera.username,
                    password = camera.password
                )
            }
        }
    }

    suspend fun stopMonitoringAllCameras() {
        integrationService.stopAll()
    }
}
```

---

## Конфигурация

### Параметры мониторинга

```kotlin
integrationService.startMonitoring(
    cameraId = "camera-1",
    cameraName = "Camera",
    cameraUrl = "http://192.168.1.100",
    username = "admin",
    password = "password",
    pullInterval = 5000 // Интервал опроса в миллисекундах (по умолчанию 5000)
)
```

**Рекомендации по `pullInterval`:**
- **1000-2000 мс** - для камер с частыми событиями (детекция движения)
- **5000 мс** - стандартное значение (баланс между производительностью и задержкой)
- **10000 мс** - для камер с редкими событиями (экономия ресурсов)

---

## Обработка ошибок

```kotlin
val result = integrationService.startMonitoring(
    cameraId = "camera-1",
    cameraName = "Camera",
    cameraUrl = "http://192.168.1.100",
    username = "admin",
    password = "password"
)

result.fold(
    onSuccess = {
        println("Monitoring started successfully")
    },
    onFailure = { error ->
        when (error) {
            is OnvifAuthenticationException -> {
                println("Authentication failed: ${error.message}")
            }
            is OnvifNotSupportedException -> {
                println("Event service not supported: ${error.message}")
            }
            is OnvifNetworkException -> {
                println("Network error: ${error.message}")
            }
            else -> {
                println("Unknown error: ${error.message}")
            }
        }
    }
)
```

---

## Жизненный цикл

### Запуск при старте приложения

```kotlin
class Application : Application() {
    private lateinit var integrationService: OnvifEventIntegrationService

    override fun onCreate() {
        super.onCreate()

        // Инициализация сервиса
        val eventService = OnvifEventServiceImpl(httpClientEngine)
        val eventRepository = EventRepositoryImplSqlDelight(database)
        integrationService = OnvifEventIntegrationService(
            eventService = eventService,
            eventRepository = eventRepository
        )

        // Запуск мониторинга для всех камер
        lifecycleScope.launch {
            startMonitoringForAllCameras()
        }
    }

    private suspend fun startMonitoringForAllCameras() {
        // Получить все камеры из репозитория
        val cameras = cameraRepository.getAllCameras()

        cameras.forEach { camera ->
            integrationService.startMonitoring(
                cameraId = camera.id,
                cameraName = camera.name,
                cameraUrl = camera.url,
                username = camera.username,
                password = camera.password
            )
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        // Остановить все мониторинги при завершении приложения
        runBlocking {
            integrationService.stopAll()
        }
    }
}
```

---

## Производительность

### Оптимизация

1. **Используйте разумный `pullInterval`**
   - Слишком частые опросы увеличивают нагрузку на камеру
   - Слишком редкие опросы увеличивают задержку обнаружения событий

2. **Фильтрация событий на уровне камеры**
   ```kotlin
   val filter = OnvifEventFilter(
       topicExpression = "tns1:VideoSource/MotionAlarm" // Только события движения
   )

   integrationService.startMonitoring(
       cameraId = "camera-1",
       cameraName = "Camera",
       cameraUrl = "http://192.168.1.100",
       filter = filter
   )
   ```

3. **Мониторинг только активных камер**
   - Не запускайте мониторинг для отключенных камер
   - Останавливайте мониторинг при отключении камеры

---

## Тестирование

### Unit-тесты

```kotlin
class OnvifEventIntegrationServiceTest {

    @Test
    fun testStartMonitoring() = runTest {
        val eventService = MockOnvifEventService()
        val eventRepository = MockEventRepository()
        val integrationService = OnvifEventIntegrationService(
            eventService = eventService,
            eventRepository = eventRepository
        )

        val result = integrationService.startMonitoring(
            cameraId = "camera-1",
            cameraName = "Test Camera",
            cameraUrl = "http://192.168.1.100"
        )

        assertTrue(result.isSuccess)
        assertTrue(integrationService.isMonitoring("camera-1"))
    }
}
```

### Integration-тесты

```kotlin
class OnvifEventIntegrationServiceIntegrationTest {

    @Test
    fun testRealCameraIntegration() = runTest {
        // Тест с реальной камерой (требует доступ к тестовой камере)
        val eventService = OnvifEventServiceImpl(realHttpClientEngine)
        val eventRepository = EventRepositoryImplSqlDelight(realDatabase)
        val integrationService = OnvifEventIntegrationService(
            eventService = eventService,
            eventRepository = eventRepository
        )

        val result = integrationService.startMonitoring(
            cameraId = "test-camera",
            cameraName = "Test Camera",
            cameraUrl = "http://192.168.1.100",
            username = "admin",
            password = "password"
        )

        assertTrue(result.isSuccess)

        // Подождать получения событий
        delay(10000)

        // Проверить, что события были сохранены
        val events = eventRepository.getEvents(cameraId = "test-camera")
        assertTrue(events.data.isNotEmpty())
    }
}
```

---

## Troubleshooting

### Проблема: Мониторинг не запускается

**Возможные причины:**
1. Камера не поддерживает Event Service
2. Неверные учетные данные
3. Сетевая ошибка

**Решение:**
```kotlin
// Проверить поддержку Event Service
val capabilities = onvifClient.getCapabilities(cameraUrl, username, password)
if (capabilities?.eventServiceUrl == null) {
    println("Camera does not support Event Service")
}

// Проверить подключение
val testResult = onvifClient.testConnection(cameraUrl, username, password)
when (testResult) {
    is ConnectionTestResult.Success -> {
        println("Connection OK")
    }
    is ConnectionTestResult.Failure -> {
        println("Connection failed: ${testResult.error}")
    }
}
```

### Проблема: События не сохраняются

**Возможные причины:**
1. EventRepository не инициализирован
2. Ошибка при сохранении в базу данных
3. События не приходят от камеры

**Решение:**
```kotlin
// Проверить логи
// События должны логироваться при получении

// Проверить подписку
val subscriptions = eventService.getSubscriptionsForCamera(cameraUrl)
if (subscriptions.isEmpty()) {
    println("No active subscriptions")
}

// Проверить события вручную
val events = eventService.pullMessages(subscriptionId, timeout = 5000)
events.fold(
    onSuccess = { onvifEvents ->
        println("Received ${onvifEvents.size} events")
    },
    onFailure = { error ->
        println("Failed to pull events: ${error.message}")
    }
)
```

---

## API Reference

### OnvifEventIntegrationService

#### Методы

##### `startMonitoring()`
Запустить мониторинг событий для камеры.

```kotlin
suspend fun startMonitoring(
    cameraId: String,
    cameraName: String,
    cameraUrl: String,
    username: String? = null,
    password: String? = null,
    pullInterval: Long = 5000
): Result<Unit>
```

**Параметры:**
- `cameraId` - ID камеры в системе
- `cameraName` - Имя камеры
- `cameraUrl` - URL камеры (например, `http://192.168.1.100`)
- `username` - Имя пользователя (опционально)
- `password` - Пароль (опционально)
- `pullInterval` - Интервал опроса событий в миллисекундах (по умолчанию 5000)

**Возвращает:** `Result<Unit>` - успех или ошибка

##### `stopMonitoring()`
Остановить мониторинг событий для камеры.

```kotlin
suspend fun stopMonitoring(cameraId: String): Result<Unit>
```

**Параметры:**
- `cameraId` - ID камеры

**Возвращает:** `Result<Unit>` - успех или ошибка

##### `stopAll()`
Остановить все активные мониторинги.

```kotlin
suspend fun stopAll(): Result<Unit>
```

**Возвращает:** `Result<Unit>` - успех или ошибка

##### `isMonitoring()`
Проверить, активен ли мониторинг для камеры.

```kotlin
fun isMonitoring(cameraId: String): Boolean
```

**Параметры:**
- `cameraId` - ID камеры

**Возвращает:** `Boolean` - true если мониторинг активен

##### `getMonitoredCameras()`
Получить список камер с активным мониторингом.

```kotlin
fun getMonitoredCameras(): List<String>
```

**Возвращает:** `List<String>` - список ID камер

---

## Связанные документы

- **[ONVIF_CLIENT.md](ONVIF_CLIENT.md)** - Основная документация ONVIF клиента
- **[ONVIF_CLIENT_STAGE_4.4_DETAILS.md](../archive/docs/onvif/ONVIF_CLIENT_STAGE_4.4_DETAILS.md)** - Детализация этапа 4.4
- **[ONVIF_CLIENT_IMPLEMENTATION_PLAN.md](../archive/docs/onvif/ONVIF_CLIENT_IMPLEMENTATION_PLAN.md)** - План реализации

---

**Последнее обновление:** 2026-03-01 (добавлены детальные сценарии жизненного цикла подписок: добавление → авто-подписка, удаление → отписка, перезапуск → восстановление)
