package com.company.ipcamera.server.repository

import com.company.ipcamera.shared.domain.model.MotionEvent
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory реализация MotionEventRepository.
 * TODO: Заменить на SQLDelight когда будет синхронизирована модель.
 */
class SqlDelightMotionEventRepository : MotionEventRepository {

    private val storage = mutableMapOf<String, MotionEvent>()
    private val mutex = Mutex()

    override suspend fun findById(id: String): MotionEvent? {
        return mutex.withLock { storage[id] }
    }

    override suspend fun findByCameraId(cameraId: String, limit: Int): List<MotionEvent> {
        return mutex.withLock {
            storage.values.filter { it.cameraId == cameraId }.take(limit)
        }
    }

    override suspend fun findByTimeRange(startTime: Long, endTime: Long, limit: Int): List<MotionEvent> {
        return mutex.withLock {
            storage.values.filter { it.timestamp in startTime..endTime }.take(limit)
        }
    }

    override suspend fun findUnprocessed(): List<MotionEvent> {
        return mutex.withLock {
            storage.values.filter { !it.processed }
        }
    }

    override suspend fun save(event: MotionEvent): Result<MotionEvent> {
        return mutex.withLock {
            storage[event.id] = event
            Result.success(event)
        }
    }

    override suspend fun markProcessed(id: String): Result<Unit> {
        return mutex.withLock {
            storage.computeIfPresent(id) { _, event -> event.copy(processed = true) }
            Result.success(Unit)
        }
    }

    override suspend fun deleteOlderThan(timestamp: Long): Result<Int> {
        return mutex.withLock {
            val toDelete = storage.filterValues { it.timestamp < timestamp }
            toDelete.keys.forEach { storage.remove(it) }
            Result.success(toDelete.size)
        }
    }
}