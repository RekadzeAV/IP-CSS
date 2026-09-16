package com.company.ipcamera.core.network.integration

import com.company.ipcamera.core.network.RtspClientConfig
import com.company.ipcamera.core.network.RtspClientStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration тесты для ReconnectController и RTSP reconnect сценариев
 * * Тесты проверяют базовый функционал reconnect без реальной RTSP среды
 */
class ReconnectIntegrationTest {

    @Test
    fun `ReconnectClientConfig should be created with default values`() {
        // When
        val config = RtspClientConfig(url = "rtsp://localhost:8554/test")

        // Then
        assertNotNull(config, "Config should be created")
        assertEquals("rtsp://localhost:8554/test", config.url)
    }

    @Test
    fun `ReconnectClientConfig should accept custom reconnect settings`() {
        // Given
        val config = RtspClientConfig(
            url = "rtsp://localhost:8554/test",
            reconnectEnabled = true,
            reconnectMaxRetries = 10,
            reconnectInitialDelayMs = 2000
        )

        // Then
        assertNotNull(config, "Config should be created")
        assertTrue(config.reconnectEnabled, "Reconnect should be enabled")
        assertEquals(10, config.reconnectMaxRetries, "Max retries should be 10")
        assertEquals(2000, config.reconnectInitialDelayMs, "Initial delay should be 2000ms")
    }

    @Test
    fun `ReconnectClientConfig should support aggressive reconnect policy`() {
        // Given
        val testConfig = ReconnectTestConfig.aggressive()
        val config = RtspClientConfig(
            url = testConfig.serverUrl,
            reconnectEnabled = testConfig.reconnectEnabled,
            reconnectMaxRetries = testConfig.reconnectMaxRetries.toInt(),
            reconnectInitialDelayMs = (testConfig.reconnectInitialDelayMs / 1000).toInt(),
            reconnectMaxDelayMs = (testConfig.reconnectMaxDelayMs / 1000).toInt()
        )

        // Then
        assertTrue(config.reconnectMaxRetries >= 10, "Aggressive policy should have >= 10 retries")
        assertTrue(config.reconnectInitialDelayMs <= 1, "Aggressive policy should have short initial delay")
    }

    @Test
    fun `ReconnectClientConfig should support conservative reconnect policy`() {
        // Given
        val testConfig = ReconnectTestConfig.conservative()
        val config = RtspClientConfig(
            url = testConfig.serverUrl,
            reconnectEnabled = testConfig.reconnectEnabled,
            reconnectMaxRetries = testConfig.reconnectMaxRetries.toInt(),
            reconnectInitialDelayMs = (testConfig.reconnectInitialDelayMs / 1000).toInt()
        )

        // Then
        assertTrue(config.reconnectMaxRetries <= 5, "Conservative policy should have <= 5 retries")
        assertTrue(config.reconnectInitialDelayMs >= 3, "Conservative policy should have long initial delay")
    }

    @Test
    fun `RtspClientStatus should have expected states`() {
        // Then
        assertNotNull(RtspClientStatus.DISCONNECTED, "DISCONNECTED state should exist")
        assertNotNull(RtspClientStatus.CONNECTING, "CONNECTING state should exist")
        assertNotNull(RtspClientStatus.CONNECTED, "CONNECTED state should exist")
        assertNotNull(RtspClientStatus.PLAYING, "PLAYING state should exist")
        assertNotNull(RtspClientStatus.ERROR, "ERROR state should exist")
    }

    @Test
    fun `ReconnectTestResult should track reconnect attempts`() {
        // Given
        val result = ReconnectTestResult(
            success = true,
            reconnectAttempts = 3,
            totalReconnectTimeMs = 5000,
            finalStatus = "PLAYING"
        )

        // Then
        assertTrue(result.success, "Result should be successful")
        assertEquals(3, result.reconnectAttempts, "Should have 3 reconnect attempts")
        assertEquals(5000, result.totalReconnectTimeMs, "Total time should be 5000ms")
    }

    @Test
    fun `ReconnectTestResult should track failures`() {
        // Given
        val result = ReconnectTestResult(
            success = false,
            reconnectAttempts = 5,
            totalReconnectTimeMs = 30000,
            finalStatus = "ERROR",
            errorMessage = "Connection timeout"
        )

        // Then
        assertTrue(!result.success, "Result should be failed")
        assertEquals(5, result.reconnectAttempts, "Should have 5 reconnect attempts")
        assertNotNull(result.errorMessage, "Should have error message")
    }

    @Test
    fun `ServerHealthStatus should have all expected states`() {
        // Then
        assertEquals(4, ServerHealthStatus.values().size, "Should have 4 health states")
        assertNotNull(ServerHealthStatus.HEALTHY)
        assertNotNull(ServerHealthStatus.UNHEALTHY)
        assertNotNull(ServerHealthStatus.STOPPED)
        assertNotNull(ServerHealthStatus.UNKNOWN)
    }
}
