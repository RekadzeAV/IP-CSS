package com.company.ipcamera.server.service

import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.repository.CameraRepository
import com.company.ipcamera.shared.domain.service.CameraEventMonitoringService
import com.company.ipcamera.shared.domain.service.OnvifEventIntegrationService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class CameraEventMonitoringServiceJvmTest {

    private val camera = Camera(
        id = "cam-1",
        name = "Test Camera",
        url = "http://192.168.1.50",
        username = "admin",
        password = "secret"
    )

    @Test
    fun `ensureMonitoring is idempotent`() = runBlocking {
        val cameraRepository = mockk<CameraRepository>()
        val integrationService = mockk<OnvifEventIntegrationService>()
        val service = CameraEventMonitoringService(
            cameraRepository = cameraRepository,
            eventIntegrationService = integrationService,
            scope = CoroutineScope(Dispatchers.Default + Job()),
            retryIntervalMs = 60_000L
        )

        coEvery { cameraRepository.getCameras() } returns listOf(camera)
        coEvery { integrationService.startMonitoring(any(), any(), any(), any(), any(), any()) } returns Result.success(Unit)
        every { integrationService.getMonitoredCameras() } returns listOf(camera.id)

        service.initialize()
        delay(120)
        service.ensureMonitoring()
        delay(120)
        service.shutdown()

        coVerify(exactly = 1) {
            integrationService.startMonitoring(
                cameraId = camera.id,
                cameraName = camera.name,
                cameraUrl = camera.url,
                username = camera.username,
                password = camera.password,
                pullInterval = 5000
            )
        }
    }

    @Test
    fun `ensureMonitoring retries after transient start failure`() = runBlocking {
        val cameraRepository = mockk<CameraRepository>()
        val integrationService = mockk<OnvifEventIntegrationService>()
        val service = CameraEventMonitoringService(
            cameraRepository = cameraRepository,
            eventIntegrationService = integrationService,
            scope = CoroutineScope(Dispatchers.Default + Job()),
            retryIntervalMs = 60_000L
        )

        coEvery { cameraRepository.getCameras() } returns listOf(camera)
        coEvery { integrationService.startMonitoring(any(), any(), any(), any(), any(), any()) } returnsMany listOf(
            Result.failure(RuntimeException("transient network error")),
            Result.success(Unit)
        )
        every { integrationService.getMonitoredCameras() } returnsMany listOf(emptyList(), emptyList(), listOf(camera.id))

        service.initialize()
        delay(120)
        service.ensureMonitoring()
        delay(120)
        service.shutdown()

        coVerify(atLeast = 2) { integrationService.startMonitoring(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `race add remove camera during ensureMonitoring does not crash`() = runBlocking {
        val cameraRepository = mockk<CameraRepository>()
        val integrationService = mockk<OnvifEventIntegrationService>()
        val service = CameraEventMonitoringService(
            cameraRepository = cameraRepository,
            eventIntegrationService = integrationService,
            scope = CoroutineScope(Dispatchers.Default + Job()),
            retryIntervalMs = 60_000L
        )

        coEvery { integrationService.startMonitoring(any(), any(), any(), any(), any(), any()) } returns Result.success(Unit)
        every { integrationService.getMonitoredCameras() } returnsMany listOf(emptyList(), listOf(camera.id), emptyList(), listOf(camera.id))
        coEvery { cameraRepository.getCameras() } returnsMany listOf(
            listOf(camera),
            emptyList(),
            listOf(camera),
            emptyList(),
            listOf(camera)
        )

        service.initialize()
        delay(120)
        val waves = List(4) { launch { service.ensureMonitoring() } }
        waves.forEach { it.join() }
        delay(120)
        service.shutdown()

        assertTrue(true)
    }
}
