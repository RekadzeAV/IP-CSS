package com.company.ipcamera.server.service

import mu.KotlinLogging
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}

/**
 * Мониторинг производительности PostgreSQL-пула соединений (HikariCP).
 */
class DatabasePerformanceService(
    private val dataSource: DataSource,
    private val slowQueryThresholdMs: Long = 1000L
) {
    private val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
        val t = Thread(r, "db-performance-service")
        t.isDaemon = true
        t
    }

    fun start() {
        logger.info { "Database performance monitoring started" }
    }

    fun stop() {
        scheduler.shutdown()
    }

    /** Текущая статистика пула соединений HikariCP. */
    fun getPoolStats(): Map<String, Any> {
        val hikari = dataSource as? com.zaxxer.hikari.HikariDataSource ?: return emptyMap()
        return mapOf(
            "activeConnections" to hikari.hikariPoolMXBean.activeConnections,
            "idleConnections" to hikari.hikariPoolMXBean.idleConnections,
            "totalConnections" to hikari.hikariPoolMXBean.totalConnections,
            "threadsAwaitingConnection" to hikari.hikariPoolMXBean.threadsAwaitingConnection,
            "connectionTimeoutMs" to hikari.connectionTimeout,
            "maxPoolSize" to hikari.maximumPoolSize,
            "minIdle" to hikari.minimumIdle,
        )
    }

    fun checkHealth(): Map<String, Any> {
        return try {
            val connection = dataSource.connection
            try {
                val valid = connection.isValid(2)
                mapOf(
                    "healthy" to valid,
                    "database" to connection.metaData.databaseProductName,
                    "version" to connection.metaData.databaseProductVersion,
                )
            } finally {
                connection.close()
            }
        } catch (e: Exception) {
            logger.error(e) { "Database health check failed" }
            mapOf("healthy" to false, "error" to (e.message ?: "Health check failed"))
        }
    }
}