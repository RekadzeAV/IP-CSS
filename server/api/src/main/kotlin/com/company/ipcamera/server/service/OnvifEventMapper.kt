package com.company.ipcamera.server.service

import com.company.ipcamera.core.network.onvif.OnvifEvent
import com.company.ipcamera.core.network.onvif.OnvifEventType
import com.company.ipcamera.shared.domain.model.Event
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Маппер для преобразования ONVIF событий в доменные события
 */
object OnvifEventMapper {

    /**
     * Преобразовать ONVIF событие в доменное событие
     *
     * @param onvifEvent ONVIF событие
     * @param cameraId ID камеры
     * @param cameraName Название камеры (опционально)
     * @return Доменное событие
     */
    fun mapToDomainEvent(
        onvifEvent: OnvifEvent,
        cameraId: String,
        cameraName: String? = null
    ): Event {
        val (eventType, severity) = mapEventType(onvifEvent.topic)
        val description = onvifEvent.message?.takeUnless { it.isBlank() }
            ?: generateDescription(onvifEvent.topic, onvifEvent.properties)

        return Event(
            id = java.util.UUID.randomUUID().toString(),
            cameraId = cameraId,
            cameraName = cameraName,
            type = eventType,
            severity = severity,
            timestamp = onvifEvent.timestamp,
            description = description,
            metadata = onvifEvent.properties,
            acknowledged = false,
            acknowledgedAt = null,
            acknowledgedBy = null,
            thumbnailUrl = null,
            videoUrl = null
        )
    }

    /**
     * Маппинг типа события ONVIF в доменный тип и важность (см. docs/ONVIF_EVENT_INTEGRATION.md).
     */
    private fun mapEventType(topic: String): Pair<EventType, EventSeverity> {
        return mapVideoSourceEvents(topic)
            ?: mapTamperEvents(topic)
            ?: mapStorageEvents(topic)
            ?: mapHardwareEvents(topic)
            ?: mapMotionEvents(topic)
            ?: mapLineDetectionEvents(topic)
            ?: mapIntrusionEvents(topic)
            ?: mapAlarmEvents(topic)
            ?: mapDeviceEvents(topic)
            ?: mapRecordingEvents(topic)
            ?: mapIoPortEvents(topic)
            ?: mapVideoSourceConfigEvents(topic)
            ?: mapDateTimeEvents(topic)
            ?: mapAnalyticsEvents(topic)
            ?: mapSystemErrorEvents(topic)
            ?: defaultMapping(topic)
    }

    private fun mapVideoSourceEvents(topic: String): Pair<EventType, EventSeverity>? {
        if (OnvifEventType.isVideoSourceLost(topic)) return EventType.CAMERA_OFFLINE to EventSeverity.WARNING
        if (OnvifEventType.isVideoSourceRecovered(topic)) return EventType.CAMERA_ONLINE to EventSeverity.INFO
        return null
    }

    private fun mapTamperEvents(topic: String): Pair<EventType, EventSeverity>? {
        return if (topic.contains("Tamper", ignoreCase = true)) {
            EventType.OTHER to EventSeverity.CRITICAL
        } else null
    }

    private fun mapStorageEvents(topic: String): Pair<EventType, EventSeverity>? {
        return if (OnvifEventType.isStorageFull(topic)) EventType.STORAGE_FULL to EventSeverity.WARNING else null
    }

    private fun mapHardwareEvents(topic: String): Pair<EventType, EventSeverity>? {
        return if (OnvifEventType.isHardwareFailure(topic)) EventType.SYSTEM_ERROR to EventSeverity.WARNING else null
    }

    private fun mapMotionEvents(topic: String): Pair<EventType, EventSeverity>? {
        return if (OnvifEventType.isMotionDetection(topic)) EventType.MOTION_DETECTION to EventSeverity.INFO else null
    }

    private fun mapLineDetectionEvents(topic: String): Pair<EventType, EventSeverity>? {
        return if (OnvifEventType.isLineDetectorCrossed(topic)) EventType.OBJECT_DETECTION to EventSeverity.WARNING else null
    }

    private fun mapIntrusionEvents(topic: String): Pair<EventType, EventSeverity>? {
        return if (OnvifEventType.isIntrusionDetection(topic)) EventType.OBJECT_DETECTION to EventSeverity.CRITICAL else null
    }

    private fun mapAlarmEvents(topic: String): Pair<EventType, EventSeverity>? {
        return if (OnvifEventType.isAlarm(topic)) EventType.OTHER to EventSeverity.ERROR else null
    }

