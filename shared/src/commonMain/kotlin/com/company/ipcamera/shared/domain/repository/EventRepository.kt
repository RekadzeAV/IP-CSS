package com.company.ipcamera.shared.domain.repository

import com.company.ipcamera.shared.domain.model.Event
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType

/**
 * Репозиторий для работы с событиями
 */
interface EventRepository {
    /**
     * Получить все события
     */
    suspend fun getEvents(
        type: EventType? = null,
        cameraId: String? = null,
        severity: EventSeverity? = null,
        acknowledged: Boolean? = null,
        startTime: Long? = null,
        endTime: Long? = null,
        page: Int = 1,
        limit: Int = 20
    ): PaginatedResult<Event>
    
    /**
     * Получить событие по ID
     */
    suspend fun getEventById(id: String): Event?
    
    /**
     * Добавить новое событие
     */
    suspend fun addEvent(event: Event): Result<Event>
    
    /**
     * Обновить событие
     */
    suspend fun updateEvent(event: Event): Result<Event>
    
    /**
     * Подтвердить событие
     */
    suspend fun acknowledgeEvent(id: String, userId: String): Result<Event>
    
    /**
     * Подтвердить несколько событий
     */
    suspend fun acknowledgeEvents(ids: List<String>, userId: String): Result<List<Event>>
    
    /**
     * Удалить событие
     */
    suspend fun deleteEvent(id: String): Result<Unit>
    
    /**
     * Получить статистику событий
     */
    suspend fun getEventStatistics(
        cameraId: String? = null,
        startTime: Long? = null,
        endTime: Long? = null
    ): Result<Map<String, Any>>
}


