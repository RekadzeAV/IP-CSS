package com.company.ipcamera.shared.data.local

import com.company.ipcamera.shared.TestPasswordEncryption
import com.company.ipcamera.shared.test.TestDatabaseFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CameraCredentialMigrationTest {
    @Test
    fun `migratePlaintextPasswords migrates only legacy plaintext rows`() {
        val db = TestDatabaseFactory.createTestDatabase()
        val now = System.currentTimeMillis()
        val testEncryption = TestPasswordEncryption()

        val migratedCount =
            kotlinx.coroutines.runBlocking {
                db.cameraDatabaseQueries.insertCamera(
                    id = "cam-plain",
                    name = "Plain",
                    url = "rtsp://127.0.0.1/plain",
                    username = "admin",
                    password = "plain-password",
                    model = null,
                    status = "ONLINE",
                    resolution_width = null,
                    resolution_height = null,
                    fps = 25,
                    bitrate = 4096,
                    codec = "H.264",
                    audio = 1,
                    ptz_config = null,
                    streams = null,
                    settings = "{}",
                    statistics = null,
                    created_at = now,
                    updated_at = now,
                    last_seen = null,
                )

                db.cameraDatabaseQueries.insertCamera(
                    id = "cam-encrypted",
                    name = "Encrypted",
                    url = "rtsp://127.0.0.1/encrypted",
                    username = "admin",
                    password = "ENC:already-encrypted",
                    model = null,
                    status = "ONLINE",
                    resolution_width = null,
                    resolution_height = null,
                    fps = 25,
                    bitrate = 4096,
                    codec = "H.264",
                    audio = 1,
                    ptz_config = null,
                    streams = null,
                    settings = "{}",
                    statistics = null,
                    created_at = now,
                    updated_at = now,
                    last_seen = null,
                )

                CameraCredentialMigration.migratePlaintextPasswords(db, testEncryption)
            }
        assertEquals(1, migratedCount)

        val plainRow = db.cameraDatabaseQueries.selectById("cam-plain").executeAsOne()
        val encryptedRow = db.cameraDatabaseQueries.selectById("cam-encrypted").executeAsOne()

        assertNotNull(plainRow.password)
        assertTrue(plainRow.password!!.startsWith("ENC:"))
        assertTrue(plainRow.password != "plain-password")
        assertEquals("ENC:already-encrypted", encryptedRow.password)
    }
}
