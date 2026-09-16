package com.company.ipcamera.core.network

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for RTSP Client reconnect configuration and diagnostics tracking.
 * * These tests verify that reconnect parameters are properly configured and * diagnostics are tracked correctly. They run without native library by testing
 * the configuration and state management aspects.
 * * Test coverage:
 * - Reconnect configuration parameters validation
 * - Diagnostics tracking (reconnectAttempts, reconnectSuccesses, reconnectFailures)
 * - Status callback registration without crashes
 * - Client behavior with different reconnect configs
 * - Error state management and recovery
 * * Note: Full integration tests with real streams are in RtspRealStreamTest (jvmTest).
 */
class RtspClientReconnectIntegrationTest {

    @Test
    fun `reconnect config - default parameters are valid`() {
        // Arrange & Act
        val config = RtspClientConfig(
            url = "rtsp://test.local:554/stream"
        )

        // Assert
        assertEquals("rtsp://test.local:554/stream", config.url)
        assertEquals(5, config.reconnectMaxRetries)
        assertEquals(500, config.reconnectInitialDelayMs)
        assertEquals(10000, config.reconnectMaxDelayMs)
        assertEquals(2.0f, config.reconnectBackoffMultiplier)
        assertEquals(0.15f, config.reconnectJitterRatio)
        assertTrue(config.reconnectEnabled)
    }

    @Test
    fun `reconnect config - custom parameters are applied`() {
        // Arrange & Act
        val config = RtspClientConfig(
            url = "rtsp://test.local:554/stream",
            reconnectEnabled = true,
            reconnectMaxRetries = 3,
            reconnectInitialDelayMs = 100,
            reconnectMaxDelayMs = 1000,
            reconnectBackoffMultiplier = 1.5f,
            reconnectJitterRatio = 0.2f
        )

        // Assert
        assertEquals(3, config.reconnectMaxRetries)
        assertEquals(100, config.reconnectInitialDelayMs)
        assertEquals(1000, config.reconnectMaxDelayMs)
        assertEquals(1.5f, config.reconnectBackoffMultiplier)
        assertEquals(0.2f, config.reconnectJitterRatio)
    }

    @Test
    fun `client initializes with reconnect diagnostics`() = runTest {
        // Arrange
        val config = RtspClientConfig(
            url = "rtsp://test.local:554/stream",
            timeoutMillis = 1000
        )
        val client = RtspClient(config)

        // Act
        val diagnostics = client.getRuntimeDiagnostics().first()

        // Assert
        assertNotNull(diagnostics)
        assertEquals(0L, diagnostics.connectAttempts)
        assertEquals(0L, diagnostics.connectSuccesses)
        assertEquals(0L, diagnostics.connectFailures)
        assertEquals(0L, diagnostics.reconnectAttempts)
        assertEquals(0L, diagnostics.reconnectSuccesses)
        assertEquals(0L, diagnostics.reconnectFailures)
        assertEquals(0L, diagnostics.consecutiveFailures)
    }

    @Test
    fun `status callback can be registered without crashing`() = runTest {
        // Arrange
        var callbackCalled = false
        val config = RtspClientConfig(
            url = "rtsp://test.local:554/stream",
            timeoutMillis = 1000
        )
        val client = RtspClient(config)

        // Act - Register callback
        client.setStatusCallback { status, message ->
            callbackCalled = true
        }

        // Connect (will fail without native lib, but callback should be registered)
        client.connect()
        delay(200)

        // Assert
        assertTrue(callbackCalled, "Status callback should be invoked during connect")
    }

    @Test
    fun `client transitions to ERROR without native library`() = runTest {
        // Arrange
        val config = RtspClientConfig(
            url = "rtsp://test.local:554/stream",
            timeoutMillis = 1000,
            reconnectEnabled = true,
            reconnectMaxRetries = 2
        )
        val client = RtspClient(config)

        // Act
        client.connect()
        delay(500)

        val status = client.getStatus().value
        val diagnostics = client.getRuntimeDiagnostics().first()

        // Assert
        assertEquals(RtspClientStatus.ERROR, status)
        assertEquals(1L, diagnostics.connectAttempts)
        assertEquals(0L, diagnostics.connectSuccesses)
        assertEquals(1L, diagnostics.connectFailures)
        assertTrue(diagnostics.lastError != null)
    }

