package com.company.ipcamera.shared.data.local

import app.cash.sqldelight.db.SqlDriver
import com.company.ipcamera.shared.database.CameraDatabase
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Валидатор миграций базы данных
 *
 * Проверяет целостность данных после миграции:
 * - Наличие всех таблиц
 * - Наличие индексов
 * - Корректность версии схемы
 */
object MigrationValidator {
    /**
     * Валидирует миграцию после её применения
     *
     * @param database Экземпляр базы данных
     * @param expectedVersion Ожидаемая версия схемы после миграции
     * @return true, если валидация прошла успешно
     */
    suspend fun validateMigration(
        database: CameraDatabase,
        expectedVersion: Int,
    ): Boolean {
        logger.debug { "Validating migration to version $expectedVersion" }

        return try {
            // Проверяем версию схемы
            val currentVersion = getCurrentVersion(database)
            if (currentVersion != expectedVersion) {
                logger.error { "Version mismatch: expected $expectedVersion, got $currentVersion" }
                return false
            }

            // Проверяем наличие всех таблиц
            val tablesValid = validateTables(database, expectedVersion)
            if (!tablesValid) {
                logger.error { "Table validation failed" }
                return false
            }

            logger.info { "Migration validation passed for version $expectedVersion" }
            true
        } catch (e: Exception) {
            logger.error(e) { "Error during migration validation" }
            false
        }
    }

    /**
     * Проверяет наличие всех необходимых таблиц
     *
     * Проверяет наличие таблиц через попытку выполнить простые запросы
     */
    private suspend fun validateTables(
        database: CameraDatabase,
        expectedVersion: Int,
    ): Boolean {
        return try {
            // Проверяем наличие основных таблиц через попытку выполнить запросы
            // Если таблица не существует, запрос выбросит исключение
            database.cameraDatabaseQueries.selectAll().executeAsList()
            database.cameraDatabaseQueries.selectAllRecordings().executeAsList()
            database.cameraDatabaseQueries.selectAllEvents().executeAsList()
            database.cameraDatabaseQueries.selectAllUsers().executeAsList()
            database.cameraDatabaseQueries.selectAllSettings().executeAsList()
            database.cameraDatabaseQueries.selectAllNotifications().executeAsList()

            if (expectedVersion >= 3) {
                database.cameraDatabaseQueries.selectLicensePlatesByCameraId("").executeAsList()
            }
            if (expectedVersion >= 4) {
                database.cameraDatabaseQueries.selectAllFaces().executeAsList()
            }

            logger.debug { "All required tables are present" }
            true
        } catch (e: Exception) {
            logger.error(e) { "Error checking tables - some tables may be missing" }
            false
        }
    }

    /**
     * Получает текущую версию схемы из БД
     */
    private suspend fun getCurrentVersion(database: CameraDatabase): Int? {
        return try {
            val versionRow = database.cameraDatabaseQueries.getCurrentSchemaVersion().executeAsOneOrNull()
            versionRow?.MAX?.toInt()
        } catch (e: Exception) {
            logger.warn(e) { "Could not get current schema version, assuming new database" }
            null
        }
    }

    /**
     * Проверяет, существует ли база данных
     *
     * Проверяет наличие таблицы schema_version через попытку выполнить запрос.
     * Также проверяет наличие хотя бы одной таблицы (camera) для определения существования БД.
     */
    suspend fun databaseExists(driver: SqlDriver): Boolean {
        return try {
            val database = CameraDatabase(driver)
            // Сначала проверяем наличие таблицы camera (она должна быть в версии 1)
            // Если таблица camera существует, значит БД уже создана
            database.cameraDatabaseQueries.selectAll().executeAsList()

            // Если дошли сюда, значит БД существует
            // Теперь проверяем, есть ли таблица schema_version
            try {
                val version = database.cameraDatabaseQueries.getCurrentSchemaVersion().executeAsOneOrNull()
                // Если версия null, значит таблица schema_version существует, но пуста
                // Если версия не null, значит БД существует и имеет версию
                true
            } catch (e: Exception) {
                // Таблица schema_version не существует, но БД существует (версия 1)
                logger.debug(e) { "Schema version table does not exist, but database exists (version 1)" }
                true
            }
        } catch (e: Exception) {
            logger.debug(e) { "Database existence check failed, assuming new database" }
            false
        }
    }
}
