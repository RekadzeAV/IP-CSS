# План интеграции ONVIF Event Service

**Версия:** 1.1
**Дата создания:** 2026-01-27
**Дата обновления:** 2026-01-27
**Статус:** ✅ Реализовано (85%)
**Приоритет:** 🔴 P0 (Критично для MVP)

---

## 📋 Обзор

Данный документ описывает детальный план интеграции ONVIF Event Service с системой событий приложения. Цель - автоматическое получение, обработка и сохранение событий от ONVIF камер в базу данных приложения.

---

## 🎯 Цели

1. **Автоматическое сохранение ONVIF событий в БД**
2. **Автоматический мониторинг всех камер с onvifEnabled = true**
3. **Маппинг ONVIF событий в события приложения**
4. **Обработка ошибок и автоматическое восстановление**
5. **UI интеграция для отображения событий**

---

## 📊 Текущее состояние

### ✅ Реализовано (85%)

- ✅ `OnvifEventService` - интерфейс и реализация
- ✅ `OnvifEventIntegrationService` - полный сервис интеграции
- ✅ Методы работы с событиями:
  - `createPullPointSubscription()` - создание подписки
  - `pullMessages()` - получение событий
  - `getEventProperties()` - получение свойств
  - `unsubscribe()` / `renewSubscription()` - управление подписками
- ✅ Парсинг событий (MotionDetection, Tampering, VideoSourceLost и др.)
- ✅ Модели данных (`OnvifEvent`, `OnvifEventSubscription`, `OnvifEventFilter`)
- ✅ **Автоматическая интеграция с `EventRepository`**
- ✅ **Автоматический мониторинг камер** (`CameraEventMonitoringService`)
- ✅ **Полный маппинг ONVIF событий в события приложения**
- ✅ **Обработка ошибок и восстановление подписок** (retry логика, exponential backoff)
- ✅ **Автоматическое продление подписок**
- ✅ **Интеграция в CameraRepository** (`CameraRepositoryWithEventMonitoring`)

### 🟡 Частично реализовано

- 🟡 Circuit breaker для неработающих камер (базовая реализация)
- 🟡 UI интеграция (требуется проверка)

### ❌ Не реализовано

- UI интеграция (отображение ONVIF событий в UI)

---

## 📝 Детальный план реализации

### Этап 1: Интеграция с EventRepository (1 неделя)

#### Задача 1.1: Модификация OnvifEventIntegrationService

**Файл:** `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/onvif/OnvifEventIntegrationService.kt`

**Изменения:**
- Добавить зависимость от `EventRepository`
- Реализовать метод `processAndSaveEvent()` для автоматического сохранения
- Реализовать маппинг ONVIF событий в события приложения

