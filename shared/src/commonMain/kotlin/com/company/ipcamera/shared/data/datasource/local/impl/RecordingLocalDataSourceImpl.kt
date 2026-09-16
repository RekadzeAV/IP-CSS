package com.company.ipcamera.shared.data.datasource.local.impl

import app.cash.sqldelight.db.SqlDriver
import com.company.ipcamera.shared.data.datasource.local.RecordingLocalDataSource
import com.company.ipcamera.shared.data.local.DatabaseFactory
import com.company.ipcamera.shared.data.local.RecordingEntityMapper
import com.company.ipcamera.shared.data.local.createDatabaseSync
import com.company.ipcamera.shared.domain.model.Recording
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Platform-specific check for PostgreSQL JDBC driver.
 * Implemented in androidMain/jvmMain due to JDBC dependency.
 */
internal expect fun isPostgresDriver(driver: SqlDriver): Boolean

/**
 * Реализация RecordingLocalDataSource с использованием SQLDelight
 */
class RecordingLocalDataSourceImpl(
    private val databaseFactory: DatabaseFactory,
) : RecordingLocalDataSource {
    private val database = createDatabaseSync(databaseFactory.createDriver())
    private val mapper = RecordingEntityMapper()
    private val driver = databaseFactory.createDriver()

    override suspend fun getRecordings(): List<Recording> =
        withContext(Dispatchers.Default) {
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

    override suspend fun getRecordingById(id: String): Recording? =
        withContext(Dispatchers.Default) {
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

    override suspend fun getRecordingsByCameraId(cameraId: String): List<Recording> =
        withContext(Dispatchers.Default) {
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

    override suspend fun getRecordingsByDateRange(
        startTime: Long,
        endTime: Long,
    ): List<Recording> =
        withContext(
            Dispatchers.Default,
        ) {
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

    override suspend fun getRecordingsByStatus(status: String): List<Recording> =
        withContext(Dispatchers.Default) {
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

    override suspend fun saveRecording(recording: Recording): Result<Recording> =
        withContext(Dispatchers.Default) {
            upsertRecording(recording)
        }

    override suspend fun saveRecordings(recordings: List<Recording>): Result<List<Recording>> =
        withContext(
            Dispatchers.Default,
        ) {
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
                            codec = dbRecording.codec,
                            format = dbRecording.format,
                            quality = dbRecording.quality,
                            status = dbRecording.status,
                            thumbnail_url = dbRecording.thumbnail_url,
                            created_at = dbRecording.created_at,
                        )
                    }
                }
                Result.success(recordings)
            } catch (e: Exception) {
                logger.error(e) { "Error saving recordings to local database" }
                Result.failure(e)
            }
        }

    override suspend fun updateRecording(recording: Recording): Result<Recording> =
        withContext(Dispatchers.Default) {
            // INSERT OR REPLACE (см. insertRecording в .sq): чистый UPDATE не вставляет строку, если её ещё нет.
            upsertRecording(recording)
        }

    private suspend fun upsertRecording(recording: Recording): Result<Recording> {
        return try {
            val dbRecording = mapper.toDatabase(recording)

            // Check if using PostgreSQL (JDBC driver)
            val isPostgres = isPostgresDriver(driver)

            if (isPostgres) {
                // PostgreSQL path: Use native UPSERT with ON CONFLICT
                driver.execute(
                    identifier = null,
                    sql =
                        """
                        INSERT INTO recording(
                            id, camera_id, camera_name, start_time, end_time, duration,
                            file_path, file_size, codec, format, quality, status, thumbnail_url, created_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (id) DO UPDATE SET
                            camera_id = EXCLUDED.camera_id,
                            camera_name = EXCLUDED.camera_name,
                            start_time = EXCLUDED.start_time,
                            end_time = EXCLUDED.end_time,
                            duration = EXCLUDED.duration,
                            file_path = EXCLUDED.file_path,
                            file_size = EXCLUDED.file_size,
                            codec = EXCLUDED.codec,
                            format = EXCLUDED.format,
                            quality = EXCLUDED.quality,
                            status = EXCLUDED.status,
                            thumbnail_url = EXCLUDED.thumbnail_url,
                            created_at = EXCLUDED.created_at
                        """.trimIndent(),
                    parameters = 14,
                ) {
                    val createdAt = normalizeEpochForPostgresIntColumn(dbRecording.created_at)
                    val startTime = normalizeEpochForPostgresIntColumn(dbRecording.start_time)
                    val endTime = dbRecording.end_time?.let { normalizeEpochForPostgresIntColumn(it) }
                    val duration = dbRecording.duration?.let { normalizeEpochForPostgresIntColumn(it) }
                    val fileSize = dbRecording.file_size?.let { normalizeEpochForPostgresIntColumn(it) }

                    bindString(0, dbRecording.id)
                    bindString(1, dbRecording.camera_id)
                    bindString(2, dbRecording.camera_name)
                    bindLong(3, startTime)
                    bindLong(4, endTime)
                    bindLong(5, duration)
                    bindString(6, dbRecording.file_path)
                    bindLong(7, fileSize)
                    bindString(8, dbRecording.codec)
                    bindString(9, dbRecording.format)
                    bindString(10, dbRecording.quality)
                    bindString(11, dbRecording.status)
                    bindString(12, dbRecording.thumbnail_url)
                    bindLong(13, createdAt)
                }
            } else {
                // SQLite path: Use SQLDelight generated query
                database.cameraDatabaseQueries.insertRecording(
                    id = dbRecording.id,
                    camera_id = dbRecording.camera_id,
                    camera_name = dbRecording.camera_name,
                    start_time = dbRecording.start_time,
                    end_time = dbRecording.end_time,
                    duration = dbRecording.duration,
                    file_path = dbRecording.file_path,
                    file_size = dbRecording.file_size,
                    codec = dbRecording.codec,
                    format = dbRecording.format,
                    quality = dbRecording.quality,
                    status = dbRecording.status,
                    thumbnail_url = dbRecording.thumbnail_url,
                    created_at = dbRecording.created_at,
                )
            }

            Result.success(recording)
        } catch (e: Exception) {
            logger.error(e) { "Error upserting recording in local database: ${recording.id}" }
            Result.failure(e)
        }
    }

    private fun normalizeEpochForPostgresIntColumn(value: Long): Long {
        return if (value > Int.MAX_VALUE) value / 1000L else value
    }

    override suspend fun updateRecordingStatus(
        id: String,
        status: String,
    ): Result<Unit> =
        withContext(
            Dispatchers.Default,
        ) {
            try {
                database.cameraDatabaseQueries.updateRecordingStatus(status, id)
                Result.success(Unit)
            } catch (e: Exception) {
                logger.error(e) { "Error updating recording status in local database: $id" }
                Result.failure(e)
            }
        }

    override suspend fun deleteRecording(id: String): Result<Unit> =
        withContext(Dispatchers.Default) {
            try {
                database.cameraDatabaseQueries.deleteRecording(id)
                Result.success(Unit)
            } catch (e: Exception) {
                logger.error(e) { "Error deleting recording from local database: $id" }
                Result.failure(e)
            }
        }

    override suspend fun deleteRecordingsByCameraId(cameraId: String): Result<Unit> =
        withContext(Dispatchers.Default) {
            try {
                database.cameraDatabaseQueries.deleteRecordingsByCameraId(cameraId)
                Result.success(Unit)
            } catch (e: Exception) {
                logger.error(e) { "Error deleting recordings by camera id from local database: $cameraId" }
                Result.failure(e)
            }
        }

    override suspend fun deleteAllRecordings(): Result<Unit> =
        withContext(Dispatchers.Default) {
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

    override suspend fun recordingExists(id: String): Boolean =
        withContext(Dispatchers.Default) {
            try {
                database.cameraDatabaseQueries.selectRecordingById(id).executeAsOneOrNull() != null
            } catch (e: Exception) {
                logger.error(e) { "Error checking recording existence in local database: $id" }
                false
            }
        }
}
