package com.company.ipcamera.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Событие для временной шкалы
 */
@Serializable
data class TimelineEvent(
    val id: String,
    val cameraId: String,
    val cameraName: String,
    val timestamp: Long,
    val duration: Long? = null,
    val type: EventType,
    val severity: EventSeverity = EventSeverity.INFO,
    val title: String,
    val description: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val hasRecording: Boolean = false,
    val hasSnapshot: Boolean = false,
    val recordingId: String? = null,
    val snapshotPath: String? = null,
) {
    @Serializable
    enum class EventType {
        MOTION,
        OBJECT_DETECTED,
        FACE_DETECTED,
        LICENSE_PLATE,
        CAMERA_OFFLINE,
        CAMERA_ONLINE,
        RECORDING_STARTED,
        RECORDING_STOPPED,
        USER_ACTION,
        SYSTEM_ALERT,
        CUSTOM,
    }

    @Serializable
    enum class EventSeverity {
        INFO,
        WARNING,
        ERROR,
        CRITICAL,
    }
}

/**
 * Агрегированные данные для временной шкалы
 */
@Serializable
data class TimelineData(
    val cameraId: String,
    val startTime: Long,
    val endTime: Long,
    val events: List<TimelineEvent>,
    val eventCounts: Map<String, Int>, // type -> count
    val peakTimes: List<Long>, // timestamps с высокой активностью
    val recordings: List<RecordingSegment>,
    val gaps: List<TimeGap>, // Периоды без данных
) {
    @Serializable
    data class RecordingSegment(
        val id: String,
        val startTime: Long,
        val endTime: Long,
        val duration: Long,
    )

    @Serializable
    data class TimeGap(
        val startTime: Long,
        val endTime: Long,
        val duration: Long,
        val type: GapType,
    ) {
        @Serializable
        enum class GapType {
            CAMERA_OFFLINE,
            NO_RECORDING,
            NO_EVENTS,
        }
    }
}

/**
 * Запрос для получения временной шкалы
 */
@Serializable
data class TimelineRequest(
    val cameraIds: List<String>,
    val startTime: Long,
    val endTime: Long,
    val eventTypes: List<TimelineEvent.EventType>? = null,
    val minSeverity: TimelineEvent.EventSeverity? = null,
    val includeRecordings: Boolean = true,
    val includeGaps: Boolean = true,
    val aggregationMinutes: Int = 5, // Агрегация по N минут
)
