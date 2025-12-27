package com.company.ipcamera.server.repository

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
            val allRecordings = when {
                cameraId != null && startTime != null && endTime != null -> {
                    // Фильтр по камере и диапазону времени
                    database.cameraDatabaseQueries.selectRecordingsByDateRange(
                        startTime = startTime,
                        endTime = endTime
                    ).executeAsList()
                        .filter { it.camera_id == cameraId }
                        .map { mapper.toDomain(it) }
                }
                cameraId != null -> {
                    // Фильтр только по камере
                    database.cameraDatabaseQueries.selectRecordingsByCameraId(cameraId)
                        .executeAsList()
                        .map { mapper.toDomain(it) }
                }
                startTime != null && endTime != null -> {
                    // Фильтр только по диапазону времени
                    database.cameraDatabaseQueries.selectRecordingsByDateRange(
                        startTime = startTime,
                        endTime = endTime
                    ).executeAsList()
                        .map { mapper.toDomain(it) }
                }
                else -> {
                    // Все записи
                    database.cameraDatabaseQueries.selectAllRecordings()
                        .executeAsList()
                        .map { mapper.toDomain(it) }
                }
            }
            
            // Дополнительная фильтрация по времени (если не использовали запрос)
            val filteredRecordings = allRecordings.filter { recording ->
                var matches = true
                if (startTime != null && recording.startTime < startTime) {
                    matches = false
                }
                if (endTime != null && (recording.endTime == null || recording.endTime > endTime)) {
                    matches = false
                }
                matches
            }
            
            // Сортировка по времени создания (новые сначала)
            val sortedRecordings = filteredRecordings.sortedByDescending { it.createdAt }
            
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
            logger.error(e) { "Error getting recordings" }
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
            
            database.cameraDatabaseQueries.deleteRecording(id)
            logger.info { "Recording deleted: $id" }
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
            
            // TODO: Реализовать экспорт записи в указанном формате и качестве
            // Пока возвращаем URL для скачивания
            val exportUrl = "/api/v1/recordings/$id/export?format=$format&quality=$quality"
            logger.info { "Recording export requested: $id, format: $format, quality: $quality" }
            Result.success(exportUrl)
        } catch (e: Exception) {
            logger.error(e) { "Error exporting recording: $id" }
            Result.failure(e)
        }
    }
}