    private fun mapDeviceEvents(topic: String): Pair<EventType, EventSeverity>? {
        if (!topic.contains("Device", ignoreCase = true)) return null
        if (topic.contains("Offline", ignoreCase = true)) return EventType.CAMERA_OFFLINE to EventSeverity.WARNING
        if (topic.contains("Online", ignoreCase = true)) return EventType.CAMERA_ONLINE to EventSeverity.INFO
        return EventType.OTHER to EventSeverity.INFO
    }

    private fun mapRecordingEvents(topic: String): Pair<EventType, EventSeverity>? {
        if (!topic.contains("Recording", ignoreCase = true)) return null
        if (topic.contains("Started", ignoreCase = true)) return EventType.RECORDING_STARTED to EventSeverity.INFO
        if (topic.contains("Stopped", ignoreCase = true)) return EventType.RECORDING_STOPPED to EventSeverity.INFO
        return EventType.OTHER to EventSeverity.INFO
    }

    private fun mapIoPortEvents(topic: String): Pair<EventType, EventSeverity>? {
        return if (OnvifEventType.isDeviceIoPortState(topic)) EventType.OTHER to EventSeverity.WARNING else null
    }

    private fun mapVideoSourceConfigEvents(topic: String): Pair<EventType, EventSeverity>? {
        return if (OnvifEventType.isVideoSourceConfiguration(topic)) EventType.OTHER to EventSeverity.INFO else null
    }

    private fun mapDateTimeEvents(topic: String): Pair<EventType, EventSeverity>? {
        return if (OnvifEventType.isSystemDateTimeChanged(topic)) EventType.OTHER to EventSeverity.INFO else null
    }

    private fun mapAnalyticsEvents(topic: String): Pair<EventType, EventSeverity>? {
        return if (OnvifEventType.isAnalyticsStream(topic)) EventType.OTHER to EventSeverity.INFO else null
    }

    private fun mapSystemErrorEvents(topic: String): Pair<EventType, EventSeverity>? {
        return if (OnvifEventType.isSystemError(topic)) EventType.SYSTEM_ERROR to EventSeverity.WARNING else null
    }

    private fun defaultMapping(topic: String): Pair<EventType, EventSeverity> {
        logger.debug { "Unknown ONVIF event topic: $topic, mapping to OTHER" }
        return EventType.OTHER to EventSeverity.INFO
    }

    /**
     * Генерация описания события на основе топика и свойств
     */
    private fun generateDescription(topic: String, properties: Map<String, String>): String {
        val topicName = topic.split("/").lastOrNull() ?: topic

        return describeMotionDetection(topic)
            ?: describeIntrusionDetection(topic)
            ?: describeTamper(topic)
            ?: describeOffline(topic)
            ?: describeOnline(topic)
            ?: describeIoPort(topic)
            ?: describeVideoSourceConfig(topic)
            ?: describeDateTime(topic)
            ?: describeAnalytics(topic)
            ?: describeStorageFull(topic)
            ?: describeHardwareFailure(topic)
            ?: "Событие: $topicName"
    }

    private fun describeMotionDetection(topic: String): String? =
        if (OnvifEventType.isMotionDetection(topic)) "Обнаружено движение на камере" else null

    private fun describeIntrusionDetection(topic: String): String? =
        if (OnvifEventType.isIntrusionDetection(topic)) {
            if (OnvifEventType.isLineDetectorCrossed(topic)) "Пересечение линии" else "Обнаружено вторжение в зону"
        } else null

    private fun describeTamper(topic: String): String? =
        if (topic.contains("Tamper", ignoreCase = true)) "Обнаружена попытка вмешательства в работу камеры" else null

    private fun describeOffline(topic: String): String? =
        if (OnvifEventType.isVideoSourceLost(topic) || topic.contains("Offline", ignoreCase = true)) "Камера недоступна" else null

    private fun describeOnline(topic: String): String? =
        if (OnvifEventType.isVideoSourceRecovered(topic) || topic.contains("Online", ignoreCase = true)) "Камера восстановлена" else null

    private fun describeIoPort(topic: String): String? =
        if (OnvifEventType.isDeviceIoPortState(topic)) "Изменение состояния порта ввода-вывода" else null

    private fun describeVideoSourceConfig(topic: String): String? =
        if (OnvifEventType.isVideoSourceConfiguration(topic)) "Изменена конфигурация видеоисточника" else null

    private fun describeDateTime(topic: String): String? =
        if (OnvifEventType.isSystemDateTimeChanged(topic)) "Изменение системной даты/времени" else null

    private fun describeAnalytics(topic: String): String? =
        if (OnvifEventType.isAnalyticsStream(topic)) "Событие потока аналитики" else null

    private fun describeStorageFull(topic: String): String? =
        if (OnvifEventType.isStorageFull(topic)) "Переполнение хранилища" else null

    private fun describeHardwareFailure(topic: String): String? =
        if (OnvifEventType.isHardwareFailure(topic)) "Аппаратный сбой устройства" else null
}
