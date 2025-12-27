package com.company.ipcamera.shared.data.local

import app.cash.sqldelight.db.SqlDriver
import com.company.ipcamera.shared.database.CameraDatabase
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
 * Создает экземпляр базы данных с применением миграций
 *
 * SQLDelight автоматически применяет миграции из папки migrations
 * при вызове CameraDatabase.Schema.create() или CameraDatabase.Schema.migrate()
 *
 * Миграции должны быть в shared/src/commonMain/sqldelight/migrations/
 * и названы как 1.sqm, 2.sqm, и т.д.
 *
 * SQLDelight автоматически определяет текущую версию схемы и применяет все необходимые миграции.
 */
fun createDatabase(driver: SqlDriver): CameraDatabase {
    try {
        // SQLDelight автоматически применяет миграции при создании/обновлении схемы
        // Если база данных существует, будут применены только новые миграции
        // Если база данных не существует, будет создана новая схема
        CameraDatabase.Schema.create(driver)
        logger.info { "Database schema created/verified successfully" }
    } catch (e: Exception) {
        logger.error(e) { "Failed to create database schema" }
        // Пробрасываем ошибку дальше, так как без схемы работа невозможна
        throw e
    }
    return CameraDatabase(driver)
}
