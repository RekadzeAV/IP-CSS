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
    val codec: String? = null,
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
        codec = this.codec,
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

/**
 * DTO для запроса начала записи
 */
@Serializable
data class StartRecordingRequest(
    val cameraId: String,
    val format: String? = "MP4",
    val quality: String? = "HIGH",
    val duration: Long? = null, // в миллисекундах, null = бесконечная запись
    /** null = авто: HEVC при высоте кадра >1080 и `RECORDING_HEVC_DISABLE` не установлен. */
    val useH265: Boolean? = null
)

/**
 * DTO для ответа на начало записи
 */
@Serializable
data class StartRecordingResponse(
    val recordingId: String,
    val cameraId: String,
    val startTime: Long,
    val estimatedEndTime: Long? = null
)

@Serializable
data class RecordingPassportDto(
    val recordingId: String,
    val cameraId: String,
    val declaredFormat: String,
    val declaredQuality: String,
    val status: String,
    val declaredCodec: String? = null,
    val actualCodec: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val durationSeconds: String? = null,
    val bitrate: String? = null,
    val fileSizeBytes: Long? = null
)

/**
 * DTO для запроса экспорта записи
 */
@Serializable
data class ExportRecordingRequest(
    val format: String? = "mp4", // mp4, mkv, avi, mov, flv
    val quality: String? = "medium", // low, medium, high, ultra
    val startTime: Long? = null, // Начало обрезки в миллисекундах (опционально)
    val endTime: Long? = null, // Конец обрезки в миллисекундах (опционально)
    val useH265: Boolean = false // Использовать H.265 кодек
)

/**
 * DTO для запроса массового удаления записей
 */
@Serializable
data class BulkDeleteRecordingsRequest(
    val ids: List<String> // Список ID записей для удаления
)/**
 * DTO для ответа на массовое удаление записей
 */
@Serializable
data class BulkDeleteRecordingsResponse(
    val deletedCount: Int,
    val failedCount: Int,
    val failedIds: List<String> = emptyList()
)/**
 * DTO для запроса массового экспорта записей
 */
@Serializable
data class BulkExportRecordingsRequest(
    val ids: List<String>, // Список ID записей для экспорта
    val format: String? = "mp4", // mp4, mkv, avi, mov, flv
    val quality: String? = "medium", // low, medium, high, ultra
    val startTime: Long? = null, // Начало обрезки в миллисекундах (опционально)
    val endTime: Long? = null, // Конец обрезки в миллисекундах (опционально)
    val useH265: Boolean = false // Использовать H.265 кодек
)/**
 * DTO для ответа на массовый экспорт записей
 */
@Serializable
data class BulkExportRecordingsResponse(
    val exportedCount: Int,
    val failedCount: Int,
    val failedIds: List<String> = emptyList(),
    val exportUrls: List<String> = emptyList()
)