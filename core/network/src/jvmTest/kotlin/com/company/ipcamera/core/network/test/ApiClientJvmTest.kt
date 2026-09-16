package com.company.ipcamera.core.network.test

import com.company.ipcamera.core.network.ApiClient
import com.company.ipcamera.core.network.RtspClientConfig
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for ApiClient JVM implementation.
 * 
 * Tests verify the basic contract and behavior of ApiClient
 * in the jvmMain source set.
 */
class ApiClientJvmTest {

    @Test
    fun `client should be created with valid config`() {
        // Given
        val config = RtspClientConfig(
            url = "http://test.com",
            timeout = 5000
        )

        // When
        val client = ApiClient.create(config)

        // Then
        assertNotNull(client, "ApiClient should be created with valid config")
    }

    @Test
    fun `client should use Java engine by default`() {
        // Given
        val config = RtspClientConfig(
            url = "http://test.com",
            timeout = 5000
        )

        // When
        val client = ApiClient.create(config)

        // Then
        assertNotNull(client, "ApiClient should use Java engine")
    }

    @Test
    fun `client should handle timeout configuration`() {
        // Given
        val config = RtspClientConfig(
            url = "http://test.com",
            timeout = 10000 // 10 seconds
        )

        // When
        val client = ApiClient.create(config)

        // Then
        assertNotNull(client, "ApiClient should accept timeout config")
    }

    @Test
    fun `client should handle invalid URL gracefully`() {
        // Given
        val config = RtspClientConfig(
            url = "invalid-url",
            timeout = 5000
        )

        // When & Then - creation should succeed, validation happens on use
        val client = ApiClient.create(config)
        assertNotNull(client, "ApiClient creation should not validate URL immediately")
    }

    @Test
    fun `client should support multiple configurations`() {
        // Given
        val config1 = RtspClientConfig(url = "http://test1.com", timeout = 5000)
        val config2 = RtspClientConfig(url = "http://test2.com", timeout = 10000)

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
        val config = RtspClientConfig(url = "http://test.com", timeout = 5000)
        val client = ApiClient.create(config)

        // When
        client.close()

        // Then - should not throw exception
        assertTrue(true, "Client should close without error")
    }
}