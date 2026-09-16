package com.company.ipcamera.server.service

import com.company.ipcamera.core.network.OnvifClient
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.model.PTZConfig
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Юнит-тесты [CameraControlService]: маппинг команд на ONVIF-вызовы,
 * валидация PTZ-конфигурации, clamp скорости, обработка отказов камеры.
 */
class CameraControlServiceTest {

    private val onvifClient: OnvifClient = mockk(relaxed = true)

    private val service = CameraControlService(onvifClient = onvifClient, commandTimeoutMs = 2000)

    private fun camera(ptzEnabled: Boolean = true): Camera = Camera(
        id = "cam-1",
        name = "Test Camera",
        url = "rtsp://192.168.1.100:554/stream",
        username = "admin",
        password = "secret",
        ptz = PTZConfig(enabled = ptzEnabled)
    )

    @Test
    fun `fails when PTZ is not enabled`() = runTest {
        val result = service.execute(camera(ptzEnabled = false), "up")

        assertTrue(result is CameraControlService.ControlResult.Failure)
        assertTrue(result.message.startsWith("PTZ is not enabled"))
    }

    @Test
    fun `fails on unsupported action`() = runTest {
        val result = service.execute(camera(), "spin_around")

        assertTrue(result is CameraControlService.ControlResult.Failure)
        assertTrue(result.message.startsWith("Unsupported action"))
    }

    @Test
    fun `preset action requires preset parameter`() = runTest {
        val result = service.execute(camera(), "preset")

        assertTrue(result is CameraControlService.ControlResult.Failure)
        assertTrue(result.message.startsWith("Preset command requires"))
    }

    @Test
    fun `move up maps to movePtz with UP direction`() = runTest {
        coEvery { onvifClient.movePtz(any(), any(), any(), any(), any()) } returns true

        val result = service.execute(camera(), "up", mapOf("speed" to "0.8"))

        assertTrue(result is CameraControlService.ControlResult.Success)
        coVerify {
            onvifClient.movePtz(
                url = "rtsp://192.168.1.100:554/stream",
                direction = com.company.ipcamera.core.network.PtzDirection.UP,
                speed = 0.8f,
                username = "admin",
                password = "secret",
            )
        }
    }

    @Test
    fun `speed is clamped to valid range`() = runTest {
        coEvery { onvifClient.movePtz(any(), any(), any(), any(), any()) } returns true

        service.execute(camera(), "left", mapOf("speed" to "99"))

        coVerify { onvifClient.movePtz(any(), any(), speed = 1.0f, username = any(), password = any()) }
    }

    @Test
    fun `camera rejection produces Failure`() = runTest {
        coEvery { onvifClient.movePtz(any(), any(), any(), any(), any()) } returns false

        val result = service.execute(camera(), "right")

        assertTrue(result is CameraControlService.ControlResult.Failure)
        assertTrue(result.message.contains("rejected"))
    }

    @Test
    fun `zoom_in maps to zoomIn`() = runTest {
        coEvery { onvifClient.zoomIn(any(), any(), any(), any(), any()) } returns true

        val result = service.execute(camera(), "zoom_in")

        assertTrue(result is CameraControlService.ControlResult.Success)
        coVerify { onvifClient.zoomIn(any(), any(), username = "admin", password = "secret") }
    }

    @Test
    fun `zoom_out maps to zoomOut`() = runTest {
        coEvery { onvifClient.zoomOut(any(), any(), any(), any(), any()) } returns true

        val result = service.execute(camera(), "zoom-out")

        assertTrue(result is CameraControlService.ControlResult.Success)
        coVerify { onvifClient.zoomOut(any(), any(), username = "admin", password = "secret") }
    }

    @Test
    fun `stop maps to stopPtz`() = runTest {
        coEvery { onvifClient.stopPtz(any(), any(), any(), any()) } returns true

        val result = service.execute(camera(), "stop")

        assertTrue(result is CameraControlService.ControlResult.Success)
        coVerify { onvifClient.stopPtz(any(), username = "admin", password = "secret") }
    }

    @Test
    fun `preset uses token parameter`() = runTest {
        coEvery { onvifClient.gotoPreset(any(), any(), any(), any(), any()) } returns true

        val result = service.execute(camera(), "preset", mapOf("preset" to "3"))

        assertTrue(result is CameraControlService.ControlResult.Success)
        coVerify {
            onvifClient.gotoPreset(
                url = "rtsp://192.168.1.100:554/stream",
                presetToken = "3",
                username = "admin",
                password = "secret",
            )
        }
    }

    @Test
    fun `diagonal actions map to combined directions`() = runTest {
        coEvery { onvifClient.movePtz(any(), any(), any(), any(), any()) } returns true
        val dirSlot = slot<com.company.ipcamera.core.network.PtzDirection>()

        service.execute(camera(), "down_left")

        coVerify { onvifClient.movePtz(any(), capture(dirSlot), any(), any(), any()) }
        assertEquals(com.company.ipcamera.core.network.PtzDirection.DOWN_LEFT, dirSlot.captured)
    }
}
