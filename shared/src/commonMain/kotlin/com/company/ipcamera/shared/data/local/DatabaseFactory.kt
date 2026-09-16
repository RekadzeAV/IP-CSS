package com.company.ipcamera.shared.data.local

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.db.SqlDriver
import com.company.ipcamera.shared.database.CameraDatabase
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Фабрика для создания экземпляра базы данных
 *
 * Для Android требуется Context в конструкторе, для iOS - null
 */
expect class DatabaseFactory(context: Any?) {
    fun createDriver(): SqlDriver
}

/**
 * Создает экземпляр базы данных с применением миграций.
 *
 * Единая точка входа для всех платформ (Android, iOS, Desktop, JVM/Server): все используют
 * createDatabase/createDatabaseSync из shared — миграции 1→2, 2→3 и т.д. применяются здесь.
 *
 * Использует MigrationManager для:
 * - Определения текущей версии БД
 * - Применения необходимых миграций (в т.ч. 1→2)
 * - Валидации результата: после applyMigrations всегда вызывается MigrationManager.validateMigration
 *
 * Миграции в shared/.../migrations/ (1.sqm, 2.sqm, …).
 */
suspend fun createDatabase(driver: SqlDriver): CameraDatabase {
    try {
        // Создаем временный экземпляр БД для проверки версии
        val tempDatabase = CameraDatabase(driver)

        // Проверяем, существует ли БД
        val databaseExists = MigrationValidator.databaseExists(driver)

        // Получаем текущую и целевую версию
        val currentVersion =
            if (databaseExists) {
                MigrationManager.getCurrentVersion(tempDatabase)
            } else {
                null
            }
        val targetVersion = MigrationManager.getTargetVersion()

        MigrationLogger.logDatabaseVersion(currentVersion, targetVersion)

        // PostgreSQL + Flyway: схема от server `db/migration` уже создана. [isPostgresFlywayParityDriver] — JVM-only
        // (JdbcDriver vs JdbcSqliteDriver), тесты на in-memory SQLite не в parity.
        val useParity =
            databaseExists &&
                isPostgresFlywayParityDriver(driver) &&
                (
                    PostgresFlywaySchemaSync.shouldApplyPostgresFlywayParitySync() ||
                        PostgresFlywaySchemaSync.isPostgresWithFlywayHistory(driver)
                )
        if (useParity) {
            logger.info {
                "PostgreSQL (Flyway parity): applying SQLDelight parity DDL instead of generated .sqm migrate()"
            }
            PostgresFlywaySchemaSync.ensureSqlDelightParityAfterFlyway(driver, targetVersion)
        } else {
            MigrationManager.applyMigrations(
                driver = driver,
                fromVersion = currentVersion,
                toVersion = targetVersion,
            )
        }

        // Создаем финальный экземпляр БД
        val database = CameraDatabase(driver)

        // migration-safe шаг: перешифровываем legacy plaintext пароли камер (опционально, не блокирует запуск)
        runCatching {
            val migratedCameraCredentials = CameraCredentialMigration.migratePlaintextPasswords(database)
            MigrationLogger.logCameraCredentialMigrationResult(migratedCameraCredentials)
        }.onFailure { e ->
            logger.warn { "Camera credential migration skipped (legacy passwords not found or error): ${e.message}" }
        }

        // Валидируем миграцию
        val validationResult = MigrationManager.validateMigration(database, targetVersion)
        if (!validationResult) {
            logger.warn { "Migration validation failed, but continuing..." }
        }

        logger.info { "Database initialized successfully with version $targetVersion" }
        return database
    } catch (e: Exception) {
        logger.error(e) { "Failed to create/initialize database" }
        val message = e.message.orEmpty()
        if (message.contains("no such table", ignoreCase = true)) {
            logger.warn { "Attempting force schema bootstrap after missing-table error" }
            return forceBootstrapDatabase(driver)
        }
        // Пробрасываем ошибку дальше, так как без схемы работа невозможна
        throw e
    }
}

private suspend fun forceBootstrapDatabase(driver: SqlDriver): CameraDatabase {
    return try {
        // Fallback path for partially initialized local databases.
        CameraDatabase.Schema.create(driver).await()
        val database = CameraDatabase(driver)
        val targetVersion = MigrationManager.getTargetVersion()
        runCatching {
            database.cameraDatabaseQueries.insertSchemaVersion(
                version = targetVersion.toLong(),
                applied_at = Clock.System.now().toEpochMilliseconds() / 1000,
                description = "Force bootstrap schema version",
            )
        }
        logger.info { "Force schema bootstrap completed (version=$targetVersion)" }
        database
    } catch (bootstrapError: Exception) {
        logger.error(bootstrapError) { "Force schema bootstrap failed" }
        throw bootstrapError
    }
}

/**
 * Синхронная обертка для createDatabase
 *
 * Используется в Koin модулях, где suspend функции не поддерживаются напрямую.
 * Инициализация БД выполняется один раз при старте приложения, поэтому блокирующий вызов допустим.
 *
 * @param driver Драйвер базы данных
 * @return Экземпляр базы данных
 */
fun createDatabaseSync(driver: SqlDriver): CameraDatabase {
    return runBlocking {
        createDatabase(driver)
    }
}
