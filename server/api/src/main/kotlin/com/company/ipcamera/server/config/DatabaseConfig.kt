package com.company.ipcamera.server.config

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.asJdbcDriver
import com.company.ipcamera.shared.database.CameraDatabase
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging
import java.sql.Connection
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}

/**
 * Конфигурация базы данных PostgreSQL для сервера
 *
 * Использует HikariCP для connection pooling
 */
object DatabaseConfig {

    private var dataSource: HikariDataSource? = null

    /**
     * Создает SqlDriver для PostgreSQL с использованием HikariCP connection pool
     */
    fun createPostgresDriver(): SqlDriver {
        val jdbcUrl = System.getenv("DATABASE_URL")
            ?: System.getenv("POSTGRES_URL")
            ?: "jdbc:postgresql://localhost:5432/ipcss"

        val username = System.getenv("DATABASE_USER") ?: "postgres"
        val password = System.getenv("DATABASE_PASSWORD") ?: "postgres"
        val maxPoolSize = System.getenv("DATABASE_MAX_POOL_SIZE")?.toIntOrNull() ?: 10

        logger.info { "Connecting to PostgreSQL database: ${jdbcUrl.replace(Regex("://.*@"), "://***@")}" }

        val config = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.username = username
            this.password = password
            driverClassName = "org.postgresql.Driver"

            // Connection pool settings
            maximumPoolSize = maxPoolSize
            minimumIdle = 2
            connectionTimeout = 30000 // 30 seconds
            idleTimeout = 600000 // 10 minutes
            maxLifetime = 1800000 // 30 minutes
            leakDetectionThreshold = 60000 // 1 minute

            // PostgreSQL specific settings
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            addDataSourceProperty("useServerPrepStmts", "true")
            addDataSourceProperty("useLocalSessionState", "true")
            addDataSourceProperty("rewriteBatchedStatements", "true")
            addDataSourceProperty("cacheResultSetMetadata", "true")
            addDataSourceProperty("cacheServerConfiguration", "true")
            addDataSourceProperty("elideSetAutoCommits", "true")
            addDataSourceProperty("maintainTimeStats", "false")
        }

        dataSource = HikariDataSource(config)
        val driver = dataSource!!.asJdbcDriver()

        // Создаем схему базы данных, если она не существует
        try {
            CameraDatabase.Schema.create(driver)
            logger.info { "Database schema created/verified successfully" }
        } catch (e: Exception) {
            logger.warn(e) { "Schema creation failed, assuming it already exists" }
        }

        return driver
    }

    /**
     * Закрывает connection pool
     */
    fun closeDataSource() {
        try {
            dataSource?.close()
            dataSource = null
            logger.info { "Database connection pool closed" }
        } catch (e: Exception) {
            logger.error(e) { "Error closing database connection pool" }
        }
    }
}

