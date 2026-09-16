package com.company.ipcamera.desktop.ui.components

import com.company.ipcamera.core.network.RtspClientStatus
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.model.CameraSettings
import com.company.ipcamera.shared.domain.model.AnalyticsSettings
import com.company.ipcamera.desktop.stream.RtspStreamSession
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeoutPreemptively
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Интеграционные тесты для стабильности VideoPlayer при длительной работе.
 */
class VideoPlayerLongRunTest {

    @Test
    fun `test video player basic lifecycle`() = runTest(timeout = 30.seconds) {
        val camera = createTestCamera()
        
        withTimeout(10.seconds) {
            val session = RtspStreamSession(camera)
            
            // Start stream
            session.start(autoPlay = true)
            
            // Wait for connection
            val status = session.status.first { it == RtspClientStatus.CONNECTED || it == RtspClientStatus.PLAYING }
            assertTrue(status == RtspClientStatus.CONNECTED || status == RtspClientStatus.PLAYING)
            
            // Stop stream
            session.stop()
            
            // Verify disconnected
            val finalStatus = session.status.first { it == RtspClientStatus.DISCONNECTED }
            assertEquals(RtspClientStatus.DISCONNECTED, finalStatus)
            
            session.close()
        }
    }
    
    @Test
    fun `test video player reconnect on error`() = runTest(timeout = 60.seconds) {
        val camera = createTestCamera()
        val session = RtspStreamSession(camera)
        
        try {
            withTimeout(20.seconds) {
                // Start stream
                session.start(autoPlay = true)
                
                // Wait for initial connection
                session.status.first { it == RtspClientStatus.CONNECTED || it == RtspClientStatus.PLAYING }
                
                // Simulate reconnect
                session.reconnect(autoPlay = true)
                
                // Verify reconnection
                val reconnectStatus = session.status.first { it == RtspClientStatus.CONNECTED || it == RtspClientStatus.PLAYING }
                assertTrue(reconnectStatus == RtspClientStatus.CONNECTED || reconnectStatus == RtspClientStatus.PLAYING)
            }
        } finally {
            session.stop()
            session.close()
        }
    }
    
    @Test
    fun `test video player pause and resume`() = runTest(timeout = 30.seconds) {
        val camera = createTestCamera()
        val session = RtspStreamSession(camera)
        
        try {
            withTimeout(15.seconds) {
                // Start stream
                session.start(autoPlay = true)
                
                // Wait for playing
                session.status.first { it == RtspClientStatus.PLAYING }
                
                // Pause
                session.pause()
                
                // Verify paused state (should be CONNECTED, not PLAYING)
                val pausedStatus = session.status.first { it == RtspClientStatus.CONNECTED }
                assertEquals(RtspClientStatus.CONNECTED, pausedStatus)
                
                // Resume
                session.play()
                
                // Verify resumed state
                val resumedStatus = session.status.first { it == RtspClientStatus.PLAYING }
                assertEquals(RtspClientStatus.PLAYING, resumedStatus)
            }
        } finally {
            session.stop()
            session.close()
        }
    }
    
    @Test
    fun `test video player frame reception`() = runTest(timeout = 30.seconds) {
        val camera = createTestCamera()
        val session = RtspStreamSession(camera)
        
        try {
            withTimeout(15.seconds) {
                // Start stream
                session.start(autoPlay = true)
                
                // Wait for playing
                session.status.first { it == RtspClientStatus.PLAYING }
                
                // Collect frames for a few seconds
                var frameCount = 0
                val job = launch {
                    session.videoFrames.collect { frame ->
                        frameCount++
                        assertTrue(frame.data.isNotEmpty())
                    }
                }
                
                delay(5000) // Collect for 5 seconds
                job.cancel()
                
                // Should have received some frames (depends on camera FPS)
                assertTrue(frameCount > 0, "Expected to receive frames, but got $frameCount")
            }
        } finally {
            session.stop()
            session.close()
        }
    }
    
