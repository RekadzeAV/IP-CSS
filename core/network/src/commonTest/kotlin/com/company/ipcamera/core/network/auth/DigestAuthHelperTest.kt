package com.company.ipcamera.core.network.auth

import kotlin.test.*

/**
 * Unit тесты для DigestAuthHelper
 * Тестирует парсинг WWW-Authenticate заголовка и генерацию Digest ответа
 */
class DigestAuthHelperTest {

    @Test
    fun `test parseWWWAuthenticate with standard format`() {
        val header = """Digest realm="IP Camera", nonce="abc123", algorithm=MD5, qop="auth""""

        val params = DigestAuthHelper.parseWWWAuthenticate(header)

        assertNotNull(params)
        assertEquals("IP Camera", params.realm)
        assertEquals("abc123", params.nonce)
        assertEquals("MD5", params.algorithm)
        assertEquals("auth", params.qop)
    }

    @Test
    fun `test parseWWWAuthenticate with all parameters`() {
        val header = """Digest realm="Test Realm", nonce="xyz789", algorithm=MD5, qop="auth", opaque="opaque123", stale=true"""

        val params = DigestAuthHelper.parseWWWAuthenticate(header)

        assertNotNull(params)
        assertEquals("Test Realm", params.realm)
        assertEquals("xyz789", params.nonce)
        assertEquals("MD5", params.algorithm)
        assertEquals("auth", params.qop)
        assertEquals("opaque123", params.opaque)
        assertTrue(params.stale)
    }

    @Test
    fun `test parseWWWAuthenticate without qop`() {
        val header = """Digest realm="IP Camera", nonce="abc123", algorithm=MD5"""

        val params = DigestAuthHelper.parseWWWAuthenticate(header)

        assertNotNull(params)
        assertEquals("IP Camera", params.realm)
        assertEquals("abc123", params.nonce)
        assertEquals("MD5", params.algorithm)
        assertNull(params.qop)
    }

    @Test
    fun `test parseWWWAuthenticate with auth-int qop`() {
        val header = """Digest realm="IP Camera", nonce="abc123", algorithm=MD5, qop="auth-int""""

        val params = DigestAuthHelper.parseWWWAuthenticate(header)

        assertNotNull(params)
        assertEquals("auth-int", params.qop)
    }

    @Test
    fun `test parseWWWAuthenticate with qop list chooses auth`() {
        val header = """Digest realm="IP Camera", nonce="abc123", algorithm=MD5, qop="auth,auth-int""""

        val params = DigestAuthHelper.parseWWWAuthenticate(header)

        assertNotNull(params)
        assertEquals("auth", params.qop)
    }

    @Test
    fun `test parseWWWAuthenticate with SHA-256 algorithm`() {
        val header = """Digest realm="IP Camera", nonce="abc123", algorithm=SHA-256, qop="auth""""

        val params = DigestAuthHelper.parseWWWAuthenticate(header)

        assertNotNull(params)
        assertEquals("SHA-256", params.algorithm)
    }

    @Test
    fun `test parseWWWAuthenticate with case insensitive`() {
        val header = """digest realm="IP Camera", nonce="abc123", algorithm=md5, qop="auth""""

        val params = DigestAuthHelper.parseWWWAuthenticate(header)

        assertNotNull(params)
        assertEquals("IP Camera", params.realm)
        assertEquals("abc123", params.nonce)
    }

    @Test
    fun `test parseWWWAuthenticate with mixed-case scheme and extra spaces`() {
        val header = """dIgEsT   realm="IP Camera", nonce="abc123", algorithm=MD5, qop="auth""""

        val params = DigestAuthHelper.parseWWWAuthenticate(header)

        assertNotNull(params)
        assertEquals("IP Camera", params.realm)
        assertEquals("abc123", params.nonce)
        assertEquals("MD5", params.algorithm)
        assertEquals("auth", params.qop)
    }

    @Test
    fun `test parseWWWAuthenticate returns null for non-Digest header`() {
        val header = """Basic realm="IP Camera""""

        val params = DigestAuthHelper.parseWWWAuthenticate(header)

        assertNull(params)
    }

    @Test
    fun `test parseWWWAuthenticate returns null for missing realm`() {
        val header = """Digest nonce="abc123", algorithm=MD5"""

        val params = DigestAuthHelper.parseWWWAuthenticate(header)

        assertNull(params)
    }

    @Test
    fun `test parseWWWAuthenticate returns null for missing nonce`() {
        val header = """Digest realm="IP Camera", algorithm=MD5"""

        val params = DigestAuthHelper.parseWWWAuthenticate(header)

        assertNull(params)
    }

