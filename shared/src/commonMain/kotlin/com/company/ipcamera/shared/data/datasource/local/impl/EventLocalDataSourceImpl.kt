package com.company.ipcamera.shared.data.datasource.local.impl

import com.company.ipcamera.shared.data.datasource.local.EventLocalDataSource
import com.company.ipcamera.shared.data.local.DatabaseFactory
import com.company.ipcamera.shared.data.local.EventEntityMapper
import com.company.ipcamera.shared.data.local.createDatabase
import com.company.ipcamera.shared.domain.model.Event
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Реализация EventLocalDataSource с использованием SQLDelight
 */
class EventLocalDataSourceImpl(
    private val databaseFactory: DatabaseFactory
) : EventLocalDataSource {

    private val database = createDatabase(databaseFactory.createDriver())
    private val mapper = EventEntityMapper()

    override suspend fun getEvents(): List<Event> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries
                .selectAllEvents()
                .executeAsList()
                .map { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting events from local database" }
            emptyList()
        }
    }

    override suspend fun getEventById(id: String): Event? = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries
                .selectEventById(id)
                .executeAsOneOrNull()
                ?.let { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting event by id from local database: $id" }
            null
        }
    }

    override suspend fun getEventsByCameraId(cameraId: String): List<Event> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries
                .selectEventsByCameraId(cameraId)
                .executeAsList()
                .map { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting events by camera id from local database: $cameraId" }
            emptyList()
        }
    }

    override suspend fun getEventsByType(type: EventType): List<Event> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries
                .selectEventsByType(type.name)
                .executeAsList()
                .map { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting events by type from local database: $type" }
            emptyList()
        }
    }

    override suspend fun getEventsBySeverity(severity: EventSeverity): List<Event> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries
                .selectEventsBySeverity(severity.name)
                .executeAsList()
                .map { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting events by severity from local database: $severity" }
            emptyList()
        }
    }

    override suspend fun getUnacknowledgedEvents(): List<Event> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries
                .selectUnacknowledgedEvents()
                .executeAsList()
                .map { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting unacknowledged events from local database" }
            emptyList()
        }
    }

    override suspend fun getEventsByDateRange(startTime: Long, endTime: Long): List<Event> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries
                .selectEventsByDateRange(startTime, endTime)
                .executeAsList()
                .map { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting events by date range from local database" }
            emptyList()
        }
    }

    override suspend fun saveEvent(event: Event): Result<Event> = withContext(Dispatchers.Default) {
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
            logger.error(e) { "Error saving event to local database: ${event.id}" }
            Result.failure(e)
        }
    }

    override suspend fun saveEvents(events: List<Event>): Result<List<Event>> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.transaction {
                events.forEach { event ->
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
                }
            }
            Result.success(events)
        } catch (e: Exception) {
            logger.error(e) { "Error saving events to local database" }
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
            logger.error(e) { "Error updating event in local database: ${event.id}" }
            Result.failure(e)
        }
    }

    override suspend fun acknowledgeEvent(id: String, userId: String, timestamp: Long): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.acknowledgeEvent(
                acknowledged_at = timestamp,
                acknowledged_by = userId,
                id = id
            )
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error acknowledging event in local database: $id" }
            Result.failure(e)
        }
    }

    override suspend fun acknowledgeEvents(ids: List<String>, userId: String, timestamp: Long): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.transaction {
                ids.forEach { id ->
                    database.cameraDatabaseQueries.acknowledgeEvent(
                        acknowledged_at = timestamp,
                        acknowledged_by = userId,
                        id = id
                    )
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error acknowledging events in local database" }
            Result.failure(e)
        }
    }

    override suspend fun deleteEvent(id: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.deleteEvent(id)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error deleting event from local database: $id" }
            Result.failure(e)
        }
    }

    override suspend fun deleteEventsByCameraId(cameraId: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.deleteEventsByCameraId(cameraId)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error deleting events by camera id from local database: $cameraId" }
            Result.failure(e)
        }
    }

    override suspend fun deleteOldEvents(beforeTimestamp: Long): Result<Int> = withContext(Dispatchers.Default) {
        try {
            // Сначала получаем количество удаляемых событий
            val count = database.cameraDatabaseQueries
                .selectEventsByDateRange(0, beforeTimestamp)
                .executeAsList()
                .size

            database.cameraDatabaseQueries.deleteOldEvents(beforeTimestamp)
            Result.success(count)
        } catch (e: Exception) {
            logger.error(e) { "Error deleting old events from local database" }
            Result.failure(e)
        }
    }

    override suspend fun deleteAllEvents(): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            val allEvents = database.cameraDatabaseQueries.selectAllEvents().executeAsList()
            database.cameraDatabaseQueries.transaction {
                allEvents.forEach { event ->
                    database.cameraDatabaseQueries.deleteEvent(event.id)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error deleting all events from local database" }
            Result.failure(e)
        }
    }

    override suspend fun eventExists(id: String): Boolean = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.selectEventById(id).executeAsOneOrNull() != null
        } catch (e: Exception) {
            logger.error(e) { "Error checking event existence in local database: $id" }
            false
        }
    }
}

