package com.company.ipcamera.server.repository

import com.company.ipcamera.server.websocket.WebSocketManager
import com.company.ipcamera.server.websocket.WebSocketChannel
import com.company.ipcamera.shared.database.CameraDatabase
import com.company.ipcamera.shared.database.Event as DbEvent
import com.company.ipcamera.shared.domain.model.Event
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.domain.repository.EventRepository
import com.company.ipcamera.shared.domain.repository.PaginatedResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Серверная реализация [EventRepository] на SQLDelight / PostgreSQL.
 *
 * Использует общий экземпляр [CameraDatabase] (единый пул соединений).
 * Заменяет прежнюю in-memory реализацию [ServerEventRepository] (миграция 1.4).
 * Сохраняет WebSocket-оповещения о создании/подтверждении событий.
 */
class ServerEventRepositorySqlDelight(
    private val database: CameraDatabase,
) : EventRepository {

    private val mapper = ServerEventSqlDelightMapper()

    override suspend fun getEvents(
        type: EventType?,
        cameraId: String?,
        severity: EventSeverity?,
        acknowledged: Boolean?,
        startTime: Long?,
        endTime: Long?,
        page: Int,
        limit: Int,
    ): PaginatedResult<Event> = withContext(Dispatchers.IO) {
        try {
            var allEvents: List<DbEvent> =
                database.cameraDatabaseQueries.selectAllEvents().executeAsList()

            if (cameraId != null) {
                allEvents = allEvents.filter { it.camera_id == cameraId }
            }
            if (type != null) {
                allEvents = allEvents.filter { it.type == type.name }
            }
            if (severity != null) {
                allEvents = allEvents.filter { it.severity == severity.name }
            }
            if (acknowledged != null) {
                allEvents = allEvents.filter { (it.acknowledged == 1L) == acknowledged }
            }
            if (startTime != null) {
                allEvents = allEvents.filter { it.timestamp >= startTime }
            }
            if (endTime != null) {
                allEvents = allEvents.filter { it.timestamp <= endTime }
            }

            val sorted = allEvents.sortedByDescending { it.timestamp }
            val total = sorted.size
            val offset = (page - 1) * limit

            PaginatedResult(
                items = sorted.drop(offset).take(limit).map { mapper.toDomain(it) },
                total = total,
                page = page,
                limit = limit,
                hasMore = offset + limit < total,
            )
        } catch (e: Exception) {
            logger.error(e) { "Error getting events from PostgreSQL" }
            PaginatedResult(emptyList(), 0, page, limit, false)
        }
    }

    override suspend fun getEventById(id: String): Event? = withContext(Dispatchers.IO) {
        try {
            database.cameraDatabaseQueries.selectEventById(id)
                .executeAsOneOrNull()
                ?.let { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting event by id: $id" }
            null
        }
    }

    override suspend fun addEvent(event: Event): Result<Event> = withContext(Dispatchers.IO) {
        try {
            val db = mapper.toDatabase(event)
            database.cameraDatabaseQueries.insertEvent(
                id = db.id,
                camera_id = db.camera_id,
                camera_name = db.camera_name,
                type = db.type,
                severity = db.severity,
                timestamp = db.timestamp,
                description = db.description,
                metadata = db.metadata,
                acknowledged = db.acknowledged,
                acknowledged_at = db.acknowledged_at,
                acknowledged_by = db.acknowledged_by,
                thumbnail_url = db.thumbnail_url,
                video_url = db.video_url,
            )
            logger.info { "Event persisted: ${event.id} (type: ${event.type}, severity: ${event.severity})" }

            broadcastEventCreated(event)
            Result.success(event)
        } catch (e: Exception) {
            logger.error(e) { "Error adding event: ${event.id}" }
            Result.failure(e)
        }
    }

    override suspend fun updateEvent(event: Event): Result<Event> = withContext(Dispatchers.IO) {
        // INSERT OR REPLACE: чистый UPDATE не вставляет строку, если её ещё нет.
        try {
            val db = mapper.toDatabase(event)
            database.cameraDatabaseQueries.insertEvent(
                id = db.id,
                camera_id = db.camera_id,
                camera_name = db.camera_name,
                type = db.type,
                severity = db.severity,
                timestamp = db.timestamp,
                description = db.description,
                metadata = db.metadata,
                acknowledged = db.acknowledged,
                acknowledged_at = db.acknowledged_at,
                acknowledged_by = db.acknowledged_by,
                thumbnail_url = db.thumbnail_url,
                video_url = db.video_url,
            )
            Result.success(event)
        } catch (e: Exception) {
            logger.error(e) { "Error updating event: ${event.id}" }
            Result.failure(e)
        }
    }

    override suspend fun acknowledgeEvent(id: String, userId: String): Result<Event> =
        withContext(Dispatchers.IO) {
            try {
                val existing = database.cameraDatabaseQueries.selectEventById(id)
                    .executeAsOneOrNull()
                    ?: return@withContext Result.failure(IllegalArgumentException("Event not found: $id"))
                val now = System.currentTimeMillis()
                database.cameraDatabaseQueries.acknowledgeEvent(
                    acknowledged_at = now,
                    acknowledged_by = userId,
                    id = id,
                )
                val updatedDb = database.cameraDatabaseQueries.selectEventById(id).executeAsOne()
                val updated = mapper.toDomain(updatedDb)

                broadcastEventAcknowledged(updated, userId)
                Result.success(updated)
            } catch (e: Exception) {
                logger.error(e) { "Error acknowledging event: $id" }
                Result.failure(e)
            }
        }

    override suspend fun acknowledgeEvents(ids: List<String>, userId: String): Result<List<Event>> =
        withContext(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                val acknowledgedEvents = mutableListOf<Event>()
                for (id in ids) {
                    database.cameraDatabaseQueries.acknowledgeEvent(
                        acknowledged_at = now,
                        acknowledged_by = userId,
                        id = id,
                    )
                    database.cameraDatabaseQueries.selectEventById(id)
                        .executeAsOneOrNull()
                        ?.let { acknowledgedEvents.add(mapper.toDomain(it)) }
                }
                logger.info { "Events acknowledged in PostgreSQL: ${ids.size} by user: $userId" }
                broadcastEventsAcknowledged(ids, acknowledgedEvents.size, userId)
                Result.success(acknowledgedEvents)
            } catch (e: Exception) {
                logger.error(e) { "Error acknowledging events in PostgreSQL" }
                Result.failure(e)
            }
        }

    override suspend fun deleteEvent(id: String): Result<Unit> = withContext(Dispatchers.IO) {
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
        endTime: Long?,
    ): Result<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            var allEvents =
                database.cameraDatabaseQueries.selectAllEvents().executeAsList()
            if (cameraId != null) {
                allEvents = allEvents.filter { it.camera_id == cameraId }
            }
            if (startTime != null) {
                allEvents = allEvents.filter { it.timestamp >= startTime }
            }
            if (endTime != null) {
                allEvents = allEvents.filter { it.timestamp <= endTime }
            }

            Result.success(
                mapOf(
                    "total" to allEvents.size,
                    "byType" to allEvents.groupingBy { it.type }.eachCount(),
                    "bySeverity" to allEvents.groupingBy { it.severity }.eachCount(),
                    "acknowledged" to allEvents.count { it.acknowledged == 1L },
                    "unacknowledged" to allEvents.count { it.acknowledged == 0L },
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Error getting event statistics from PostgreSQL" }
            Result.failure(e)
        }
    }
private suspend fun broadcastEventCreated(event: Event) {
        try {
            WebSocketManager.broadcastEvent(
                WebSocketChannel.EVENTS,
                "event_created",
                buildJsonObject {
                    put("eventId", event.id)
                    put("cameraId", event.cameraId)
                    put("cameraName", event.cameraName ?: "")
                    put("type", event.type.name)
                    put("severity", event.severity.name)
                    put("description", event.description ?: "")
                    put("timestamp", event.timestamp)
                }
            )
        } catch (e: Exception) {
            logger.warn(e) { "Failed to send WebSocket event_created" }
        }
    }

    private suspend fun broadcastEventAcknowledged(event: Event, userId: String) {
        try {
            WebSocketManager.broadcastEvent(
                WebSocketChannel.EVENTS,
                "event_acknowledged",
                buildJsonObject {
                    put("eventId", event.id)
                    put("userId", userId)
                    put("acknowledgedAt", event.acknowledgedAt ?: System.currentTimeMillis())
                    put("timestamp", System.currentTimeMillis())
                }
            )
        } catch (e: Exception) {
            logger.warn(e) { "Failed to send WebSocket event_acknowledged" }
        }
    }

    private suspend fun broadcastEventsAcknowledged(ids: List<String>, count: Int, userId: String) {
        try {
            WebSocketManager.broadcastEvent(
                WebSocketChannel.EVENTS,
                "events_acknowledged",
                buildJsonObject {
                    put("eventIds", JsonArray(ids.map { JsonPrimitive(it) }))
                    put("userId", userId)
                    put("count", count)
                    put("timestamp", System.currentTimeMillis())
                }
            )
        } catch (e: Exception) {
            logger.warn(e) { "Failed to send WebSocket events_acknowledged" }
        }
    }
}

