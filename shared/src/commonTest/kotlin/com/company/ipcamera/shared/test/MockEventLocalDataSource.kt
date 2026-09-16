package com.company.ipcamera.shared.test

import com.company.ipcamera.shared.data.datasource.local.EventLocalDataSource
import com.company.ipcamera.shared.domain.model.Event
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType

/**
 * Mock реализация EventLocalDataSource для тестов
 */
class MockEventLocalDataSource(
    private var events: MutableList<Event> = mutableListOf(),
) : EventLocalDataSource {
    var shouldFailOnSave: Boolean = false
    var shouldFailOnUpdate: Boolean = false
    var shouldFailOnDelete: Boolean = false

    override suspend fun getEvents(): List<Event> = events.toList()

    override suspend fun getEventById(id: String): Event? = events.find { it.id == id }

    override suspend fun getEventsByCameraId(cameraId: String): List<Event> {
        return events.filter { it.cameraId == cameraId }
    }

    override suspend fun getEventsByType(type: EventType): List<Event> {
        return events.filter { it.type == type }
    }

    override suspend fun getEventsBySeverity(severity: EventSeverity): List<Event> {
        return events.filter { it.severity == severity }
    }

    override suspend fun getUnacknowledgedEvents(): List<Event> {
        return events.filter { !it.acknowledged }
    }

    override suspend fun getEventsByDateRange(
        startTime: Long,
        endTime: Long,
    ): List<Event> {
        return events.filter { it.timestamp >= startTime && it.timestamp <= endTime }
    }

    override suspend fun saveEvent(event: Event): Result<Event> {
        return if (shouldFailOnSave) {
            Result.failure(Exception("Mock save failure"))
        } else {
            events.add(event)
            Result.success(event)
        }
    }

    override suspend fun saveEvents(events: List<Event>): Result<List<Event>> {
        return if (shouldFailOnSave) {
            Result.failure(Exception("Mock save failure"))
        } else {
            this.events.addAll(events)
            Result.success(events)
        }
    }

    override suspend fun updateEvent(event: Event): Result<Event> {
        return if (shouldFailOnUpdate) {
            Result.failure(Exception("Mock update failure"))
        } else {
            val index = events.indexOfFirst { it.id == event.id }
            if (index >= 0) {
                events[index] = event
                Result.success(event)
            } else {
                Result.failure(Exception("Event not found: ${event.id}"))
            }
        }
    }

    override suspend fun acknowledgeEvent(
        id: String,
        userId: String,
        timestamp: Long,
    ): Result<Unit> {
        val event = events.find { it.id == id }
        return if (event != null) {
            val updated =
                event.copy(
                    acknowledged = true,
                    acknowledgedAt = timestamp,
                    acknowledgedBy = userId,
                )
            val index = events.indexOfFirst { it.id == id }
            events[index] = updated
            Result.success(Unit)
        } else {
            Result.failure(Exception("Event not found: $id"))
        }
    }

    override suspend fun acknowledgeEvents(
        ids: List<String>,
        userId: String,
        timestamp: Long,
    ): Result<Unit> {
        ids.forEach { id ->
            val event = events.find { it.id == id }
            if (event != null) {
                val updated =
                    event.copy(
                        acknowledged = true,
                        acknowledgedAt = timestamp,
                        acknowledgedBy = userId,
                    )
                val index = events.indexOfFirst { it.id == id }
                events[index] = updated
            }
        }
        return Result.success(Unit)
    }

    override suspend fun deleteEvent(id: String): Result<Unit> {
        return if (shouldFailOnDelete) {
            Result.failure(Exception("Mock delete failure"))
        } else {
            events.removeIf { it.id == id }
            Result.success(Unit)
        }
    }

    override suspend fun deleteEventsByCameraId(cameraId: String): Result<Unit> {
        events.removeIf { it.cameraId == cameraId }
        return Result.success(Unit)
    }

    override suspend fun deleteOldEvents(beforeTimestamp: Long): Result<Int> {
        val count = events.count { it.timestamp < beforeTimestamp }
        events.removeIf { it.timestamp < beforeTimestamp }
        return Result.success(count)
    }

    override suspend fun deleteAllEvents(): Result<Unit> {
        events.clear()
        return Result.success(Unit)
    }

    override suspend fun eventExists(id: String): Boolean {
        return events.any { it.id == id }
    }

    fun clear() {
        events.clear()
    }

    fun addEventDirectly(event: Event) {
        events.add(event)
    }
}
