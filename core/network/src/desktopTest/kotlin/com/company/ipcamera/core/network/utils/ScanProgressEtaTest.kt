package com.company.ipcamera.core.network.utils

import com.company.ipcamera.core.network.NetworkScanProgress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Юнит-тесты расчёта оставшегося времени сканирования ([ScannerUtils]).
 */
class ScanProgressEtaTest {

    private fun progress(progress: Float, elapsedMs: Long?) = NetworkScanProgress(
        currentActivity = "test",
        progress = progress,
        hostsScanned = (progress * 100).toInt(),
        hostsTotal = 100,
        camerasFound = 0,
        discoveredCameras = emptyList(),
        elapsedMs = elapsedMs
    )

    @Test
    fun `remainingTimeSeconds is null without elapsedMs`() {
        assertNull(progress(0.5f, null).remainingTimeSeconds)
        assertNull(progress(0.5f, null).remainingTimeFormatted)
    }

    @Test
    fun `remainingTimeSeconds is null at zero progress`() {
        assertNull(progress(0f, 1000L).remainingTimeSeconds)
    }

    @Test
    fun `half done half remaining`() {
        // 10s прошло на 50% → осталось 10s
        assertEquals(10L, progress(0.5f, 10_000L).remainingTimeSeconds)
    }

    @Test
    fun `quarter done three quarters remaining`() {
        // 5s прошло на 25% → всего 20s, осталось 15s
        assertEquals(15L, progress(0.25f, 5_000L).remainingTimeSeconds)
    }

    @Test
    fun `completed scan has zero remaining`() {
        assertEquals(0L, progress(1f, 30_000L).remainingTimeSeconds)
    }

    @Test
    fun `formatted output uses minutes and seconds`() {
        assertEquals("<1с", progress(0.99f, 60_000L).remainingTimeFormatted)
        assertEquals("45с", progress(0.25f, 15_000L).remainingTimeFormatted)
        assertEquals("2м 30с", progress(0.25f, 50_000L).remainingTimeFormatted)
    }

    @Test
    fun `never negative`() {
        val eta = progress(0.9f, 1L).remainingTimeSeconds
        assertTrue(eta != null && eta >= 0L)
    }
}
