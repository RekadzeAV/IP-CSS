package com.company.ipcamera.shared.data.repository

import com.company.ipcamera.shared.data.local.CameraEntityMapper
import com.company.ipcamera.shared.data.local.DatabaseFactory
import com.company.ipcamera.shared.data.local.createDatabase
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.model.CameraStatus
import com.company.ipcamera.shared.domain.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Реализация CameraRepository с использованием SQLDelight
 */
class CameraRepositoryImpl(
    private val databaseFactory: DatabaseFactory
) : CameraRepository {
    
    private val database = createDatabase(databaseFactory.createDriver())
    private val mapper = CameraEntityMapper()
    
    override suspend fun getCameras(): List<Camera> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.selectAll().executeAsList().map { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting cameras" }
            emptyList()
        }
    }
    
    override suspend fun getCameraById(id: String): Camera? = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.selectById(id).executeAsOneOrNull()?.let { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting camera by id: $id" }
            null
        }
    }
    
    override suspend fun addCamera(camera: Camera): Result<Camera> = withContext(Dispatchers.Default) {
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
            logger.error(e) { "Error adding camera: ${camera.id}" }
            Result.failure(e)
        }
    }
    
    override suspend fun updateCamera(camera: Camera): Result<Camera> = withContext(Dispatchers.Default) {
        try {
            val dbCamera = mapper.toDatabase(camera.copy(updatedAt = System.currentTimeMillis()))
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
            Result.success(camera.copy(updatedAt = System.currentTimeMillis()))
        } catch (e: Exception) {
            logger.error(e) { "Error updating camera: ${camera.id}" }
            Result.failure(e)
        }
    }
    
    override suspend fun removeCamera(id: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.deleteCamera(id)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error removing camera: $id" }
            Result.failure(e)
        }
    }
    
    override suspend fun discoverCameras(): List<DiscoveredCamera> = withContext(Dispatchers.Default) {
        // TODO: Реализовать обнаружение камер через ONVIF или UPnP
        emptyList()
    }
    
    override suspend fun testConnection(camera: Camera): ConnectionTestResult = withContext(Dispatchers.Default) {
        // TODO: Реализовать проверку подключения к камере
        ConnectionTestResult.Failure(
            error = "Not implemented",
            code = ErrorCode.UNKNOWN
        )
    }
    
    override suspend fun getCameraStatus(id: String): CameraStatus = withContext(Dispatchers.Default) {
        try {
            val camera = getCameraById(id)
            camera?.status ?: CameraStatus.UNKNOWN
        } catch (e: Exception) {
            logger.error(e) { "Error getting camera status: $id" }
            CameraStatus.ERROR
        }
    }
}

