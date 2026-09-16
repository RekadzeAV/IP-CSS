package com.company.ipcamera.server.repository

import com.company.ipcamera.shared.domain.model.MotionConfig
import com.company.ipcamera.shared.domain.model.MotionZone
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.ConcurrentHashMap

/**
 * In-Memory реализация MotionConfigRepository
 * Для production используйте PostgreSQL реализацию
 */
class MotionConfigRepositoryInMemory : MotionConfigRepository {
    private val configs = ConcurrentHashMap<String, MotionConfig>()
    
    override suspend fun findById(id: String): MotionConfig? {
        return configs[id]
    }
    
    override suspend fun findByCameraId(cameraId: String): MotionConfig? {
        return configs.values.find { it.cameraId == cameraId }
    }
    
    override suspend fun findAll(): List<MotionConfig> {
        return configs.values.toList()
    }
    
    override suspend fun save(config: MotionConfig): Result<MotionConfig> {
        return try {
            val now = System.currentTimeMillis()
            val configWithTimestamps = if (configs.containsKey(config.id)) {
                config.copy(updatedAt = now)
            } else {
                config.copy(createdAt = now, updatedAt = now)
            }
            
            configs[config.id] = configWithTimestamps
            Result.success(configWithTimestamps)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun delete(id: String): Result<Unit> {
        return try {
            configs.remove(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateStatus(cameraId: String, enabled: Boolean): Result<MotionConfig> {
        val config = findByCameraId(cameraId)
            ?: return Result.failure(Exception("Configuration not found for camera: $cameraId"))
        
        val updatedConfig = config.copy(enabled = enabled, updatedAt = System.currentTimeMillis())
        return save(updatedConfig)
    }
}
