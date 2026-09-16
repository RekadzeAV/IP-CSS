package com.company.ipcamera.shared.data.local

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Логгер для миграций базы данных
 *
 * Обеспечивает детальное логирование процесса миграции:
 * - Начало/окончание миграции
 * - Примененные изменения
 * - Ошибки и предупреждения
 */
object MigrationLogger {
    /**
     * Логирует начало миграции
     */
    fun logMigrationStart(
        fromVersion: Int,
        toVersion: Int,
    ) {
        logger.info { "Starting database migration from version $fromVersion to $toVersion" }
    }

    /**
     * Логирует окончание миграции
     */
    fun logMigrationComplete(
        fromVersion: Int,
        toVersion: Int,
        durationMs: Long,
    ) {
        logger.info { "Database migration completed successfully from version $fromVersion to $toVersion in ${durationMs}ms" }
    }

    /**
     * Логирует применение конкретной миграции
     */
    fun logApplyingMigration(
        migrationNumber: Int,
        description: String?,
    ) {
        val desc = description?.let { " ($it)" } ?: ""
        logger.info { "Applying migration $migrationNumber$desc" }
    }

    /**
     * Логирует ошибку миграции
     */
    fun logMigrationError(
        fromVersion: Int,
        toVersion: Int,
        error: Throwable,
    ) {
        logger.error(error) { "Failed to migrate database from version $fromVersion to $toVersion" }
    }

    /**
     * Логирует предупреждение
     */
    fun logWarning(message: String) {
        logger.warn { "Migration warning: $message" }
    }

    /**
     * Логирует информацию о версии БД
     */
    fun logDatabaseVersion(
        currentVersion: Int?,
        targetVersion: Int,
    ) {
        when (currentVersion) {
            null -> logger.info { "Database is new, will be created with version $targetVersion" }
            else -> logger.info { "Current database version: $currentVersion, target version: $targetVersion" }
        }
    }

    /**
     * Логирует создание новой БД
     */
    fun logDatabaseCreation(targetVersion: Int) {
        logger.info { "Creating new database with schema version $targetVersion" }
    }

    /**
     * Логирует валидацию миграции
     */
    fun logValidationStart() {
        logger.debug { "Starting migration validation" }
    }

    /**
     * Логирует результат валидации
     */
    fun logValidationResult(
        success: Boolean,
        message: String,
    ) {
        if (success) {
            logger.info { "Migration validation passed: $message" }
        } else {
            logger.error { "Migration validation failed: $message" }
        }
    }

    /**
     * Логирует итог миграции plaintext паролей камер
     */
    fun logCameraCredentialMigrationResult(migratedCount: Int) {
        if (migratedCount > 0) {
            logger.warn {
                "Camera credential migration completed: migrated $migratedCount plaintext password(s) to encrypted format"
            }
        } else {
            logger.debug { "Camera credential migration: no plaintext passwords found" }
        }
    }
}
