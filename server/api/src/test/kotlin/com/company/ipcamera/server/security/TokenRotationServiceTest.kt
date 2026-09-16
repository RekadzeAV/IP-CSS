package com.company.ipcamera.server.security

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.every
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TokenRotationServiceTest {

    private val jwtService = JwtService(secret = "rotation-test-secret-key-32-characters!!")
    private val blacklist = mockk<TokenBlacklistService>()

    private fun service() = TokenRotationService(jwtService, blacklist)

    private fun validOldToken(userId: String = "u-1") =
        jwtService.generateAccessToken(userId = userId, username = "alice", role = "ADMIN")

    @Test
    fun `rotate access token produces new token and blacklists old`() = runBlocking {
        val old = validOldToken()
        coEvery { blacklist.blacklistToken(any()) } returns Unit

        val newToken = service().rotateAccessToken(old, "u-1", "alice", "ADMIN", listOf("camera:view"))
        assertTrue(newToken.isNotBlank())
        // новый токен валиден
        assertEquals("u-1", jwtService.validateAccessToken(newToken).subject)
        // старый занесён в blacklist
        coVerify(exactly = 1) { blacklist.blacklistToken(old) }
    }

    @Test
    fun `rotate access token with invalid old token throws`() = runBlocking {
        assertFailsWith<InvalidTokenException> {
            service().rotateAccessToken("garbage-token", "u-1", "alice", "ADMIN", emptyList())
        }
        // blacklist не должен вызываться (валидация провалилась)
        coVerify(exactly = 0) { blacklist.blacklistToken(any()) }
    }

    @Test
    fun `rotate refresh token works end to end`() = runBlocking {
        val oldRefresh = jwtService.generateRefreshToken("u-1")
        coEvery { blacklist.blacklistToken(any()) } returns Unit
        coEvery { blacklist.isTokenBlacklisted(any()) } returns false

        val newRefresh = service().rotateRefreshToken(oldRefresh, "u-1")
        assertEquals("u-1", jwtService.validateRefreshToken(newRefresh).subject)
        coVerify(exactly = 1) { blacklist.blacklistToken(oldRefresh) }
    }

    @Test
    fun `rotate refresh token rejected when already blacklisted`() = runBlocking {
        val oldRefresh = jwtService.generateRefreshToken("u-1")
        coEvery { blacklist.isTokenBlacklisted(any()) } returns true
        coEvery { blacklist.blacklistToken(any()) } returns Unit

        assertFailsWith<InvalidTokenException> {
            service().rotateRefreshToken(oldRefresh, "u-1")
        }
    }

    @Test
    fun `full rotation returns both tokens`() = runBlocking {
        val oldRefresh = jwtService.generateRefreshToken("u-1")
        coEvery { blacklist.blacklistToken(any()) } returns Unit
        coEvery { blacklist.isTokenBlacklisted(any()) } returns false

        val result = service().fullTokenRotation(oldRefresh, "u-1", "alice", "ADMIN", listOf("recording:start"))
        assertTrue(result.accessToken.isNotBlank())
        assertTrue(result.refreshToken.isNotBlank())
        assertEquals("u-1", jwtService.validateAccessToken(result.accessToken).subject)
        assertEquals("u-1", jwtService.validateRefreshToken(result.refreshToken).subject)
        assertTrue(result.accessTokenExpiresIn > 0)
        assertTrue(result.refreshTokenExpiresIn > 0)
    }

    @Test
    fun `canTokenBeRotated true for valid non-blacklisted token`() = runBlocking {
        val token = validOldToken()
        coEvery { blacklist.isTokenBlacklisted(any()) } returns false
        assertTrue(service().canTokenBeRotated(token))
    }

    @Test
    fun `canTokenBeRotated false for garbage token`() = runBlocking {
        coEvery { blacklist.isTokenBlacklisted(any()) } returns false
        assertTrue(!service().canTokenBeRotated("garbage"))
    }
}