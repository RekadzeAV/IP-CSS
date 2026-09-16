# Примеры интеграции ONVIF Event Integration Service

**Версия:** 1.0
**Дата:** 2026-01-26

---

## Пример 1: Базовое использование в Android приложении

```kotlin
class CameraMonitoringManager(
    private val integrationService: OnvifEventIntegrationService,
    private val eventRepository: EventRepository
) {

    /**
     * Запустить мониторинг для камеры
     */
    suspend fun startMonitoringCamera(camera: Camera) {
        val result = integrationService.startMonitoring(
            cameraId = camera.id,
            cameraName = camera.name,
            cameraUrl = camera.url,
            username = camera.username,
            password = camera.password,
            pullInterval = 5000
        )

        result.fold(
            onSuccess = {
                Log.d(TAG, "Started monitoring camera: ${camera.id}")
            },
            onFailure = { error ->
                Log.e(TAG, "Failed to start monitoring: ${error.message}", error)
            }
        )
    }

    /**
     * Остановить мониторинг для камеры
     */
    suspend fun stopMonitoringCamera(cameraId: String) {
        val result = integrationService.stopMonitoring(cameraId)
        result.fold(
            onSuccess = {
                Log.d(TAG, "Stopped monitoring camera: $cameraId")
            },
            onFailure = { error ->
                Log.e(TAG, "Failed to stop monitoring: ${error.message}", error)
            }
        )
    }

    /**
     * Получить события для камеры
     */
    fun observeCameraEvents(cameraId: String): Flow<List<Event>> = flow {
        while (true) {
            val result = eventRepository.getEvents(
                cameraId = cameraId,
                limit = 50
            )
            emit(result.items)
            delay(5000) // Обновление каждые 5 секунд
        }
    }

    companion object {
        private const val TAG = "CameraMonitoringManager"
    }
}
```

### Использование в ViewModel

```kotlin
class CameraViewModel(
    private val monitoringManager: CameraMonitoringManager
) : ViewModel() {

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    fun startMonitoring(camera: Camera) {
        viewModelScope.launch {
            monitoringManager.startMonitoringCamera(camera)
            _isMonitoring.value = true

            // Подписка на события
            monitoringManager.observeCameraEvents(camera.id)
                .collect { events ->
                    _events.value = events
                }
        }
    }

    fun stopMonitoring(cameraId: String) {
        viewModelScope.launch {
            monitoringManager.stopMonitoringCamera(cameraId)
            _isMonitoring.value = false
        }
    }
}
```

---

## Пример 2: Интеграция с DI (Koin)

```kotlin
// Модуль DI
val onvifModule = module {
    // OnvifClient
    single<OnvifClient> {
        val engine = get<HttpClientEngine>()
        OnvifClient(engine)
    }

    // OnvifEventService
    single<OnvifEventService> {
        val engine = get<HttpClientEngine>()
        OnvifEventServiceImpl(engine)
    }

    // OnvifEventIntegrationService
    single<OnvifEventIntegrationService> {
        val eventService = get<OnvifEventService>()
        val eventRepository = get<EventRepository>()
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        OnvifEventIntegrationService(
            eventService = eventService,
            eventRepository = eventRepository,
            scope = scope
        )
    }

    // CameraMonitoringManager
    single<CameraMonitoringManager> {
        val integrationService = get<OnvifEventIntegrationService>()
        val eventRepository = get<EventRepository>()
        CameraMonitoringManager(integrationService, eventRepository)
    }
}

// Использование
class MainActivity : AppCompatActivity() {
    private val monitoringManager: CameraMonitoringManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Запустить мониторинг при старте приложения
        lifecycleScope.launch {
            val cameras = cameraRepository.getAllCameras()
            cameras.forEach { camera ->
                monitoringManager.startMonitoringCamera(camera)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Остановить мониторинг при завершении
        lifecycleScope.launch {
            monitoringManager.stopAll()
        }
    }
}
```

---

## Пример 3: Автоматический запуск при добавлении камеры

