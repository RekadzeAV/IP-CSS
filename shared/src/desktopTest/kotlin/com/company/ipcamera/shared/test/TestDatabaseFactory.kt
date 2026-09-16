package com.company.ipcamera.shared.test

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.company.ipcamera.shared.data.local.createDatabaseSync
import com.company.ipcamera.shared.database.CameraDatabase

/**
 * Тестовая фабрика для создания in-memory базы данных
 * Использует JdbcSqliteDriver для тестов
 */
object TestDatabaseFactory {
    /**
     * Создает in-memory SQLite драйвер для тестов.
     * Схему не создаём здесь: [createDatabaseSync] применяет [CameraDatabase] + миграции и версию схемы.
     */
    fun createDriver(): SqlDriver {
        return JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    }

    /**
     * Создает тестовую базу данных
     */
    fun createTestDatabase(): CameraDatabase {
        return createDatabaseSync(createDriver())
    }
}
