package com.company.ipcamera.shared.data.local

import com.company.ipcamera.shared.database.Event as DbEvent
import com.company.ipcamera.shared.domain.model.Event
import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.domain.model.EventSeverity
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Маппер между сущностью базы данных и доменной моделью Event
 */
internal class EventEntityMapper {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun toDomain(dbEvent: DbEvent): Event {
        return Event(
            id = dbEvent.id,
            cameraId = dbEvent.camera_id,
            cameraName = dbEvent.camera_name,
            type = EventType.valueOf(dbEvent.type),
            severity = EventSeverity.valueOf(dbEvent.severity),
            timestamp = dbEvent.timestamp,
            description = dbEvent.description,
            metadata = dbEvent.metadata?.let { 
                json.decodeFromString<Map<String, String>>(it) 
            } ?: emptyMap(),
            acknowledged = dbEvent.acknowledged == 1L,
            acknowledgedAt = dbEvent.acknowledged_at,
            acknowledgedBy = dbEvent.acknowledged_by,
            thumbnailUrl = dbEvent.thumbnail_url,
            videoUrl = dbEvent.video_url
        )
    }

    fun toDatabase(event: Event): DbEvent {
        return DbEvent(
            id = event.id,
            camera_id = event.cameraId,
            camera_name = event.cameraName,
            type = event.type.name,
            severity = event.severity.name,
            timestamp = event.timestamp,
            description = event.description,
            metadata = if (event.metadata.isNotEmpty()) {
                json.encodeToString(event.metadata)
            } else null,
            acknowledged = if (event.acknowledged) 1L else 0L,
            acknowledged_at = event.acknowledgedAt,
            acknowledged_by = event.acknowledgedBy,
            thumbnail_url = event.thumbnailUrl,
            video_url = event.videoUrl
        )
    }
}