```kotlin
class CameraRepositoryWithEventMonitoring(
    private val cameraRepository: CameraRepository,
    private val integrationService: OnvifEventIntegrationService
) : CameraRepository by cameraRepository {

    override suspend fun addCamera(camera: Camera): Result<Camera> {
        val result = cameraRepository.addCamera(camera)

        result.fold(
            onSuccess = { addedCamera ->
                // Автоматически запустить мониторинг событий
                if (addedCamera.onvifEnabled) {
                    integrationService.startMonitoring(
                        cameraId = addedCamera.id,
                        cameraName = addedCamera.name,
                        cameraUrl = addedCamera.url,
                        username = addedCamera.username,
                        password = addedCamera.password
                    )
                }
            },
            onFailure = { /* Обработка ошибки */ }
        )

        return result
    }

    override suspend fun deleteCamera(cameraId: String): Result<Unit> {
        // Остановить мониторинг перед удалением
        integrationService.stopMonitoring(cameraId)

        return cameraRepository.deleteCamera(cameraId)
    }

    override suspend fun updateCamera(camera: Camera): Result<Camera> {
        val result = cameraRepository.updateCamera(camera)

        result.fold(
            onSuccess = { updatedCamera ->
                // Перезапустить мониторинг если изменились учетные данные
                if (updatedCamera.onvifEnabled) {
                    integrationService.stopMonitoring(updatedCamera.id)
                    integrationService.startMonitoring(
                        cameraId = updatedCamera.id,
                        cameraName = updatedCamera.name,
                        cameraUrl = updatedCamera.url,
                        username = updatedCamera.username,
                        password = updatedCamera.password
                    )
                } else {
                    integrationService.stopMonitoring(updatedCamera.id)
                }
            },
            onFailure = { /* Обработка ошибки */ }
        )

        return result
    }
}
```

---

## Пример 4: Обработка событий с уведомлениями

```kotlin
class EventNotificationService(
    private val eventRepository: EventRepository,
    private val notificationManager: NotificationManager
) {

    /**
     * Обработка новых событий с отправкой уведомлений
     */
    suspend fun processNewEvents() {
        val recentEvents = eventRepository.getEvents(
            acknowledged = false,
            startTime = System.currentTimeMillis() - 60000, // Последняя минута
            limit = 100
        )

        recentEvents.items.forEach { event ->
            when (event.severity) {
                EventSeverity.CRITICAL -> {
                    sendCriticalNotification(event)
                }
                EventSeverity.ERROR -> {
                    sendErrorNotification(event)
                }
                EventSeverity.WARNING -> {
                    sendWarningNotification(event)
                }
                EventSeverity.INFO -> {
                    // Не отправляем уведомления для INFO событий
                }
            }
        }
    }

    private suspend fun sendCriticalNotification(event: Event) {
        val notification = NotificationCompat.Builder(context, CHANNEL_CRITICAL)
            .setSmallIcon(R.drawable.ic_alert)
            .setContentTitle("Critical Event: ${event.cameraName}")
            .setContentText(event.description ?: event.type.name)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(event.id.hashCode(), notification)
    }

    private suspend fun sendErrorNotification(event: Event) {
        // Аналогично для ERROR
    }

    private suspend fun sendWarningNotification(event: Event) {
        // Аналогично для WARNING
    }
}

// Использование
class EventObserverService : Service() {
    private val eventNotificationService: EventNotificationService by inject()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleScope.launch {
            while (true) {
                eventNotificationService.processNewEvents()
                delay(10000) // Проверка каждые 10 секунд
            }
        }
        return START_STICKY
    }
}
```

---

## Пример 5: Статистика событий

