package com.company.ipcamera.shared.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.test.runTest
import java.security.NoSuchProviderException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Дополнительные integration-проверки безопасной миграции:
 * - сохранность legacy данных;
 * - auto-migration plaintext camera passwords during DB bootstrap.
 */
class MigrationDataSafetyIntegrationTest {
    @Test
    fun `legacy v2 rows are preserved after migration to target`() =
        runTest {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            seedLegacyV2Schema(driver)

            driver.execute(
                null,
                """
                INSERT INTO camera(
                    id, name, url, username, password, model, status,
                    resolution_width, resolution_height, fps, bitrate, codec, audio,
                    ptz_config, streams, settings, statistics, created_at, updated_at, last_seen
                ) VALUES (
                    'cam-legacy', 'Legacy camera', 'rtsp://legacy/stream', 'admin', 'legacy-pass', 'legacy-model', 'ONLINE',
                    NULL, NULL, 25, 4096, 'H.264', 1,
                    NULL, NULL, '{}', NULL, 1111, 1111, NULL
                );
                """.trimIndent(),
                0,
                null,
            )
            driver.execute(
                null,
                """
                INSERT INTO recording(
                    id, camera_id, camera_name, start_time, end_time, duration,
                    file_path, file_size, codec, format, quality, status, thumbnail_url, created_at
                ) VALUES (
                    'rec-legacy', 'cam-legacy', 'Legacy camera', 2000, 5000, 3000,
                    '/var/lib/legacy.mp4', 1024, 'H.264', 'MP4', 'HIGH', 'COMPLETED', NULL, 2000
                );
                """.trimIndent(),
                0,
                null,
            )

            val database = createDatabaseOrSkipUnsupportedCrypto(driver) ?: return@runTest
            val target = MigrationManager.getTargetVersion()

            val version = database.cameraDatabaseQueries.getCurrentSchemaVersion().executeAsOneOrNull()
            assertNotNull(version)
            val current = version.MAX?.toInt() ?: fail("schema_version MAX is null after migration")
            assertEquals(target, current)

            val recording = database.cameraDatabaseQueries.selectRecordingById("rec-legacy").executeAsOneOrNull()
            assertNotNull(recording)
            assertEquals("cam-legacy", recording.camera_id)
            assertEquals("/var/lib/legacy.mp4", recording.file_path)
            assertEquals("H.264", recording.codec)
        }

    @Test
    fun `createDatabase migrates plaintext camera password to encrypted format`() =
        runTest {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            seedLegacyV2Schema(driver)
            driver.execute(
                null,
                """
                INSERT INTO camera(
                    id, name, url, username, password, model, status,
                    resolution_width, resolution_height, fps, bitrate, codec, audio,
                    ptz_config, streams, settings, statistics, created_at, updated_at, last_seen
                ) VALUES (
                    'cam-plain', 'Plain camera', 'rtsp://plain/stream', 'admin', 'plain-secret', NULL, 'ONLINE',
                    NULL, NULL, 25, 4096, 'H.264', 1,
                    NULL, NULL, '{}', NULL, 3000, 3000, NULL
                );
                """.trimIndent(),
                0,
                null,
            )

            val database = createDatabaseOrSkipUnsupportedCrypto(driver) ?: return@runTest
            val migratedRow = database.cameraDatabaseQueries.selectById("cam-plain").executeAsOneOrNull()
            assertNotNull(migratedRow)
            assertNotNull(migratedRow.password)
            assertTrue(migratedRow.password!!.startsWith("ENC:"))
            assertTrue(migratedRow.password != "plain-secret")
        }

    private fun seedLegacyV2Schema(driver: JdbcSqliteDriver) {
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
        driver.execute(
            null,
            """
            CREATE TABLE recording (
                id TEXT NOT NULL PRIMARY KEY,
                camera_id TEXT NOT NULL,
                camera_name TEXT,
                start_time INTEGER NOT NULL,
                end_time INTEGER,
                duration INTEGER NOT NULL,
                file_path TEXT,
                file_size INTEGER,
                codec TEXT,
                format TEXT NOT NULL DEFAULT 'MP4',
                quality TEXT NOT NULL DEFAULT 'HIGH',
                status TEXT NOT NULL DEFAULT 'ACTIVE',
                thumbnail_url TEXT,
                created_at INTEGER NOT NULL
            );
            """.trimIndent(),
            0,
            null,
        )
        driver.execute(
            null,
            "CREATE TABLE event (id TEXT NOT NULL PRIMARY KEY, camera_id TEXT NOT NULL, camera_name TEXT, type TEXT NOT NULL, severity TEXT NOT NULL DEFAULT 'INFO', timestamp INTEGER NOT NULL, description TEXT, metadata TEXT, acknowledged INTEGER NOT NULL DEFAULT 0, acknowledged_at INTEGER, acknowledged_by TEXT, thumbnail_url TEXT, video_url TEXT);",
            0,
            null,
        )
        driver.execute(
            null,
            "CREATE TABLE user (id TEXT NOT NULL PRIMARY KEY, username TEXT NOT NULL UNIQUE, email TEXT, full_name TEXT, role TEXT NOT NULL, permissions TEXT, created_at INTEGER NOT NULL, last_login_at INTEGER, is_active INTEGER NOT NULL DEFAULT 1);",
            0,
            null,
        )
        driver.execute(
            null,
            "CREATE TABLE setting (id TEXT NOT NULL PRIMARY KEY, category TEXT NOT NULL, key TEXT NOT NULL, value TEXT NOT NULL, type TEXT NOT NULL DEFAULT 'STRING', description TEXT, updated_at INTEGER NOT NULL, UNIQUE(category, key));",
            0,
            null,
        )
        driver.execute(
            null,
            "CREATE TABLE notification (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, message TEXT NOT NULL, type TEXT NOT NULL, priority TEXT NOT NULL DEFAULT 'NORMAL', camera_id TEXT, event_id TEXT, recording_id TEXT, channel_id TEXT, icon TEXT, sound INTEGER NOT NULL DEFAULT 1, vibration INTEGER NOT NULL DEFAULT 0, read INTEGER NOT NULL DEFAULT 0, read_at INTEGER, timestamp INTEGER NOT NULL, extras TEXT);",
            0,
            null,
        )
        driver.execute(
            null,
            """
            CREATE TABLE schema_version (
                version INTEGER NOT NULL PRIMARY KEY,
                applied_at INTEGER NOT NULL,
                description TEXT
            );
            """.trimIndent(),
            0,
            null,
        )
        driver.execute(
            null,
            "INSERT INTO schema_version(version, applied_at, description) VALUES (2, 0, 'legacy v2 baseline');",
            0,
            null,
        )
    }

    private suspend fun createDatabaseOrSkipUnsupportedCrypto(
        driver: JdbcSqliteDriver,
    ): com.company.ipcamera.shared.database.CameraDatabase? {
        return try {
            createDatabase(driver)
        } catch (e: IllegalStateException) {
            if (hasNoSuchProviderCause(e)) {
                // Local JVM unit runtime may not provide AndroidKeyStore provider.
                return null
            }
            throw e
        } catch (e: SecurityException) {
            if (hasNoSuchProviderCause(e)) {
                // Local JVM unit runtime may not provide AndroidKeyStore provider.
                return null
            }
            throw e
        }
    }

    private fun hasNoSuchProviderCause(error: Throwable): Boolean {
        var cursor: Throwable? = error
        while (cursor != null) {
            if (cursor is NoSuchProviderException) {
                return true
            }
            cursor = cursor.cause
        }
        return false
    }
}