    @Test
    fun `reconnectWithBackoff handles missing native gracefully`() = runTest {
        // Arrange
        val config = RtspClientConfig(
            url = "rtsp://test.local:554/stream",
            timeoutMillis = 1000,
            reconnectEnabled = true,
            reconnectMaxRetries = 2,
            reconnectInitialDelayMs = 50,
            reconnectMaxDelayMs = 200
        )
        val client = RtspClient(config)

        // Act
        client.connect()
        delay(300)

        val initialDiagnostics = client.getRuntimeDiagnostics().first()

        // Try to reconnect (will fail without native lib, but should not crash)
        val reconnectResult = client.reconnectWithBackoff(
            maxAttempts = 2,
            initialDelayMs = 50,
            maxDelayMs = 200
        )

        delay(500)
        val finalDiagnostics = client.getRuntimeDiagnostics().first()

        // Assert
        assertFalse(reconnectResult, "Reconnect should fail without native library")
        assertEquals(1L, initialDiagnostics.connectAttempts)
        assertTrue(finalDiagnostics.reconnectAttempts >= 1, "Should attempt reconnect")
        assertTrue(finalDiagnostics.reconnectFailures >= 1, "Should record reconnect failure")
    }

    @Test
    fun `diagnostics track consecutive failures correctly`() = runTest {
        // Arrange
        val config = RtspClientConfig(
            url = "rtsp://test.local:554/stream",
            timeoutMillis = 500,
            reconnectEnabled = true,
            reconnectMaxRetries = 3
        )
        val client = RtspClient(config)

        // Act - Multiple connect attempts
        client.connect()
        delay(300)

        val diagnostics1 = client.getRuntimeDiagnostics().first()

        // Try reconnect
        client.reconnectWithBackoff(maxAttempts = 2, initialDelayMs = 50, maxDelayMs = 100)
        delay(500)

        val diagnostics2 = client.getRuntimeDiagnostics().first()

        // Assert
        assertTrue(diagnostics2.consecutiveFailures >= 1, "Should track consecutive failures")
        assertTrue(diagnostics2.lastError != null, "Should have last error recorded")
        assertTrue(diagnostics2.lastErrorAt != null, "Should have last error timestamp")
    }

    @Test
    fun `client with disabled reconnect still works`() = runTest {
        // Arrange
        val config = RtspClientConfig(
            url = "rtsp://test.local:554/stream",
            timeoutMillis = 1000,
            reconnectEnabled = false,
            reconnectMaxRetries = 0
        )
        val client = RtspClient(config)

        // Act
        client.connect()
        delay(300)

        val status = client.getStatus().value
        val diagnostics = client.getRuntimeDiagnostics().first()

        // Assert
        assertEquals(RtspClientStatus.ERROR, status)
        assertEquals(1L, diagnostics.connectAttempts)
        // With reconnect disabled, reconnectAttempts should remain 0
        assertEquals(0L, diagnostics.reconnectAttempts)
    }

    @Test
    fun `disconnect resets connection state`() = runTest {
        // Arrange
        val config = RtspClientConfig(
            url = "rtsp://test.local:554/stream",
            timeoutMillis = 1000
        )
        val client = RtspClient(config)

        // Act
        client.connect()
        delay(300)

        client.disconnect()
        delay(200)

        val status = client.getStatus().value

        // Assert
        assertEquals(RtspClientStatus.DISCONNECTED, status)
    }

    @Test
    fun `client handles multiple reconnect attempts with backoff timing`() = runBlocking {
        // Arrange
        val config = RtspClientConfig(
            url = "rtsp://test.local:554/stream",
            timeoutMillis = 500,
            reconnectEnabled = true,
            reconnectMaxRetries = 3,
            reconnectInitialDelayMs = 50,
            reconnectMaxDelayMs = 200,
            reconnectBackoffMultiplier = 2.0f
        )
        val client = RtspClient(config)

        // Act - First connect
        client.connect()
        delay(300)

        val startTime = System.currentTimeMillis()

        // Multiple reconnect attempts with backoff
        client.reconnectWithBackoff(
            maxAttempts = 3,
            initialDelayMs = 50,
            maxDelayMs = 200
        )

        val elapsed = System.currentTimeMillis() - startTime

        // Assert
        // NB: используем runBlocking (реальное время) — runTest ускоряет delay виртуальным временем,
        // поэтому измерение реального backoff некорректно.
        // К несуществующему хосту connect() быстро падает, поэтому суммарное время
        // зависит от попыток+задержек; проверяем, что было несколько попыток (backoff активен)
        // и что хотя бы первая задержка применена.
        val diagnostics = client.getRuntimeDiagnostics().value
        assertTrue(
            diagnostics.reconnectAttempts.toInt() >= 2,
            "Expected multiple reconnect attempts, got ${diagnostics.reconnectAttempts}"
        )
        assertTrue(elapsed >= 40, "Backoff delays should be applied, took ${elapsed}ms")
        assertTrue(elapsed < 3000, "Should not take too long, took ${elapsed}ms")
    }
}