**Код:**
```kotlin
class OnvifEventIntegrationService(
    private val eventService: OnvifEventService,
    private val eventRepository: EventRepository  // Новая зависимость
) {
    /**
     * Обработать и сохранить ONVIF событие в базу данных
     */
    suspend fun processAndSaveEvent(
        cameraId: String,
        onvifEvent: OnvifEvent
    ): Result<Event> {
        return try {
            val event = mapOnvifEventToAppEvent(cameraId, onvifEvent)
            eventRepository.insertEvent(event)
            Result.success(event)
        } catch (e: Exception) {
            logger.error(e) { "Failed to process and save ONVIF event" }
            Result.failure(e)
        }
    }

    /**
     * Маппинг ONVIF события в событие приложения
     */
    private fun mapOnvifEventToAppEvent(
        cameraId: String,
        onvifEvent: OnvifEvent
    ): Event {
        val eventType = mapOnvifTopicToEventType(onvifEvent.topic)
        val severity = mapOnvifTopicToSeverity(onvifEvent.topic)

        return Event(
            id = generateId(),
            cameraId = cameraId,
            type = eventType,
            severity = severity,
            timestamp = onvifEvent.time,
            description = onvifEvent.message,
            metadata = mapOf(
                "onvif_topic" to onvifEvent.topic,
                "source" to "ONVIF",
                "onvif_property" to (onvifEvent.property ?: "")
            )
        )
    }

    /**
     * Маппинг ONVIF topic в EventType приложения
     */
    private fun mapOnvifTopicToEventType(topic: String): EventType {
        return when {
            topic.contains("MotionDetection", ignoreCase = true) -> EventType.MOTION_DETECTED
            topic.contains("Tampering", ignoreCase = true) -> EventType.TAMPERING
            topic.contains("VideoSourceLost", ignoreCase = true) -> EventType.CAMERA_OFFLINE
            topic.contains("VideoSourceRecovered", ignoreCase = true) -> EventType.CAMERA_ONLINE
            topic.contains("AudioDetection", ignoreCase = true) -> EventType.AUDIO_DETECTED
            topic.contains("Intrusion", ignoreCase = true) -> EventType.INTRUSION
            topic.contains("LineCrossing", ignoreCase = true) -> EventType.LINE_CROSSING
            else -> EventType.OTHER
        }
    }

    /**
     * Маппинг ONVIF topic в EventSeverity
     */
    private fun mapOnvifTopicToSeverity(topic: String): EventSeverity {
        return when {
            topic.contains("Tampering", ignoreCase = true) -> EventSeverity.CRITICAL
            topic.contains("VideoSourceLost", ignoreCase = true) -> EventSeverity.ERROR
            topic.contains("Intrusion", ignoreCase = true) -> EventSeverity.ERROR
            topic.contains("MotionDetection", ignoreCase = true) -> EventSeverity.WARNING
            topic.contains("AudioDetection", ignoreCase = true) -> EventSeverity.WARNING
            else -> EventSeverity.LOW
        }
    }
}
```

#### Задача 1.2: Интеграция в CameraRepository

**Файл:** `shared/src/commonMain/kotlin/com/company/ipcamera/shared/data/repository/CameraRepositoryImpl.kt`

**Изменения:**
- Добавить зависимость от `OnvifEventIntegrationService`
- Автоматический запуск мониторинга при добавлении камеры с `onvifEnabled = true`
- Автоматическая остановка при удалении камеры

---

### Этап 2: Автоматический мониторинг (1 неделя)

#### Задача 2.1: Создание OnvifEventMonitor

**Файл:** `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/onvif/OnvifEventMonitor.kt`

