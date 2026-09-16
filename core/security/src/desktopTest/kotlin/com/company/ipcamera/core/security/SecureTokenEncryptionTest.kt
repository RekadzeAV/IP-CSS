package com.company.ipcamera.core.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Тесты контракта SecureTokenEncryption (JVM/desktop реализация).
 * Фиксируют: round-trip, детерминизм->не совпадение с открытым текстом, isValid.
 */
class SecureTokenEncryptionTest {

    private val encryption = SecureTokenEncryption()

    @Test
    fun `encrypt then decrypt returns original token`() {
        val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyIn0"
        val encrypted = encryption.encrypt(token)
        assertEquals(token, encryption.decrypt(encrypted))
    }

    @Test
    fun `encrypt does not equal plaintext`() {
        val token = "secret-token-12345"
        assertNotEquals(token, encryption.encrypt(token))
    }

    @Test
    fun `isValid returns true for valid encrypted token`() {
        val encrypted = encryption.encrypt("valid")
        assertTrue(encryption.isValid(encrypted))
    }

    @Test
    fun `isValid returns false for garbage`() {
        assertFalse(encryption.isValid("!!!not-base64url!!!="))
    }

    @Test
    fun `empty token round-trips`() {
        val encrypted = encryption.encrypt("")
        assertEquals("", encryption.decrypt(encrypted))
    }

    @Test
    fun `unicode token round-trips`() {
        val token = "токен-🚀-secreta"
        val encrypted = encryption.encrypt(token)
        assertEquals(token, encryption.decrypt(encrypted))
    }
}