package com.company.ipcamera.core.network.onvif

import kotlin.test.Test
import kotlin.test.*

class OnvifDigestAuthTest {

    private val digestAuth = OnvifDigestAuth()

    @Test
    fun `parseWwwAuthenticate returns null for null header`() {
        val result = digestAuth.parseWwwAuthenticate(null)
        assertNull(result)
    }

    @Test
    fun `parseWwwAuthenticate returns null for non-Digest header`() {
        val result = digestAuth.parseWwwAuthenticate("Basic realm=\"test\"")
        assertNull(result)
    }

    @Test
    fun `parseWwwAuthenticate parses valid Digest challenge`() {
        val header = """Digest realm="ONVIF", nonce="abc123xyz", opaque="def456", qop="auth", algorithm="MD5""""
        val result = digestAuth.parseWwwAuthenticate(header)
        assertNotNull(result)
        assertEquals("ONVIF", result!!.realm)
        assertEquals("abc123xyz", result.nonce)
        assertEquals("def456", result.opaque)
        assertEquals("auth", result.qop)
        assertEquals("MD5", result.algorithm)
        assertFalse(result.stale)
    }

    @Test
    fun `parseWwwAuthenticate handles minimal challenge`() {
        val header = """Digest realm="ONVIF", nonce="abc123""""
        val result = digestAuth.parseWwwAuthenticate(header)
        assertNotNull(result)
        assertEquals("ONVIF", result!!.realm)
        assertEquals("abc123", result.nonce)
        assertNull(result.opaque)
        assertNull(result.qop)
        assertEquals("MD5", result.algorithm)
    }

    @Test
    fun `buildAuthorizationHeader produces correct format`() {
        val challenge = DigestChallenge(
            realm = "ONVIF",
            nonce = "abc123",
            opaque = "def456",
            qop = "auth",
            algorithm = "MD5"
        )
        val header = digestAuth.buildAuthorizationHeader(
            challenge = challenge,
            username = "admin",
            password = "password123",
            method = "POST",
            uri = "/onvif/device_service",
            nc = "00000001",
            cnonce = "testcnonce"
        )

        assertTrue(header.startsWith("Digest "))
        assertTrue(header.contains("username=\"admin\""))
        assertTrue(header.contains("realm=\"ONVIF\""))
        assertTrue(header.contains("nonce=\"abc123\""))
        assertTrue(header.contains("uri=\"/onvif/device_service\""))
        assertTrue(header.contains("algorithm=MD5"))
        assertTrue(header.contains("qop=auth"))
        assertTrue(header.contains("opaque=\"def456\""))
        assertTrue(header.contains("nc=00000001"))
        assertTrue(header.contains("cnonce=\"testcnonce\""))
        assertTrue(header.contains("response=\""))
    }

    @Test
    fun `authenticate returns null for invalid header`() {
        val result = digestAuth.authenticate(
            wwwAuthHeader = "Invalid",
            username = "admin",
            password = "pass",
            method = "GET",
            uri = "/test"
        )
        assertNull(result)
    }

    @Test
    fun `authenticate returns valid header for valid challenge`() {
        val header = """Digest realm="ONVIF", nonce="abc123", qop="auth""""
        val result = digestAuth.authenticate(
            wwwAuthHeader = header,
            username = "admin",
            password = "pass",
            method = "POST",
            uri = "/onvif/device_service"
        )
        assertNotNull(result)
        assertTrue(result!!.startsWith("Digest "))
        assertTrue(result.contains("response=\""))
    }

    @Test
    fun `same input produces same MD5 hash`() {
        val challenge = DigestChallenge(
            realm = "ONVIF",
            nonce = "abc123",
            qop = "auth"
        )
        val header1 = digestAuth.buildAuthorizationHeader(
            challenge, "admin", "pass", "POST", "/onvif/device_service",
            nc = "00000001", cnonce = "fixedcnonce"
        )
        val header2 = digestAuth.buildAuthorizationHeader(
            challenge, "admin", "pass", "POST", "/onvif/device_service",
            nc = "00000001", cnonce = "fixedcnonce"
        )
        assertEquals(header1, header2)
    }

    @Test
    fun `different passwords produce different responses`() {
        val challenge = DigestChallenge(
            realm = "ONVIF",
            nonce = "abc123",
            qop = "auth"
        )
        val header1 = digestAuth.buildAuthorizationHeader(
            challenge, "admin", "pass1", "POST", "/onvif/device_service",
            nc = "00000001", cnonce = "fixedcnonce"
        )
        val header2 = digestAuth.buildAuthorizationHeader(
            challenge, "admin", "pass2", "POST", "/onvif/device_service",
            nc = "00000001", cnonce = "fixedcnonce"
        )
        assertNotEquals(header1, header2)
    }

    @Test
    fun `different nonces produce different responses`() {
        val challenge1 = DigestChallenge(realm = "ONVIF", nonce = "nonce1", qop = "auth")
        val challenge2 = DigestChallenge(realm = "ONVIF", nonce = "nonce2", qop = "auth")
        val header1 = digestAuth.buildAuthorizationHeader(
            challenge1, "admin", "pass", "POST", "/onvif/device_service",
            nc = "00000001", cnonce = "fixedcnonce"
        )
        val header2 = digestAuth.buildAuthorizationHeader(
            challenge2, "admin", "pass", "POST", "/onvif/device_service",
            nc = "00000001", cnonce = "fixedcnonce"
        )
        assertNotEquals(header1, header2)
    }

    @Test
    fun `parseWwwAuthenticate detects stale flag`() {
        val header = """Digest realm="ONVIF", nonce="abc", stale="true""""
        val result = digestAuth.parseWwwAuthenticate(header)
        assertNotNull(result)
        assertTrue(result!!.stale)
    }

    @Test
    fun `parseWwwAuthenticate handles spaces in header`() {
        val header = """Digest   realm="ONVIF" ,   nonce="abc" ,   qop="auth" """
        val result = digestAuth.parseWwwAuthenticate(header)
        assertNotNull(result)
        assertEquals("ONVIF", result!!.realm)
        assertEquals("abc", result.nonce)
        assertEquals("auth", result.qop)
    }
}
