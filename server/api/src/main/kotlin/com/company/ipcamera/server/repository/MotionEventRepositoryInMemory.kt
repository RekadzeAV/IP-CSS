package com.company.ipcamera.server.repository

import com.company.ipcamera.shared.domain.model.MotionEvent
import java.util.concurrent.ConcurrentHashMap

/**
 * In-Memory реализация MotionEventRepository
 * Для production используйте PostgreSQL реализацию
 */
class MotionEventRepositoryInMemory : MotionEventRepository {
    private val events = ConcurrentHashMap<String, MotionEvent>()
    
    override suspend fun findById(id: String): MotionEvent? {
        return events[id]
    }
    
    override suspend fun findByCameraId(cameraId: String, limit: Int): List<MotionEvent> {
        return events.values
            .filter { it.cameraId == cameraId }
            .sortedByDescending { it.timestamp }
            .take(limit)
    }
    
    override suspend fun findByTimeRange(startTime: Long, endTime: Long, limit: Int): List<MotionEvent> {
        return events.values
            .filter { it.timestamp in startTime..endTime }
            .sortedByDescending { it.timestamp }
            .take(limit)
    }
    
    override suspend fun findUnprocessed(): List<MotionEvent> {
        return events.values.filter { !it.processed }
    }
    
    override suspend fun save(event: MotionEvent): Result<MotionEvent> {
        return try {
            events[event.id] = event
            Result.success(event)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun markProcessed(id: String): Result<Unit> {
        val event = events[id]
            ?: return Result.failure(Exception("Event not found: $id"))
        
        events[id] = event.copy(processed = true)
        return Result.success(Unit)
    }
    
    override suspend fun deleteOlderThan(timestamp: Long): Result<Int> {
        return try {
            val toDelete = events.values.filter { it.timestamp < timestamp }
            toDelete.forEach { events.remove(it.id) }
            Result.success(toDelete.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
