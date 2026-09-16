package com.company.ipcamera.core.network

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.seconds

@Serializable
data class TestResponse(val message: String)

@Serializable
data class TestRequest(val data: String)

class ApiClientTest {

    @Test
    fun testApiClientConfigDefault() = runTest {
        val config = ApiClientConfig.default("https://api.example.com")

        assertEquals("https://api.example.com", config.baseUrl)
        assertEquals(false, config.requireHttps)
        assertEquals(30.seconds, config.connectTimeout)
        assertEquals(true, config.enableLogging)
        assertEquals(true, config.enableRetry)
        assertEquals(3, config.maxRetries)
    }

    @Test
    fun testApiClientCreateThrowsWhenRequireHttpsAndHttpBaseUrl() = runTest {
        val config = ApiClientConfig.default(baseUrl = "http://insecure.example.com", requireHttps = true)
        assertFailsWith<IllegalArgumentException> {
            ApiClient.create(config)
        }
    }

    @Test
    fun testApiErrorMessages() {
        val networkError = ApiError.NetworkError(Exception("Connection failed"))
        assertEquals("Network error: Connection failed", networkError.message)

        val httpError = ApiError.HttpError(404, "Not Found", "Resource not found")
        assertEquals("HTTP 404: Not Found", httpError.message)

        val timeoutError = ApiError.TimeoutError("Request timeout")
        assertEquals("Timeout: Request timeout", timeoutError.message)
    }

    @Test
    fun testApiResultFold() {
        val success = ApiResult.Success("test")
        val result = success.fold(
            onSuccess = { it },
            onError = { null }
        )
        assertEquals("test", result)

        val error = ApiResult.Error(ApiError.UnknownError(Exception("test")))
        val errorResult = error.fold(
            onSuccess = { null },
            onError = { it.message }
        )
        assertNotNull(errorResult)
    }

    @Test
    fun testResponseCache() {
        val cache = ResponseCache(maxSize = 10, expirationTime = kotlin.time.Duration.parse("1m"))

        cache.put("key1", "value1")
        assertEquals("value1", cache.get("key1"))

        cache.put("key2", "value2")
        assertEquals("value2", cache.get("key2"))

        cache.clear()
        assertEquals(null, cache.get("key1"))
    }

    @Test
    fun testResponseCacheExpiration() {
        var now = 1_000L
        val cache = ResponseCache(
            maxSize = 10,
            expirationTime = kotlin.time.Duration.parse("100ms"),
            nowProvider = { now }
        )

        cache.put("key1", "value1")
        assertEquals("value1", cache.get("key1"))

        // Имитируем истечение времени без реального сна (flaky-free).
        now += 150

        // Кэш должен быть пустым
        assertEquals(null, cache.get("key1"))
    }

    @Test
    fun testResponseCacheMaxSize() {
        val cache = ResponseCache(maxSize = 2, expirationTime = kotlin.time.Duration.parse("1m"))

        cache.put("key1", "value1")
        cache.put("key2", "value2")
        cache.put("key3", "value3")

        // Самый старый элемент должен быть удален
        assertEquals(null, cache.get("key1"))
        assertEquals("value2", cache.get("key2"))
        assertEquals("value3", cache.get("key3"))
    }
}
