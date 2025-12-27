package com.company.ipcamera.shared.data.repository

import com.company.ipcamera.shared.data.local.DatabaseFactory
import com.company.ipcamera.shared.data.local.EventEntityMapper
import com.company.ipcamera.shared.data.local.createDatabase
import com.company.ipcamera.shared.domain.model.Event
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.domain.repository.EventRepository
import com.company.ipcamera.shared.domain.repository.PaginatedResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Реализация EventRepository с использованием SQLDelight
 */
class EventRepositoryImplSqlDelight(
    private val databaseFactory: DatabaseFactory
) : EventRepository {

    private val database = createDatabase(databaseFactory.createDriver())
    private val mapper = EventEntityMapper()

    override suspend fun getEvents(
        type: EventType?,
        cameraId: String?,
        severity: EventSeverity?,
        acknowledged: Boolean?,
        startTime: Long?,
        endTime: Long?,
        page: Int,
        limit: Int
    ): PaginatedResult<Event> = withContext(Dispatchers.Default) {
        try {
            val offset = (page - 1) * limit

            // Получаем все события в зависимости от фильтров
            val allEvents = when {
                cameraId != null && type != null && severity != null && acknowledged != null -> {
                    val events = database.cameraDatabaseQueries
                        .selectEventsByCameraId(cameraId)
                        .executeAsList()
                        .filter {
                            it.type == type.name &&
                            it.severity == severity.name &&
                            (it.acknowledged == 1L) == acknowledged
                        }
                    if (startTime != null && endTime != null) {
                        events.filter { it.timestamp >= startTime && it.timestamp <= endTime }
                    } else {
                        events
                    }
                }
                cameraId != null && type != null -> {
                    val events = database.cameraDatabaseQueries
                        .selectEventsByCameraId(cameraId)
                        .executeAsList()
                        .filter { it.type == type.name }
                    if (startTime != null && endTime != null) {
                        events.filter { it.timestamp >= startTime && it.timestamp <= endTime }
                    } else {
                        events
                    }
                }
                cameraId != null && severity != null -> {
                    val events = database.cameraDatabaseQueries
                        .selectEventsByCameraId(cameraId)
                        .executeAsList()
                        .filter { it.severity == severity.name }
                    if (startTime != null && endTime != null) {
                        events.filter { it.timestamp >= startTime && it.timestamp <= endTime }
                    } else {
                        events
                    }
                }
                cameraId != null -> {
                    val events = database.cameraDatabaseQueries
                        .selectEventsByCameraId(cameraId)
                        .executeAsList()
                    if (startTime != null && endTime != null) {
                        events.filter { it.timestamp >= startTime && it.timestamp <= endTime }
                    } else if (acknowledged != null) {
                        events.filter { (it.acknowledged == 1L) == acknowledged }
                    } else {
                        events
                    }
                }
                type != null -> {
                    val events = database.cameraDatabaseQueries
                        .selectEventsByType(type.name)
                        .executeAsList()
                    if (startTime != null && endTime != null) {
                        events.filter { it.timestamp >= startTime && it.timestamp <= endTime }
                    } else if (acknowledged != null) {
                        events.filter { (it.acknowledged == 1L) == acknowledged }
                    } else {
                        events
                    }
                }
                severity != null -> {
                    val events = database.cameraDatabaseQueries
                        .selectEventsBySeverity(severity.name)
                        .executeAsList()
                    if (startTime != null && endTime != null) {
                        events.filter { it.timestamp >= startTime && it.timestamp <= endTime }
                    } else if (acknowledged != null) {
                        events.filter { (it.acknowledged == 1L) == acknowledged }
                    } else {
                        events
                    }
                }
                acknowledged == false -> {
                    val events = database.cameraDatabaseQueries
                        .selectUnacknowledgedEvents()
                        .executeAsList()
                    if (startTime != null && endTime != null) {
                        events.filter { it.timestamp >= startTime && it.timestamp <= endTime }
                    } else {
                        events
                    }
                }
                startTime != null && endTime != null -> {
                    database.cameraDatabaseQueries
                        .selectEventsByDateRange(startTime, endTime)
                        .executeAsList()
                        .let { events ->
                            if (acknowledged != null) {
                                events.filter { (it.acknowledged == 1L) == acknowledged }
                            } else {
                                events
                            }
                        }
                }
                else -> {
                    val events = database.cameraDatabaseQueries
                        .selectAllEvents()
                        .executeAsList()
                    if (acknowledged != null) {
                        events.filter { (it.acknowledged == 1L) == acknowledged }
                    } else {
                        events
                    }
                }
            }

            val total = allEvents.size
            val paginatedEvents = allEvents.drop(offset).take(limit)

            PaginatedResult(
                items = paginatedEvents.map { mapper.toDomain(it) },
                total = total,
                page = page,
                limit = limit,
                hasMore = offset + limit < total
            )
        } catch (e: Exception) {
            logger.error(e) { "Error getting events" }
            PaginatedResult(emptyList(), 0, page, limit, false)
        }
    }

    override suspend fun getEventById(id: String): Event? = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries
                .selectEventById(id)
                .executeAsOneOrNull()
                ?.let { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting event by id: $id" }
            null
        }
    }

    override suspend fun addEvent(event: Event): Result<Event> = withContext(Dispatchers.Default) {
        try {
            val dbEvent = mapper.toDatabase(event)
            database.cameraDatabaseQueries.insertEvent(
                id = dbEvent.id,
                camera_id = dbEvent.camera_id,
                camera_name = dbEvent.camera_name,
                type = dbEvent.type,
                severity = dbEvent.severity,
                timestamp = dbEvent.timestamp,
                description = dbEvent.description,
                metadata = dbEvent.metadata,
                acknowledged = dbEvent.acknowledged,
                acknowledged_at = dbEvent.acknowledged_at,
                acknowledged_by = dbEvent.acknowledged_by,
                thumbnail_url = dbEvent.thumbnail_url,
                video_url = dbEvent.video_url
            )
            Result.success(event)
        } catch (e: Exception) {
            logger.error(e) { "Error adding event: ${event.id}" }
            Result.failure(e)
        }
    }

    override suspend fun updateEvent(event: Event): Result<Event> = withContext(Dispatchers.Default) {
        try {
            val dbEvent = mapper.toDatabase(event)
            database.cameraDatabaseQueries.updateEvent(
                camera_id = dbEvent.camera_id,
                camera_name = dbEvent.camera_name,
                type = dbEvent.type,
                severity = dbEvent.severity,
                timestamp = dbEvent.timestamp,
                description = dbEvent.description,
                metadata = dbEvent.metadata,
                acknowledged = dbEvent.acknowledged,
                acknowledged_at = dbEvent.acknowledged_at,
                acknowledged_by = dbEvent.acknowledged_by,
                thumbnail_url = dbEvent.thumbnail_url,
                video_url = dbEvent.video_url,
                id = dbEvent.id
            )
            Result.success(event)
        } catch (e: Exception) {
            logger.error(e) { "Error updating event: ${event.id}" }
            Result.failure(e)
        }
    }

    override suspend fun acknowledgeEvent(id: String, userId: String): Result<Event> = withContext(Dispatchers.Default) {
        try {
            val now = System.currentTimeMillis()
            database.cameraDatabaseQueries.acknowledgeEvent(
                acknowledged_at = now,
                acknowledged_by = userId,
                id = id
            )
            getEventById(id)?.let { event ->
                Result.success(event)
            } ?: Result.failure(IllegalArgumentException("Event not found: $id"))
        } catch (e: Exception) {
            logger.error(e) { "Error acknowledging event: $id" }
            Result.failure(e)
        }
    }

    override suspend fun acknowledgeEvents(ids: List<String>, userId: String): Result<List<Event>> = withContext(Dispatchers.Default) {
        try {
            val now = System.currentTimeMillis()
            ids.forEach { id ->
                database.cameraDatabaseQueries.acknowledgeEvent(
                    acknowledged_at = now,
                    acknowledged_by = userId,
                    id = id
                )
            }
            val events = ids.mapNotNull { getEventById(it) }
            Result.success(events)
        } catch (e: Exception) {
            logger.error(e) { "Error acknowledging events" }
            Result.failure(e)
        }
    }

    override suspend fun deleteEvent(id: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.deleteEvent(id)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error deleting event: $id" }
            Result.failure(e)
        }
    }

    override suspend fun getEventStatistics(
        cameraId: String?,
        startTime: Long?,
        endTime: Long?
    ): Result<Map<String, Any>> = withContext(Dispatchers.Default) {
        try {
            val events = when {
                cameraId != null && startTime != null && endTime != null -> {
                    database.cameraDatabaseQueries
                        .selectEventsByDateRange(startTime, endTime)
                        .executeAsList()
                        .filter { it.camera_id == cameraId }
                }
                cameraId != null -> {
                    database.cameraDatabaseQueries
                        .selectEventsByCameraId(cameraId)
                        .executeAsList()
                }
                startTime != null && endTime != null -> {
                    database.cameraDatabaseQueries
                        .selectEventsByDateRange(startTime, endTime)
                        .executeAsList()
                }
                else -> {
                    database.cameraDatabaseQueries
                        .selectAllEvents()
                        .executeAsList()
                }
            }

            val stats = mapOf(
                "total" to events.size,
                "byType" to events.groupingBy { it.type }.eachCount(),
                "bySeverity" to events.groupingBy { it.severity }.eachCount(),
                "acknowledged" to events.count { it.acknowledged == 1L },
                "unacknowledged" to events.count { it.acknowledged == 0L }
            )
            Result.success(stats)
        } catch (e: Exception) {
            logger.error(e) { "Error getting event statistics" }
            Result.failure(e)
        }
    }
}


