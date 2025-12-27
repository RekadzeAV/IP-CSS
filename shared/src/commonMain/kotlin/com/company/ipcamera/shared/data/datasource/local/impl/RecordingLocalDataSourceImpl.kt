package com.company.ipcamera.shared.data.datasource.local.impl

import com.company.ipcamera.shared.data.datasource.local.RecordingLocalDataSource
import com.company.ipcamera.shared.data.local.DatabaseFactory
import com.company.ipcamera.shared.data.local.RecordingEntityMapper
import com.company.ipcamera.shared.data.local.createDatabase
import com.company.ipcamera.shared.domain.model.Recording
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Реализация RecordingLocalDataSource с использованием SQLDelight
 */
class RecordingLocalDataSourceImpl(
    private val databaseFactory: DatabaseFactory
) : RecordingLocalDataSource {

    private val database = createDatabase(databaseFactory.createDriver())
    private val mapper = RecordingEntityMapper()

    override suspend fun getRecordings(): List<Recording> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries
                .selectAllRecordings()
                .executeAsList()
                .map { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting recordings from local database" }
            emptyList()
        }
    }

    override suspend fun getRecordingById(id: String): Recording? = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries
                .selectRecordingById(id)
                .executeAsOneOrNull()
                ?.let { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting recording by id from local database: $id" }
            null
        }
    }

    override suspend fun getRecordingsByCameraId(cameraId: String): List<Recording> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries
                .selectRecordingsByCameraId(cameraId)
                .executeAsList()
                .map { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting recordings by camera id from local database: $cameraId" }
            emptyList()
        }
    }

    override suspend fun getRecordingsByDateRange(startTime: Long, endTime: Long): List<Recording> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries
                .selectRecordingsByDateRange(startTime, endTime)
                .executeAsList()
                .map { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting recordings by date range from local database" }
            emptyList()
        }
    }

    override suspend fun getRecordingsByStatus(status: String): List<Recording> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries
                .selectRecordingsByStatus(status)
                .executeAsList()
                .map { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting recordings by status from local database: $status" }
            emptyList()
        }
    }

    override suspend fun saveRecording(recording: Recording): Result<Recording> = withContext(Dispatchers.Default) {
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
            logger.error(e) { "Error saving recording to local database: ${recording.id}" }
            Result.failure(e)
        }
    }

    override suspend fun saveRecordings(recordings: List<Recording>): Result<List<Recording>> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.transaction {
                recordings.forEach { recording ->
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
                }
            }
            Result.success(recordings)
        } catch (e: Exception) {
            logger.error(e) { "Error saving recordings to local database" }
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
            logger.error(e) { "Error updating recording in local database: ${recording.id}" }
            Result.failure(e)
        }
    }

    override suspend fun updateRecordingStatus(id: String, status: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.updateRecordingStatus(status, id)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error updating recording status in local database: $id" }
            Result.failure(e)
        }
    }

    override suspend fun deleteRecording(id: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.deleteRecording(id)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error deleting recording from local database: $id" }
            Result.failure(e)
        }
    }

    override suspend fun deleteRecordingsByCameraId(cameraId: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.deleteRecordingsByCameraId(cameraId)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error deleting recordings by camera id from local database: $cameraId" }
            Result.failure(e)
        }
    }

    override suspend fun deleteAllRecordings(): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            val allRecordings = database.cameraDatabaseQueries.selectAllRecordings().executeAsList()
            database.cameraDatabaseQueries.transaction {
                allRecordings.forEach { recording ->
                    database.cameraDatabaseQueries.deleteRecording(recording.id)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error deleting all recordings from local database" }
            Result.failure(e)
        }
    }

    override suspend fun recordingExists(id: String): Boolean = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.selectRecordingById(id).executeAsOneOrNull() != null
        } catch (e: Exception) {
            logger.error(e) { "Error checking recording existence in local database: $id" }
            false
        }
    }
}