    @Test
    fun `test generateDigestAuthHeader with qop`() {
        val params = DigestAuthParams(
            realm = "IP Camera",
            nonce = "abc123",
            algorithm = "MD5",
            qop = "auth"
        )

        val header = DigestAuthHelper.generateDigestAuthHeader(
            username = "admin",
            password = "password123",
            method = "POST",
            uri = "http://192.168.1.100/onvif/device_service",
            params = params,
            nc = "00000001"
        )

        assertTrue(header.startsWith("Digest "))
        assertTrue(header.contains("username=\"admin\""))
        assertTrue(header.contains("realm=\"IP Camera\""))
        assertTrue(header.contains("nonce=\"abc123\""))
        assertTrue(header.contains("uri=\"http://192.168.1.100/onvif/device_service\""))
        assertTrue(header.contains("response=\""))
        assertTrue(header.contains("qop=\"auth\""))
        assertTrue(header.contains("nc=00000001"))
        assertTrue(header.contains("cnonce=\""))
        assertTrue(header.contains("algorithm=MD5"))
    }

    @Test
    fun `test generateDigestAuthHeader uses single qop token when list provided`() {
        val params = DigestAuthParams(
            realm = "IP Camera",
            nonce = "abc123",
            algorithm = "MD5",
            qop = "auth,auth-int"
        )

        val header = DigestAuthHelper.generateDigestAuthHeader(
            username = "admin",
            password = "password123",
            method = "POST",
            uri = "http://192.168.1.100/onvif/device_service",
            params = params,
            nc = "00000001",
            cnonce = "0a4f113b"
        )

        assertTrue(header.contains("qop=\"auth\""))
        assertFalse(header.contains("qop=\"auth,auth-int\""))
    }

    @Test
    fun `test generateDigestAuthHeader without qop`() {
        val params = DigestAuthParams(
            realm = "IP Camera",
            nonce = "abc123",
            algorithm = "MD5",
            qop = null
        )

        val header = DigestAuthHelper.generateDigestAuthHeader(
            username = "admin",
            password = "password123",
            method = "GET",
            uri = "http://192.168.1.100/onvif/device_service",
            params = params
        )

        assertTrue(header.startsWith("Digest "))
        assertTrue(header.contains("username=\"admin\""))
        assertTrue(header.contains("realm=\"IP Camera\""))
        assertTrue(header.contains("nonce=\"abc123\""))
        assertTrue(header.contains("uri=\"http://192.168.1.100/onvif/device_service\""))
        assertTrue(header.contains("response=\""))
        assertFalse(header.contains("qop="))
        assertFalse(header.contains("nc="))
        assertFalse(header.contains("cnonce="))
    }

    @Test
    fun `test generateDigestAuthHeader with opaque`() {
        val params = DigestAuthParams(
            realm = "IP Camera",
            nonce = "abc123",
            algorithm = "MD5",
            qop = "auth",
            opaque = "opaque123"
        )

        val header = DigestAuthHelper.generateDigestAuthHeader(
            username = "admin",
            password = "password123",
            method = "POST",
            uri = "http://192.168.1.100/onvif/device_service",
            params = params
        )

        assertTrue(header.contains("opaque=\"opaque123\""))
    }

    @Test
    fun `test generateDigestAuthHeader generates different cnonce each time`() {
        val params = DigestAuthParams(
            realm = "IP Camera",
            nonce = "abc123",
            algorithm = "MD5",
            qop = "auth"
        )

        val header1 = DigestAuthHelper.generateDigestAuthHeader(
            username = "admin",
            password = "password123",
            method = "POST",
            uri = "http://192.168.1.100/onvif/device_service",
            params = params
        )

        val header2 = DigestAuthHelper.generateDigestAuthHeader(
            username = "admin",
            password = "password123",
            method = "POST",
            uri = "http://192.168.1.100/onvif/device_service",
            params = params
        )

        // Извлекаем cnonce из заголовков
        val cnonce1 = extractCnonce(header1)
        val cnonce2 = extractCnonce(header2)

        assertNotNull(cnonce1)
        assertNotNull(cnonce2)
        // cnonce должны быть разными (с высокой вероятностью)
        assertNotEquals(cnonce1, cnonce2)
    }

    @Test
    fun `test generateDigestAuthHeader with different nc values`() {
        val params = DigestAuthParams(
            realm = "IP Camera",
            nonce = "abc123",
            algorithm = "MD5",
            qop = "auth"
        )

        val header1 = DigestAuthHelper.generateDigestAuthHeader(
            username = "admin",
            password = "password123",
            method = "POST",
            uri = "http://192.168.1.100/onvif/device_service",
            params = params,
            nc = "00000001"
        )

        val header2 = DigestAuthHelper.generateDigestAuthHeader(
            username = "admin",
            password = "password123",
            method = "POST",
            uri = "http://192.168.1.100/onvif/device_service",
            params = params,
            nc = "00000002"
        )

        assertTrue(header1.contains("nc=00000001"))
        assertTrue(header2.contains("nc=00000002"))
    }

