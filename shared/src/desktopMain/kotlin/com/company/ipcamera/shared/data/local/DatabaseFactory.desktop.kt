package com.company.ipcamera.shared.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

/**
 * Desktop/JVM реализация DatabaseFactory
 *
 * Использует:
 * - PostgreSQL, если установлена переменная окружения DATABASE_URL или POSTGRES_URL (для сервера)
 * - SQLite для Desktop приложений (база данных сохраняется в домашней директории пользователя)
 */
actual class DatabaseFactory actual constructor(context: Any?) {
    actual fun createDriver(): SqlDriver {
        cachedDriver?.let { return it }

        val dbMode = System.getenv("DB_MODE")?.trim()?.lowercase()
        val explicitPostgresMode = dbMode == "postgres"

        // Проверяем, используется ли PostgreSQL (для сервера)
        val postgresUrl = System.getenv("DATABASE_URL") ?: System.getenv("POSTGRES_URL")

        if (postgresUrl != null || explicitPostgresMode) {
            // Используем PostgreSQL через DatabaseConfig (только для сервера)
            // Это требует, чтобы DatabaseConfig был доступен в classpath
            return try {
                val databaseConfigClass = Class.forName("com.company.ipcamera.server.config.DatabaseConfig")
                val validateRequirementsMethod =
                    databaseConfigClass.getDeclaredMethod("validateDatabaseRequirementsForServerStartup")
                val createPostgresDriverMethod = databaseConfigClass.getDeclaredMethod("createPostgresDriver")
                // DatabaseConfig — Kotlin object, поэтому нужно вызывать метод на INSTANCE.
                val instanceField = databaseConfigClass.getField("INSTANCE")
                val instance = instanceField.get(null)
                validateRequirementsMethod.invoke(instance)
                val postgresDriver = createPostgresDriverMethod.invoke(instance) as SqlDriver
                cachedDriver = postgresDriver
                postgresDriver
            } catch (e: ClassNotFoundException) {
                if (explicitPostgresMode) {
                    throw IllegalStateException(
                        "DB_MODE=postgres requires server DatabaseConfig in classpath",
                        e,
                    )
                }
                // Если серверный модуль недоступен (desktop client), используем SQLite.
                createSqliteDriver()
            } catch (e: NoSuchFieldException) {
                if (explicitPostgresMode) {
                    throw IllegalStateException(
                        "DB_MODE=postgres requires valid server DatabaseConfig singleton",
                        e,
                    )
                }
                // Если не удалось получить INSTANCE у DatabaseConfig, используем SQLite.
                createSqliteDriver()
            } catch (e: NoSuchMethodException) {
                if (explicitPostgresMode) {
                    throw IllegalStateException(
                        "DB_MODE=postgres requires compatible server DatabaseConfig methods",
                        e,
                    )
                }
                // Если сигнатура server-конфига не совпала, используем SQLite.
                createSqliteDriver()
            } catch (e: Exception) {
                // Если серверный конфиг найден, но PostgreSQL не поднялся — не маскируем проблему fallback'ом.
                throw IllegalStateException("Failed to initialize PostgreSQL driver from server DatabaseConfig", e)
            }
        }

        // Используем SQLite для Desktop приложений
        return createSqliteDriver()
    }

    private fun createSqliteDriver(): SqlDriver {
        val databasePath = File(System.getProperty("user.home"), ".ip-css/camera_database.db")
        databasePath.parentFile?.mkdirs()

        // Создаем драйвер без инициализации схемы
        // Схема будет создана/обновлена через createDatabase() с миграциями
        val sqliteDriver = JdbcSqliteDriver("jdbc:sqlite:${databasePath.absolutePath},")
        cachedDriver = sqliteDriver
        return sqliteDriver
    }

    companion object {
        @Volatile
        private var cachedDriver: SqlDriver? = null
    }
}
