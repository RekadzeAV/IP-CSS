package com.company.ipcamera.core.uibridge

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class AuthenticationBridgeTest {

    private lateinit var bridge: AuthenticationBridge

    @Test
    fun testLoginSuccess() = runTest {
        bridge = DesktopAuthenticationBridge(null, null)

        val result = bridge.login("testuser", "password123")

        assertIs<LoginResult.Success>(result)
        assertNotNull(result.user)
        assertEquals("testuser", result.user.username)
    }

    @Test
    fun testLoginFailure() = runTest {
        // Шифратор, который бросает исключение -> login должен вернуть Failure, а не прокинуть исключение
        val failingEncryption = object : com.company.ipcamera.core.common.security.PasswordEncryption {
            override fun encrypt(password: String): String =
                throw IllegalStateException("encryption failure")
            override fun decrypt(encryptedPassword: String): String = ""
            override fun isEncrypted(value: String): Boolean = false
        }
        bridge = DesktopAuthenticationBridge(null, failingEncryption)

        val result = bridge.login("invalid", "invalid")

        assertIs<LoginResult.Failure>(result)
    }

    @Test
    fun testLogout() = runTest {
        bridge = DesktopAuthenticationBridge(null, null)

        bridge.logout()

        // Logout should not throw exception
    }

    @Test
    fun testIsAuthorizedInitiallyFalse() = runTest {
        bridge = DesktopAuthenticationBridge(null, null)

        val authorized = bridge.isAuthorized()

        assertEquals(false, authorized)
    }

    @Test
    fun testGetCurrentUserInitiallyNull() = runTest {
        bridge = DesktopAuthenticationBridge(null, null)

        val user = bridge.getCurrentUser()

        assertEquals(null, user)
    }

    @Test
    fun testRegisterNotImplemented() = runTest {
        bridge = DesktopAuthenticationBridge(null, null)

        val result = bridge.register("newuser", "password", "user@test.com")

        assertIs<RegistrationResult.Failure>(result)
    }
}
