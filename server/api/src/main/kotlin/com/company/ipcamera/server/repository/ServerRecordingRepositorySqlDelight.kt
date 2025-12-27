package com.company.ipcamera.server.repository

import com.company.ipcamera.server.websocket.WebSocketManager
import com.company.ipcamera.server.websocket.WebSocketChannel
import com.company.ipcamera.shared.data.local.DatabaseFactory
import com.company.ipcamera.shared.data.local.createDatabase
import com.company.ipcamera.shared.data.local.RecordingEntityMapper
import com.company.ipcamera.shared.domain.model.Recording
import com.company.ipcamera.shared.domain.model.RecordingFormat
import com.company.ipcamera.shared.domain.model.RecordingStatus
import com.company.ipcamera.shared.domain.model.Quality
import com.company.ipcamera.shared.domain.repository.PaginatedResult
import com.company.ipcamera.shared.domain.repository.RecordingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Серверная реализация RecordingRepository с использованием SQLDelight
 *
 * Использует SQLDelight для персистентного хранения записей
 */
class ServerRecordingRepositorySqlDelight(
    private val databaseFactory: DatabaseFactory
) : RecordingRepository {

    private val database = createDatabase(databaseFactory.createDriver())
    private val mapper = RecordingEntityMapper()

    override suspend fun getRecordings(
        cameraId: String?,
        startTime: Long?,
        endTime: Long?,
        page: Int,
        limit: Int
    ): PaginatedResult<Recording> = withContext(Dispatchers.IO) {
        try {
            // Оптимизированные запросы с использованием индексов БД
            val allRecordings = when {
                cameraId != null && startTime != null && endTime != null -> {
                    // Фильтр по камере и диапазону времени - используем индекс по camera_id и start_time
                    database.cameraDatabaseQueries.selectRecordingsByCameraId(cameraId)
                        .executeAsList()
                        .filter {
                            it.start_time >= startTime &&
                            (it.end_time == null || it.end_time <= endTime)
                        }
                        .map { mapper.toDomain(it) }
                }
                cameraId != null -> {
                    // Фильтр только по камере - используем индекс по camera_id
                    database.cameraDatabaseQueries.selectRecordingsByCameraId(cameraId)
                        .executeAsList()
                        .map { mapper.toDomain(it) }
                }
                startTime != null && endTime != null -> {
                    // Фильтр только по диапазону времени - используем индекс по start_time
                    database.cameraDatabaseQueries.selectRecordingsByDateRange(
                        startTime = startTime,
                        endTime = endTime
                    ).executeAsList()
                        .map { mapper.toDomain(it) }
                }
                else -> {
                    // Все записи - используем индекс по created_at для сортировки
                    database.cameraDatabaseQueries.selectAllRecordings()
                        .executeAsList()
                        .map { mapper.toDomain(it) }
                }
            }

            // Сортировка по времени создания (новые сначала)
            // SQLDelight уже сортирует в запросе, но убеждаемся что сортировка правильная
            val sortedRecordings = allRecordings.sortedByDescending { it.createdAt }

            // Пагинация
            val total = sortedRecordings.size
            val offset = (page - 1) * limit
            val paginatedItems = sortedRecordings.drop(offset).take(limit)
            val hasMore = offset + limit < total

            PaginatedResult(
                items = paginatedItems,
                total = total,
                page = page,
                limit = limit,
                hasMore = hasMore
            )
        } catch (e: Exception) {
            logger.error(e) { "Error getting recordings from PostgreSQL" }
            // Возвращаем пустой результат вместо падения
            PaginatedResult(emptyList(), 0, page, limit, false)
        }
    }

    override suspend fun getRecordingById(id: String): Recording? = withContext(Dispatchers.IO) {
        try {
            database.cameraDatabaseQueries.selectRecordingById(id)
                .executeAsOneOrNull()
                ?.let { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting recording by id: $id" }
            null
        }
    }

    override suspend fun addRecording(recording: Recording): Result<Recording> = withContext(Dispatchers.IO) {
        try {
            val dbRecording = mapper.toDatabase(recording)
            database.cameraDatabaseQueries.insertRecording(
                id = dbRecording.id,
                camera_id = dbRecording.camera_id,
                camera_name = dbRecording.camera_name,
                start_time = dbRecording.start_time,
                end_time = dbRecording.end_time,
                duration = dbRecording.duration,
                file_path = dbRecording.file_path,
                file_size = dbRecording.file_size,
                format = dbRecording.format,
                quality = dbRecording.quality,
                status = dbRecording.status,
                thumbnail_url = dbRecording.thumbnail_url,
                created_at = dbRecording.created_at
            )
            logger.info { "Recording added: ${recording.id} for camera ${recording.cameraId}" }

            // Отправляем WebSocket событие о новой записи
            try {
                WebSocketManager.broadcastEvent(
                    WebSocketChannel.RECORDINGS,
                    "recording_created",
                    jsonObject {
                        put("recordingId", recording.id)
                        put("cameraId", recording.cameraId)
                        put("status", recording.status.name)
                        put("format", recording.format.name)
                        put("quality", recording.quality.name)
                        put("startTime", recording.startTime)
                        put("filePath", recording.filePath ?: "")
                        put("fileSize", recording.fileSize ?: 0L)
                    }
                )
            } catch (e: Exception) {
                logger.warn(e) { "Failed to send WebSocket event for new recording" }
            }

            Result.success(recording)
        } catch (e: Exception) {
            logger.error(e) { "Error adding recording: ${recording.id}" }
            Result.failure(e)
        }
    }

    override suspend fun updateRecording(recording: Recording): Result<Recording> = withContext(Dispatchers.IO) {
        try {
            val existing = database.cameraDatabaseQueries.selectRecordingById(recording.id)
                .executeAsOneOrNull()

            if (existing == null) {
                return@withContext Result.failure(IllegalArgumentException("Recording not found: ${recording.id}"))
            }

            val oldStatus = mapper.toDomain(existing).status
            val dbRecording = mapper.toDatabase(recording)
            database.cameraDatabaseQueries.updateRecording(
                camera_id = dbRecording.camera_id,
                camera_name = dbRecording.camera_name,
                start_time = dbRecording.start_time,
                end_time = dbRecording.end_time,
                duration = dbRecording.duration,
                file_path = dbRecording.file_path,
                file_size = dbRecording.file_size,
                format = dbRecording.format,
                quality = dbRecording.quality,
                status = dbRecording.status,
                thumbnail_url = dbRecording.thumbnail_url,
                id = dbRecording.id
            )
            logger.info { "Recording updated: ${recording.id}" }

            // Отправляем WebSocket событие об обновлении записи
            try {
                WebSocketManager.broadcastEvent(
                    WebSocketChannel.RECORDINGS,
                    "recording_updated",
                    jsonObject {
                        put("recordingId", recording.id)
                        put("cameraId", recording.cameraId)
                        put("status", recording.status.name)
                        put("oldStatus", oldStatus.name)
                        put("endTime", recording.endTime ?: 0L)
                        put("fileSize", recording.fileSize ?: 0L)
                        put("timestamp", System.currentTimeMillis())
                    }
                )
            } catch (e: Exception) {
                logger.warn(e) { "Failed to send WebSocket event for recording update" }
            }

            Result.success(recording)
        } catch (e: Exception) {
            logger.error(e) { "Error updating recording: ${recording.id}" }
            Result.failure(e)
        }
    }

    override suspend fun deleteRecording(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = database.cameraDatabaseQueries.selectRecordingById(id)
                .executeAsOneOrNull()

            if (existing == null) {
                return@withContext Result.failure(IllegalArgumentException("Recording not found: $id"))
            }

            val recording = mapper.toDomain(existing)
            database.cameraDatabaseQueries.deleteRecording(id)
            logger.info { "Recording deleted: $id" }

            // Отправляем WebSocket событие об удалении записи
            try {
                WebSocketManager.broadcastEvent(
                    WebSocketChannel.RECORDINGS,
                    "recording_deleted",
                    jsonObject {
                        put("recordingId", id)
                        put("cameraId", recording.cameraId)
                        put("timestamp", System.currentTimeMillis())
                    }
                )
            } catch (e: Exception) {
                logger.warn(e) { "Failed to send WebSocket event for recording deletion" }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error deleting recording: $id" }
            Result.failure(e)
        }
    }

    override suspend fun getDownloadUrl(id: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val recording = database.cameraDatabaseQueries.selectRecordingById(id)
                .executeAsOneOrNull()
                ?.let { mapper.toDomain(it) }

            if (recording == null) {
                return@withContext Result.failure(IllegalArgumentException("Recording not found: $id"))
            }

            if (recording.filePath == null) {
                return@withContext Result.failure(IllegalStateException("Recording file not available: $id"))
            }

            // Генерируем временный URL для скачивания
            // TODO: Реализовать генерацию подписанных URL с истечением срока действия
            val downloadUrl = "/api/v1/recordings/$id/download/file"
            Result.success(downloadUrl)
        } catch (e: Exception) {
            logger.error(e) { "Error getting download URL for recording: $id" }
            Result.failure(e)
        }
    }

    override suspend fun exportRecording(id: String, format: String, quality: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val recording = database.cameraDatabaseQueries.selectRecordingById(id)
                .executeAsOneOrNull()
                ?.let { mapper.toDomain(it) }

            if (recording == null) {
                return@withContext Result.failure(IllegalArgumentException("Recording not found: $id"))
            }

            // Экспорт реализован в RecordingRoutes через FfmpegService
            // Возвращаем URL для экспорта
            val exportUrl = "/api/v1/recordings/$id/export?format=$format&quality=$quality"
            logger.info { "Recording export requested: $id, format: $format, quality: $quality" }
            Result.success(exportUrl)
        } catch (e: Exception) {
            logger.error(e) { "Error exporting recording: $id" }
            Result.failure(e)
        }
    }
}