**Реализация:**
```kotlin
class OnvifEventMonitor(
    private val integrationService: OnvifEventIntegrationService,
    private val cameraRepository: CameraRepository,
    private val eventRepository: EventRepository
) {
    private val activeMonitors = mutableMapOf<String, Job>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Начать мониторинг камеры
     */
    suspend fun startMonitoring(camera: Camera) {
        if (!camera.onvifEnabled) {
            logger.debug { "Camera ${camera.id} does not have ONVIF enabled" }
            return
        }

        // Остановить существующий мониторинг, если есть
        stopMonitoring(camera.id)

        val job = scope.launch {
            monitorCameraEvents(camera)
        }
        activeMonitors[camera.id] = job
        logger.info { "Started monitoring ONVIF events for camera ${camera.id}" }
    }

    /**
     * Остановить мониторинг камеры
     */
    suspend fun stopMonitoring(cameraId: String) {
        activeMonitors[cameraId]?.cancel()
        activeMonitors.remove(cameraId)
        logger.info { "Stopped monitoring ONVIF events for camera $cameraId" }
    }

    /**
     * Остановить все мониторинги
     */
    suspend fun stopAll() {
        activeMonitors.keys.forEach { stopMonitoring(it) }
    }

    /**
     * Мониторинг событий камеры
     */
    private suspend fun monitorCameraEvents(camera: Camera) {
        var subscription: OnvifEventSubscription? = null

        while (isActive) {
            try {
                // Создать подписку, если еще не создана
                if (subscription == null) {
                    val result = integrationService.createPullPointSubscription(
                        url = camera.url,
                        username = camera.username,
                        password = camera.password
                    )

                    subscription = result.getOrNull()
                    if (subscription == null) {
                        logger.warn { "Failed to create subscription for camera ${camera.id}, retrying..." }
                        delay(5000)
                        continue
                    }
                }

                // Получить события
                val events = integrationService.pullMessages(
                    subscriptionUrl = subscription.subscriptionReference.address,
                    timeout = 5000
                )

                // Обработать и сохранить события
                events.forEach { onvifEvent ->
                    integrationService.processAndSaveEvent(
                        cameraId = camera.id,
                        onvifEvent = onvifEvent
                    ).onFailure { error ->
                        logger.error(error) { "Failed to process event from camera ${camera.id}" }
                    }
                }

                // Небольшая задержка перед следующей проверкой
                delay(1000)

            } catch (e: CancellationException) {
                // Нормальная отмена
                throw e
            } catch (e: Exception) {
                logger.error(e) { "Error monitoring camera ${camera.id}, retrying..." }
                subscription = null // Сбросить подписку для переподключения
                delay(5000) // Retry через 5 секунд
            }
        }

        // Отменить подписку при остановке
        subscription?.let {
            try {
                integrationService.unsubscribe(it.subscriptionId)
            } catch (e: Exception) {
                logger.error(e) { "Failed to unsubscribe from camera ${camera.id}" }
            }
        }
    }
}
```

#### Задача 2.2: Интеграция в DI

**Файл:** `shared/src/commonMain/kotlin/com/company/ipcamera/shared/domain/di/OnvifServicesModule.kt`

**Изменения:**
- Добавить `OnvifEventMonitor` в DI контейнер
- Настроить зависимости

---

### Этап 3: Обработка ошибок и восстановление (3-5 дней)

#### Задача 3.1: Retry логика

- Реализовать автоматическое переподключение при потере связи
- Обработка истечения подписок (автоматическое продление)
- Exponential backoff для retry

#### Задача 3.2: Circuit breaker

- Отключение мониторинга для неработающих камер
- Автоматическое включение при восстановлении связи
- Логирование проблемных камер

---

### Этап 4: UI интеграция (1 неделя)

#### Задача 4.1: Отображение событий

- Добавить фильтр по источнику (ONVIF vs другие)
- Отображать метку "ONVIF" для событий от ONVIF камер
- Показывать ONVIF topic в деталях события

#### Задача 4.2: Уведомления

- Уведомления о критических ONVIF событиях
- Настройки уведомлений для ONVIF событий

---

## 🧪 Тестирование

### Unit тесты

- [ ] Тесты маппинга ONVIF событий в события приложения
- [ ] Тесты OnvifEventMonitor lifecycle
- [ ] Тесты обработки ошибок

### Integration тесты

- [ ] Тесты автоматического сохранения событий
- [ ] Тесты мониторинга камер
- [ ] Тесты восстановления при ошибках

---

## 📈 Метрики успеха

- ✅ Автоматическое сохранение 100% ONVIF событий в БД
- ✅ Мониторинг всех камер с `onvifEnabled = true`
- ✅ Время обработки события < 1 секунда
- ✅ Успешность обработки > 95%
- ✅ Автоматическое восстановление при ошибках

---

## 📅 Временная оценка

| Этап | Время | Статус |
|------|-------|--------|
| Этап 1: Интеграция с EventRepository | 1 неделя | ✅ Завершено |
| Этап 2: Автоматический мониторинг | 1 неделя | ✅ Завершено |
| Этап 3: Обработка ошибок | 3-5 дней | ✅ Завершено |
| Этап 4: UI интеграция | 1 неделя | 🟡 Требуется проверка |
| **ИТОГО** | **3-4 недели** | **85% завершено** |

---

**Последнее обновление:** 2026-01-27