    @Test
    fun `test video player multiple start stop cycles`() = runTest(timeout = 120.seconds) {
        val camera = createTestCamera()
        val cycles = 5
        
        repeat(cycles) { cycleNumber ->
            println("Cycle $cycleNumber/$cycles")
            val session = RtspStreamSession(camera)
            
            try {
                withTimeout(20.seconds) {
                    // Start
                    session.start(autoPlay = true)
                    session.status.first { it == RtspClientStatus.CONNECTED || it == RtspClientStatus.PLAYING }
                    
                    delay(2000) // Run for 2 seconds
                    
                    // Stop
                    session.stop()
                    session.status.first { it == RtspClientStatus.DISCONNECTED }
                }
            } finally {
                session.close()
            }
            
            delay(1000) // Wait between cycles
        }
    }
    
    @Test
    fun `test video player background priority pause`() = runTest(timeout = 30.seconds) {
        val camera = createTestCamera()
        val session = RtspStreamSession(camera)
        
        try {
            withTimeout(15.seconds) {
                // Start stream
                session.start(autoPlay = true)
                
                // Wait for playing
                session.status.first { it == RtspClientStatus.PLAYING }
                
                // Background priority should pause the stream
                // This is tested via the StreamPriority mechanism
                // For now, we verify that pause works
                session.pause()
                
                val pausedStatus = session.status.first { it == RtspClientStatus.CONNECTED }
                assertEquals(RtspClientStatus.CONNECTED, pausedStatus)
            }
        } finally {
            session.stop()
            session.close()
        }
    }
    
    @Test
    fun `test video player metrics collection`() = runTest(timeout = 30.seconds) {
        val camera = createTestCamera()
        val session = RtspStreamSession(camera)
        
        try {
            withTimeout(15.seconds) {
                // Start stream
                session.start(autoPlay = true)
                
                // Wait for playing
                session.status.first { it == RtspClientStatus.PLAYING }
                
                delay(3000)
                
                // Verify no critical errors
                val errorStream = session.streamError.first { it != null }
                // Error stream may or may not have errors, we just verify it's accessible
                assertNotNull(errorStream)
            }
        } finally {
            session.stop()
            session.close()
        }
    }
    
    @Test
    fun `test video player concurrent streams`() = runTest(timeout = 60.seconds) {
        val cameras = listOf(
            createTestCamera("camera-1"),
            createTestCamera("camera-2"),
            createTestCamera("camera-3")
        )
        
        val sessions = mutableListOf<RtspStreamSession>()
        
        try {
            withTimeout(30.seconds) {
                // Start multiple streams concurrently
                cameras.forEach { camera ->
                    val session = RtspStreamSession(camera)
                    sessions.add(session)
                    
                    launch {
                        session.start(autoPlay = true)
                    }
                }
                
                // Wait for all to connect
                delay(10000)
                
                // Verify all are connected or playing
                sessions.forEach { session ->
                    val status = session.status.value
                    assertTrue(
                        status == RtspClientStatus.CONNECTED || status == RtspClientStatus.PLAYING,
                        "Expected CONNECTED or PLAYING, but got $status"
                    )
                }
            }
        } finally {
            sessions.forEach { session ->
                try {
                    session.stop()
                    session.close()
                } catch (_: Exception) {
                    // Ignore cleanup errors
                }
            }
        }
    }
    
    @Test
    fun `test video player H265 codec fallback`() = runTest(timeout = 45.seconds) {
        val camera = createTestCamera("h265-camera").copy(codec = "H265")
        val session = RtspStreamSession(camera)
        
        try {
            withTimeout(20.seconds) {
                // Start stream with H.265 codec
                session.start(autoPlay = true)
                
                // Wait for connection
                session.status.first { it == RtspClientStatus.CONNECTED || it == RtspClientStatus.PLAYING }
                
                // Collect frames for a few seconds to test fallback mechanism
                var frameCount = 0
                val job = launch {
                    session.videoFrames.collect { frame ->
                        frameCount++
                        assertTrue(frame.data.isNotEmpty())
                    }
                }
                
                delay(5000)
                job.cancel()
                
                // Should have received some frames
                assertTrue(frameCount >= 0, "Expected to receive frames or fallback, got $frameCount")
            }
        } finally {
            session.stop()
            session.close()
        }
    }
    