```kotlin
class EventStatisticsService(
    private val eventRepository: EventRepository
) {

    /**
     * Получить статистику событий за период
     */
    suspend fun getEventStatistics(
        cameraId: String? = null,
        startTime: Long,
        endTime: Long
    ): EventStatistics {
        val result = eventRepository.getEventStatistics(
            cameraId = cameraId,
            startTime = startTime,
            endTime = endTime
        )

        return result.fold(
            onSuccess = { stats ->
                EventStatistics(
                    total = stats["total"] as? Int ?: 0,
                    byType = stats["byType"] as? Map<EventType, Int> ?: emptyMap(),
                    bySeverity = stats["bySeverity"] as? Map<EventSeverity, Int> ?: emptyMap()
                )
            },
            onFailure = { error ->
                EventStatistics(0, emptyMap(), emptyMap())
            }
        )
    }

    /**
     * Получить график событий по времени
     */
    suspend fun getEventTimeline(
        cameraId: String? = null,
        startTime: Long,
        endTime: Long,
        intervalMinutes: Int = 60
    ): List<EventTimelinePoint> {
        val events = eventRepository.getEvents(
            cameraId = cameraId,
            startTime = startTime,
            endTime = endTime,
            limit = 10000
        )

        // Группировка по интервалам
        val timeline = mutableMapOf<Long, Int>()
        var currentTime = startTime

        while (currentTime < endTime) {
            timeline[currentTime] = 0
            currentTime += intervalMinutes * 60 * 1000
        }

        events.items.forEach { event ->
            val interval = (event.timestamp / (intervalMinutes * 60 * 1000)) * (intervalMinutes * 60 * 1000)
            timeline[interval] = (timeline[interval] ?: 0) + 1
        }

        return timeline.map { (time, count) ->
            EventTimelinePoint(time, count)
        }.sortedBy { it.time }
    }
}

data class EventStatistics(
    val total: Int,
    val byType: Map<EventType, Int>,
    val bySeverity: Map<EventSeverity, Int>
)

data class EventTimelinePoint(
    val time: Long,
    val count: Int
)
```

---

## Пример 6: Фильтрация и поиск событий

```kotlin
class EventSearchService(
    private val eventRepository: EventRepository
) {

    /**
     * Поиск событий по тексту
     */
    suspend fun searchEvents(
        query: String,
        cameraId: String? = null
    ): List<Event> {
        val allEvents = eventRepository.getEvents(
            cameraId = cameraId,
            limit = 1000
        )

        return allEvents.items.filter { event ->
            event.description?.contains(query, ignoreCase = true) == true ||
            event.cameraName?.contains(query, ignoreCase = true) == true ||
            event.type.name.contains(query, ignoreCase = true)
        }
    }

    /**
     * Фильтрация событий по типу и серьезности
     */
    suspend fun filterEvents(
        types: List<EventType>? = null,
        severities: List<EventSeverity>? = null,
        cameraIds: List<String>? = null,
        startTime: Long? = null,
        endTime: Long? = null
    ): List<Event> {
        val result = eventRepository.getEvents(
            startTime = startTime,
            endTime = endTime,
            limit = 1000
        )

        return result.items.filter { event ->
            (types == null || types.contains(event.type)) &&
            (severities == null || severities.contains(event.severity)) &&
            (cameraIds == null || cameraIds.contains(event.cameraId))
        }
    }
}
```

---

## Пример 7: Экспорт событий

```kotlin
class EventExportService(
    private val eventRepository: EventRepository
) {

    /**
     * Экспорт событий в CSV
     */
    suspend fun exportToCsv(
        cameraId: String? = null,
        startTime: Long? = null,
        endTime: Long? = null
    ): String {
        val events = eventRepository.getEvents(
            cameraId = cameraId,
            startTime = startTime,
            endTime = endTime,
            limit = 10000
        )

        val csv = StringBuilder()
        csv.appendLine("Timestamp,Camera ID,Camera Name,Type,Severity,Description")

        events.items.forEach { event ->
            csv.appendLine(
                "${event.timestamp}," +
                "${event.cameraId}," +
                "${event.cameraName ?: ""}," +
                "${event.type}," +
                "${event.severity}," +
                "\"${event.description?.replace("\"", "\"\"") ?: ""}\""
            )
        }

        return csv.toString()
    }

    /**
     * Экспорт событий в JSON
     */
    suspend fun exportToJson(
        cameraId: String? = null,
        startTime: Long? = null,
        endTime: Long? = null
    ): String {
        val events = eventRepository.getEvents(
            cameraId = cameraId,
            startTime = startTime,
            endTime = endTime,
            limit = 10000
        )

        return Json.encodeToString(events.items)
    }
}
```

