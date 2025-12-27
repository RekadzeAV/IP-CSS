package com.company.ipcamera.server.repository

import com.company.ipcamera.shared.domain.model.Event
import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.repository.EventRepository
import com.company.ipcamera.shared.domain.repository.PaginatedResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Серверная реализация EventRepository
 * 
 * TODO: Мигрировать на SQLDelight/PostgreSQL для продакшена
 */
class ServerEventRepository : EventRepository {
    private val events = mutableMapOf<String, Event>()
    private val mutex = Mutex()
    
    override suspend fun getEvents(
        type: EventType?,
        cameraId: String?,
        severity: EventSeverity?,
        acknowledged: Boolean?,
        startTime: Long?,
        endTime: Long?,
        page: Int,
        limit: Int
    ): PaginatedResult<Event> = mutex.withLock {
        var filteredEvents = events.values.toList()
        
        // Фильтрация
        if (cameraId != null) {
            filteredEvents = filteredEvents.filter { it.cameraId == cameraId }
        }
        if (type != null) {
            filteredEvents = filteredEvents.filter { it.type == type }
        }
        if (severity != null) {
            filteredEvents = filteredEvents.filter { it.severity == severity }
        }
        if (acknowledged != null) {
            filteredEvents = filteredEvents.filter { it.acknowledged == acknowledged }
        }
        if (startTime != null) {
            filteredEvents = filteredEvents.filter { it.timestamp >= startTime }
        }
        if (endTime != null) {
            filteredEvents = filteredEvents.filter { it.timestamp <= endTime }
        }
        
        // Сортировка по времени (новые сначала)
        filteredEvents = filteredEvents.sortedByDescending { it.timestamp }
        
        val total = filteredEvents.size
        val offset = (page - 1) * limit
        val paginatedItems = filteredEvents.drop(offset).take(limit)
        val hasMore = offset + limit < total
        
        return PaginatedResult(
            items = paginatedItems,
            total = total,
            page = page,
            limit = limit,
            hasMore = hasMore
        )
    }
    
    override suspend fun getEventById(id: String): Event? = mutex.withLock {
        return events[id]
    }
    
    override suspend fun addEvent(event: Event): Result<Event> = mutex.withLock {
        try {
            events[event.id] = event
            logger.info { "Event added: ${event.id} (type: ${event.type}, severity: ${event.severity})" }
            Result.success(event)
        } catch (e: Exception) {
            logger.error(e) { "Error adding event: ${event.id}" }
            Result.failure(e)
        }
    }
    
    override suspend fun updateEvent(event: Event): Result<Event> = mutex.withLock {
        try {
            if (!events.containsKey(event.id)) {
                return Result.failure(IllegalArgumentException("Event not found: ${event.id}"))
            }
            events[event.id] = event
            logger.info { "Event updated: ${event.id}" }
            Result.success(event)
        } catch (e: Exception) {
            logger.error(e) { "Error updating event: ${event.id}" }
            Result.failure(e)
        }
    }
    
    override suspend fun deleteEvent(id: String): Result<Unit> = mutex.withLock {
        try {
            if (!events.containsKey(id)) {
                return Result.failure(IllegalArgumentException("Event not found: $id"))
            }
            events.remove(id)
            logger.info { "Event deleted: $id" }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error deleting event: $id" }
            Result.failure(e)
        }
    }
    
    override suspend fun acknowledgeEvent(id: String, userId: String): Result<Event> = mutex.withLock {
        try {
            val event = events[id] ?: return Result.failure(IllegalArgumentException("Event not found: $id"))
            val updatedEvent = event.copy(
                acknowledged = true,
                acknowledgedAt = System.currentTimeMillis(),
                acknowledgedBy = userId
            )
            events[id] = updatedEvent
            logger.info { "Event acknowledged: $id by user: $userId" }
            Result.success(updatedEvent)
        } catch (e: Exception) {
            logger.error(e) { "Error acknowledging event: $id" }
            Result.failure(e)
        }
    }
    
    override suspend fun acknowledgeEvents(ids: List<String>, userId: String): Result<List<Event>> = mutex.withLock {
        try {
            val acknowledgedEvents = mutableListOf<Event>()
            for (id in ids) {
                val event = events[id] ?: continue
                val updatedEvent = event.copy(
                    acknowledged = true,
                    acknowledgedAt = System.currentTimeMillis(),
                    acknowledgedBy = userId
                )
                events[id] = updatedEvent
                acknowledgedEvents.add(updatedEvent)
            }
            logger.info { "Events acknowledged: ${ids.size} events by user: $userId" }
            Result.success(acknowledgedEvents)
        } catch (e: Exception) {
            logger.error(e) { "Error acknowledging events" }
            Result.failure(e)
        }
    }
    
    override suspend fun getEventStatistics(
        cameraId: String?,
        startTime: Long?,
        endTime: Long?
    ): Result<Map<String, Any>> = mutex.withLock {
        try {
            var filteredEvents = events.values.toList()
            
            if (cameraId != null) {
                filteredEvents = filteredEvents.filter { it.cameraId == cameraId }
            }
            if (startTime != null) {
                filteredEvents = filteredEvents.filter { it.timestamp >= startTime }
            }
            if (endTime != null) {
                filteredEvents = filteredEvents.filter { it.timestamp <= endTime }
            }
            
            val statistics = mapOf(
                "total" to filteredEvents.size,
                "byType" to filteredEvents.groupingBy { it.type.name }.eachCount(),
                "bySeverity" to filteredEvents.groupingBy { it.severity.name }.eachCount(),
                "acknowledged" to filteredEvents.count { it.acknowledged },
                "unacknowledged" to filteredEvents.count { !it.acknowledged }
            )
            
            Result.success(statistics)
        } catch (e: Exception) {
            logger.error(e) { "Error getting event statistics" }
            Result.failure(e)
        }
    }
}

