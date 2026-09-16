package com.company.ipcamera.server.config

import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}

/**
 * Конфигурация миграций базы данных через Flyway
 *
 * Flyway управляет версионированием схемы БД и автоматически применяет миграции при запуске
 */
object DatabaseMigrationConfig {

    /**
     * Применить миграции к базе данных
     *
     * @param dataSource DataSource для подключения к БД
     * @param migrationLocation Путь к файлам миграций (по умолчанию: db/migration)
     * @return Результат миграции
     */
    fun migrate(
        dataSource: DataSource,
        migrationLocation: String = "db/migration"
    ): Result<Int> {
        return try {
            logger.info { "Starting database migrations from: $migrationLocation" }

            val flyway: FluentConfiguration = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:$migrationLocation")
                .baselineOnMigrate(true) // Создать baseline, если таблица flyway_schema_history не существует
                .validateOnMigrate(true) // Проверять целостность миграций
                .cleanDisabled(true) // Отключить clean для безопасности

            val flywayInstance = flyway.load()
            val result = flywayInstance.migrate()
            val count: Int = result.migrationsExecuted

            logger.info { "Database migrations completed. Applied: $count migrations" }
            Result.success(count)
        } catch (e: Exception) {
            logger.error(e) { "Error applying database migrations" }
            Result.failure(e)
        }
    }

    /**
     * Проверить состояние миграций
     *
     * @param dataSource DataSource для подключения к БД
     * @return Информация о миграциях
     */
    fun info(dataSource: DataSource): MigrationInfo {
        return try {
            val flyway = Flyway.configure()
                .dataSource(dataSource)
                .load()

            val info = flyway.info()
            val pending = info.pending().size
            val applied = info.applied().size

            MigrationInfo(
                pending = pending,
                applied = applied,
                total = pending + applied,
                currentVersion = info.current()?.version?.toString(),
                isUpToDate = pending == 0
            )
        } catch (e: Exception) {
            logger.error(e) { "Error getting migration info" }
            MigrationInfo(
                pending = 0,
                applied = 0,
                total = 0,
                currentVersion = null,
                isUpToDate = false,
                error = e.message
            )
        }
    }

    /**
     * Валидация миграций (проверка целостности)
     *
     * @param dataSource DataSource для подключения к БД
     * @return Результат валидации
     */
    fun validate(dataSource: DataSource): Result<Unit> {
        return try {
            val flyway = Flyway.configure()
                .dataSource(dataSource)
                .load()

            flyway.validate()
            logger.info { "Database migrations validation passed" }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Database migrations validation failed" }
            Result.failure(e)
        }
    }
}

/**
 * Информация о миграциях
 */
data class MigrationInfo(
    val pending: Int,
    val applied: Int,
    val total: Int,
    val currentVersion: String?,
    val isUpToDate: Boolean,
    val error: String? = null
)
