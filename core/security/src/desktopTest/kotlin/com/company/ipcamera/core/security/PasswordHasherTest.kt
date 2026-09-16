package com.company.ipcamera.core.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PasswordHasherTest {

    private val hasher = SecurePasswordHasher()

    @Test
    fun testHashPassword() {
        val password = "MySecurePassword123!".toCharArray()
        val hashed = hasher.hash(password)

        assertTrue(hasher.isValidHash(hashed), "Hash should be valid format")
        assertTrue(hashed.startsWith("\$pbkdf2-sha256\$"), "Hash should start with correct prefix")
    }

    @Test
    fun testVerifyCorrectPassword() {
        val password = "CorrectPassword123".toCharArray()
        val hashed = hasher.hash(password)

        assertTrue(hasher.verify(password, hashed), "Correct password should verify")
    }

    @Test
    fun testVerifyIncorrectPassword() {
        val password = "CorrectPassword123".toCharArray()
        val wrongPassword = "WrongPassword456".toCharArray()
        val hashed = hasher.hash(password)

        assertFalse(hasher.verify(wrongPassword, hashed), "Wrong password should not verify")
    }

    @Test
    fun testGetAlgorithm() {
        val algorithm = hasher.getAlgorithm()
        assertEquals("PBKDF2WithHmacSHA256", algorithm)
    }

    @Test
    fun testDifferentHashesForSamePassword() {
        val password = "SamePassword".toCharArray()
        val hash1 = hasher.hash(password)
        val hash2 = hasher.hash(password)

        assertNotEquals(hash1, hash2, "Different hashes should be generated for same password (due to salt)")
    }

    @Test
    fun testValidHashFormat() {
        val validHash = "\$pbkdf2-sha256\$100000\$YWJjZGVmZ2hpams=\$bG1ub3BxcnN0dXZ3eHl6"
        assertTrue(hasher.isValidHash(validHash))
    }

    @Test
    fun testInvalidHashFormat() {
        val invalidHash = "not-a-valid-hash"
        assertFalse(hasher.isValidHash(invalidHash))
    }

    @Test
    fun testEmptyPassword() {
        val emptyPassword = "".toCharArray()
        val hashed = hasher.hash(emptyPassword)

        assertTrue(hasher.isValidHash(hashed))
        assertTrue(hasher.verify(emptyPassword, hashed))
    }

    @Test
    fun testSpecialCharactersInPassword() {
        val specialPassword = "P@ssw0rd!#\$%^&*()".toCharArray()
        val hashed = hasher.hash(specialPassword)

        assertTrue(hasher.verify(specialPassword, hashed))
    }

    @Test
    fun testUnicodePassword() {
        val unicodePassword = "пароль🔒密码".toCharArray()
        val hashed = hasher.hash(unicodePassword)

        assertTrue(hasher.verify(unicodePassword, hashed))
    }

    @Test
    fun testLongPassword() {
        val longPassword = "a".repeat(1000)
        val hashed = hasher.hash(longPassword.toCharArray())

        assertTrue(hasher.verify(longPassword.toCharArray(), hashed))
    }
}
