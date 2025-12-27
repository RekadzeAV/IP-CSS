package com.company.ipcamera.server.service

import kotlin.test.*

/**
 * Тесты для PasswordService
 */
class PasswordServiceTest {

    @Test
    fun `test hash password`() {
        // Act
        val hashed = PasswordService.hashPassword("testPassword123")

        // Assert
        assertNotNull(hashed)
        assertTrue(hashed.isNotEmpty())
        assertTrue(PasswordService.isHashed(hashed))
        assertNotEquals("testPassword123", hashed)
    }

    @Test
    fun `test verify password correct`() {
        // Arrange
        val password = "testPassword123"
        val hashed = PasswordService.hashPassword(password)

        // Act
        val isValid = PasswordService.verifyPassword(password, hashed)

        // Assert
        assertTrue(isValid)
    }

    @Test
    fun `test verify password incorrect`() {
        // Arrange
        val password = "testPassword123"
        val wrongPassword = "wrongPassword"
        val hashed = PasswordService.hashPassword(password)

        // Act
        val isValid = PasswordService.verifyPassword(wrongPassword, hashed)

        // Assert
        assertFalse(isValid)
    }

    @Test
    fun `test same password produces different hashes`() {
        // Arrange
        val password = "testPassword123"

        // Act
        val hash1 = PasswordService.hashPassword(password)
        val hash2 = PasswordService.hashPassword(password)

        // Assert
        assertNotEquals(hash1, hash2) // BCrypt includes salt, so same password = different hash
    }

    @Test
    fun `test isHashed recognizes BCrypt hash`() {
        // Arrange
        val hashed = PasswordService.hashPassword("test")

        // Act & Assert
        assertTrue(PasswordService.isHashed(hashed))
    }

    @Test
    fun `test isHashed returns false for plain text`() {
        // Act & Assert
        assertFalse(PasswordService.isHashed("plaintext"))
        assertFalse(PasswordService.isHashed("password123"))
    }

    @Test
    fun `test verify password with different BCrypt variants`() {
        // Arrange
        val password = "testPassword123"
        val hash2a = PasswordService.hashPassword(password) // Should produce $2a$ or $2b$

        // Act
        val isValid = PasswordService.verifyPassword(password, hash2a)

        // Assert
        assertTrue(isValid)
        assertTrue(hash2a.startsWith("$2a$") || hash2a.startsWith("$2b$") || hash2a.startsWith("$2y$"))
    }

    @Test
    fun `test verify password with invalid hash format`() {
        // Arrange
        val invalidHash = "invalid_hash_format"

        // Act & Assert - should return false without throwing exception
        val result = PasswordService.verifyPassword("password", invalidHash)
        assertFalse(result)
    }

    @Test
    fun `test hash password with empty string`() {
        // Act
        val hashed = PasswordService.hashPassword("")

        // Assert
        assertNotNull(hashed)
        assertTrue(hashed.isNotEmpty())
        assertTrue(PasswordService.isHashed(hashed))
    }

    @Test
    fun `test hash password with special characters`() {
        // Arrange
        val password = "p@ssw0rd!@#\$%^&*()"

        // Act
        val hashed = PasswordService.hashPassword(password)
        val isValid = PasswordService.verifyPassword(password, hashed)

        // Assert
        assertTrue(isValid)
    }
}

