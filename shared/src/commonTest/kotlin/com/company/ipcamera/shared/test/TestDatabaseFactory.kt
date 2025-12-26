package com.company.ipcamera.shared.test

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.company.ipcamera.shared.data.local.createDatabase
import com.company.ipcamera.shared.database.CameraDatabase

/**
 * Тестовая фабрика для создания in-memory базы данных
 * Использует JdbcSqliteDriver для тестов
 */
object TestDatabaseFactory {
    /**
     * Создает in-memory SQLite драйвер для тестов
     */
    fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CameraDatabase.Schema.create(driver)
        return driver
    }
    
    /**
     * Создает тестовую базу данных
     */
    fun createTestDatabase(): CameraDatabase {
        return createDatabase(createDriver())
    }
}

