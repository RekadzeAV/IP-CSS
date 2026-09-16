package com.company.ipcamera.shared.data.local

import com.company.ipcamera.core.common.security.PasswordEncryptionFactory
import com.company.ipcamera.shared.database.CameraDatabase
import kotlinx.datetime.Clock
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Миграция legacy-учётных данных камер:
 * перевод plaintext паролей в зашифрованный формат ENC:...
 */
object CameraCredentialMigration {
    suspend fun migratePlaintextPasswords(
        database: CameraDatabase,
        encryption: com.company.ipcamera.core.common.security.PasswordEncryption = PasswordEncryptionFactory.create(),
    ): Int {
        val now = Clock.System.now().toEpochMilliseconds()
        var migratedCount = 0

        val legacyRows =
            database.cameraDatabaseQueries
                .selectCamerasWithPlaintextPassword()
                .executeAsList()

        if (legacyRows.isEmpty()) {
            return 0
        }

        database.cameraDatabaseQueries.transaction {
            legacyRows.forEach { row ->
                val cameraId = row.id
                val plaintextPassword = row.password

                if (plaintextPassword.isNullOrBlank()) {
                    return@forEach
                }

                try {
                    val encrypted = encryption.encrypt(plaintextPassword)
                    database.cameraDatabaseQueries.updateCameraPasswordById(
                        password = encrypted,
                        updated_at = now,
                        id = cameraId,
                    )
                    migratedCount++
                } catch (e: Exception) {
                    logger.error(e) { "Failed to migrate camera password for camera $cameraId" }
                    throw IllegalStateException(
                        "Failed to migrate legacy plaintext password for camera '$cameraId'.",
                        e,
                    )
                }
            }
        }

        return migratedCount
    }
}
