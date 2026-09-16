package com.company.ipcamera.server.repository

import com.company.ipcamera.shared.domain.model.MotionConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory реализация MotionConfigRepository.
 * TODO: Заменить на SQLDelight когда будет синхронизирована модель.
 */
class SqlDelightMotionConfigRepository : MotionConfigRepository {

    private val storage = mutableMapOf<String, MotionConfig>()
    private val mutex = Mutex()

    override suspend fun findById(id: String): MotionConfig? {
        return mutex.withLock { storage[id] }
    }

    override suspend fun findByCameraId(cameraId: String): MotionConfig? {
        return mutex.withLock { storage.values.firstOrNull { it.cameraId == cameraId } }
    }

    override suspend fun findAll(): List<MotionConfig> {
        return mutex.withLock { storage.values.toList() }
    }

    override suspend fun save(config: MotionConfig): Result<MotionConfig> {
        return mutex.withLock {
            storage[config.id] = config
            Result.success(config)
        }
    }

    override suspend fun delete(id: String): Result<Unit> {
        return mutex.withLock {
            storage.remove(id)
            Result.success(Unit)
        }
    }

    override suspend fun updateStatus(cameraId: String, enabled: Boolean): Result<MotionConfig> {
        return mutex.withLock {
            val config = storage.values.firstOrNull { it.cameraId == cameraId }
                ?: return@withLock Result.failure(NoSuchElementException("Motion config not found for camera: $cameraId"))
            val updated = config.copy(enabled = enabled)
            storage[config.id] = updated
            Result.success(updated)
        }
    }
}