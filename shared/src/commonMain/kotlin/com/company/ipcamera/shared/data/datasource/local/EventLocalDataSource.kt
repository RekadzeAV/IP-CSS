package com.company.ipcamera.shared.data.datasource.local

import com.company.ipcamera.shared.domain.model.Event
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType

/**
 * Локальный источник данных для событий.
 * Отвечает за работу с локальной базой данных SQLite.
 */
interface EventLocalDataSource {
    /**
     * Получить все события из локальной БД
     */
    suspend fun getEvents(): List<Event>

    /**
     * Получить событие по ID из локальной БД
     */
    suspend fun getEventById(id: String): Event?

    /**
     * Получить события по ID камеры
     */
    suspend fun getEventsByCameraId(cameraId: String): List<Event>

    /**
     * Получить события по типу
     */
    suspend fun getEventsByType(type: EventType): List<Event>

    /**
     * Получить события по уровню серьезности
     */
    suspend fun getEventsBySeverity(severity: EventSeverity): List<Event>

    /**
     * Получить неподтвержденные события
     */
    suspend fun getUnacknowledgedEvents(): List<Event>

    /**
     * Получить события в диапазоне дат
     */
    suspend fun getEventsByDateRange(startTime: Long, endTime: Long): List<Event>

    /**
     * Сохранить событие в локальную БД
     */
    suspend fun saveEvent(event: Event): Result<Event>

    /**
     * Сохранить список событий в локальную БД (batch операция)
     */
    suspend fun saveEvents(events: List<Event>): Result<List<Event>>

    /**
     * Обновить событие в локальной БД
     */
    suspend fun updateEvent(event: Event): Result<Event>

    /**
     * Подтвердить событие
     */
    suspend fun acknowledgeEvent(id: String, userId: String, timestamp: Long): Result<Unit>

    /**
     * Подтвердить несколько событий
     */
    suspend fun acknowledgeEvents(ids: List<String>, userId: String, timestamp: Long): Result<Unit>

    /**
     * Удалить событие из локальной БД
     */
    suspend fun deleteEvent(id: String): Result<Unit>

    /**
     * Удалить события по ID камеры
     */
    suspend fun deleteEventsByCameraId(cameraId: String): Result<Unit>

    /**
     * Удалить старые события (до указанной даты)
     */
    suspend fun deleteOldEvents(beforeTimestamp: Long): Result<Int>

    /**
     * Удалить все события из локальной БД
     */
    suspend fun deleteAllEvents(): Result<Unit>

    /**
     * Проверить существование события
     */
    suspend fun eventExists(id: String): Boolean
}

