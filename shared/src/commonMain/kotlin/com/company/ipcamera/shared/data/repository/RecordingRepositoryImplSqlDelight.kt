package com.company.ipcamera.shared.data.repository

import com.company.ipcamera.shared.data.local.DatabaseFactory
import com.company.ipcamera.shared.data.local.RecordingEntityMapper
import com.company.ipcamera.shared.data.local.createDatabase
import com.company.ipcamera.shared.domain.model.Recording
import com.company.ipcamera.shared.domain.repository.PaginatedResult
import com.company.ipcamera.shared.domain.repository.RecordingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Реализация RecordingRepository с использованием SQLDelight
 */
class RecordingRepositoryImplSqlDelight(
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
    ): PaginatedResult<Recording> = withContext(Dispatchers.Default) {
        try {
            val offset = (page - 1) * limit
            val recordings = when {
                cameraId != null && startTime != null && endTime != null -> {
                    // Фильтр по камере и диапазону дат
                    database.cameraDatabaseQueries
                        .selectRecordingsByDateRange(startTime, endTime)
                        .executeAsList()
                        .filter { it.camera_id == cameraId }
                        .drop(offset)
                        .take(limit)
                }
                cameraId != null -> {
                    // Фильтр по камере
                    database.cameraDatabaseQueries
                        .selectRecordingsByCameraId(cameraId)
                        .executeAsList()
                        .drop(offset)
                        .take(limit)
                }
                startTime != null && endTime != null -> {
                    // Фильтр по диапазону дат
                    database.cameraDatabaseQueries
                        .selectRecordingsByDateRange(startTime, endTime)
                        .executeAsList()
                        .drop(offset)
                        .take(limit)
                }
                else -> {
                    // Все записи
                    database.cameraDatabaseQueries
                        .selectAllRecordings()
                        .executeAsList()
                        .drop(offset)
                        .take(limit)
                }
            }

            // Подсчет общего количества для пагинации
            val total = when {
                cameraId != null && startTime != null && endTime != null -> {
                    database.cameraDatabaseQueries
                        .selectRecordingsByDateRange(startTime, endTime)
                        .executeAsList()
                        .count { it.camera_id == cameraId }
                }
                cameraId != null -> {
                    database.cameraDatabaseQueries
                        .selectRecordingsByCameraId(cameraId)
                        .executeAsList()
                        .size
                }
                startTime != null && endTime != null -> {
                    database.cameraDatabaseQueries
                        .selectRecordingsByDateRange(startTime, endTime)
                        .executeAsList()
                        .size
                }
                else -> {
                    database.cameraDatabaseQueries
                        .selectAllRecordings()
                        .executeAsList()
                        .size
                }
            }

            PaginatedResult(
                items = recordings.map { mapper.toDomain(it) },
                total = total,
                page = page,
                limit = limit,
                hasMore = offset + limit < total
            )
        } catch (e: Exception) {
            logger.error(e) { "Error getting recordings" }
            PaginatedResult(emptyList(), 0, page, limit, false)
        }
    }

    override suspend fun getRecordingById(id: String): Recording? = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries
                .selectRecordingById(id)
                .executeAsOneOrNull()
                ?.let { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting recording by id: $id" }
            null
        }
    }

    override suspend fun addRecording(recording: Recording): Result<Recording> = withContext(Dispatchers.Default) {
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
            Result.success(recording)
        } catch (e: Exception) {
            logger.error(e) { "Error adding recording: ${recording.id}" }
            Result.failure(e)
        }
    }

    override suspend fun updateRecording(recording: Recording): Result<Recording> = withContext(Dispatchers.Default) {
        try {
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
            Result.success(recording)
        } catch (e: Exception) {
            logger.error(e) { "Error updating recording: ${recording.id}" }
            Result.failure(e)
        }
    }

    override suspend fun deleteRecording(id: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.deleteRecording(id)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error deleting recording: $id" }
            Result.failure(e)
        }
    }

    override suspend fun getDownloadUrl(id: String): Result<String> = withContext(Dispatchers.Default) {
        try {
            val recording = getRecordingById(id)
            recording?.filePath?.let { filePath ->
                Result.success(filePath)
            } ?: Result.failure(IllegalArgumentException("Recording not found: $id"))
        } catch (e: Exception) {
            logger.error(e) { "Error getting download URL for recording: $id" }
            Result.failure(e)
        }
    }

    override suspend fun exportRecording(id: String, format: String, quality: String): Result<String> = withContext(Dispatchers.Default) {
        try {
            // Экспорт записи - в локальной реализации просто возвращаем путь к файлу
            // Для полноценного экспорта нужна дополнительная логика конвертации
            val recording = getRecordingById(id)
            recording?.filePath?.let { filePath ->
                Result.success(filePath)
            } ?: Result.failure(IllegalArgumentException("Recording not found: $id"))
        } catch (e: Exception) {
            logger.error(e) { "Error exporting recording: $id" }
            Result.failure(e)
        }
    }
}