    @Test
    fun `test extractWWWAuthenticate from headers map`() {
        val headers = mapOf(
            "WWW-Authenticate" to listOf("""Digest realm="IP Camera", nonce="abc123", algorithm=MD5, qop="auth"""")
        )

        val header = DigestAuthHelper.extractWWWAuthenticate(headers)

        assertNotNull(header)
        assertTrue(header.startsWith("Digest", ignoreCase = true))
    }

    @Test
    fun `test extractWWWAuthenticate with case insensitive key`() {
        val headers = mapOf(
            "www-authenticate" to listOf("""Digest realm="IP Camera", nonce="abc123", algorithm=MD5, qop="auth"""")
        )

        val header = DigestAuthHelper.extractWWWAuthenticate(headers)

        assertNotNull(header)
    }

    @Test
    fun `test extractWWWAuthenticate prefers Digest over Basic`() {
        val headers = mapOf(
            "WWW-Authenticate" to listOf(
                """Basic realm="IP Camera"""",
                """Digest realm="IP Camera", nonce="abc123", algorithm=MD5, qop="auth""""
            )
        )

        val header = DigestAuthHelper.extractWWWAuthenticate(headers)

        assertNotNull(header)
        assertTrue(header.startsWith("Digest", ignoreCase = true))
    }

    @Test
    fun `test extractWWWAuthenticate returns null when not found`() {
        val headers = mapOf<String, List<String>>(
            "Content-Type" to listOf("application/json")
        )

        val header = DigestAuthHelper.extractWWWAuthenticate(headers)

        assertNull(header)
    }

    @Test
    fun `test full digest authentication flow`() {
        // Симуляция полного цикла Digest Authentication
        val wwwAuthenticate = """Digest realm="IP Camera", nonce="abc123def456", algorithm=MD5, qop="auth""""

        // Парсинг заголовка
        val params = DigestAuthHelper.parseWWWAuthenticate(wwwAuthenticate)
        assertNotNull(params)

        // Генерация Authorization заголовка
        val authHeader = DigestAuthHelper.generateDigestAuthHeader(
            username = "admin",
            password = "password123",
            method = "POST",
            uri = "http://192.168.1.100/onvif/device_service",
            params = params
        )

        // Проверка структуры заголовка
        assertTrue(authHeader.startsWith("Digest "))
        assertTrue(authHeader.contains("username=\"admin\""))
        assertTrue(authHeader.contains("realm=\"IP Camera\""))
        assertTrue(authHeader.contains("nonce=\"abc123def456\""))
        assertTrue(authHeader.contains("response=\""))
    }

    @Test
    fun `test digest response matches RFC2617 MD5 sample`() {
        val params = DigestAuthParams(
            realm = "testrealm@host.com",
            nonce = "dcd98b7102dd2f0e8b11d0f600bfb0c093",
            algorithm = "MD5",
            qop = "auth"
        )

        val authHeader = DigestAuthHelper.generateDigestAuthHeader(
            username = "Mufasa",
            password = "Circle Of Life",
            method = "GET",
            uri = "/dir/index.html",
            params = params,
            cnonce = "0a4f113b",
            nc = "00000001"
        )

        val response = extractAuthParam(authHeader, "response")
        assertEquals("6629fae49393a05397450978507c4ef1", response)
    }

    @Test
    fun `test digest response deterministic for SHA-256 auth`() {
        val params = DigestAuthParams(
            realm = "http-auth@example.org",
            nonce = "f84f1cec41e6cbe5aea9c8e88d359",
            algorithm = "SHA-256",
            qop = "auth"
        )

        val authHeader = DigestAuthHelper.generateDigestAuthHeader(
            username = "Mufasa",
            password = "Circle Of Life",
            method = "GET",
            uri = "/dir/index.html",
            params = params,
            cnonce = "0a4f113b",
            nc = "00000001"
        )

        val response = extractAuthParam(authHeader, "response")
        assertEquals("5d5b67952df5a169328b364dcb73269986eccee9a8eec09a4b3179ffd92f4902", response)
    }

    @Test
    fun `test digest response deterministic for MD5-sess auth`() {
        val params = DigestAuthParams(
            realm = "testrealm@host.com",
            nonce = "dcd98b7102dd2f0e8b11d0f600bfb0c093",
            algorithm = "MD5-sess",
            qop = "auth"
        )

        val authHeader = DigestAuthHelper.generateDigestAuthHeader(
            username = "Mufasa",
            password = "Circle Of Life",
            method = "GET",
            uri = "/dir/index.html",
            params = params,
            cnonce = "0a4f113b",
            nc = "00000001"
        )

        val response = extractAuthParam(authHeader, "response")
        assertEquals("8e3825c57e897f5a0dec6c2d4e5059d0", response)
    }

    @Test
    fun `test digest response deterministic for SHA-256-sess auth`() {
        val params = DigestAuthParams(
            realm = "testrealm@host.com",
            nonce = "dcd98b7102dd2f0e8b11d0f600bfb0c093",
            algorithm = "SHA-256-sess",
            qop = "auth"
        )

        val authHeader = DigestAuthHelper.generateDigestAuthHeader(
            username = "Mufasa",
            password = "Circle Of Life",
            method = "GET",
            uri = "/dir/index.html",
            params = params,
            cnonce = "0a4f113b",
            nc = "00000001"
        )

        val response = extractAuthParam(authHeader, "response")
        assertEquals("b8822e12417cb7750f4e2b8515f0dcf25b7dd26993e80bee1426201446a7f59b", response)
    }

    @Test
    fun `test digest response deterministic for MD5 auth-int`() {
        val params = DigestAuthParams(
            realm = "testrealm@host.com",
            nonce = "dcd98b7102dd2f0e8b11d0f600bfb0c093",
            algorithm = "MD5",
            qop = "auth-int"
        )

        val authHeader = DigestAuthHelper.generateDigestAuthHeader(
            username = "Mufasa",
            password = "Circle Of Life",
            method = "POST",
            uri = "/dir/index.html",
            params = params,
            cnonce = "0a4f113b",
            nc = "00000001",
            entityBody = "hello=world"
        )

        val response = extractAuthParam(authHeader, "response")
        assertEquals("fa9e05fdda0f18ca8fa3f636420a366e", response)
    }

    @Test
    fun `test digest response deterministic for SHA-256 auth-int`() {
        val params = DigestAuthParams(
            realm = "testrealm@host.com",
            nonce = "dcd98b7102dd2f0e8b11d0f600bfb0c093",
            algorithm = "SHA-256",
            qop = "auth-int"
        )

        val authHeader = DigestAuthHelper.generateDigestAuthHeader(
            username = "Mufasa",
            password = "Circle Of Life",
            method = "POST",
            uri = "/dir/index.html",
            params = params,
            cnonce = "0a4f113b",
            nc = "00000001",
            entityBody = "hello=world"
        )

        val response = extractAuthParam(authHeader, "response")
        assertEquals("1b4ac0b96422b819374bfacd6d1341400cffc286cde71abcd99bc223e9c07db2", response)
    }

    /**
     * Вспомогательная функция для извлечения cnonce из заголовка
     */
    private fun extractCnonce(header: String): String? {
        val regex = Regex("""cnonce="([^"]+)"""")
        return regex.find(header)?.groupValues?.get(1)
    }

    @Test
    fun `test stale nonce triggers warning but still generates header`() {
        val params = DigestAuthParams(
            realm = "IP Camera",
            nonce = "stale_nonce_123",
            algorithm = "MD5",
            qop = "auth",
            stale = true
        )

        val header = DigestAuthHelper.generateDigestAuthHeader(
            username = "admin",
            password = "password123",
            method = "POST",
            uri = "http://192.168.1.100/onvif/device_service",
            params = params,
            nc = "00000001"
        )

        assertTrue(header.startsWith("Digest "))
        assertTrue(header.contains("nonce=\"stale_nonce_123\""))
        assertTrue(header.contains("response=\""))
    }

    @Test
    fun `test realm caching pattern via params equality`() {
        val params1 = DigestAuthParams(
            realm = "IP Camera",
            nonce = "nonce1",
            algorithm = "MD5",
            qop = "auth"
        )
        val params2 = params1.copy(nonce = "nonce2")

        assertEquals(params1.realm, params2.realm)
        assertEquals(params1.algorithm, params2.algorithm)
        assertNotEquals(params1.nonce, params2.nonce)
    }

    @Test
    fun `test extractWWWAuthenticate handles empty list`() {
        val headers = mapOf(
            "WWW-Authenticate" to emptyList<String>()
        )
        assertNull(DigestAuthHelper.extractWWWAuthenticate(headers))
    }

    @Test
    fun `test parseWWWAuthenticate handles garbage input gracefully`() {
        val params = DigestAuthHelper.parseWWWAuthenticate("not even close to digest")
        assertNull(params)
    }

    private fun extractAuthParam(header: String, name: String): String? {
        val regex = Regex("""\b$name="?([^",\s]+)"?""")
        return regex.find(header)?.groupValues?.get(1)
    }
}
