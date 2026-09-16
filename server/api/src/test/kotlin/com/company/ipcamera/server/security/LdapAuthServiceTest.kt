package com.company.ipcamera.server.security

import com.company.ipcamera.server.config.EnterpriseAuthConfig
import com.company.ipcamera.shared.domain.model.User
import com.company.ipcamera.shared.domain.model.UserRole
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Unit тесты для LdapAuthService
 */
class LdapAuthServiceTest {

    private lateinit var config: EnterpriseAuthConfig
    private lateinit var createOrGetLocalUser: suspend (String, String?, String?, UserRole) -> User
    private lateinit var ldapAuthService: LdapAuthService

    @BeforeEach
    fun setup() {
        config = mockk()
        createOrGetLocalUser = mockk()

        every { config.ldapEnabled } returns true
        every { config.ldapUrl } returns "ldap://localhost:389"
        every { config.ldapUserBaseDn } returns "dc=test,dc=local"
        every { config.ldapBindDn } returns "cn=admin,dc=test,dc=local"
        every { config.ldapBindPassword } returns "password"
        every { config.ldapUserBaseDn } returns "ou=users,dc=test,dc=local"
        every { config.ldapUserSearchFilter } returns "(uid={0})"
        every { config.ldapRoleMapping } returns mapOf(
            "admin" to "ADMIN",
            "operator" to "OPERATOR",
            "viewer" to "VIEWER"
        )

        ldapAuthService = LdapAuthService(config, createOrGetLocalUser)
    }

    @Test
    fun `id should return ldap`() {
        assertEquals("ldap", ldapAuthService.id)
    }

    @Test
    fun `isEnabled should return true when LDAP is enabled`() {
        every { config.ldapEnabled } returns true
        assertTrue(ldapAuthService.isEnabled())
    }

    @Test
    fun `isEnabled should return false when LDAP is disabled`() {
        every { config.ldapEnabled } returns false
        assertFalse(ldapAuthService.isEnabled())
    }

    @Test
    fun `checkAvailability should return false when disabled`() = runTest {
        // Given
        every { config.ldapEnabled } returns false

        // When
        val result = ldapAuthService.checkAvailability()

        // Then
        assertEquals(false to "disabled", result)
    }

    @Test
    fun `authenticate should return null when disabled`() = runTest {
        // Given
        every { config.ldapEnabled } returns false

        // When
        val result = ldapAuthService.authenticate("user", "password")

        // Then
        assertNull(result)
    }

    @Test
    fun `authenticate should return null when password is blank`() = runTest {
        // Given
        every { config.ldapEnabled } returns true

        // When
        val result = ldapAuthService.authenticate("user", "")

        // Then
        assertNull(result)
    }

    @Test
    fun `authenticate should return null temporarily due to SDK issue`() = runTest {
        // Given
        every { config.ldapEnabled } returns true

        // When
        val result = ldapAuthService.authenticate("user", "password")

        // Then
        assertNull(result)
    }
}
