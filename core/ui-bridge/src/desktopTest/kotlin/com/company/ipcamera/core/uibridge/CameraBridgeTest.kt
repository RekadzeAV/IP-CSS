package com.company.ipcamera.core.uibridge

import com.company.ipcamera.core.common.model.Resolution
import com.company.ipcamera.shared.domain.model.Camera
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CameraBridgeTest {

    private lateinit var bridge: CameraBridge

    @Test
    fun testGetCamerasReturnsEmptyList() = runTest {
        bridge = DesktopCameraBridge(null, null, null, null, null, null, null, null)

        val cameras = bridge.getCameras()

        assertTrue(cameras.isEmpty())
    }

    @Test
    fun testAddCameraSuccess() = runTest {
        bridge = DesktopCameraBridge(null, null, null, null, null, null, null, null)

        val camera = Camera(
            id = "1",
            name = "Test Camera",
            url = "rtsp://192.168.1.100:554/stream",
            username = "admin",
            password = "password",
            resolution = Resolution(width = 1920, height = 1080),
            fps = 30
        )

        val result = bridge.addCamera(camera)

        assertTrue(result.isSuccess)
        assertEquals(camera.id, result.getOrNull()?.id)
    }

    @Test
    fun testUpdateCameraSuccess() = runTest {
        bridge = DesktopCameraBridge(null, null, null, null, null, null, null, null)

        val result = bridge.updateCamera(
            Camera(
                id = "1",
                name = "Updated Camera",
                url = "rtsp://192.168.1.100:554/stream",
                username = "admin",
                password = "password",
                resolution = Resolution(width = 1920, height = 1080),
                fps = 30
            )
        )

        assertTrue(result.isSuccess)
    }

    @Test
    fun testDeleteCameraSuccess() = runTest {
        bridge = DesktopCameraBridge(null, null, null, null, null, null, null, null)

        val result = bridge.deleteCamera("1")

        assertTrue(result.isSuccess)
    }

    @Test
    fun testDiscoverCamerasReturnsEmptyList() = runTest {
        bridge = DesktopCameraBridge(null, null, null, null, null, null, null, null)

        val cameras = bridge.discoverCameras()

        assertTrue(cameras.isEmpty())
    }

    @Test
    fun testTestCameraSuccess() = runTest {
        bridge = DesktopCameraBridge(null, null, null, null, null, null, null, null)

        val camera = Camera(
            id = "1",
            name = "Test Camera",
            url = "rtsp://192.168.1.100:554/stream",
            username = "admin",
            password = "password",
            resolution = Resolution(width = 1920, height = 1080),
            fps = 30
        )

        val result = bridge.testCamera(camera)

        assertIs<CameraTestResult.Success>(result)
        assertEquals(camera.url, result.streamUrl)
        assertEquals("1920x1080", result.resolution)
    }

    @Test
    fun testControlPtzSuccess() = runTest {
        bridge = DesktopCameraBridge(null, null, null, null, null, null, null, null)

        val result = bridge.controlPtz("1", PtzDirection.UP, 1.0f)

        assertTrue(result.isSuccess)
    }

    @Test
    fun testPtzDirectionValues() {
        assertEquals(6, PtzDirection.values().size)
        assertTrue(PtzDirection.values().contains(PtzDirection.UP))
        assertTrue(PtzDirection.values().contains(PtzDirection.DOWN))
        assertTrue(PtzDirection.values().contains(PtzDirection.LEFT))
        assertTrue(PtzDirection.values().contains(PtzDirection.RIGHT))
        assertTrue(PtzDirection.values().contains(PtzDirection.ZOOM_IN))
        assertTrue(PtzDirection.values().contains(PtzDirection.ZOOM_OUT))
    }

    @Test
    fun testCameraProfileCreation() {
        val profile = CameraProfile(
            name = "HD Profile",
            resolution = "1920x1080",
            fps = 30,
            bitrate = 4000
        )

        assertEquals("HD Profile", profile.name)
        assertEquals("1920x1080", profile.resolution)
        assertEquals(30, profile.fps)
        assertEquals(4000, profile.bitrate)
    }
}