/**
 * Маппер SQLDelight-сущности Event ↔ доменной [Event]. Локальный для серверного
 * модуля (в shared `EventEntityMapper` имеет internal-видимость).
 */
private class ServerEventSqlDelightMapper {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun toDomain(db: DbEvent): Event = Event(
        id = db.id,
        cameraId = db.camera_id,
        cameraName = db.camera_name,
        type = EventType.valueOf(db.type),
        severity = EventSeverity.valueOf(db.severity),
        timestamp = db.timestamp,
        description = db.description,
        metadata = db.metadata?.let {
            runCatching { json.decodeFromString<Map<String, String>>(it) }.getOrNull()
        } ?: emptyMap(),
        acknowledged = db.acknowledged == 1L,
        acknowledgedAt = db.acknowledged_at,
        acknowledgedBy = db.acknowledged_by,
        thumbnailUrl = db.thumbnail_url,
        videoUrl = db.video_url,
    )

    fun toDatabase(event: Event): DbEvent = DbEvent(
        id = event.id,
        camera_id = event.cameraId,
        camera_name = event.cameraName,
        type = event.type.name,
        severity = event.severity.name,
        timestamp = event.timestamp,
        description = event.description,
        metadata = event.metadata.takeIf { it.isNotEmpty() }?.let { json.encodeToString(it) },
        acknowledged = if (event.acknowledged) 1L else 0L,
        acknowledged_at = event.acknowledgedAt,
        acknowledged_by = event.acknowledgedBy,
        thumbnail_url = event.thumbnailUrl,
        video_url = event.videoUrl,
    )
}


