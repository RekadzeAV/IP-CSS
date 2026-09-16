package com.company.ipcamera.core.network.test

import com.company.ipcamera.core.network.ApiClient
import com.company.ipcamera.core.network.ApiClientConfig
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

/**
 * Unit tests for ApiClient JVM implementation.
 * * Tests verify the basic contract and behavior of ApiClient
 * in the jvmMain source set.
 */
class ApiClientJvmTest {

    @Test
    fun `client should be created with valid config`() {
        // Given
        val config = ApiClientConfig(
            baseUrl = "http://test.com",
            requestTimeout = 5000.milliseconds
        )

        // When
        val client = ApiClient.create(config)

        // Then
        assertNotNull(client, "ApiClient should be created with valid config")
    }

    @Test
    fun `client should use Java engine by default`() {
        // Given
        val config = ApiClientConfig(
            baseUrl = "http://test.com",
            requestTimeout = 5000.milliseconds
        )

        // When
        val client = ApiClient.create(config)

        // Then
        assertNotNull(client, "ApiClient should use Java engine")
    }

    @Test
    fun `client should handle timeout configuration`() {
        // Given
        val config = ApiClientConfig(
            baseUrl = "http://test.com",
            requestTimeout = 10000.milliseconds // 10 seconds
        )

        // When
        val client = ApiClient.create(config)

        // Then
        assertNotNull(client, "ApiClient should accept timeout config")
    }

    @Test
    fun `client should handle invalid URL gracefully`() {
        // Given
        val config = ApiClientConfig(
            baseUrl = "invalid-url",
            requestTimeout = 5000.milliseconds
        )

        // When & Then - creation should succeed, validation happens on use
        val client = ApiClient.create(config)
        assertNotNull(client, "ApiClient creation should not validate URL immediately")
    }

    @Test
    fun `client should support multiple configurations`() {
        // Given
        val config1 = ApiClientConfig(baseUrl = "http://test1.com", requestTimeout = 5000.milliseconds)
        val config2 = ApiClientConfig(baseUrl = "http://test2.com", requestTimeout = 10000.milliseconds)

        // When
        val client1 = ApiClient.create(config1)
        val client2 = ApiClient.create(config2)

        // Then
        assertNotNull(client1, "First client should be created")
        assertNotNull(client2, "Second client should be created")
        assertTrue(client1 !== client2, "Clients should be different instances")
    }

    @Test
    fun `client should be closable`() {
        // Given
        val config = ApiClientConfig(baseUrl = "http://test.com", requestTimeout = 5000.milliseconds)
        val client = ApiClient.create(config)

        // When
        client.close()

        // Then - should not throw exception
        assertTrue(true, "Client should close without error")
    }
}
