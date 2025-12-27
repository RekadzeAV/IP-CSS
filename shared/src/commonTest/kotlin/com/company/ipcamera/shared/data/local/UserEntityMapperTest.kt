package com.company.ipcamera.shared.data.local

import com.company.ipcamera.shared.domain.model.UserRole
import com.company.ipcamera.shared.test.TestDataFactory
import kotlin.test.*

/**
 * Тесты для UserEntityMapper
 */
class UserEntityMapperTest {

    private val mapper = UserEntityMapper()

    @Test
    fun `test toDomain basic user`() {
        // Arrange
        val dbUser = com.company.ipcamera.shared.database.User(
            id = "user-1",
            username = "testuser",
            email = "test@example.com",
            full_name = "Test User",
            role = "VIEWER",
            permissions = null,
            created_at = 1000L,
            last_login_at = 2000L,
            is_active = 1L
        )

        // Act
        val domainUser = mapper.toDomain(dbUser)

        // Assert
        assertEquals("user-1", domainUser.id)
        assertEquals("testuser", domainUser.username)
        assertEquals("test@example.com", domainUser.email)
        assertEquals("Test User", domainUser.fullName)
        assertEquals(UserRole.VIEWER, domainUser.role)
        assertTrue(domainUser.permissions.isEmpty())
        assertTrue(domainUser.isActive)
        assertEquals(2000L, domainUser.lastLoginAt)
    }

    @Test
    fun `test toDomain user with permissions`() {
        // Arrange
        val permissionsJson = """["read:cameras","write:cameras"]"""
        val dbUser = com.company.ipcamera.shared.database.User(
            id = "user-2",
            username = "admin",
            email = null,
            full_name = null,
            role = "ADMIN",
            permissions = permissionsJson,
            created_at = 1000L,
            last_login_at = null,
            is_active = 0L
        )

        // Act
        val domainUser = mapper.toDomain(dbUser)

        // Assert
        assertEquals("admin", domainUser.username)
        assertNull(domainUser.email)
        assertNull(domainUser.fullName)
        assertEquals(UserRole.ADMIN, domainUser.role)
        assertEquals(2, domainUser.permissions.size)
        assertTrue(domainUser.permissions.contains("read:cameras"))
        assertTrue(domainUser.permissions.contains("write:cameras"))
        assertFalse(domainUser.isActive)
        assertNull(domainUser.lastLoginAt)
    }

    @Test
    fun `test toDatabase basic user`() {
        // Arrange
        val domainUser = TestDataFactory.createTestUser(
            id = "user-1",
            username = "testuser",
            role = UserRole.VIEWER
        )

        // Act
        val dbUser = mapper.toDatabase(domainUser)

        // Assert
        assertEquals("user-1", dbUser.id)
        assertEquals("testuser", dbUser.username)
        assertEquals("VIEWER", dbUser.role)
        assertEquals(1L, dbUser.is_active)
    }

    @Test
    fun `test toDatabase user with permissions`() {
        // Arrange
        val domainUser = TestDataFactory.createTestUser(
            id = "user-2",
            username = "admin",
            role = UserRole.ADMIN,
            permissions = listOf("read:cameras", "write:cameras")
        )

        // Act
        val dbUser = mapper.toDatabase(domainUser)

        // Assert
        assertNotNull(dbUser.permissions)
        assertTrue(dbUser.permissions!!.contains("read:cameras"))
    }

    @Test
    fun `test round trip conversion`() {
        // Arrange
        val originalUser = TestDataFactory.createTestUser(
            id = "user-1",
            username = "testuser",
            role = UserRole.OPERATOR,
            permissions = listOf("read:cameras")
        )

        // Act
        val dbUser = mapper.toDatabase(originalUser)
        val convertedUser = mapper.toDomain(dbUser)

        // Assert
        assertEquals(originalUser.id, convertedUser.id)
        assertEquals(originalUser.username, convertedUser.username)
        assertEquals(originalUser.role, convertedUser.role)
        assertEquals(originalUser.permissions, convertedUser.permissions)
    }

    @Test
    fun `test toDomain all user roles`() {
        val roles = UserRole.values()

        roles.forEach { role ->
            val dbUser = com.company.ipcamera.shared.database.User(
                id = "user-$role",
                username = "testuser",
                email = null,
                full_name = null,
                role = role.name,
                permissions = null,
                created_at = System.currentTimeMillis(),
                last_login_at = null,
                is_active = 1L
            )

            val domainUser = mapper.toDomain(dbUser)
            assertEquals(role, domainUser.role)
        }
    }
}

