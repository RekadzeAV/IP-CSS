package com.company.ipcamera.shared.data.local

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.db.SqlDriver
import com.company.ipcamera.shared.database.CameraDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Менеджер миграций базы данных
 *
 * Управляет процессом миграции БД:
 * - Определяет текущую и целевую версию
 * - Применяет необходимые миграции
 * - Валидирует результат миграции
 */
object MigrationManager {
    /**
     * Текущая версия схемы БД (версия 4 добавляет face_gallery для face recognition)
     */
    private const val CURRENT_SCHEMA_VERSION = 4

    /**
     * Получает текущую версию БД
     *
     * @param database Экземпляр базы данных
     * @return Текущая версия БД, 1 если БД существует без schema_version, или null если БД новая
     */
    suspend fun getCurrentVersion(database: CameraDatabase): Int? {
        return withContext(Dispatchers.Default) {
            try {
                // Пытаемся получить версию из schema_version (SQLDelight: GetCurrentSchemaVersion(MAX))
                val versionRow = database.cameraDatabaseQueries.getCurrentSchemaVersion().executeAsOneOrNull()
                val maxVersion = versionRow?.MAX
                if (maxVersion != null) {
                    return@withContext maxVersion.toInt()
                }

                // Если schema_version пуста, проверяем наличие таблицы camera
                // Если camera существует, значит это версия 1 (старая БД без schema_version)
                try {
                    database.cameraDatabaseQueries.selectAll().executeAsList()
                    logger.debug { "Database exists without schema_version table, assuming version 1" }
                    return@withContext 1
                } catch (e: Exception) {
                    logger.debug(e) { "Database does not exist, will create new" }
                    return@withContext null
                }
            } catch (e: Exception) {
                // Если таблица schema_version не существует, проверяем наличие camera
                try {
                    database.cameraDatabaseQueries.selectAll().executeAsList()
                    logger.debug { "Database exists without schema_version table, assuming version 1" }
                    return@withContext 1
                } catch (e2: Exception) {
                    logger.debug(e) { "Could not get current schema version, assuming new database" }
                    return@withContext null
                }
            }
        }
    }

    /**
     * Получает целевую версию схемы
     *
     * @return Целевая версия схемы
     */
    fun getTargetVersion(): Int {
        return CURRENT_SCHEMA_VERSION
    }

    /**
     * Применяет миграции от текущей версии к целевой
     *
     * @param driver Драйвер базы данных
     * @param fromVersion Текущая версия (null для новой БД)
     * @param toVersion Целевая версия
     */
    suspend fun applyMigrations(
        driver: SqlDriver,
        fromVersion: Int?,
        toVersion: Int,
    ) {
        withContext(Dispatchers.Default) {
            val startTime = Clock.System.now().toEpochMilliseconds()

            try {
                MigrationLogger.logMigrationStart(fromVersion ?: 0, toVersion)

                if (fromVersion == null) {
                    // Новая БД - создаем схему
                    MigrationLogger.logDatabaseCreation(toVersion)
                    CameraDatabase.Schema.create(driver).await()

                    // Инициализируем версию схемы
                    val database = CameraDatabase(driver)
                    initializeSchemaVersion(database, toVersion)
                } else if (fromVersion < toVersion) {
                    // Применяем миграции через SQLDelight
                    MigrationLogger.logApplyingMigration(
                        migrationNumber = fromVersion + 1,
                        description = "Migration from version $fromVersion to $toVersion",
                    )

                    CameraDatabase.Schema.migrate(
                        driver = driver,
                        oldVersion = (fromVersion ?: 0).toLong(),
                        newVersion = toVersion.toLong(),
                    ).await()

                    // SQLDelight .sqm уже вставляют строки в schema_version (2, 3, 4 …); повторный INSERT
                    // с тем же PRIMARY KEY(version) падает. Дописываем версию только если её ещё нет.
                    val database = CameraDatabase(driver)
                    val recordedVersion = getCurrentVersion(database)
                    if (recordedVersion == null || recordedVersion < toVersion) {
                        updateSchemaVersion(database, toVersion, "Migration from version $fromVersion to $toVersion")
                    } else {
                        logger.info {
                            "Schema version already at $recordedVersion after SQLDelight migrate; skipping duplicate schema_version row"
                        }
                    }
                } else if (fromVersion > toVersion) {
                    throw IllegalStateException(
                        "Database version ($fromVersion) is higher than target version ($toVersion). " +
                            "Downgrade is not supported.",
                    )
                } else {
                    // Версии совпадают, миграция не требуется
                    MigrationLogger.logWarning("Database is already at target version $toVersion")
                }

                val duration = Clock.System.now().toEpochMilliseconds() - startTime
                MigrationLogger.logMigrationComplete(fromVersion ?: 0, toVersion, duration)
            } catch (e: Exception) {
                MigrationLogger.logMigrationError(fromVersion ?: 0, toVersion, e)
                throw e
            }
        }
    }

    /**
     * Инициализирует версию схемы для новой БД
     */
    private suspend fun initializeSchemaVersion(
        database: CameraDatabase,
        version: Int,
    ) {
        withContext(Dispatchers.Default) {
            try {
                database.cameraDatabaseQueries.insertSchemaVersion(
                    version = version.toLong(),
                    applied_at = Clock.System.now().toEpochMilliseconds() / 1000,
                    description = "Initial schema version",
                )
                logger.info { "Initialized schema version $version" }
            } catch (e: Exception) {
                logger.error(e) { "Failed to initialize schema version" }
                throw e
            }
        }
    }

    /**
     * Обновляет версию схемы после миграции
     */
    private suspend fun updateSchemaVersion(
        database: CameraDatabase,
        version: Int,
        description: String,
    ) {
        withContext(Dispatchers.Default) {
            try {
                val versionRow = database.cameraDatabaseQueries.getCurrentSchemaVersion().executeAsOneOrNull()
                val current = versionRow?.MAX?.toInt() ?: 0
                if (current >= version) {
                    logger.info {
                        "Schema version already at $current (target $version); skipping duplicate insert " +
                            "(migration .sqm may have recorded the version)"
                    }
                    return@withContext
                }
                database.cameraDatabaseQueries.insertSchemaVersion(
                    version = version.toLong(),
                    applied_at = Clock.System.now().toEpochMilliseconds() / 1000,
                    description = description,
                )
                logger.info { "Updated schema version to $version" }
            } catch (e: Exception) {
                logger.error(e) { "Failed to update schema version" }
                throw e
            }
        }
    }

    /**
     * Валидирует миграцию после её применения
     *
     * @param database Экземпляр базы данных
     * @param expectedVersion Ожидаемая версия после миграции
     * @return true, если валидация прошла успешно
     */
    suspend fun validateMigration(
        database: CameraDatabase,
        expectedVersion: Int,
    ): Boolean {
        return MigrationValidator.validateMigration(database, expectedVersion)
    }
}
