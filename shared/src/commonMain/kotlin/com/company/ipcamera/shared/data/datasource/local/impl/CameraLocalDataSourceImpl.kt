package com.company.ipcamera.shared.data.datasource.local.impl

import com.company.ipcamera.shared.data.datasource.local.CameraLocalDataSource
import com.company.ipcamera.shared.data.local.CameraEntityMapper
import com.company.ipcamera.shared.data.local.DatabaseFactory
import com.company.ipcamera.shared.data.local.createDatabaseSync
import com.company.ipcamera.shared.data.local.isPostgresFlywayParityDriver
import com.company.ipcamera.shared.domain.model.Camera
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Реализация CameraLocalDataSource с использованием SQLDelight
 */
class CameraLocalDataSourceImpl(
    private val databaseFactory: DatabaseFactory,
) : CameraLocalDataSource {
    private val driver = databaseFactory.createDriver()
    private val database = createDatabaseSync(driver)
    private val mapper = CameraEntityMapper.forProduction()

    override suspend fun getCameras(): List<Camera> =
        withContext(Dispatchers.Default) {
            try {
                database.cameraDatabaseQueries.selectAll().executeAsList().map { mapper.toDomain(it) }
            } catch (e: Exception) {
                logger.error(e) { "Error getting cameras from local database" }
                emptyList()
            }
        }

    override suspend fun getCameraById(id: String): Camera? =
        withContext(Dispatchers.Default) {
            try {
                database.cameraDatabaseQueries.selectById(id).executeAsOneOrNull()?.let { mapper.toDomain(it) }
            } catch (e: Exception) {
                logger.error(e) { "Error getting camera by id from local database: $id" }
                null
            }
        }

    override suspend fun saveCamera(camera: Camera): Result<Camera> =
        withContext(Dispatchers.Default) {
            try {
                val dbCamera = mapper.toDatabase(camera)
                insertOrUpsertCamera(dbCamera)
                Result.success(camera)
            } catch (e: Exception) {
                logger.error(e) { "Error saving camera to local database: ${camera.id}" }
                Result.failure(e)
            }
        }

    override suspend fun saveCameras(cameras: List<Camera>): Result<List<Camera>> =
        withContext(Dispatchers.Default) {
            try {
                // Используем транзакцию для batch операции
                database.cameraDatabaseQueries.transaction {
                    cameras.forEach { camera ->
                        val dbCamera = mapper.toDatabase(camera)
                        insertOrUpsertCamera(dbCamera)
                    }
                }
                Result.success(cameras)
            } catch (e: Exception) {
                logger.error(e) { "Error saving cameras to local database" }
                Result.failure(e)
            }
        }

    override suspend fun updateCamera(camera: Camera): Result<Camera> =
        withContext(Dispatchers.Default) {
            try {
                val dbCamera = mapper.toDatabase(camera.copy(updatedAt = Clock.System.now().toEpochMilliseconds()))
                database.cameraDatabaseQueries.updateCamera(
                    name = dbCamera.name,
                    url = dbCamera.url,
                    username = dbCamera.username,
                    password = dbCamera.password,
                    model = dbCamera.model,
                    status = dbCamera.status,
                    resolution_width = dbCamera.resolution_width,
                    resolution_height = dbCamera.resolution_height,
                    fps = dbCamera.fps,
                    bitrate = dbCamera.bitrate,
                    codec = dbCamera.codec,
                    audio = dbCamera.audio,
                    ptz_config = dbCamera.ptz_config,
                    streams = dbCamera.streams,
                    settings = dbCamera.settings,
                    statistics = dbCamera.statistics,
                    updated_at = dbCamera.updated_at,
                    last_seen = dbCamera.last_seen,
                    id = dbCamera.id,
                )
                Result.success(camera.copy(updatedAt = Clock.System.now().toEpochMilliseconds()))
            } catch (e: Exception) {
                logger.error(e) { "Error updating camera in local database: ${camera.id}" }
                Result.failure(e)
            }
        }

    override suspend fun deleteCamera(id: String): Result<Unit> =
        withContext(Dispatchers.Default) {
            try {
                database.cameraDatabaseQueries.deleteCamera(id)
                Result.success(Unit)
            } catch (e: Exception) {
                logger.error(e) { "Error deleting camera from local database: $id" }
                Result.failure(e)
            }
        }

    override suspend fun deleteAllCameras(): Result<Unit> =
        withContext(Dispatchers.Default) {
            try {
                database.cameraDatabaseQueries.deleteAllCameras()
                Result.success(Unit)
            } catch (e: Exception) {
                logger.error(e) { "Error deleting all cameras from local database" }
                Result.failure(e)
            }
        }

    override suspend fun updateCameraStatus(
        id: String,
        status: String,
        lastSeen: Long?,
    ): Result<Unit> =
        withContext(
            Dispatchers.Default,
        ) {
            try {
                database.cameraDatabaseQueries.updateCameraStatus(
                    status = status,
                    last_seen = lastSeen,
                    updated_at = Clock.System.now().toEpochMilliseconds(),
                    id = id,
                )
                Result.success(Unit)
            } catch (e: Exception) {
                logger.error(e) { "Error updating camera status in local database: $id" }
                Result.failure(e)
            }
        }

    override suspend fun cameraExists(id: String): Boolean =
        withContext(Dispatchers.Default) {
            try {
                database.cameraDatabaseQueries.selectById(id).executeAsOneOrNull() != null
            } catch (e: Exception) {
                logger.error(e) { "Error checking camera existence in local database: $id" }
                false
            }
        }

    private suspend fun insertOrUpsertCamera(dbCamera: com.company.ipcamera.shared.database.Camera) {
        if (!isPostgresFlywayParityDriver(driver)) {
            database.cameraDatabaseQueries.insertCamera(
                id = dbCamera.id,
                name = dbCamera.name,
                url = dbCamera.url,
                username = dbCamera.username,
                password = dbCamera.password,
                model = dbCamera.model,
                status = dbCamera.status,
                resolution_width = dbCamera.resolution_width,
                resolution_height = dbCamera.resolution_height,
                fps = dbCamera.fps,
                bitrate = dbCamera.bitrate,
                codec = dbCamera.codec,
                audio = dbCamera.audio,
                ptz_config = dbCamera.ptz_config,
                streams = dbCamera.streams,
                settings = dbCamera.settings,
                statistics = dbCamera.statistics,
                created_at = dbCamera.created_at,
                updated_at = dbCamera.updated_at,
                last_seen = dbCamera.last_seen,
            )
            return
        }

        driver.execute(
            identifier = null,
            sql =
                """
                INSERT INTO camera(
                    id, name, url, username, password, model, status,
                    resolution_width, resolution_height, fps, bitrate, codec, audio,
                    ptz_config, streams, settings, statistics,
                    created_at, updated_at, last_seen
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    name = EXCLUDED.name,
                    url = EXCLUDED.url,
                    username = EXCLUDED.username,
                    password = EXCLUDED.password,
                    model = EXCLUDED.model,
                    status = EXCLUDED.status,
                    resolution_width = EXCLUDED.resolution_width,
                    resolution_height = EXCLUDED.resolution_height,
                    fps = EXCLUDED.fps,
                    bitrate = EXCLUDED.bitrate,
                    codec = EXCLUDED.codec,
                    audio = EXCLUDED.audio,
                    ptz_config = EXCLUDED.ptz_config,
                    streams = EXCLUDED.streams,
                    settings = EXCLUDED.settings,
                    statistics = EXCLUDED.statistics,
                    created_at = EXCLUDED.created_at,
                    updated_at = EXCLUDED.updated_at,
                    last_seen = EXCLUDED.last_seen
                """.trimIndent(),
            parameters = 20,
        ) {
            val createdAt = normalizeEpochForPostgresIntColumn(dbCamera.created_at)
            val updatedAt = normalizeEpochForPostgresIntColumn(dbCamera.updated_at)
            val lastSeen = dbCamera.last_seen?.let { normalizeEpochForPostgresIntColumn(it) }
            bindString(0, dbCamera.id)
            bindString(1, dbCamera.name)
            bindString(2, dbCamera.url)
            bindString(3, dbCamera.username)
            bindString(4, dbCamera.password)
            bindString(5, dbCamera.model)
            bindString(6, dbCamera.status)
            bindLong(7, dbCamera.resolution_width)
            bindLong(8, dbCamera.resolution_height)
            bindLong(9, dbCamera.fps)
            bindLong(10, dbCamera.bitrate)
            bindString(11, dbCamera.codec)
            bindLong(12, dbCamera.audio)
            bindString(13, dbCamera.ptz_config)
            bindString(14, dbCamera.streams)
            bindString(15, dbCamera.settings)
            bindString(16, dbCamera.statistics)
            bindLong(17, createdAt)
            bindLong(18, updatedAt)
            bindLong(19, lastSeen)
        }
    }

    private fun normalizeEpochForPostgresIntColumn(value: Long): Long {
        // Postgres INTEGER epoch columns store seconds, domain timestamps often come in milliseconds.
        return if (value > Int.MAX_VALUE) value / 1000L else value
    }
}
