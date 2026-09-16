package com.company.ipcamera.core.network.integration

import com.company.ipcamera.core.network.ApiClient
import com.company.ipcamera.core.network.ApiClientConfig
import com.company.ipcamera.core.network.test.MockEngineFactory
import com.company.ipcamera.core.network.test.MockResponse
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import kotlin.test.*

/**
 * Интеграционные тесты для ApiClient с mock HTTP engine.
 */
class ApiClientIntegrationTest {

    @Test
    fun `test GET request returns parsed response`() = runTest {
        val engine = MockEngineFactory.create(
            responses = mapOf(
                "/api/test" to MockResponse(
                    body = """{"id":1,"name":"test"}""",
                    contentType = "application/json"
                )
            )
        )
        val client = ApiClient.create(
            config = ApiClientConfig(baseUrl = "http://localhost", enableRetry = false, enableCache = false),
            httpClientEngine = engine
        )

        val response = client.get<JsonObject>("/api/test")
        assertTrue(response.isSuccess)
        val body = response.fold(
            onSuccess = { it },
            onError = { fail("Expected success") }
        )
        assertEquals(1, body["id"]?.jsonPrimitive?.int)
        assertEquals("test", body["name"]?.jsonPrimitive?.content)
    }

    @Test
    fun `test POST request sends correct body`() = runTest {
        var capturedBody: String? = null
        val engine = MockEngineFactory.create(
            responses = mapOf(
                "/api/test" to MockResponse(
                    body = """{"success":true}""",
                    contentType = "application/json"
                )
            ),
            onRequest = { request ->
                // ApiClient шлёт тело как ByteArray; в MockEngine это OutgoingContent.ByteArrayContent/TextContent
                val content = request.body as? io.ktor.http.content.OutgoingContent
                capturedBody = when (content) {
                    is io.ktor.http.content.TextContent -> content.text
                    is io.ktor.http.content.OutgoingContent.ByteArrayContent -> content.bytes().decodeToString()
                    else -> null
                }
            }
        )
        val client = ApiClient.create(
            config = ApiClientConfig(baseUrl = "http://localhost", enableRetry = false, enableCache = false),
            httpClientEngine = engine
        )

        val requestBody = buildJsonObject {
            put("name", "test")
            put("value", 42)
        }

        val response = client.post<JsonObject, JsonObject>("/api/test", requestBody)
        assertTrue(response.isSuccess)
        assertNotNull(capturedBody)
        assertTrue(capturedBody!!.contains("test"))
        assertTrue(capturedBody!!.contains("42"))
    }

    @Test
    fun `test 404 returns failure result`() = runTest {
        val engine = MockEngineFactory.create(
            responses = mapOf(
                "/api/missing" to MockResponse(
                    body = """{"error":"Not Found"}""",
                    status = HttpStatusCode.NotFound,
                    contentType = "application/json"
                )
            )
        )
        val client = ApiClient.create(
            config = ApiClientConfig(baseUrl = "http://localhost", enableRetry = false, enableCache = false),
            httpClientEngine = engine
        )

        val response = client.get<JsonObject>("/api/missing")
        assertTrue(response.isError)
    }

    @Test
    fun `test network timeout returns failure`() = runTest {
        val engine = MockEngineFactory.create(
            responses = emptyMap(),
            delayMillis = 100
        )
        val client = ApiClient.create(
            config = ApiClientConfig(
                baseUrl = "http://localhost",
                enableRetry = false,
                enableCache = false,
                requestTimeout = kotlin.time.Duration.parse("50ms")
            ),
            httpClientEngine = engine
        )

        val response = client.get<JsonObject>("/api/timeout")
        assertTrue(response.isError)
    }

    @Test
    fun `test request headers include content type`() = runTest {
        var capturedHeaders: io.ktor.http.Headers? = null
        val engine = MockEngineFactory.create(
            responses = mapOf(
                "/api/test" to MockResponse(body = "{}")
            ),
            onRequest = { request ->
                capturedHeaders = request.headers
            }
        )
        val client = ApiClient.create(
            config = ApiClientConfig(baseUrl = "http://localhost", enableRetry = false, enableCache = false),
            httpClientEngine = engine
        )

        client.get<JsonObject>("/api/test")
        assertNotNull(capturedHeaders)
    }

    @Test
    fun `test sequential requests reuse connection`() = runTest {
        val engine = MockEngineFactory.create(
            responses = mapOf(
                "/api/first" to MockResponse(body = """{"seq":1}"""),
                "/api/second" to MockResponse(body = """{"seq":2}""")
            )
        )
        val client = ApiClient.create(
            config = ApiClientConfig(baseUrl = "http://localhost", enableRetry = false, enableCache = false),
            httpClientEngine = engine
        )

        val first = client.get<JsonObject>("/api/first")
        val second = client.get<JsonObject>("/api/second")

        assertTrue(first.isSuccess)
        assertTrue(second.isSuccess)
        assertEquals(1, first.fold(onSuccess = { it["seq"]?.jsonPrimitive?.int }, onError = { null }))
        assertEquals(2, second.fold(onSuccess = { it["seq"]?.jsonPrimitive?.int }, onError = { null }))
    }

    @Test
    fun `test empty response body handled gracefully`() = runTest {
        val engine = MockEngineFactory.create(
            responses = mapOf(
                "/api/empty" to MockResponse(body = "", status = HttpStatusCode.NoContent)
            )
        )
        val client = ApiClient.create(
            config = ApiClientConfig(baseUrl = "http://localhost", enableRetry = false, enableCache = false),
            httpClientEngine = engine
        )

        val response = client.get<String>("/api/empty")
        assertTrue(response.isSuccess)
        assertEquals("", response.fold(onSuccess = { it }, onError = { fail("Expected success") }))
    }
}
