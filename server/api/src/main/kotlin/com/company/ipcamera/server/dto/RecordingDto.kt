package com.company.ipcamera.server.dto

import com.company.ipcamera.shared.domain.model.Recording
import com.company.ipcamera.shared.domain.repository.PaginatedResult
import kotlinx.serialization.Serializable

/**
 * DTO для ответа с записью
 */
@Serializable
data class RecordingDto(
    val id: String,
    val cameraId: String,
    val cameraName: String? = null,
    val startTime: Long,
    val endTime: Long? = null,
    val duration: Long,
    val filePath: String? = null,
    val fileSize: Long? = null,
    val format: String,
    val quality: String,
    val status: String,
    val thumbnailUrl: String? = null,
    val createdAt: Long
)

/**
 * DTO для пагинированного ответа с записями
 */
@Serializable
data class PaginatedRecordingResponse(
    val items: List<RecordingDto>,
    val total: Int,
    val page: Int,
    val limit: Int,
    val hasMore: Boolean
)

/**
 * Extension функции для конвертации
 */
fun Recording.toDto(): RecordingDto {
    return RecordingDto(
        id = this.id,
        cameraId = this.cameraId,
        cameraName = this.cameraName,
        startTime = this.startTime,
        endTime = this.endTime,
        duration = this.duration,
        filePath = this.filePath,
        fileSize = this.fileSize,
        format = this.format.name,
        quality = this.quality.name,
        status = this.status.name,
        thumbnailUrl = this.thumbnailUrl,
        createdAt = this.createdAt
    )
}

fun PaginatedResult<Recording>.toDto(): PaginatedRecordingResponse {
    return PaginatedRecordingResponse(
        items = this.items.map { it.toDto() },
        total = this.total,
        page = this.page,
        limit = this.limit,
        hasMore = this.hasMore
    )
}

