package com.company.ipcamera.server.dto

import com.company.ipcamera.shared.domain.model.Event
import com.company.ipcamera.shared.domain.repository.PaginatedResult
import kotlinx.serialization.Serializable

/**
 * DTO для ответа с событием
 */
@Serializable
data class EventDto(
    val id: String,
    val cameraId: String,
    val cameraName: String? = null,
    val type: String,
    val severity: String,
    val timestamp: Long,
    val description: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val thumbnailUrl: String? = null,
    val videoUrl: String? = null,
    val acknowledged: Boolean = false,
    val acknowledgedAt: Long? = null,
    val acknowledgedBy: String? = null
)

/**
 * DTO для пагинированного ответа с событиями
 */
@Serializable
data class PaginatedEventResponse(
    val items: List<EventDto>,
    val total: Int,
    val page: Int,
    val limit: Int,
    val hasMore: Boolean
)

/**
 * DTO для запроса массового подтверждения событий
 */
@Serializable
data class AcknowledgeEventsRequest(
    val ids: List<String>
)

/**
 * Extension функции для конвертации
 */
fun Event.toDto(): EventDto {
    return EventDto(
        id = this.id,
        cameraId = this.cameraId,
        cameraName = this.cameraName,
        type = this.type.name,
        severity = this.severity.name,
        timestamp = this.timestamp,
        description = this.description,
        metadata = this.metadata,
        thumbnailUrl = this.thumbnailUrl,
        videoUrl = this.videoUrl,
        acknowledged = this.acknowledged,
        acknowledgedAt = this.acknowledgedAt,
        acknowledgedBy = this.acknowledgedBy
    )
}

fun PaginatedResult<Event>.toDto(): PaginatedEventResponse {
    return PaginatedEventResponse(
        items = this.items.map { it.toDto() },
        total = this.total,
        page = this.page,
        limit = this.limit,
        hasMore = this.hasMore
    )
}

