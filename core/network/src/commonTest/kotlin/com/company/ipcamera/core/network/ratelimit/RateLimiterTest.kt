package com.company.ipcamera.core.network.ratelimit

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration

class RateLimiterTest {

    @Test
    fun testTokenBucketRateLimiter() = runTest {
        val limiter = TokenBucketRateLimiter(
            maxTokens = 5,
            refillRate = Duration.parse("1s"),
            tokensPerRefill = 1
        )

        // Первые 5 запросов должны быть разрешены
        for (i in 1..5) {
            assertTrue(limiter.acquire())
        }

        // 6-й запрос должен быть заблокирован
        assertFalse(limiter.acquire())
    }

    @Test
    fun testFixedIntervalRateLimiter() = runTest {
        val limiter = FixedIntervalRateLimiter(
            interval = Duration.parse("1s"),
            maxRequests = 2
        )

        // Первые 2 запроса должны быть разрешены
        assertTrue(limiter.acquire())
        assertTrue(limiter.acquire())

        // 3-й запрос должен быть заблокирован
        assertFalse(limiter.acquire())
    }

    @Test
    fun testGetWaitTime() = runTest {
        val limiter = TokenBucketRateLimiter(
            maxTokens = 1,
            refillRate = Duration.parse("100ms"),
            tokensPerRefill = 1
        )

        assertTrue(limiter.acquire())
        assertFalse(limiter.acquire())

        val waitTime = limiter.getWaitTime()
        assertTrue(waitTime > Duration.ZERO)
        assertTrue(waitTime <= Duration.parse("100ms"))
    }
}

