package com.company.ipcamera.shared.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.company.ipcamera.shared.database.CameraDatabase
import java.io.File

/**
 * Desktop реализация DatabaseFactory
 *
 * Использует JdbcSqliteDriver для работы с SQLite на Desktop платформах
 * База данных сохраняется в домашней директории пользователя
 */
actual class DatabaseFactory(actual context: Any?) {
    actual fun createDriver(): SqlDriver {
        val databasePath = File(System.getProperty("user.home"), ".ip-css/camera_database.db")
        databasePath.parentFile?.mkdirs()

        val driver = JdbcSqliteDriver("jdbc:sqlite:${databasePath.absolutePath}")
        CameraDatabase.Schema.create(driver)
        return driver
    }
}


