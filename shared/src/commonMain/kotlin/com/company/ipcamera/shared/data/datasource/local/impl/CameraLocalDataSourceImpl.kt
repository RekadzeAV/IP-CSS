package com.company.ipcamera.shared.data.datasource.local.impl

import com.company.ipcamera.shared.data.datasource.local.CameraLocalDataSource
import com.company.ipcamera.shared.data.local.CameraEntityMapper
import com.company.ipcamera.shared.data.local.DatabaseFactory
import com.company.ipcamera.shared.data.local.createDatabase
import com.company.ipcamera.shared.domain.model.Camera
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Реализация CameraLocalDataSource с использованием SQLDelight
 */
class CameraLocalDataSourceImpl(
    private val databaseFactory: DatabaseFactory
) : CameraLocalDataSource {

    private val database = createDatabase(databaseFactory.createDriver())
    private val mapper = CameraEntityMapper()

    override suspend fun getCameras(): List<Camera> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.selectAll().executeAsList().map { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting cameras from local database" }
            emptyList()
        }
    }

    override suspend fun getCameraById(id: String): Camera? = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.selectById(id).executeAsOneOrNull()?.let { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting camera by id from local database: $id" }
            null
        }
    }

    override suspend fun saveCamera(camera: Camera): Result<Camera> = withContext(Dispatchers.Default) {
        try {
            val dbCamera = mapper.toDatabase(camera)
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
                last_seen = dbCamera.last_seen
            )
            Result.success(camera)
        } catch (e: Exception) {
            logger.error(e) { "Error saving camera to local database: ${camera.id}" }
            Result.failure(e)
        }
    }

    override suspend fun saveCameras(cameras: List<Camera>): Result<List<Camera>> = withContext(Dispatchers.Default) {
        try {
            // Используем транзакцию для batch операции
            database.cameraDatabaseQueries.transaction {
                cameras.forEach { camera ->
                    val dbCamera = mapper.toDatabase(camera)
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
                        last_seen = dbCamera.last_seen
                    )
                }
            }
            Result.success(cameras)
        } catch (e: Exception) {
            logger.error(e) { "Error saving cameras to local database" }
            Result.failure(e)
        }
    }

    override suspend fun updateCamera(camera: Camera): Result<Camera> = withContext(Dispatchers.Default) {
        try {
            val dbCamera = mapper.toDatabase(camera.copy(updatedAt = System.currentTimeMillis()))
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
                id = dbCamera.id
            )
            Result.success(camera.copy(updatedAt = System.currentTimeMillis()))
        } catch (e: Exception) {
            logger.error(e) { "Error updating camera in local database: ${camera.id}" }
            Result.failure(e)
        }
    }

    override suspend fun deleteCamera(id: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.deleteCamera(id)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error deleting camera from local database: $id" }
            Result.failure(e)
        }
    }

    override suspend fun deleteAllCameras(): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            // SQLDelight не имеет прямого метода deleteAll, поэтому удаляем по одной
            // В будущем можно добавить специальный запрос в .sq файл
            val allCameras = database.cameraDatabaseQueries.selectAll().executeAsList()
            database.cameraDatabaseQueries.transaction {
                allCameras.forEach { camera ->
                    database.cameraDatabaseQueries.deleteCamera(camera.id)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error deleting all cameras from local database" }
            Result.failure(e)
        }
    }

    override suspend fun updateCameraStatus(id: String, status: String, lastSeen: Long?): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.updateCameraStatus(
                status = status,
                last_seen = lastSeen,
                updated_at = System.currentTimeMillis(),
                id = id
            )
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error updating camera status in local database: $id" }
            Result.failure(e)
        }
    }

    override suspend fun cameraExists(id: String): Boolean = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.selectById(id).executeAsOneOrNull() != null
        } catch (e: Exception) {
            logger.error(e) { "Error checking camera existence in local database: $id" }
            false
        }
    }
}

