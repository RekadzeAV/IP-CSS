package com.company.ipcamera.server

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.Assume
import org.junit.BeforeClass
import org.junit.Test
import kotlin.test.*

/**
 * Интеграционные тесты API.
 * Проверяют эндпоинты сервера через HTTP.
 */
class ApiIntegrationTest {

    companion object {
        private const val BASE_URL = "http://localhost:8080/api/v1"
        private val client = HttpClient(CIO) {
            expectSuccess = false
        }
        private var authToken: String? = null

        private fun isServerAvailable(): Boolean {
            return try {
                runBlocking {
                    val response = client.get("$BASE_URL/health")
                    response.status.isSuccess()
                }
            } catch (e: Exception) {
                false
            }
        }

        @BeforeClass
        @JvmStatic
        fun setup() {
            Assume.assumeTrue("Skip API integration tests: server is not available at localhost:8080", isServerAvailable())
        }

        @AfterClass
        @JvmStatic
        fun teardown() {
            client.close()
        }
    }

    @Test
    fun `health endpoint returns 200`() = runBlocking {
        val response = client.get("$BASE_URL/health")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("status") || body.contains("healthy"))
    }

    @Test
    fun `login with invalid credentials returns 401`() = runBlocking {
        val response = client.post("$BASE_URL/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"invalid","password":"invalid"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `cameras endpoint without auth returns 401`() = runBlocking {
        val response = client.get("$BASE_URL/cameras")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `openapi yaml is accessible`() = runBlocking {
        val response = client.get("$BASE_URL/openapi.yaml")
        assertTrue(response.status.isSuccess())
        val body = response.bodyAsText()
        assertTrue(body.contains("openapi"))
    }

    @Test
    fun `events endpoint returns valid response`() = runBlocking {
        val response = client.get("$BASE_URL/events") {
            header(HttpHeaders.Authorization, "Bearer test-token")
        }
        assertTrue(response.status.value in listOf(200, 401))
    }

    @Test
    fun `settings endpoint returns settings`() = runBlocking {
        val response = client.get("$BASE_URL/settings") {
            header(HttpHeaders.Authorization, "Bearer test-token")
        }
        assertTrue(response.status.value in listOf(200, 401))
    }

    @Test
    fun `cluster health endpoint works`() = runBlocking {
        val response = client.get("$BASE_URL/cluster/health")
        assertTrue(response.status.isSuccess() || response.status == HttpStatusCode.Unauthorized)
    }

    @Test
    fun `swagger docs page is accessible`() = runBlocking {
        val response = client.get("http://localhost:8080/api/v1/docs")
        assertTrue(response.status.isSuccess())
        val body = response.bodyAsText()
        assertTrue(body.contains("swagger") || body.contains("SwaggerUI") || body.contains("html"))
    }

    @Test
    fun `cors headers are present`() = runBlocking {
        val response = client.get("$BASE_URL/health")
        val corsHeader = response.headers[HttpHeaders.AccessControlAllowOrigin]
        assertNotNull(corsHeader)
        Unit
    }

    @Test
    fun `content type is json for api endpoints`() = runBlocking {
        val response = client.get("$BASE_URL/health")
        val contentType = response.contentType()
        assertTrue(contentType?.match(ContentType.Application.Json) == true ||
                  contentType?.match(ContentType.parse("text/vnd.yaml")) == true)
    }
}