---

## Пример 8: Интеграция с WebSocket для real-time обновлений

```kotlin
class EventWebSocketService(
    private val eventRepository: EventRepository,
    private val webSocketClient: WebSocketClient
) {

    /**
     * Отправка новых событий через WebSocket
     */
    suspend fun broadcastNewEvent(event: Event) {
        val message = WebSocketMessage.EventMessage(
            channel = WebSocketChannel.EVENTS,
            event = "event_created",
            data = jsonObject {
                put("eventId", event.id)
                put("cameraId", event.cameraId)
                put("cameraName", event.cameraName)
                put("type", event.type.name)
                put("severity", event.severity.name)
                put("timestamp", event.timestamp)
                put("description", event.description)
            }
        )

        webSocketClient.sendMessage(message)
    }

    /**
     * Подписка на события через WebSocket
     */
    fun subscribeToEvents(cameraId: String? = null) {
        webSocketClient.subscribe(WebSocketChannel.EVENTS)

        // Отправка существующих событий
        lifecycleScope.launch {
            val events = eventRepository.getEvents(
                cameraId = cameraId,
                limit = 50
            )

            events.items.forEach { event ->
                broadcastNewEvent(event)
            }
        }
    }
}
```

---

## Пример 9: Автоматическое подтверждение событий

```kotlin
class EventAutoAcknowledgeService(
    private val eventRepository: EventRepository,
    private val userId: String
) {

    /**
     * Автоматическое подтверждение событий через определенное время
     */
    suspend fun autoAcknowledgeOldEvents(maxAgeMinutes: Int = 60) {
        val cutoffTime = System.currentTimeMillis() - (maxAgeMinutes * 60 * 1000)

        val oldEvents = eventRepository.getEvents(
            acknowledged = false,
            endTime = cutoffTime,
            limit = 1000
        )

        oldEvents.items.forEach { event ->
            if (event.severity != EventSeverity.CRITICAL) {
                eventRepository.acknowledgeEvent(event.id, userId)
            }
        }
    }

    /**
     * Автоматическое подтверждение INFO событий сразу
     */
    suspend fun autoAcknowledgeInfoEvents() {
        val infoEvents = eventRepository.getEvents(
            type = EventType.OTHER, // Или другой тип для INFO
            severity = EventSeverity.INFO,
            acknowledged = false,
            limit = 1000
        )

        infoEvents.items.forEach { event ->
            eventRepository.acknowledgeEvent(event.id, userId)
        }
    }
}
```

---

## Пример 10: Полная интеграция в приложении

```kotlin
class CameraEventMonitoringApplication : Application() {

    private lateinit var integrationService: OnvifEventIntegrationService
    private lateinit var eventNotificationService: EventNotificationService
    private lateinit var eventWebSocketService: EventWebSocketService

    override fun onCreate() {
        super.onCreate()

        // Инициализация сервисов
        val eventService = OnvifEventServiceImpl(httpClientEngine)
        val eventRepository = EventRepositoryImplSqlDelight(database)

        integrationService = OnvifEventIntegrationService(
            eventService = eventService,
            eventRepository = eventRepository
        )

        eventNotificationService = EventNotificationService(
            eventRepository = eventRepository,
            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        )

        eventWebSocketService = EventWebSocketService(
            eventRepository = eventRepository,
            webSocketClient = webSocketClient
        )

        // Запуск мониторинга для всех камер
        lifecycleScope.launch {
            startMonitoringForAllCameras()
        }

        // Запуск обработки уведомлений
        lifecycleScope.launch {
            while (true) {
                eventNotificationService.processNewEvents()
                delay(10000)
            }
        }

        // Подписка на WebSocket события
        eventWebSocketService.subscribeToEvents()
    }

    private suspend fun startMonitoringForAllCameras() {
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

    override fun onTerminate() {
        super.onTerminate()
        runBlocking {
            integrationService.stopAll()
        }
    }
}
```

---

**Последнее обновление:** 2026-01-26
