package com.company.ipcamera.server.repository

import com.company.ipcamera.server.websocket.WebSocketManager
import com.company.ipcamera.server.websocket.WebSocketChannel
import com.company.ipcamera.shared.domain.model.Recording
import com.company.ipcamera.shared.domain.model.RecordingFormat
import com.company.ipcamera.shared.domain.model.RecordingStatus
import com.company.ipcamera.shared.domain.model.Quality
import com.company.ipcamera.shared.domain.repository.PaginatedResult
import com.company.ipcamera.shared.domain.repository.RecordingRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import java.util.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Серверная реализация RecordingRepository
 *
 * TODO: Мигрировать на SQLDelight/PostgreSQL для продакшена
 * Сейчас используется in-memory хранилище для MVP
 */
class ServerRecordingRepository : RecordingRepository {
    private val recordings = mutableMapOf<String, Recording>()
    private val mutex = Mutex()

    override suspend fun getRecordings(
        cameraId: String?,
        startTime: Long?,
        endTime: Long?,
        page: Int,
        limit: Int
    ): PaginatedResult<Recording> = mutex.withLock {
        var filteredRecordings = recordings.values.toList()

        // Фильтрация по cameraId
        if (cameraId != null) {
            filteredRecordings = filteredRecordings.filter { it.cameraId == cameraId }
        }

        // Фильтрация по времени
        if (startTime != null) {
            filteredRecordings = filteredRecordings.filter { it.startTime >= startTime }
        }
        if (endTime != null) {
            filteredRecordings = filteredRecordings.filter {
                it.endTime == null || it.endTime <= endTime
            }
        }

        // Сортировка по времени создания (новые сначала)
        filteredRecordings = filteredRecordings.sortedByDescending { it.createdAt }

        val total = filteredRecordings.size
        val offset = (page - 1) * limit
        val paginatedItems = filteredRecordings.drop(offset).take(limit)
        val hasMore = offset + limit < total

        return PaginatedResult(
            items = paginatedItems,
            total = total,
            page = page,
            limit = limit,
            hasMore = hasMore
        )
    }

    override suspend fun getRecordingById(id: String): Recording? = mutex.withLock {
        return recordings[id]
    }

    override suspend fun addRecording(recording: Recording): Result<Recording> = mutex.withLock {
        try {
            recordings[recording.id] = recording
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

    override suspend fun updateRecording(recording: Recording): Result<Recording> = mutex.withLock {
        try {
            if (!recordings.containsKey(recording.id)) {
                return Result.failure(IllegalArgumentException("Recording not found: ${recording.id}"))
            }
            val oldRecording = recordings[recording.id]
            recordings[recording.id] = recording
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
                        put("oldStatus", oldRecording?.status?.name ?: "")
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

    override suspend fun deleteRecording(id: String): Result<Unit> = mutex.withLock {
        try {
            val recording = recordings[id]
            if (recording == null) {
                return Result.failure(IllegalArgumentException("Recording not found: $id"))
            }
            recordings.remove(id)
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

    override suspend fun getDownloadUrl(id: String): Result<String> = mutex.withLock {
        val recording = recordings[id]
        if (recording == null) {
            return Result.failure(IllegalArgumentException("Recording not found: $id"))
        }

        if (recording.filePath == null) {
            return Result.failure(IllegalStateException("Recording file not available: $id"))
        }

        // Генерируем временный URL для скачивания
        // TODO: Реализовать генерацию подписанных URL с истечением срока действия
        val downloadUrl = "/api/v1/recordings/$id/download/file"
        Result.success(downloadUrl)
    }

    override suspend fun exportRecording(id: String, format: String, quality: String): Result<String> = mutex.withLock {
        val recording = recordings[id]
        if (recording == null) {
            return Result.failure(IllegalArgumentException("Recording not found: $id"))
        }

        // TODO: Реализовать экспорт записи в указанном формате и качестве
        // Пока возвращаем URL для скачивания
        val exportUrl = "/api/v1/recordings/$id/export?format=$format&quality=$quality"
        logger.info { "Recording export requested: $id, format: $format, quality: $quality" }
        Result.success(exportUrl)
    }
}

