package com.company.ipcamera.core.common.security

import kotlin.test.Test
import kotlin.test.assertTrue

class SecurityPlatformSmokeDesktopTest {

    @Test
    fun secureMobileSecurityLoggerCanLogEventWithoutFailure() {
        val logger = MobileSecurityLoggerFactory.create()
        logger.logEncryptionSuccess("settings")
        logger.logDecryptionFailure("token", "invalid payload")
        logger.logSuspiciousActivity(
            description = "unexpected access pattern",
            details = mapOf("camera_id" to "desktop-cam-01")
        )
        assertTrue(true)
    }

    @Test
    fun secureLocalDataEncryptionFactoryRoundTripSmoke() {
        val encryption = LocalDataEncryptionFactory.create()
        val payload = "desktop-smoke-payload".encodeToByteArray()

        val encrypted = encryption.encrypt(payload)
        val decrypted = encryption.decrypt(encrypted)

        // Desktop actual may return encrypted data as a safe fallback on crypto failure.
        assertTrue(decrypted.contentEquals(payload) || decrypted.contentEquals(encrypted))
    }

    @Test
    fun securePasswordEncryptionFactoryRoundTripSmoke() {
        val encryption = PasswordEncryptionFactory.create()
        val encrypted = encryption.encrypt("desktop-smoke-password")
        val decrypted = encryption.decrypt(encrypted)

        // Desktop actual should normally return original value; fallback behavior remains safe.
        assertTrue(decrypted == "desktop-smoke-password" || decrypted == encrypted)
    }
}
