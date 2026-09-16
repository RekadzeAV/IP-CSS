package com.company.ipcamera.server.config

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.asJdbcDriver
import com.company.ipcamera.shared.database.CameraDatabase
import kotlinx.coroutines.runBlocking
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
    /** 4.2.3 Read replica pool (опционально, если задан DATABASE_READ_REPLICA_URL). */
    private var readReplicaDataSource: HikariDataSource? = null

    /**
     * Определяет production-окружение для серверного процесса.
     */
    fun isProductionEnvironment(): Boolean {
        val environment = System.getenv("ENVIRONMENT")?.trim()?.lowercase()
        val nodeEnv = System.getenv("NODE_ENV")?.trim()?.lowercase()
        return environment == "production" || nodeEnv == "production"
    }

    /**
     * Явный режим работы БД для server-процесса.
     * Поддерживаемые значения: "postgres", "embedded".
     */
    fun isPostgresModeEnabled(): Boolean {
        val dbMode = System.getenv("DB_MODE")?.trim()?.lowercase()
        return dbMode == "postgres"
    }

    /**
     * Проверяет, что PostgreSQL URL явно задан.
     */
    fun isPostgresConfigured(): Boolean {
        val jdbcUrl = System.getenv("DATABASE_URL")?.takeIf { it.isNotBlank() }
            ?: System.getenv("POSTGRES_URL")?.takeIf { it.isNotBlank() }
        return jdbcUrl != null
    }

    /**
     * Fail-fast preflight для production: сервер не должен запускаться без PostgreSQL конфигурации.
     */
    fun validateDatabaseRequirementsForServerStartup() {
        if (!isProductionEnvironment() && !isPostgresModeEnabled()) return

        val jdbcUrl = System.getenv("DATABASE_URL")?.takeIf { it.isNotBlank() }
            ?: System.getenv("POSTGRES_URL")?.takeIf { it.isNotBlank() }
        val username = System.getenv("DATABASE_USER")?.takeIf { it.isNotBlank() }
        val password = System.getenv("DATABASE_PASSWORD")?.takeIf { it.isNotBlank() }
            ?: System.getenv("DB_PASSWORD")?.takeIf { it.isNotBlank() }

        require(jdbcUrl != null) {
            "Production startup blocked: DATABASE_URL (or POSTGRES_URL) is required."
        }
        require(username != null) {
            "Production startup blocked: DATABASE_USER is required."
        }
        require(password != null) {
            "Production startup blocked: DATABASE_PASSWORD (or DB_PASSWORD) is required."
        }
    }

    /**
     * Создает SqlDriver для PostgreSQL с использованием HikariCP connection pool
     */
    fun createPostgresDriver(): SqlDriver {
        validateDatabaseRequirementsForServerStartup()

        val jdbcUrl = System.getenv("DATABASE_URL") ?: System.getenv("POSTGRES_URL") ?: "jdbc:postgresql://localhost:5432/ipcss"
        val username = System.getenv("DATABASE_USER") ?: "postgres"
        val password = System.getenv("DATABASE_PASSWORD") ?: System.getenv("DB_PASSWORD") ?: "postgres"
        
        val driver = createPrimaryDataSource(jdbcUrl, username, password)
        
        createReadReplicaDataSource(username, password)
        applyDatabaseMigrations(driver)

        return driver
    }

    private fun createPrimaryDataSource(jdbcUrl: String, username: String, password: String): SqlDriver {
        logger.info { "Connecting to PostgreSQL database: ${jdbcUrl.replace(Regex("://.*@"), "://***@")}" }
        
        val maxPoolSize = System.getenv("DATABASE_MAX_POOL_SIZE")?.toIntOrNull() ?: 10
        val config = buildHikariConfig(jdbcUrl, username, password, maxPoolSize)
        
        dataSource = HikariDataSource(config)
        return dataSource!!.asJdbcDriver()
    }

    private fun buildHikariConfig(jdbcUrl: String, username: String, password: String, maxPoolSize: Int): HikariConfig {
        return HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.username = username
            this.password = password
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = maxPoolSize
            minimumIdle = maxOf(2, maxPoolSize / 4)
            connectionTimeout = 30000
            idleTimeout = 600000
            maxLifetime = 1800000
            leakDetectionThreshold = 60000
            validationTimeout = 5000
            keepaliveTime = 300000
            // Включаем JMX метрики и имя пула для мониторинга
            setRegisterMbeans(true)
            poolName = "IP-CSS-Primary-Pool"
            addPostgresDataSourceProperties()
        }
    }

    private fun HikariConfig.addPostgresDataSourceProperties() {
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

    private fun createReadReplicaDataSource(username: String, password: String) {
        val readReplicaUrl = System.getenv("DATABASE_READ_REPLICA_URL")?.takeIf { it.isNotBlank() } ?: return
        
        val replicaPoolSize = System.getenv("DATABASE_READ_REPLICA_POOL_SIZE")?.toIntOrNull() ?: 5
        val replicaConfig = HikariConfig().apply {
            this.jdbcUrl = readReplicaUrl
            this.username = username
            this.password = password
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = replicaPoolSize
            minimumIdle = maxOf(1, replicaPoolSize / 2)
            connectionTimeout = 10000
            idleTimeout = 600000
            maxLifetime = 1800000
            addDataSourceProperty("readOnly", "true")
        }
        
        readReplicaDataSource = HikariDataSource(replicaConfig)
        logger.info { "Read replica pool created (pool size=$replicaPoolSize)" }
    }

    private fun applyDatabaseMigrations(driver: SqlDriver) {
        val enableFlyway = System.getenv("ENABLE_FLYWAY")?.toBoolean() ?: true
        
        if (enableFlyway) {
            runFlywayMigrations(driver)
        } else {
            createSchemaViaSqlDelight(driver)
        }
    }

    private fun runFlywayMigrations(driver: SqlDriver) {
        try {
            val migrationsApplied = DatabaseMigrationConfig.migrate(dataSource!!)
            migrationsApplied.fold(
                onSuccess = { count -> logger.info { "Database migrations applied successfully. Migrations: $count" } },
                onFailure = { error ->
                    logger.warn(error) { "Database migrations failed, falling back to schema creation" }
                    createSchemaViaSqlDelight(driver)
                }
            )
        } catch (e: Exception) {
            logger.warn(e) { "Flyway migration failed, using SQLDelight schema creation" }
            createSchemaViaSqlDelight(driver)
        }
    }

    private fun createSchemaViaSqlDelight(driver: SqlDriver) {
        try {
            runBlocking { CameraDatabase.Schema.create(driver).await() }
            logger.info { "Database schema created/verified successfully" }
        } catch (e: Exception) {
            logger.warn(e) { "Schema creation failed, assuming it already exists" }
        }
    }

    /**
     * Получить DataSource (для мониторинга)
     */
    fun getDataSource(): HikariDataSource? = dataSource

    /**
     * 4.2.3 Получить пул read replica (только чтение). null, если реплика не настроена.
     */
    fun getReadReplicaDataSource(): HikariDataSource? = readReplicaDataSource

    /**
     * Закрывает connection pool (primary и read replica).
     */
    fun closeDataSource() {
        try {
            readReplicaDataSource?.close()
            readReplicaDataSource = null
            dataSource?.close()
            dataSource = null
            logger.info { "Database connection pool closed" }
        } catch (e: Exception) {
            logger.error(e) { "Error closing database connection pool" }
        }
    }
}

