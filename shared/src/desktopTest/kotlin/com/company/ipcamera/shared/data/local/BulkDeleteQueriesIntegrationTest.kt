package com.company.ipcamera.shared.data.local

import com.company.ipcamera.shared.test.TestDatabaseFactory
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Интеграционные тесты bulk-delete SQLDelight запросов.
 */
class BulkDeleteQueriesIntegrationTest {
    @Test
    fun `deleteAllCameras removes all camera rows`() =
        runTest {
            val database = TestDatabaseFactory.createTestDatabase()

            database.cameraDatabaseQueries.insertCamera(
                id = "cam-1",
                name = "Camera 1",
                url = "rtsp://127.0.0.1/1",
                username = null,
                password = null,
                model = null,
                status = "ONLINE",
                resolution_width = 1920,
                resolution_height = 1080,
                fps = 25,
                bitrate = 4096,
                codec = "H.264",
                audio = 1,
                ptz_config = null,
                streams = null,
                settings = null,
                statistics = null,
                created_at = 1_700_000_000_000,
                updated_at = 1_700_000_000_000,
                last_seen = null,
            )
            database.cameraDatabaseQueries.insertCamera(
                id = "cam-2",
                name = "Camera 2",
                url = "rtsp://127.0.0.1/2",
                username = null,
                password = null,
                model = null,
                status = "ONLINE",
                resolution_width = 1280,
                resolution_height = 720,
                fps = 25,
                bitrate = 2048,
                codec = "H.264",
                audio = 0,
                ptz_config = null,
                streams = null,
                settings = null,
                statistics = null,
                created_at = 1_700_000_000_001,
                updated_at = 1_700_000_000_001,
                last_seen = null,
            )

            assertEquals(
                2,
                database.cameraDatabaseQueries.selectAll().executeAsList().size,
            )
            database.cameraDatabaseQueries.deleteAllCameras()
            assertEquals(
                0,
                database.cameraDatabaseQueries.selectAll().executeAsList().size,
            )
        }

    @Test
    fun `deleteAllEvents removes all event rows`() =
        runTest {
            val database = TestDatabaseFactory.createTestDatabase()

            database.cameraDatabaseQueries.insertEvent(
                id = "evt-1",
                camera_id = "cam-1",
                camera_name = "Camera 1",
                type = "MOTION",
                severity = "WARNING",
                timestamp = 1_700_000_000_000,
                description = "motion 1",
                metadata = null,
                acknowledged = 0,
                acknowledged_at = null,
                acknowledged_by = null,
                thumbnail_url = null,
                video_url = null,
            )
            database.cameraDatabaseQueries.insertEvent(
                id = "evt-2",
                camera_id = "cam-2",
                camera_name = "Camera 2",
                type = "OFFLINE",
                severity = "CRITICAL",
                timestamp = 1_700_000_000_100,
                description = "offline",
                metadata = null,
                acknowledged = 0,
                acknowledged_at = null,
                acknowledged_by = null,
                thumbnail_url = null,
                video_url = null,
            )

            assertEquals(
                2,
                database.cameraDatabaseQueries.selectAllEvents().executeAsList().size,
            )
            database.cameraDatabaseQueries.deleteAllEvents()
            assertEquals(
                0,
                database.cameraDatabaseQueries.selectAllEvents().executeAsList().size,
            )
        }

    @Test
    fun `deleteAllUsers removes all user rows`() =
        runTest {
            val database = TestDatabaseFactory.createTestDatabase()

            database.cameraDatabaseQueries.insertUser(
                id = "usr-1",
                username = "admin",
                email = "admin@example.com",
                full_name = "Admin User",
                role = "ADMIN",
                permissions = "[]",
                created_at = 1_700_000_000_000,
                last_login_at = null,
                is_active = 1,
            )
            database.cameraDatabaseQueries.insertUser(
                id = "usr-2",
                username = "operator",
                email = "operator@example.com",
                full_name = "Operator User",
                role = "OPERATOR",
                permissions = "[]",
                created_at = 1_700_000_000_050,
                last_login_at = null,
                is_active = 1,
            )

            assertEquals(
                2,
                database.cameraDatabaseQueries.selectAllUsers().executeAsList().size,
            )
            database.cameraDatabaseQueries.deleteAllUsers()
            assertEquals(
                0,
                database.cameraDatabaseQueries.selectAllUsers().executeAsList().size,
            )
        }

    @Test
    fun `deleteAllNotifications removes all notification rows`() =
        runTest {
            val database = TestDatabaseFactory.createTestDatabase()

            database.cameraDatabaseQueries.insertNotification(
                id = "ntf-1",
                title = "Alert 1",
                message = "message 1",
                type = "EVENT",
                priority = "HIGH",
                camera_id = "cam-1",
                event_id = "evt-1",
                recording_id = null,
                channel_id = null,
                icon = null,
                sound = 1,
                vibration = 1,
                read = 0,
                read_at = null,
                timestamp = 1_700_000_000_000,
                extras = null,
            )
            database.cameraDatabaseQueries.insertNotification(
                id = "ntf-2",
                title = "Alert 2",
                message = "message 2",
                type = "SYSTEM",
                priority = "NORMAL",
                camera_id = null,
                event_id = null,
                recording_id = null,
                channel_id = null,
                icon = null,
                sound = 1,
                vibration = 0,
                read = 0,
                read_at = null,
                timestamp = 1_700_000_000_010,
                extras = null,
            )

            assertEquals(
                2,
                database.cameraDatabaseQueries.selectAllNotifications().executeAsList().size,
            )
            database.cameraDatabaseQueries.deleteAllNotifications()
            assertEquals(
                0,
                database.cameraDatabaseQueries.selectAllNotifications().executeAsList().size,
            )
        }

    @Test
    fun `deleteAll queries are idempotent on empty tables`() =
        runTest {
            val database = TestDatabaseFactory.createTestDatabase()

            // First pass on already-empty tables should be safe.
            database.cameraDatabaseQueries.deleteAllCameras()
            database.cameraDatabaseQueries.deleteAllEvents()
            database.cameraDatabaseQueries.deleteAllUsers()
            database.cameraDatabaseQueries.deleteAllNotifications()

            // Second pass confirms idempotency.
            database.cameraDatabaseQueries.deleteAllCameras()
            database.cameraDatabaseQueries.deleteAllEvents()
            database.cameraDatabaseQueries.deleteAllUsers()
            database.cameraDatabaseQueries.deleteAllNotifications()

            assertEquals(
                0,
                database.cameraDatabaseQueries.selectAll().executeAsList().size,
            )
            assertEquals(
                0,
                database.cameraDatabaseQueries.selectAllEvents().executeAsList().size,
            )
            assertEquals(
                0,
                database.cameraDatabaseQueries.selectAllUsers().executeAsList().size,
            )
            assertEquals(
                0,
                database.cameraDatabaseQueries.selectAllNotifications().executeAsList().size,
            )
        }
}
