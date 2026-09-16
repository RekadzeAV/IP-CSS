package com.company.ipcamera.server.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JwtServiceTest {

    private val service = JwtService(secret = "test-secret-key-at-least-32-characters!!")

    @Test
    fun `access token round trip`() {
        val token = service.generateAccessToken(
            userId = "u-1",
            username = "alice",
            role = "ADMIN",
            permissions = listOf("camera:view", "recording:start")
        )
        val decoded = service.validateAccessToken(token)

        assertEquals("u-1", decoded.subject)
        assertEquals("alice", service.extractUsername(token))
        assertEquals("ADMIN", service.extractRole(token))
        assertEquals(listOf("camera:view", "recording:start"), service.extractPermissions(token))
        assertFalse(service.isTokenExpired(token))
    }

    @Test
    fun `refresh token validates only as refresh`() {
        val token = service.generateRefreshToken(userId = "u-2")
        val decoded = service.validateRefreshToken(token)
        assertEquals("u-2", decoded.subject)

        assertFailsWith<InvalidTokenException> { service.validateAccessToken(token) }
        assertFailsWith<InvalidTokenException> { service.validate2FaTempToken(token) }
    }

    @Test
    fun `2fa temp token marks pending_2fa`() {
        val token = service.generate2FaTempToken(userId = "u-3")
        val decoded = service.validate2FaTempToken(token)
        assertEquals("2fa_temp", decoded.getClaim("type").asString())
        assertTrue(decoded.getClaim("pending_2fa").asBoolean())

        assertFailsWith<InvalidTokenException> { service.validateAccessToken(token) }
    }

    @Test
    fun `invalid signature rejected`() {
        val other = JwtService(secret = "another-secret-key-at-least-32-chars!")
        val forged = other.generateAccessToken(userId = "x", username = "x", role = "VIEWER")

        assertFailsWith<InvalidTokenException> { service.verifyToken(forged) }
        assertFailsWith<InvalidTokenException> { service.validateAccessToken(forged) }
    }

    @Test
    fun `garbage token rejected`() {
        assertFailsWith<InvalidTokenException> { service.verifyToken("not-a-jwt") }
        assertTrue(service.isTokenExpired("not-a-jwt"))
    }

    @Test
    fun `expired token detected`() {
        val token = service.generateAccessToken(
            userId = "u-4", username = "bob", role = "VIEWER",
            expirationMillis = -1000L // уже истёк
        )
        assertTrue(service.isTokenExpired(token))
        assertFailsWith<InvalidTokenException> { service.validateAccessToken(token) }
    }

    @Test
    fun `token info exposes claims`() {
        val before = System.currentTimeMillis()
        val token = service.generateAccessToken(
            userId = "u-5", username = "carol", role = "OPERATOR",
            permissions = listOf("ptz")
        )
        val info = service.getTokenInfo(token)

        assertEquals("u-5", info.userId)
        assertEquals("carol", info.username)
        assertEquals("OPERATOR", info.role)
        assertEquals(listOf("ptz"), info.permissions)
        assertEquals("access", info.tokenType)
        assertFalse(info.isExpired)
        assertNotNull(info.issuedAt)
        assertNotNull(info.expiresAt)
        assertTrue(info.issuedAt!!.time >= before - 1000)
        assertTrue(info.expiresAt!!.time > info.issuedAt!!.time)
    }

    @Test
    fun `different secrets produce different tokens`() {
        val t1 = service.generateAccessToken(userId = "u", username = "u", role = "VIEWER")
        val t2 = JwtService(secret = "other-secret-key-32-characters-long!!!")
            .generateAccessToken(userId = "u", username = "u", role = "VIEWER")
        assertNotEquals(t1, t2)
        assertTrue(t1.count { it == '.' } == 2)
    }
}
