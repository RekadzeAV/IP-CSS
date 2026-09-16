package com.company.ipcamera.server.security

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * In-memory реализация AuditLogRepository (4.3.3).
 * Для продакшена заменить на запись в БД (таблица audit_log).
 */
class InMemoryAuditLogRepository(private val maxEvents: Int = 10_000) : AuditLogRepository {

    private val events = mutableListOf<SecurityEvent>()
    private val mutex = Mutex()

    override suspend fun append(event: SecurityEvent) = mutex.withLock {
        events.add(event)
        if (events.size > maxEvents) {
            events.removeAt(0)
        }
    }

    override suspend fun getRecent(
        limit: Int,
        offset: Int,
        type: SecurityEventType?,
        userId: String?,
        fromTimestamp: Long?,
        toTimestamp: Long?
    ): List<SecurityEvent> = mutex.withLock {
        var list: List<SecurityEvent> = events.asReversed()
        type?.let { list = list.filter { it.type == type } }
        userId?.let { list = list.filter { it.userId == userId } }
        fromTimestamp?.let { t -> list = list.filter { it.timestamp >= t } }
        toTimestamp?.let { t -> list = list.filter { it.timestamp <= t } }
        list.drop(offset).take(limit)
    }

    override suspend fun verifyIntegrityChain(): Boolean = true
}
