package com.company.ipcamera.server.service

import com.company.ipcamera.server.websocket.WebSocketManager
import com.company.ipcamera.server.websocket.WebSocketChannel
import com.company.ipcamera.shared.domain.model.Event
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.domain.repository.EventRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import mu.KotlinLogging
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * Сервис для управления событиями
 *
 * Управляет созданием, обработкой и уведомлениями о событиях системы.
 * Интегрирован с WebSocket для real-time доставки событий и NotificationService
 * для отправки уведомлений о важных событиях.
 */
class EventService(
    private val eventRepository: EventRepository,
    private val notificationService: NotificationService? = null
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Создать событие
     *
     * @param cameraId ID камеры, связанной с событием
     * @param cameraName Название камеры (опционально)
     * @param type Тип события
     * @param severity Важность события
     * @param description Описание события
     * @param metadata Дополнительные метаданные
     * @param thumbnailUrl URL изображения, связанного с событием
     * @param videoUrl URL видео, связанного с событием
     * @return созданное событие
     */
    suspend fun createEvent(
        cameraId: String,
        cameraName: String? = null,
        type: EventType,
        severity: EventSeverity = EventSeverity.INFO,
        description: String? = null,
        metadata: Map<String, String> = emptyMap(),
        thumbnailUrl: String? = null,
        videoUrl: String? = null
    ): Result<Event> {
        return try {
            val eventId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()

            val event = Event(
                id = eventId,
                cameraId = cameraId,
                cameraName = cameraName,
                type = type,
                severity = severity,
                timestamp = timestamp,
                description = description,
                metadata = metadata,
                acknowledged = false,
                acknowledgedAt = null,
                acknowledgedBy = null,
                thumbnailUrl = thumbnailUrl,
                videoUrl = videoUrl
            )

            val result = eventRepository.addEvent(event)

            result.fold(
                onSuccess = { createdEvent ->
                    logger.info {
                        "Event created: ${createdEvent.id}, type: ${createdEvent.type}, " +
                        "severity: ${createdEvent.severity}, camera: $cameraId"
                    }

                    // Отправляем WebSocket событие
                    scope.launch {
                        sendEventViaWebSocket(createdEvent)
                    }

                    // Отправляем уведомление для важных событий
                    if (createdEvent.severity == EventSeverity.CRITICAL ||
                        createdEvent.severity == EventSeverity.ERROR) {
                        scope.launch {
                            sendEventNotification(createdEvent)
                        }
                    }

                    Result.success(createdEvent)
                },
                onFailure = { error ->
                    logger.error(error) { "Error creating event: $eventId" }
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error creating event" }
            Result.failure(e)
        }
    }

    /**
     * Создать событие детекции движения
     */
    suspend fun createMotionDetectionEvent(
        cameraId: String,
        cameraName: String? = null,
        description: String? = null,
        thumbnailUrl: String? = null,
        metadata: Map<String, String> = emptyMap()
    ): Result<Event> {
        return createEvent(
            cameraId = cameraId,
            cameraName = cameraName,
            type = EventType.MOTION_DETECTION,
            severity = EventSeverity.WARNING,
            description = description ?: "Обнаружено движение",
            metadata = metadata,
            thumbnailUrl = thumbnailUrl
        )
    }

    /**
     * Создать событие детекции объекта
     */
    suspend fun createObjectDetectionEvent(
        cameraId: String,
        cameraName: String? = null,
        objectType: String,
        confidence: Float? = null,
        description: String? = null,
        thumbnailUrl: String? = null,
        metadata: Map<String, String> = emptyMap()
    ): Result<Event> {
        val eventMetadata = metadata.toMutableMap()
        eventMetadata["objectType"] = objectType
        confidence?.let { eventMetadata["confidence"] = it.toString() }

        return createEvent(
            cameraId = cameraId,
            cameraName = cameraName,
            type = EventType.OBJECT_DETECTION,
            severity = EventSeverity.WARNING,
            description = description ?: "Обнаружен объект: $objectType",
            metadata = eventMetadata,
            thumbnailUrl = thumbnailUrl
        )
    }

    /**
     * Создать событие камера offline
     */
    suspend fun createCameraOfflineEvent(
        cameraId: String,
        cameraName: String? = null,
        description: String? = null
    ): Result<Event> {
        return createEvent(
            cameraId = cameraId,
            cameraName = cameraName,
            type = EventType.CAMERA_OFFLINE,
            severity = EventSeverity.ERROR,
            description = description ?: "Камера недоступна"
        )
    }

    /**
     * Создать событие камера online
     */
    suspend fun createCameraOnlineEvent(
        cameraId: String,
        cameraName: String? = null,
        description: String? = null
    ): Result<Event> {
        return createEvent(
            cameraId = cameraId,
            cameraName = cameraName,
            type = EventType.CAMERA_ONLINE,
            severity = EventSeverity.INFO,
            description = description ?: "Камера снова доступна"
        )
    }

    /**
     * Создать событие записи начата
     */
    suspend fun createRecordingStartedEvent(
        cameraId: String,
        cameraName: String? = null,
        recordingId: String? = null,
        description: String? = null
    ): Result<Event> {
        val metadata = if (recordingId != null) {
            mapOf("recordingId" to recordingId)
        } else {
            emptyMap()
        }

        return createEvent(
            cameraId = cameraId,
            cameraName = cameraName,
            type = EventType.RECORDING_STARTED,
            severity = EventSeverity.INFO,
            description = description ?: "Запись начата",
            metadata = metadata
        )
    }

    /**
     * Создать событие записи остановлена
     */
    suspend fun createRecordingStoppedEvent(
        cameraId: String,
        cameraName: String? = null,
        recordingId: String? = null,
        description: String? = null
    ): Result<Event> {
        val metadata = if (recordingId != null) {
            mapOf("recordingId" to recordingId)
        } else {
            emptyMap()
        }

        return createEvent(
            cameraId = cameraId,
            cameraName = cameraName,
            type = EventType.RECORDING_STOPPED,
            severity = EventSeverity.INFO,
            description = description ?: "Запись остановлена",
            metadata = metadata
        )
    }

    /**
     * Создать событие переполнения хранилища
     */
    suspend fun createStorageFullEvent(
        cameraId: String? = null,
        cameraName: String? = null,
        description: String? = null
    ): Result<Event> {
        return createEvent(
            cameraId = cameraId ?: "system",
            cameraName = cameraName,
            type = EventType.STORAGE_FULL,
            severity = EventSeverity.CRITICAL,
            description = description ?: "Хранилище заполнено"
        )
    }

    /**
     * Создать событие системной ошибки
     */
    suspend fun createSystemErrorEvent(
        error: String,
        cameraId: String? = null,
        cameraName: String? = null,
        metadata: Map<String, String> = emptyMap()
    ): Result<Event> {
        return createEvent(
            cameraId = cameraId ?: "system",
            cameraName = cameraName,
            type = EventType.SYSTEM_ERROR,
            severity = EventSeverity.CRITICAL,
            description = error,
            metadata = metadata
        )
    }

    /**
     * Подтвердить событие
     *
     * @param eventId ID события
     * @param userId ID пользователя, подтверждающего событие
     * @return обновленное событие
     */
    suspend fun acknowledgeEvent(
        eventId: String,
        userId: String
    ): Result<Event> {
        return try {
            val result = eventRepository.acknowledgeEvent(eventId, userId)

            result.fold(
                onSuccess = { event ->
                    logger.info { "Event acknowledged: $eventId by user: $userId" }

                    // Отправляем WebSocket событие
                    scope.launch {
                        sendEventAcknowledgedViaWebSocket(event)
                    }

                    Result.success(event)
                },
                onFailure = { error ->
                    logger.error(error) { "Error acknowledging event: $eventId" }
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error acknowledging event: $eventId" }
            Result.failure(e)
        }
    }

    /**
     * Подтвердить несколько событий
     *
     * @param eventIds Список ID событий
     * @param userId ID пользователя, подтверждающего события
     * @return список обновленных событий
     */
    suspend fun acknowledgeEvents(
        eventIds: List<String>,
        userId: String
    ): Result<List<Event>> {
        return try {
            val result = eventRepository.acknowledgeEvents(eventIds, userId)

            result.fold(
                onSuccess = { events ->
                    logger.info { "Events acknowledged: ${eventIds.size} by user: $userId" }

                    // Отправляем WebSocket события
                    scope.launch {
                        events.forEach { event ->
                            sendEventAcknowledgedViaWebSocket(event)
                        }
                    }

                    Result.success(events)
                },
                onFailure = { error ->
                    logger.error(error) { "Error acknowledging events" }
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error acknowledging events" }
            Result.failure(e)
        }
    }

    /**
     * Удалить событие
     *
     * @param eventId ID события
     * @return результат операции
     */
    suspend fun deleteEvent(eventId: String): Result<Unit> {
        return try {
            val result = eventRepository.deleteEvent(eventId)

            result.fold(
                onSuccess = {
                    logger.info { "Event deleted: $eventId" }

                    // Отправляем WebSocket событие
                    scope.launch {
                        sendEventDeletedViaWebSocket(eventId)
                    }

                    Result.success(Unit)
                },
                onFailure = { error ->
                    logger.error(error) { "Error deleting event: $eventId" }
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error deleting event: $eventId" }
            Result.failure(e)
        }
    }

    /**
     * Отправить событие через WebSocket
     */
    private suspend fun sendEventViaWebSocket(event: Event) {
        try {
            val jsonData = buildJsonObject {
                put("id", event.id)
                put("cameraId", event.cameraId)
                event.cameraName?.let { put("cameraName", it) }
                put("type", event.type.name)
                put("severity", event.severity.name)
                put("timestamp", event.timestamp)
                event.description?.let { put("description", it) }
                event.thumbnailUrl?.let { put("thumbnailUrl", it) }
                event.videoUrl?.let { put("videoUrl", it) }
                put("acknowledged", event.acknowledged)

                if (event.metadata.isNotEmpty()) {
                    val metadataObject = buildJsonObject {
                        event.metadata.forEach { (key, value) ->
                            put(key, value)
                        }
                    }
                    put("metadata", metadataObject)
                }
            }

            WebSocketManager.broadcastEvent(
                channel = WebSocketChannel.EVENTS,
                type = "event.created",
                data = jsonData
            )

            logger.debug { "Event sent via WebSocket: ${event.id}" }
        } catch (e: Exception) {
            logger.error(e) { "Error sending event via WebSocket: ${event.id}" }
        }
    }

    /**
     * Отправить событие подтверждения через WebSocket
     */
    private suspend fun sendEventAcknowledgedViaWebSocket(event: Event) {
        try {
            val jsonData = buildJsonObject {
                put("id", event.id)
                put("acknowledged", event.acknowledged)
                event.acknowledgedAt?.let { put("acknowledgedAt", it) }
                event.acknowledgedBy?.let { put("acknowledgedBy", it) }
            }

            WebSocketManager.broadcastEvent(
                channel = WebSocketChannel.EVENTS,
                type = "event.acknowledged",
                data = jsonData
            )

            logger.debug { "Event acknowledged sent via WebSocket: ${event.id}" }
        } catch (e: Exception) {
            logger.error(e) { "Error sending event acknowledged via WebSocket: ${event.id}" }
        }
    }

    /**
     * Отправить событие удаления через WebSocket
     */
    private suspend fun sendEventDeletedViaWebSocket(eventId: String) {
        try {
            val jsonData = buildJsonObject {
                put("id", eventId)
            }

            WebSocketManager.broadcastEvent(
                channel = WebSocketChannel.EVENTS,
                type = "event.deleted",
                data = jsonData
            )

            logger.debug { "Event deleted sent via WebSocket: $eventId" }
        } catch (e: Exception) {
            logger.error(e) { "Error sending event deleted via WebSocket: $eventId" }
        }
    }

    /**
     * Отправить уведомление о событии
     */
    private suspend fun sendEventNotification(event: Event) {
        try {
            notificationService?.let { service ->
                val priority = when (event.severity) {
                    EventSeverity.CRITICAL -> com.company.ipcamera.shared.domain.model.NotificationPriority.CRITICAL
                    EventSeverity.ERROR -> com.company.ipcamera.shared.domain.model.NotificationPriority.HIGH
                    EventSeverity.WARNING -> com.company.ipcamera.shared.domain.model.NotificationPriority.NORMAL
                    EventSeverity.INFO -> com.company.ipcamera.shared.domain.model.NotificationPriority.LOW
                }

                val title = when (event.type) {
                    EventType.MOTION_DETECTION -> "Обнаружено движение"
                    EventType.OBJECT_DETECTION -> "Обнаружен объект"
                    EventType.CAMERA_OFFLINE -> "Камера недоступна"
                    EventType.CAMERA_ONLINE -> "Камера восстановлена"
                    EventType.STORAGE_FULL -> "Хранилище заполнено"
                    EventType.SYSTEM_ERROR -> "Системная ошибка"
                    else -> "Новое событие"
                }

                service.sendEventNotification(
                    eventId = event.id,
                    title = title,
                    message = event.description ?: event.type.name,
                    cameraId = event.cameraId,
                    priority = priority
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Error sending event notification: ${event.id}" }
        }
    }
}

