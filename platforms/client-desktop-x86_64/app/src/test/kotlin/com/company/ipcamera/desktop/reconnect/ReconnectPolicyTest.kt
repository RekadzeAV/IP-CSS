package com.company.ipcamera.desktop.reconnect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith

/**
 * Unit тесты для ReconnectPolicy
 */
class ReconnectPolicyTest {

    @Test
    fun `default policy should have valid values`() {
        val policy = ReconnectPolicy.BALANCED
        
        assertTrue(policy.enabled)
        assertEquals(5, policy.maxAttempts)
        assertEquals(1500L, policy.initialDelayMs)
        assertEquals(30000L, policy.maxDelayMs)
        assertEquals(ReconnectBackoffStrategy.EXPONENTIAL, policy.backoffStrategy)
        assertEquals(2.0, policy.backoffMultiplier)
        assertEquals(0.1, policy.jitterRatio)
    }

    @Test
    fun `conservative policy should have higher delays`() {
        val policy = ReconnectPolicy.CONSERVATIVE
        
        assertEquals(3, policy.maxAttempts)
        assertEquals(3000L, policy.initialDelayMs)
        assertEquals(60000L, policy.maxDelayMs)
        assertEquals(2.5, policy.backoffMultiplier)
        assertEquals(0.2, policy.jitterRatio)
    }

    @Test
    fun `aggressive policy should have lower delays`() {
        val policy = ReconnectPolicy.AGGRESSIVE
        
        assertEquals(10, policy.maxAttempts)
        assertEquals(500L, policy.initialDelayMs)
        assertEquals(15000L, policy.maxDelayMs)
        assertEquals(1.5, policy.backoffMultiplier)
        assertEquals(0.05, policy.jitterRatio)
    }

    @Test
    fun `none policy should be disabled`() {
        val policy = ReconnectPolicy.NONE
        
        assertFalse(policy.enabled)
        assertEquals(0, policy.maxAttempts)
        assertEquals(0L, policy.initialDelayMs)
        assertEquals(0L, policy.maxDelayMs)
    }

    @Test
    fun `policy validation should reject invalid maxAttempts`() {
        // maxAttempts < 1 вызывает исключение только когда enabled = true
        assertFailsWith<IllegalArgumentException> {
            ReconnectPolicy.create(maxAttempts = -1, enabled = true)
        }
        
        assertFailsWith<IllegalArgumentException> {
            ReconnectPolicy.create(maxAttempts = 0, enabled = true)
        }
        
        // Но enabled = false с maxAttempts = 0 разрешено (как NONE)
        val policy = ReconnectPolicy.create(maxAttempts = 0, enabled = false)
        assertFalse(policy.enabled)
        assertEquals(0, policy.maxAttempts)
    }

    @Test
    fun `policy validation should reject invalid initialDelayMs`() {
        assertFailsWith<IllegalArgumentException> {
            ReconnectPolicy.create(initialDelayMs = 50)
        }
    }

    @Test
    fun `policy validation should reject invalid backoffMultiplier`() {
        assertFailsWith<IllegalArgumentException> {
            ReconnectPolicy.create(backoffMultiplier = 0.5)
        }
        
        assertFailsWith<IllegalArgumentException> {
            ReconnectPolicy.create(backoffMultiplier = 15.0)
        }
    }

    @Test
    fun `policy validation should reject invalid jitterRatio`() {
        assertFailsWith<IllegalArgumentException> {
            ReconnectPolicy.create(jitterRatio = -0.1)
        }
        
        assertFailsWith<IllegalArgumentException> {
            ReconnectPolicy.create(jitterRatio = 0.6)
        }
    }

    @Test
    fun `policy validation should reject maxDelayMs less than initialDelayMs`() {
        assertFailsWith<IllegalArgumentException> {
            ReconnectPolicy.create(initialDelayMs = 1000L, maxDelayMs = 500L)
        }
    }

    @Test
    fun `calculateReconnectDelay should return 0 for disabled policy`() {
        val policy = ReconnectPolicy.NONE
        
        assertEquals(0L, calculateReconnectDelay(1, policy))
    }

    @Test
    fun `calculateReconnectDelay should return initialDelay for attempt 1`() {
        val policy = ReconnectPolicy.create(initialDelayMs = 1000L, jitterRatio = 0.0)
        
        assertEquals(1000L, calculateReconnectDelay(1, policy))
    }

    @Test
    fun `calculateReconnectDelay should use linear backoff strategy`() {
        val policy = ReconnectPolicy.create(
            initialDelayMs = 1000L,
            backoffStrategy = ReconnectBackoffStrategy.LINEAR,
            jitterRatio = 0.0
        )
        
        assertEquals(1000L, calculateReconnectDelay(1, policy))
        assertEquals(2000L, calculateReconnectDelay(2, policy))
        assertEquals(3000L, calculateReconnectDelay(3, policy))
    }

    @Test
    fun `calculateReconnectDelay should use exponential backoff strategy`() {
        val policy = ReconnectPolicy.create(
            initialDelayMs = 1000L,
            backoffStrategy = ReconnectBackoffStrategy.EXPONENTIAL,
            backoffMultiplier = 2.0,
            jitterRatio = 0.0
        )
        
        assertEquals(1000L, calculateReconnectDelay(1, policy))
        assertEquals(2000L, calculateReconnectDelay(2, policy))
        assertEquals(4000L, calculateReconnectDelay(3, policy))
        assertEquals(8000L, calculateReconnectDelay(4, policy))
    }

