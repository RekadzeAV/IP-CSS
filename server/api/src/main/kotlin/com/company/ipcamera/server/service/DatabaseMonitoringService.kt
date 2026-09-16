package com.company.ipcamera.server.service

import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.sql.Connection
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}

/**
 * Сервис для мониторинга состояния базы данных и connection pool
 */
class DatabaseMonitoringService(
    private val dataSource: DataSource
) {
    /**
     * Получить статистику connection pool
     */
    suspend fun getPoolStats(): PoolStats = withContext(Dispatchers.IO) {
        try {
            if (dataSource is HikariDataSource) {
                val pool = dataSource.hikariPoolMXBean
                PoolStats(
                    activeConnections = pool.activeConnections,
                    idleConnections = pool.idleConnections,
                    totalConnections = pool.totalConnections,
                    threadsAwaitingConnection = pool.threadsAwaitingConnection,
                    maxPoolSize = dataSource.maximumPoolSize,
                    minIdle = dataSource.minimumIdle,
                    connectionTimeout = dataSource.connectionTimeout,
                    idleTimeout = dataSource.idleTimeout,
                    maxLifetime = dataSource.maxLifetime
                )
            } else {
                PoolStats(
                    activeConnections = -1,
                    idleConnections = -1,
                    totalConnections = -1,
                    threadsAwaitingConnection = -1,
                    maxPoolSize = -1,
                    minIdle = -1,
                    connectionTimeout = -1,
                    idleTimeout = -1,
                    maxLifetime = -1
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Error getting pool stats" }
            PoolStats()
        }
    }

    /**
     * Проверить состояние подключения к БД
     */
    suspend fun checkConnection(): ConnectionHealth = withContext(Dispatchers.IO) {
        try {
            val connection = dataSource.connection
            val isValid = connection.isValid(5) // Проверка в течение 5 секунд
            val catalog = connection.catalog
            val autoCommit = connection.autoCommit
            connection.close()

            ConnectionHealth(
                isHealthy = isValid,
                databaseName = catalog,
                autoCommit = autoCommit,
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            logger.error(e) { "Error checking database connection" }
            ConnectionHealth(
                isHealthy = false,
                error = e.message,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    /**
     * Получить информацию о миграциях
     */
    suspend fun getMigrationInfo(): com.company.ipcamera.server.config.MigrationInfo =
        withContext(Dispatchers.IO) {
            try {
                com.company.ipcamera.server.config.DatabaseMigrationConfig.info(dataSource)
            } catch (e: Exception) {
                logger.error(e) { "Error getting migration info" }
                com.company.ipcamera.server.config.MigrationInfo(
                    pending = 0,
                    applied = 0,
                    total = 0,
                    currentVersion = null,
                    isUpToDate = false,
                    error = e.message
                )
            }
        }
}

/**
 * Статистика connection pool
 */
data class PoolStats(
    val activeConnections: Int = 0,
    val idleConnections: Int = 0,
    val totalConnections: Int = 0,
    val threadsAwaitingConnection: Int = 0,
    val maxPoolSize: Int = 0,
    val minIdle: Int = 0,
    val connectionTimeout: Long = 0,
    val idleTimeout: Long = 0,
    val maxLifetime: Long = 0
) {
    val utilizationPercent: Double
        get() = if (maxPoolSize > 0) {
            (activeConnections.toDouble() / maxPoolSize) * 100.0
        } else {
            0.0
        }

    val isHealthy: Boolean
        get() = utilizationPercent < 80.0 && threadsAwaitingConnection == 0
}

/**
 * Состояние подключения к БД
 */
data class ConnectionHealth(
    val isHealthy: Boolean,
    val databaseName: String? = null,
    val autoCommit: Boolean? = null,
    val error: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
