package com.company.ipcamera.core.uibridge

import com.company.ipcamera.core.common.model.Resolution
import com.company.ipcamera.shared.domain.model.Camera
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class PerformanceTest {

    @Test
    fun testCameraListLoadingPerformance() = runTest {
        val bridge = DesktopCameraBridge(null, null, null, null, null, null, null, null)
        val iterations = 100

        val startTime = System.currentTimeMillis()
        repeat(iterations) {
            bridge.getCameras()
        }
        val endTime = System.currentTimeMillis()

        val avgTime = (endTime - startTime) / iterations
        println("Average camera list load time: ${avgTime}ms")

        assertTrue(avgTime < 10, "Camera list loading should be fast, got ${avgTime}ms")
    }

    @Test
    fun testCameraAddPerformance() = runTest {
        val bridge = DesktopCameraBridge(null, null, null, null, null, null, null, null)
        val camera = Camera(
            id = "1",
            name = "Test",
            url = "rtsp://192.168.1.100:554/stream",
            username = "admin",
            password = "pass",
            resolution = Resolution(1920, 1080),
            fps = 30
        )
        val iterations = 100

        val startTime = System.currentTimeMillis()
        repeat(iterations) {
            bridge.addCamera(camera)
        }
        val endTime = System.currentTimeMillis()

        val avgTime = (endTime - startTime) / iterations
        println("Average camera add time: ${avgTime}ms")

        assertTrue(avgTime < 50, "Camera add should complete in under 50ms, got ${avgTime}ms")
    }

    @Test
    fun testRecordingStartPerformance() = runTest {
        val bridge = DesktopRecordingBridge(null, null, null, null, null, null)
        val iterations = 100

        val startTime = System.currentTimeMillis()
        repeat(iterations) {
            bridge.startRecording("camera1")
        }
        val endTime = System.currentTimeMillis()

        val avgTime = (endTime - startTime) / iterations
        println("Average recording start time: ${avgTime}ms")

        assertTrue(avgTime < 100, "Recording start should complete in under 100ms, got ${avgTime}ms")
    }

    @Test
    fun testAuthenticationLoginPerformance() = runTest {
        val bridge = DesktopAuthenticationBridge(null, null)
        val iterations = 100

        val startTime = System.currentTimeMillis()
        repeat(iterations) {
            bridge.login("testuser", "password123")
        }
        val endTime = System.currentTimeMillis()

        val avgTime = (endTime - startTime) / iterations
        println("Average login time: ${avgTime}ms")

        assertTrue(avgTime < 200, "Login should complete in under 200ms, got ${avgTime}ms")
    }

    @Test
    fun testEventDetectionPerformance() = runTest {
        val bridge = DesktopEventBridge(null, null, null, null, null)
        val iterations = 100

        val startTime = System.currentTimeMillis()
        repeat(iterations) {
            bridge.detectMotion("camera1")
        }
        val endTime = System.currentTimeMillis()

        val avgTime = (endTime - startTime) / iterations
        println("Average motion detection time: ${avgTime}ms")

        assertTrue(avgTime < 50, "Motion detection should complete in under 50ms, got ${avgTime}ms")
    }

    @Test
    fun testConcurrentBridgeOperations() = runTest {
        val authBridge = DesktopAuthenticationBridge(null, null)
        val cameraBridge = DesktopCameraBridge(null, null, null, null, null, null, null, null)
        val recordingBridge = DesktopRecordingBridge(null, null, null, null, null, null)

        val iterations = 50

        val startTime = System.currentTimeMillis()
        
        // Simulate concurrent operations
        val job1 = async {
            repeat(iterations) {
                authBridge.login("user", "pass")
            }
        }
        
        val job2 = async {
            repeat(iterations) {
                cameraBridge.getCameras()
            }
        }
        
        val job3 = async {
            repeat(iterations) {
                recordingBridge.startRecording("camera1")
            }
        }

        job1.await()
        job2.await()
        job3.await()

        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime

        println("Concurrent operations total time: ${totalTime}ms")
        assertTrue(totalTime < 5000, "Concurrent operations should complete in under 5s, got ${totalTime}ms")
    }

    @Test
    fun testBridgeInstantiationPerformance() {
        val iterations = 100

        val startTime = System.currentTimeMillis()
        repeat(iterations) {
            createUiBridge()
        }
        val endTime = System.currentTimeMillis()

        val avgTime = (endTime - startTime) / iterations
        println("Average bridge instantiation time: ${avgTime}ms")

        assertTrue(avgTime < 100, "Bridge instantiation should be fast, got ${avgTime}ms")
    }
}
