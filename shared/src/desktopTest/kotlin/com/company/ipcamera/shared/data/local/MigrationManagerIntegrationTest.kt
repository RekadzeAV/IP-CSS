package com.company.ipcamera.shared.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.company.ipcamera.shared.database.CameraDatabase
import com.company.ipcamera.shared.test.TestDatabaseFactory
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Интеграционные проверки миграций SQLDelight.
 *
 * Включает тесты для:
 * - Новая БД достигает целевой версии
 * - Повторное открытие уже мигрированной БД идемпотентно
 * - Миграция отклоняет downgrade
 * - Определение версии legacy БД (только таблица camera, без schema_version)
 */
class MigrationManagerIntegrationTest {
    @Test
    fun `fresh database reaches target schema version and validates`() =
        runTest {
            val database = TestDatabaseFactory.createTestDatabase()
            val target = MigrationManager.getTargetVersion()
            val current = database.cameraDatabaseQueries.getCurrentSchemaVersion().executeAsOneOrNull()
            assertNotNull(current)
            assertEquals(target, current!!.MAX!!.toInt())
            assertTrue(MigrationManager.validateMigration(database, target))
        }

    @Test
    fun `legacy v1 database with only camera table is detected as version 1`() =
        runTest {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            driver.execute(
                null,
                """
                CREATE TABLE camera (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    url TEXT NOT NULL,
                    username TEXT,
                    password TEXT,
                    model TEXT,
                    status TEXT NOT NULL,
                    resolution_width INTEGER,
                    resolution_height INTEGER,
                    fps INTEGER NOT NULL DEFAULT 25,
                    bitrate INTEGER NOT NULL DEFAULT 4096,
                    codec TEXT NOT NULL DEFAULT 'H.264',
                    audio INTEGER NOT NULL DEFAULT 0,
                    ptz_config TEXT,
                    streams TEXT,
                    settings TEXT,
                    statistics TEXT,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    last_seen INTEGER
                );
                """.trimIndent(),
                0,
                null,
            )

            // Создаём временную БД для проверки определения версии
            val tempDatabase = CameraDatabase(driver)
            val detectedVersion = MigrationManager.getCurrentVersion(tempDatabase)

            // Ожидаем версию 1 (таблица camera существует, schema_version отсутствует)
            assertNotNull(detectedVersion, "Legacy v1 database should be detected as version 1")
            assertEquals(1, detectedVersion, "Legacy database with only camera table should be version 1")
        }

    @Test
    fun `reopening already migrated database is idempotent`() =
        runTest {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            val target = MigrationManager.getTargetVersion()

            val firstOpen = createDatabaseSync(driver)
            val firstVersion = firstOpen.cameraDatabaseQueries.getCurrentSchemaVersion().executeAsOneOrNull()
            assertNotNull(firstVersion)
            assertEquals(target, firstVersion!!.MAX!!.toInt())

            val secondOpen = createDatabaseSync(driver)
            val secondVersion = secondOpen.cameraDatabaseQueries.getCurrentSchemaVersion().executeAsOneOrNull()
            assertNotNull(secondVersion)
            assertEquals(target, secondVersion!!.MAX!!.toInt())
            assertTrue(MigrationManager.validateMigration(secondOpen, target))

            val history = secondOpen.cameraDatabaseQueries.getSchemaVersionHistory().executeAsList()
            assertEquals(1, history.count { it.version.toInt() == target })
        }

    @Test
    fun `applyMigrations rejects downgrade attempts`() =
        runTest {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            val target = MigrationManager.getTargetVersion()

            val error =
                assertFailsWith<IllegalStateException> {
                    MigrationManager.applyMigrations(
                        driver = driver,
                        fromVersion = target + 1,
                        toVersion = target,
                    )
                }
            assertTrue(error.message?.contains("Downgrade is not supported.") == true)
        }
}