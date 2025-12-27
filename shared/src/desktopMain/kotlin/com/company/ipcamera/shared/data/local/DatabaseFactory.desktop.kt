package com.company.ipcamera.shared.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.company.ipcamera.shared.database.CameraDatabase
import java.io.File

/**
 * Desktop/JVM реализация DatabaseFactory
 *
 * Использует:
 * - PostgreSQL, если установлена переменная окружения DATABASE_URL или POSTGRES_URL (для сервера)
 * - SQLite для Desktop приложений (база данных сохраняется в домашней директории пользователя)
 */
actual class DatabaseFactory(actual context: Any?) {
    actual fun createDriver(): SqlDriver {
        // Проверяем, используется ли PostgreSQL (для сервера)
        val postgresUrl = System.getenv("DATABASE_URL") ?: System.getenv("POSTGRES_URL")

        if (postgresUrl != null) {
            // Используем PostgreSQL через DatabaseConfig (только для сервера)
            // Это требует, чтобы DatabaseConfig был доступен в classpath
            return try {
                val databaseConfigClass = Class.forName("com.company.ipcamera.server.config.DatabaseConfig")
                val createPostgresDriverMethod = databaseConfigClass.getDeclaredMethod("createPostgresDriver")
                createPostgresDriverMethod.invoke(null) as SqlDriver
            } catch (e: Exception) {
                // Если DatabaseConfig недоступен, используем SQLite как fallback
                createSqliteDriver()
            }
        }

        // Используем SQLite для Desktop приложений
        return createSqliteDriver()
    }

    private fun createSqliteDriver(): SqlDriver {
        val databasePath = File(System.getProperty("user.home"), ".ip-css/camera_database.db")
        databasePath.parentFile?.mkdirs()

        val driver = JdbcSqliteDriver("jdbc:sqlite:${databasePath.absolutePath}")
        CameraDatabase.Schema.create(driver)
        return driver
    }
}



