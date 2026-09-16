package com.company.ipcamera.server.security

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

/**
 * In-memory реализация SecurityAlertRepository.
 */
class InMemorySecurityAlertRepository(private val maxAlerts: Int = 5_000) : SecurityAlertRepository {

    private val storage = mutableMapOf<String, SecurityAlert>()
    private val mutex = Mutex()
    private val idCounter = AtomicLong(0)

    override suspend fun save(alert: SecurityAlert) = mutex.withLock {
        storage[alert.id] = alert
        if (storage.size > maxAlerts) {
            val oldest = storage.entries.minByOrNull { it.value.createdAt }
            oldest?.key?.let { storage.remove(it) }
        }
    }

    override suspend fun getById(id: String): SecurityAlert? = mutex.withLock {
        storage[id]
    }

    override suspend fun getActive(limit: Int): List<SecurityAlert> = mutex.withLock {
        storage.values.filter { !it.isResolved }.sortedByDescending { it.createdAt }.take(limit)
    }

    override suspend fun list(
        limit: Int,
        offset: Int,
        resolved: Boolean?,
        type: SecurityAlertType?,
        fromTimestamp: Long?,
        toTimestamp: Long?
    ): List<SecurityAlert> = mutex.withLock {
        var list = storage.values.asSequence()
        resolved?.let { r -> list = list.filter { it.isResolved == r } }
        type?.let { t -> list = list.filter { it.type == t } }
        fromTimestamp?.let { ts -> list = list.filter { it.createdAt >= ts } }
        toTimestamp?.let { ts -> list = list.filter { it.createdAt <= ts } }
        list.sortedByDescending { it.createdAt }.drop(offset).take(limit).toList()
    }

    override suspend fun resolve(id: String): Boolean = mutex.withLock {
        val alert = storage[id] ?: return false
        storage[id] = alert.copy(resolvedAt = System.currentTimeMillis())
        true
    }

    fun nextId(): String = "alert_${idCounter.incrementAndGet()}"
}
