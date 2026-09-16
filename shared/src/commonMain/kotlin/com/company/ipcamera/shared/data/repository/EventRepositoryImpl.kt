package com.company.ipcamera.shared.data.repository

import com.company.ipcamera.shared.data.datasource.local.EventLocalDataSource
import com.company.ipcamera.shared.data.datasource.remote.EventRemoteDataSource
import com.company.ipcamera.shared.domain.model.Event
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.domain.repository.EventRepository
import com.company.ipcamera.shared.domain.repository.PaginatedResult
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Реализация EventRepository с использованием Data Sources (новая архитектура)
 *
 * Использует стратегию local-first: сначала проверяет локальную БД,
 * затем синхронизирует с удаленным API при необходимости
 */
class EventRepositoryImpl(
    private val localDataSource: EventLocalDataSource,
    private val remoteDataSource: EventRemoteDataSource? = null,
) : EventRepository {
    override suspend fun getEvents(
        type: EventType?,
        cameraId: String?,
        severity: EventSeverity?,
        acknowledged: Boolean?,
        startTime: Long?,
        endTime: Long?,
        page: Int,
        limit: Int,
    ): PaginatedResult<Event> =
        withContext(Dispatchers.Default) {
            try {
                // Получаем события локально с фильтрацией, используя комбинацию методов
                val allEvents =
                    buildList {
                        val baseEvents =
                            when {
                                cameraId != null -> localDataSource.getEventsByCameraId(cameraId)
                                type != null -> localDataSource.getEventsByType(type)
                                severity != null -> localDataSource.getEventsBySeverity(severity)
                                acknowledged == false -> localDataSource.getUnacknowledgedEvents()
                                else -> localDataSource.getEvents()
                            }

                        addAll(
                            baseEvents.filter { event ->
                                var matches = true
                                if (type != null && event.type != type) matches = false
                                if (cameraId != null && event.cameraId != cameraId) matches = false
                                if (severity != null && event.severity != severity) matches = false
                                if (acknowledged != null && event.acknowledged != acknowledged) matches = false
                                if (startTime != null && event.timestamp < startTime) matches = false
                                if (endTime != null && event.timestamp > endTime) matches = false
                                matches
                            },
                        )
                    }

                // Применяем пагинацию
                val offset = (page - 1) * limit
                val paginatedItems = allEvents.drop(offset).take(limit)
                val localResult =
                    PaginatedResult(
                        items = paginatedItems,
                        total = allEvents.size,
                        page = page,
                        limit = limit,
                        hasMore = (offset + limit) < allEvents.size,
                    )

                // Local-first: удалённый источник только если локально нет данных по запросу
                if (remoteDataSource != null && allEvents.isEmpty()) {
                    remoteDataSource.getEvents(
                        type,
                        cameraId,
                        severity,
                        acknowledged,
                        startTime,
                        endTime,
                        page,
                        limit,
                    ).fold(
                        onSuccess = { remoteResult ->
                            // Сохраняем в локальную БД для кэширования
                            if (remoteResult.items.isNotEmpty()) {
                                localDataSource.saveEvents(remoteResult.items).getOrNull()
                            }
                            remoteResult
                        },
                        onError = { error ->
                            logger.warn(error) { "Failed to get events from remote, using local" }
                            localResult
                        },
                    )
                } else {
                    localResult
                }
            } catch (e: Exception) {
                logger.error(e) { "Error getting events" }
                PaginatedResult(emptyList(), 0, page, limit, false)
            }
        }

    override suspend fun getEventById(id: String): Event? =
        withContext(Dispatchers.Default) {
            try {
                // Сначала проверяем локально
                localDataSource.getEventById(id) ?: run {
                    // Если не найдено локально, пытаемся получить с сервера
                    remoteDataSource?.getEventById(id)?.fold(
                        onSuccess = { event ->
                            // Сохраняем в локальную БД
                            localDataSource.saveEvent(event).getOrNull()
                            event
                        },
                        onError = {
                            logger.warn(it) { "Failed to get event from remote: $id" }
                            null
                        },
                    )
                }
            } catch (e: Exception) {
                logger.error(e) { "Error getting event by id: $id" }
                null
            }
        }

    override suspend fun addEvent(event: Event): Result<Event> =
        withContext(Dispatchers.Default) {
            try {
                val localResult = localDataSource.saveEvent(event)
                if (remoteDataSource != null && localResult.isSuccess) {
                    remoteDataSource.createEvent(event).fold(
                        onSuccess = { remoteEvent ->
                            localDataSource.updateEvent(remoteEvent).getOrNull()
                            Result.success(remoteEvent)
                        },
                        onError = { error ->
                            logger.warn(error) { "Failed to sync event to remote, but saved locally: ${event.id}" }
                            localResult
                        },
                    )
                } else {
                    localResult
                }
            } catch (e: Exception) {
                logger.error(e) { "Error adding event: ${event.id}" }
                Result.failure(e)
            }
        }

    override suspend fun updateEvent(event: Event): Result<Event> =
        withContext(Dispatchers.Default) {
            try {
                // Обновляем локально
                localDataSource.updateEvent(event)
            } catch (e: Exception) {
                logger.error(e) { "Error updating event: ${event.id}" }
                Result.failure(e)
            }
        }

    override suspend fun acknowledgeEvent(
        id: String,
        userId: String,
    ): Result<Event> =
        withContext(Dispatchers.Default) {
            try {
                // Сначала обновляем локально
                val localEvent = localDataSource.getEventById(id)
                if (localEvent != null) {
                    val updatedEvent =
                        localEvent.copy(
                            acknowledged = true,
                            acknowledgedAt = Clock.System.now().toEpochMilliseconds(),
                            acknowledgedBy = userId,
                        )
                    localDataSource.updateEvent(updatedEvent)
                }

                // Если есть удаленный источник, синхронизируем
                if (remoteDataSource != null) {
                    remoteDataSource.acknowledgeEvent(id, userId).fold(
                        onSuccess = { remoteEvent ->
                            // Обновляем локальную версию данными с сервера
                            localDataSource.updateEvent(remoteEvent).getOrNull()
                            Result.success(remoteEvent)
                        },
                        onError = { error ->
                            logger.warn(error) { "Failed to acknowledge event on remote, but updated locally: $id" }
                            localEvent?.let {
                                Result.success(
                                    it.copy(
                                        acknowledged = true,
                                        acknowledgedAt = Clock.System.now().toEpochMilliseconds(),
                                        acknowledgedBy = userId,
                                    ),
                                )
                            }
                                ?: Result.failure(Exception("Event not found"))
                        },
                    )
                } else {
                    localEvent?.let {
                        Result.success(
                            it.copy(
                                acknowledged = true,
                                acknowledgedAt = Clock.System.now().toEpochMilliseconds(),
                                acknowledgedBy = userId,
                            ),
                        )
                    }
                        ?: Result.failure(Exception("Event not found"))
                }
            } catch (e: Exception) {
                logger.error(e) { "Error acknowledging event: $id" }
                Result.failure(e)
            }
        }

    override suspend fun acknowledgeEvents(
        ids: List<String>,
        userId: String,
    ): Result<List<Event>> =
        withContext(
            Dispatchers.Default,
        ) {
            try {
                // Обновляем локально
                val localEvents = ids.mapNotNull { localDataSource.getEventById(it) }
                val updatedEvents =
                    localEvents.map { event ->
                        event.copy(
                            acknowledged = true,
                            acknowledgedAt = Clock.System.now().toEpochMilliseconds(),
                            acknowledgedBy = userId,
                        )
                    }
                updatedEvents.forEach { localDataSource.updateEvent(it).getOrNull() }

                // Если есть удаленный источник, синхронизируем
                if (remoteDataSource != null) {
                    remoteDataSource.acknowledgeEvents(ids, userId).fold(
                        onSuccess = { remoteEvents ->
                            // Обновляем локальные версии данными с сервера
                            remoteEvents.forEach { localDataSource.updateEvent(it).getOrNull() }
                            Result.success(remoteEvents)
                        },
                        onError = { error ->
                            logger.warn(error) { "Failed to acknowledge events on remote, but updated locally" }
                            Result.success(updatedEvents)
                        },
                    )
                } else {
                    Result.success(updatedEvents)
                }
            } catch (e: Exception) {
                logger.error(e) { "Error acknowledging events" }
                Result.failure(e)
            }
        }

    override suspend fun deleteEvent(id: String): Result<Unit> =
        withContext(Dispatchers.Default) {
            try {
                // Удаляем локально
                val localResult = localDataSource.deleteEvent(id)

                // Если есть удаленный источник, синхронизируем
                if (remoteDataSource != null && localResult.isSuccess) {
                    remoteDataSource.deleteEvent(id).fold(
                        onSuccess = {
                            Result.success(Unit)
                        },
                        onError = { error ->
                            logger.warn(error) { "Failed to delete event from remote, but deleted locally: $id" }
                            localResult
                        },
                    )
                } else {
                    localResult
                }
            } catch (e: Exception) {
                logger.error(e) { "Error deleting event: $id" }
                Result.failure(e)
            }
        }

    override suspend fun getEventStatistics(
        cameraId: String?,
        startTime: Long?,
        endTime: Long?,
    ): Result<Map<String, Any>> =
        withContext(Dispatchers.IO) {
            try {
                // Пытаемся получить статистику с remote, но не заваливаем endpoint при недоступности.
                remoteDataSource?.getEventStatistics(cameraId, startTime, endTime)?.fold(
                    onSuccess = { statistics ->
                        Result.success(statistics)
                    },
                    onError = { error ->
                        logger.warn(error) { "Remote statistics unavailable, using local fallback" }
                        Result.success(buildLocalStatistics(cameraId, startTime, endTime))
                    },
                ) ?: Result.success(buildLocalStatistics(cameraId, startTime, endTime))
            } catch (e: Exception) {
                logger.error(e) { "Error getting event statistics" }
                Result.success(buildLocalStatistics(cameraId, startTime, endTime))
            }
        }

    private suspend fun buildLocalStatistics(
        cameraId: String?,
        startTime: Long?,
        endTime: Long?,
    ): Map<String, Any> {
        val events =
            localDataSource.getEvents().asSequence()
                .filter { event -> cameraId == null || event.cameraId == cameraId }
                .filter { event -> startTime == null || event.timestamp >= startTime }
                .filter { event -> endTime == null || event.timestamp <= endTime }
                .toList()

        val byType = events.groupingBy { it.type.name }.eachCount()
        val bySeverity = events.groupingBy { it.severity.name }.eachCount()
        val unacknowledged = events.count { !it.acknowledged }

        return mapOf(
            "total" to events.size,
            "byType" to byType,
            "bySeverity" to bySeverity,
            "unacknowledged" to unacknowledged,
        )
    }
}