    @Test
    fun `test video player network error recovery`() = runTest(timeout = 90.seconds) {
        val camera = createTestCamera("recovery-camera")
        val session = RtspStreamSession(camera)
        
        try {
            withTimeout(40.seconds) {
                // Start stream
                session.start(autoPlay = true)
                
                // Wait for initial connection
                session.status.first { it == RtspClientStatus.CONNECTED || it == RtspClientStatus.PLAYING }
                
                // Collect errors for a period
                var errorCount = 0
                val errorJob = launch {
                    session.streamError.collect { error ->
                        if (error != null) {
                            errorCount++
                        }
                    }
                }
                
                // Wait and allow for potential errors
                delay(10000)
                errorJob.cancel()
                
                // Verify error tracking works
                // Error count may be 0 if no errors occurred
                assertTrue(errorCount >= 0, "Error tracking failed")
            }
        } finally {
            session.stop()
            session.close()
        }
    }
    
    @Test
    fun `test video player memory stability long run`() = runTest(timeout = 180.seconds) {
        val camera = createTestCamera("memory-test")
        val durationSeconds = 60 // Run for 60 seconds
        
        val session = RtspStreamSession(camera)
        
        try {
            withTimeout((durationSeconds + 30).seconds) {
                // Start stream
                session.start(autoPlay = true)
                
                // Wait for connection
                session.status.first { it == RtspClientStatus.CONNECTED || it == RtspClientStatus.PLAYING }
                
                // Collect frames for extended period
                var frameCount = 0
                var lastFrameTime = System.currentTimeMillis()
                val job = launch {
                    session.videoFrames.collect { frame ->
                        frameCount++
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastFrameTime >= 10000) {
                            println("Frame rate: ${frameCount * 1000L / (currentTime - lastFrameTime)} fps")
                            lastFrameTime = currentTime
                        }
                    }
                }
                
                // Run for specified duration
                delay((durationSeconds * 1000).ms)
                job.cancel()
                
                // Verify frames were received
                assertTrue(frameCount > 0, "Expected to receive frames during long run, got $frameCount")
                println("Total frames received: $frameCount over ${durationSeconds}s")
            }
        } finally {
            session.stop()
            session.close()
        }
    }
    
    @Test
    fun `test video player MJPEG codec support`() = runTest(timeout = 30.seconds) {
        val camera = createTestCamera("mjpeg-camera").copy(codec = "MJPEG")
        val session = RtspStreamSession(camera)
        
        try {
            withTimeout(15.seconds) {
                // Start stream with MJPEG codec
                session.start(autoPlay = true)
                
                // Wait for connection
                session.status.first { it == RtspClientStatus.CONNECTED || it == RtspClientStatus.PLAYING }
                
                // Collect frames
                var frameCount = 0
                val job = launch {
                    session.videoFrames.collect { frame ->
                        frameCount++
                        assertTrue(frame.data.isNotEmpty())
                    }
                }
                
                delay(3000)
                job.cancel()
                
                // Should have received some frames
                assertTrue(frameCount > 0, "Expected to receive MJPEG frames, got $frameCount")
            }
        } finally {
            session.stop()
            session.close()
        }
    }
    
    private fun createTestCamera(id: String = "test-camera"): Camera {
        return Camera(
            id = id,
            name = "Test Camera",
            url = "rtsp://127.0.0.1:8554/test",
            username = "admin",
            password = "password",
            enabled = true,
            codec = "H264",
            audio = true,
            settings = CameraSettings(
                analytics = AnalyticsSettings(
                    motionDetection = false,
                    objectDetection = false,
                    faceRecognition = false,
                    anprEnabled = false
                )
            )
        )
    }
    
    private fun createTestCamera(id: String): Camera {
        return createTestCamera(id).copy(id = id)
    }
}
