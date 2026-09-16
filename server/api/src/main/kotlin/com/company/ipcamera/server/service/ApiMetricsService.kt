package com.company.ipcamera.server.service

import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicLong

@Serializable
data class ApiMetricsSnapshot(
    val discoverRequests: Long,
    val discoverFallbackUsed: Long,
    val discoverFailures: Long,
    val authLoginSuccess: Long,
    val authLoginFailure: Long,
    val authRefreshSuccess: Long,
    val authRefreshFailure: Long,
    val authWsTokenSuccess: Long,
    val authWsTokenFailure: Long,
    val eventStatisticsSuccess: Long,
    val eventStatisticsFailure: Long
)

/**
 * Lightweight in-memory API metrics for ops diagnostics without external dependencies.
 */
class ApiMetricsService {
    private val discoverRequests = AtomicLong(0)
    private val discoverFallbackUsed = AtomicLong(0)
    private val discoverFailures = AtomicLong(0)

    private val authLoginSuccess = AtomicLong(0)
    private val authLoginFailure = AtomicLong(0)
    private val authRefreshSuccess = AtomicLong(0)
    private val authRefreshFailure = AtomicLong(0)
    private val authWsTokenSuccess = AtomicLong(0)
    private val authWsTokenFailure = AtomicLong(0)
    private val eventStatisticsSuccess = AtomicLong(0)
    private val eventStatisticsFailure = AtomicLong(0)

    fun markDiscoverRequest(usedFallback: Boolean) {
        discoverRequests.incrementAndGet()
        if (usedFallback) discoverFallbackUsed.incrementAndGet()
    }

    fun markDiscoverFailure() {
        discoverFailures.incrementAndGet()
    }

    fun markLoginSuccess() {
        authLoginSuccess.incrementAndGet()
    }

    fun markLoginFailure() {
        authLoginFailure.incrementAndGet()
    }

    fun markRefreshSuccess() {
        authRefreshSuccess.incrementAndGet()
    }

    fun markRefreshFailure() {
        authRefreshFailure.incrementAndGet()
    }

    fun markWsTokenSuccess() {
        authWsTokenSuccess.incrementAndGet()
    }

    fun markWsTokenFailure() {
        authWsTokenFailure.incrementAndGet()
    }

    fun markEventStatisticsSuccess() {
        eventStatisticsSuccess.incrementAndGet()
    }

    fun markEventStatisticsFailure() {
        eventStatisticsFailure.incrementAndGet()
    }

    fun snapshot(): ApiMetricsSnapshot = ApiMetricsSnapshot(
        discoverRequests = discoverRequests.get(),
        discoverFallbackUsed = discoverFallbackUsed.get(),
        discoverFailures = discoverFailures.get(),
        authLoginSuccess = authLoginSuccess.get(),
        authLoginFailure = authLoginFailure.get(),
        authRefreshSuccess = authRefreshSuccess.get(),
        authRefreshFailure = authRefreshFailure.get(),
        authWsTokenSuccess = authWsTokenSuccess.get(),
        authWsTokenFailure = authWsTokenFailure.get(),
        eventStatisticsSuccess = eventStatisticsSuccess.get(),
        eventStatisticsFailure = eventStatisticsFailure.get()
    )
}
