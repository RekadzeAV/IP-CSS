package com.company.ipcamera.server.security

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.sql.DataSource

/**
 * In-memory реализация SecurityAlertRepository.
 * TODO: Заменить на PostgreSQL когда модель SecurityAlert будет синхронизирована.
 */
class PostgresSecurityAlertRepository(
    private val dataSource: DataSource
) : SecurityAlertRepository {

    private val storage = mutableMapOf<String, SecurityAlert>()
    private val mutex = Mutex()

    override suspend fun save(alert: SecurityAlert) = withContext(Dispatchers.IO) {
        mutex.withLock { storage[alert.id] = alert }
    }

    override suspend fun getById(id: String): SecurityAlert? = withContext(Dispatchers.IO) {
        mutex.withLock { storage[id] }
    }

    override suspend fun getActive(limit: Int): List<SecurityAlert> = withContext(Dispatchers.IO) {
        mutex.withLock { storage.values.filter { !it.isResolved }.take(limit) }
    }

    override suspend fun list(
        limit: Int, offset: Int, resolved: Boolean?, type: SecurityAlertType?,
        fromTimestamp: Long?, toTimestamp: Long?
    ): List<SecurityAlert> = withContext(Dispatchers.IO) {
        mutex.withLock {
            storage.values
                .filter { resolved == null || it.isResolved == resolved }
                .filter { type == null || it.type == type }
                .filter { fromTimestamp == null || it.createdAt >= fromTimestamp }
                .filter { toTimestamp == null || it.createdAt <= toTimestamp }
                .drop(offset)
                .take(limit)
        }
    }

    override suspend fun resolve(id: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val alert = storage[id] ?: return@withLock false
            storage[id] = alert.copy(resolvedAt = System.currentTimeMillis())
            true
        }
    }
}