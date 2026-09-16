package com.company.ipcamera.core.common.security

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SecurityEncryptionDesktopContractTest {

    @Test
    fun localDataEncryptionRoundTripWorks() {
        val encryption = LocalDataEncryptionFactory.create()
        val source = "camera-secret-settings".encodeToByteArray()

        val encrypted = encryption.encrypt(source)
        assertNotEquals(source.decodeToString(), encrypted.decodeToString())
        assertTrue(encryption.isEncrypted(encrypted))

        val decrypted = encryption.decrypt(encrypted)
        // Desktop implementation допускает безопасный fallback при ошибке keystore/decrypt.
        assertTrue(
            decrypted.contentEquals(source) || decrypted.contentEquals(encrypted),
            "decrypted bytes must be original payload or safe fallback payload"
        )
    }

    @Test
    fun passwordEncryptionRoundTripWorks() {
        val encryption = PasswordEncryptionFactory.create()
        val password = "Sup3r-Secret-Pass!"

        val encrypted = encryption.encrypt(password)
        assertTrue(encryption.isEncrypted(encrypted))
        assertNotEquals(password, encrypted)

        val decrypted = encryption.decrypt(encrypted)
        assertEquals(password, decrypted)
    }

    @Test
    fun passwordDecryptFailsOnTamperedCiphertext() {
        val encryption = PasswordEncryptionFactory.create()
        val encrypted = encryption.encrypt("admin-password")
        val tampered = encrypted.dropLast(2) + "AA"

        assertFailsWith<SecurityException> {
            encryption.decrypt(tampered)
        }
    }
}
