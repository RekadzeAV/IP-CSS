package com.company.ipcamera.shared.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.company.ipcamera.core.common.model.CameraStatus
import com.company.ipcamera.shared.TestPasswordEncryption
import com.company.ipcamera.shared.data.local.CameraEntityMapper
import com.company.ipcamera.shared.data.local.createDatabaseSync
import com.company.ipcamera.shared.database.CameraDatabase
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.repository.CameraRepository
import com.company.ipcamera.shared.domain.repository.ConnectionTestResult
import com.company.ipcamera.shared.domain.repository.DiscoveredCamera
import com.company.ipcamera.shared.domain.repository.DiscoveryConfig
import com.company.ipcamera.shared.domain.repository.DiscoveryProgress
import com.company.ipcamera.shared.domain.repository.ErrorCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Хелпер для создания [CameraRepository] в common-тестах с переданным [SqlDriver].
 * Обходит необходимость expect/actual [com.company.ipcamera.shared.data.local.DatabaseFactory].
 */
object CameraRepositoryImplTestHelper {
    /**
     * Создаёт реализацию репозитория поверх уже созданного драйвера (in-memory и т.д.).
     */
    fun createRepository(driver: SqlDriver): CameraRepository {
        val database = createDatabaseSync(driver)
        val mapper = CameraEntityMapper(TestPasswordEncryption())
        return CameraRepositoryImplWithDriver(database, mapper)
    }
}

/**
 * Упрощённая реализация для тестов БД: без сети, Onvif и кэша — как в проде не дублирует валидацию.
 */
private class CameraRepositoryImplWithDriver(
    private val database: CameraDatabase,
    private val mapper: CameraEntityMapper,
) : CameraRepository {
    override suspend fun getCameras(): List<Camera> {
        return try {
            database.cameraDatabaseQueries.selectAll().executeAsList().map { mapper.toDomain(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getCameraById(id: String): Camera? {
        return try {
            database.cameraDatabaseQueries.selectById(id).executeAsOneOrNull()?.let { mapper.toDomain(it) }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun addCamera(camera: Camera): Result<Camera> {
        return try {
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
                last_seen = dbCamera.last_seen,
            )
            Result.success(camera)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateCamera(camera: Camera): Result<Camera> {
        return try {
            val updatedCamera = camera.copy(updatedAt = System.currentTimeMillis())
            val dbCamera = mapper.toDatabase(updatedCamera)
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
            Result.success(updatedCamera)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeCamera(id: String): Result<Unit> {
        return try {
            database.cameraDatabaseQueries.deleteCamera(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun discoverCameras(forceRefresh: Boolean): List<DiscoveredCamera> = emptyList()

    override suspend fun discoverCamerasWithProgress(
        forceRefresh: Boolean,
        config: DiscoveryConfig,
    ): Flow<DiscoveryProgress> =
        flowOf(
            DiscoveryProgress(
                currentMethod = com.company.ipcamera.shared.domain.repository.DiscoveryMethod.WS_DISCOVERY,
                methodProgress = 1.0f,
                overallProgress = 1.0f,
                devicesFoundSoFar = 0,
                devicesTotalEstimated = 0,
                elapsedTimeMs = 0L,
                remainingTimeMs = 0L,
                currentActivity = "Mock discovery completed",
                discoveredCamerasSnapshot = emptyList(),
            ),
        )

    override suspend fun testConnection(camera: Camera): ConnectionTestResult =
        ConnectionTestResult.Failure("Not implemented", ErrorCode.UNKNOWN)

    override suspend fun getCameraStatus(id: String): CameraStatus {
        return try {
            val camera = getCameraById(id)
            camera?.status ?: CameraStatus.UNKNOWN
        } catch (e: Exception) {
            CameraStatus.ERROR
        }
    }
}
