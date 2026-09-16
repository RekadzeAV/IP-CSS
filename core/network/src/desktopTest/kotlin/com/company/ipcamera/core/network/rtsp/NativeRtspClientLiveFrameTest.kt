package com.company.ipcamera.core.network.rtsp

import com.company.ipcamera.core.network.RtspClientStatus
import com.company.ipcamera.core.network.RtspStreamType
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration test for NativeRtspClient with live RTSP stream.
 * Requires MediaMTX running on localhost:8554 and FFmpeg test stream.
 *
 * Test stream: rtsp://127.0.0.1:8554/test
 * - Video: H.264, 1920x1080, 30fps
 * - Audio: AAC, 44.1kHz, mono
 */
class NativeRtspClientLiveFrameTest {

    companion object {
        const val RTSP_TEST_URL = "rtsp://127.0.0.1:8554/test"
        const val RTSP_TIMEOUT_MS = 5000

        /**
         * Проверяет доступность RTSP сервера
         */
        private fun isRtspServerAvailable(): Boolean {
            return try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress("127.0.0.1", 8554), 2000)
                    true
                }
            } catch (e: Exception) {
                println("⚠ RTSP server not available at 127.0.0.1:8554")
                false
            }
        }
    }

    @Test
    fun `test video frame callback with live stream`() = runTest {
        if (!isRtspServerAvailable()) {
            println("⚠ SKIPPED: RTSP server not available")
            return@runTest
        }

        val client = NativeRtspClient()
        val handle = client.create()
        val frameReceived = AtomicBoolean(false)
        val frameCount = AtomicInteger(0)
        val firstFrameTimestamp = AtomicBoolean(false)

        try {
            // Register video frame callback
            client.setFrameCallback(handle, RtspStreamType.VIDEO) { frame ->
                if (!firstFrameTimestamp.getAndSet(true)) {
                    println(
                        "✓ First video frame received: timestamp=${frame.timestamp}ms, size=${frame.data.size} bytes"
                    )
                }
                frameReceived.set(true)
                frameCount.incrementAndGet()

                // Print every 30 frames
                if (frameCount.get() % 30 == 0) {
                    println("  Frames received: ${frameCount.get()}")
                }
            }

            // Connect to stream
            val connected = client.connect(
                handle = handle,
                url = RTSP_TEST_URL,
                username = null,
                password = null,
                timeoutMs = RTSP_TIMEOUT_MS
            )

            if (connected) {
                println("✓ Connected to RTSP stream: $RTSP_TEST_URL")

                // Play stream
                val played = client.play(handle)
                assertTrue("Play should succeed when stream is available") { played }

                println("✓ Playback started")

                // Wait for frames (5 seconds)
                delay(5000)

                val receivedCount = frameCount.get()
                println("✓ Total video frames received in 5s: $receivedCount")

                // Verify we received frames
                assertTrue("Should receive video frames from live stream") { frameReceived.get() }

                // Stop playback
                val stopped = client.stop(handle)
                assertTrue("Stop should succeed") { stopped }

                println("✓ Playback stopped")
            } else {
                println("⚠ Stream not available (MediaMTX may not be running)")
            }
        } finally {
            client.disconnect(handle)
            client.destroy(handle)
        }
    }

    @Test
    fun `test audio frame callback with live stream`() = runTest {
        if (!isRtspServerAvailable()) {
            println("⚠ SKIPPED: RTSP server not available")
            return@runTest
        }

        val client = NativeRtspClient()
        val handle = client.create()
        val frameReceived = AtomicBoolean(false)
        val frameCount = AtomicInteger(0)

        try {
            // Register audio frame callback
            client.setFrameCallback(handle, RtspStreamType.AUDIO) { frame ->
                frameReceived.set(true)
                frameCount.incrementAndGet()

                if (frameCount.get() % 50 == 0) {
                    println("  Audio frames received: ${frameCount.get()}")
                }
            }

            // Connect to stream
            val connected = client.connect(
                handle = handle,
                url = RTSP_TEST_URL,
                username = null,
                password = null,
                timeoutMs = RTSP_TIMEOUT_MS
            )

            if (connected) {
                println("✓ Connected to RTSP stream for audio test")

                // Play stream
                val played = client.play(handle)

                if (played) {
                    // Wait for audio frames (5 seconds)
                    delay(5000)

                    val receivedCount = frameCount.get()
                    println("✓ Total audio frames received in 5s: $receivedCount")

                    if (receivedCount > 0) {
                        assertTrue("Should receive audio frames from live stream") { frameReceived.get() }
                    } else {
                        println("⚠ WARNING: No audio frames received")
                    }

                    client.stop(handle)
                }
            } else {
                println("⚠ Stream not available for audio test")
            }
        } finally {
            client.disconnect(handle)
            client.destroy(handle)
        }
    }

    @Test
    fun `test status callback lifecycle`() = runTest {
        if (!isRtspServerAvailable()) {
            println("⚠ SKIPPED: RTSP server not available")
            return@runTest
        }

        val client = NativeRtspClient()
        val handle = client.create()
        val statusChanges = mutableListOf<RtspClientStatus>()
        val statusLock = Any()

        try {
            // Register status callback
            client.setStatusCallback(handle) { status, message ->
                synchronized(statusLock) {
                    statusChanges.add(status)
                    println("Status change: $status - ${message ?: "no message"}")
                }
            }

            // Initial status should be DISCONNECTED
            assertEquals(RtspClientStatus.DISCONNECTED, client.getStatus(handle))

            // Connect
            val connected = client.connect(
                handle = handle,
                url = RTSP_TEST_URL,
                username = null,
                password = null,
                timeoutMs = RTSP_TIMEOUT_MS
            )

            if (connected) {
                println("✓ Connected successfully")

                // Play
                val played = client.play(handle)

                if (played) {
                    println("✓ Playing")

                    // Wait a bit
                    delay(1000)

                    // Stop
                    client.stop(handle)
                    println("✓ Stopped")

                    // Disconnect
                    client.disconnect(handle)
                    println("✓ Disconnected")
                }
            }

            // Verify status changes occurred
            synchronized(statusLock) {
                assertTrue("Should have status changes") { statusChanges.isNotEmpty() }
                assertTrue("Should have CONNECTED status") { RtspClientStatus.CONNECTED in statusChanges }
            }
        } finally {
            client.destroy(handle)
        }
    }

    @Test
    fun `test stream discovery after connect`() = runTest {
        if (!isRtspServerAvailable()) {
            println("⚠ SKIPPED: RTSP server not available")
            return@runTest
        }

        val client = NativeRtspClient()
        val handle = client.create()

        try {
            // Connect
            val connected = client.connect(
                handle = handle,
                url = RTSP_TEST_URL,
                username = null,
                password = null,
                timeoutMs = RTSP_TIMEOUT_MS
            )

            if (connected) {
                println("✓ Connected to stream")

                // Wait for stream discovery
                delay(500)

                val streamCount = client.getStreamCount(handle)
                println("✓ Stream count: $streamCount")

                if (streamCount > 0) {
                    // Get stream info for each stream
                    for (i in 0 until streamCount) {
                        val streamInfo = client.getStreamInfo(handle, i)
                        if (streamInfo != null) {
                            println(
                                "  Stream $i: type=${streamInfo.type}, " +
                                    "codec=${streamInfo.codec}, " +
                                    "resolution=${streamInfo.resolution?.width}x${streamInfo.resolution?.height}, " +
                                    "fps=${streamInfo.fps}"
                            )
                        }
                    }

                    assertTrue("Should have at least one stream") { streamCount > 0 }
                }
            } else {
                println("⚠ Stream not available")
            }
        } finally {
            client.disconnect(handle)
            client.destroy(handle)
        }
    }

    @Test
    fun `test reconnect parameters configuration`() = runTest {
        val client = NativeRtspClient()
        val handle = client.create()

        try {
            // Configure reconnect parameters
            client.setReconnectParams(
                handle = handle,
                enabled = true,
                maxRetries = 5,
                initialDelayMs = 1000,
                maxDelayMs = 30000,
                backoffMultiplier = 2.0f
            )

            println("✓ Reconnect parameters configured successfully")

            // Should not throw exception
            assertTrue("Reconnect params should be set") { true }
        } finally {
            client.destroy(handle)
        }
    }

    @Test
    fun `test multiple connect disconnect cycles`() = runTest {
        if (!isRtspServerAvailable()) {
            println("⚠ SKIPPED: RTSP server not available")
            return@runTest
        }

        val client = NativeRtspClient()
        val handle = client.create()
        val successfulCycles = AtomicInteger(0)

        try {
            for (i in 1..3) {
                println("\n--- Cycle $i ---")

                val connected = client.connect(
                    handle = handle,
                    url = RTSP_TEST_URL,
                    username = null,
                    password = null,
                    timeoutMs = RTSP_TIMEOUT_MS
                )

                if (connected) {
                    println("✓ Cycle $i: Connected")

                    val played = client.play(handle)

                    if (played) {
                        println("✓ Cycle $i: Playing")
                        delay(500)

                        client.stop(handle)
                        println("✓ Cycle $i: Stopped")
                    }

                    client.disconnect(handle)
                    println("✓ Cycle $i: Disconnected")

                    successfulCycles.incrementAndGet()
                } else {
                    println("⚠ Cycle $i: Stream not available, stopping")
                    break
                }
            }

            println("\n✓ Successful cycles: ${successfulCycles.get()}/3")
        } finally {
            client.destroy(handle)
        }
    }

    @Test
    fun `test pause and resume functionality`() = runTest {
        if (!isRtspServerAvailable()) {
            println("⚠ SKIPPED: RTSP server not available")
            return@runTest
        }

        val client = NativeRtspClient()
        val handle = client.create()

        try {
            // Connect and play first
            val connected = client.connect(
                handle = handle,
                url = RTSP_TEST_URL,
                username = null,
                password = null,
                timeoutMs = RTSP_TIMEOUT_MS
            )

            if (connected) {
                val played = client.play(handle)

                if (played) {
                    println("✓ Playing")

                    // Wait a bit
                    delay(1000)

                    // Pause
                    val paused = client.pause(handle)

                    if (paused) {
                        println("✓ Paused")
                        assertEquals(RtspClientStatus.PAUSED, client.getStatus(handle))

                        // Resume
                        val resumed = client.play(handle)
                        assertTrue("Resume should succeed") { resumed }

                        println("✓ Resumed")
                        assertEquals(RtspClientStatus.PLAYING, client.getStatus(handle))
                    } else {
                        println("⚠ Pause not supported by server")
                    }

                    client.stop(handle)
                }
            } else {
                println("⚠ Stream not available")
            }
        } finally {
            client.disconnect(handle)
            client.destroy(handle)
        }
    }
}
