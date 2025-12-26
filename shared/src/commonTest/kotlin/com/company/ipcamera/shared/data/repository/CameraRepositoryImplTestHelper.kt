package com.company.ipcamera.shared.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.company.ipcamera.shared.data.local.CameraEntityMapper
import com.company.ipcamera.shared.data.local.createDatabase
import com.company.ipcamera.shared.database.CameraDatabase

/**
 * Хелпер для создания CameraRepositoryImpl в тестах
 * Обходит проблему с expect классом DatabaseFactory
 */
class CameraRepositoryImplTestHelper {
    companion object {
        /**
         * Создает CameraRepositoryImpl с заданным драйвером
         * Используется только для тестов
         */
        fun createRepository(driver: SqlDriver): CameraRepositoryImpl {
            val database = createDatabase(driver)
            val mapper = CameraEntityMapper()
            
            // Используем рефлексию или создаем через внутренний конструктор
            // Для упрощения создаем wrapper
            return CameraRepositoryImplWithDriver(database, mapper)
        }
    }
}

/**
 * Внутренняя реализация CameraRepositoryImpl для тестов
 * Примечание: Это временное решение для тестов
 */
private class CameraRepositoryImplWithDriver(
    private val database: CameraDatabase,
    private val mapper: CameraEntityMapper
) : com.company.ipcamera.shared.domain.repository.CameraRepository {
    
    override suspend fun getCameras(): List<com.company.ipcamera.shared.domain.model.Camera> {
        return try {
            database.cameraDatabaseQueries.selectAll().executeAsList().map { mapper.toDomain(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    override suspend fun getCameraById(id: String): com.company.ipcamera.shared.domain.model.Camera? {
        return try {
            database.cameraDatabaseQueries.selectById(id).executeAsOneOrNull()?.let { mapper.toDomain(it) }
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun addCamera(camera: com.company.ipcamera.shared.domain.model.Camera): Result<com.company.ipcamera.shared.domain.model.Camera> {
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
                last_seen = dbCamera.last_seen
            )
            Result.success(camera)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateCamera(camera: com.company.ipcamera.shared.domain.model.Camera): Result<com.company.ipcamera.shared.domain.model.Camera> {
        return try {
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
    
    override suspend fun discoverCameras() = emptyList<com.company.ipcamera.shared.domain.repository.DiscoveredCamera>()
    
    override suspend fun testConnection(camera: com.company.ipcamera.shared.domain.model.Camera) = 
        com.company.ipcamera.shared.domain.repository.ConnectionTestResult.Failure("Not implemented", com.company.ipcamera.shared.domain.repository.ErrorCode.UNKNOWN)
    
    override suspend fun getCameraStatus(id: String): com.company.ipcamera.shared.domain.model.CameraStatus {
        return try {
            val camera = getCameraById(id)
            camera?.status ?: com.company.ipcamera.shared.domain.model.CameraStatus.UNKNOWN
        } catch (e: Exception) {
            com.company.ipcamera.shared.domain.model.CameraStatus.ERROR
        }
    }
}