    @Test
    fun `calculateReconnectDelay should use fixed backoff strategy`() {
        val policy = ReconnectPolicy.create(
            initialDelayMs = 1000L,
            backoffStrategy = ReconnectBackoffStrategy.FIXED,
            jitterRatio = 0.0
        )
        
        assertEquals(1000L, calculateReconnectDelay(1, policy))
        assertEquals(1000L, calculateReconnectDelay(2, policy))
        assertEquals(1000L, calculateReconnectDelay(3, policy))
    }

    @Test
    fun `calculateReconnectDelay should respect maxDelayMs`() {
        val policy = ReconnectPolicy.create(
            initialDelayMs = 1000L,
            maxDelayMs = 5000L,
            backoffStrategy = ReconnectBackoffStrategy.EXPONENTIAL,
            backoffMultiplier = 2.0,
            jitterRatio = 0.0
        )
        
        assertEquals(1000L, calculateReconnectDelay(1, policy))
        assertEquals(2000L, calculateReconnectDelay(2, policy))
        assertEquals(4000L, calculateReconnectDelay(3, policy))
        assertEquals(5000L, calculateReconnectDelay(4, policy))
        assertEquals(5000L, calculateReconnectDelay(5, policy))
    }

    @Test
    fun `calculateReconnectDelay should apply jitter when configured`() {
        val policy = ReconnectPolicy.create(
            initialDelayMs = 1000L,
            maxDelayMs = 30000L,
            backoffStrategy = ReconnectBackoffStrategy.EXPONENTIAL,
            backoffMultiplier = 2.0,
            jitterRatio = 0.1
        )
        
        val delay = calculateReconnectDelay(3, policy)
        
        // Delay should be within 10% of 4000ms (2^2 * 1000)
        // With jitter: 4000 +/- 10% = 3600..4400
        // Due to random jitter, allow wider range
        assertTrue(delay in 3500L..4500L, "Delay $delay should be within jitter range [3500, 4500]")
    }

    @Test
    fun `classifyStreamError should detect timeout errors`() {
        assertEquals(StreamErrorType.TIMEOUT, classifyStreamError("Connection timed out"))
        assertEquals(StreamErrorType.TIMEOUT, classifyStreamError("Socket timeout"))
        assertEquals(StreamErrorType.TIMEOUT, classifyStreamError("Read timed out"))
    }

    @Test
    fun `classifyStreamError should detect connection refused errors`() {
        assertEquals(StreamErrorType.CONNECTION_REFUSED, classifyStreamError("Connection refused"))
        assertEquals(StreamErrorType.CONNECTION_REFUSED, classifyStreamError("Connection reset"))
    }

    @Test
    fun `classifyStreamError should detect server unavailable errors`() {
        assertEquals(StreamErrorType.SERVER_UNAVAILABLE, classifyStreamError("Service unavailable"))
        assertEquals(StreamErrorType.SERVER_UNAVAILABLE, classifyStreamError("503 Service Unavailable"))
    }

    @Test
    fun `classifyStreamError should detect network errors`() {
        assertEquals(StreamErrorType.NETWORK_ERROR, classifyStreamError("Network unreachable"))
        assertEquals(StreamErrorType.NETWORK_ERROR, classifyStreamError("Socket exception"))
        assertEquals(StreamErrorType.NETWORK_ERROR, classifyStreamError("IO exception"))
    }

    @Test
    fun `classifyStreamError should return unknown for null or unrecognized errors`() {
        assertEquals(StreamErrorType.UNKNOWN, classifyStreamError(null))
        assertEquals(StreamErrorType.UNKNOWN, classifyStreamError("Some random error"))
        assertEquals(StreamErrorType.UNKNOWN, classifyStreamError(""))
    }

    @Test
    fun `shouldRetryError should return true for retryable errors`() {
        val policy = ReconnectPolicy.create(
            retryOnTimeout = true,
            retryOnConnectionRefused = true,
            retryOnServerUnavailable = true
        )
        
        assertTrue(shouldRetryError("Connection timed out", policy))
        assertTrue(shouldRetryError("Connection refused", policy))
        assertTrue(shouldRetryError("Service unavailable", policy))
        assertTrue(shouldRetryError("Network error", policy))
    }

    @Test
    fun `shouldRetryError should return false when error type is disabled`() {
        val policy = ReconnectPolicy.create(
            retryOnTimeout = false,
            retryOnConnectionRefused = true,
            retryOnServerUnavailable = true
        )
        
        assertFalse(shouldRetryError("Connection timed out", policy))
        assertTrue(shouldRetryError("Connection refused", policy))
    }

    @Test
    fun `shouldRetryError should return false when reconnect is disabled`() {
        val policy = ReconnectPolicy.create(
            enabled = false,
            retryOnTimeout = true
        )
        
        assertFalse(shouldRetryError("Connection timed out", policy))
    }

    @Test
    fun `shouldRetryError should return true for unknown errors by default`() {
        val policy = ReconnectPolicy.create(
            retryOnTimeout = false,
            retryOnConnectionRefused = false,
            retryOnServerUnavailable = false
        )
        
        assertTrue(shouldRetryError("Some random error", policy))
    }

    @Test
    fun `fromEnvironment should use default values when env vars not set`() {
        val policy = ReconnectPolicy.fromEnvironment()
        
        assertTrue(policy.maxAttempts in 1..20)
        assertTrue(policy.initialDelayMs >= 100)
    }
}