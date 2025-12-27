package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.repository.DiscoveredCamera
import com.company.ipcamera.shared.test.MockCameraRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Тесты для AddDiscoveredCameraUseCase
 */
class AddDiscoveredCameraUseCaseTest {

    @Test
    fun `test add discovered camera success`() = runTest {
        // Arrange
        val cameraRepository = MockCameraRepository()
        val addCameraUseCase = AddCameraUseCase(cameraRepository)
        val useCase = AddDiscoveredCameraUseCase(cameraRepository, addCameraUseCase)
        val discoveredCamera = DiscoveredCamera(
            name = "Discovered Camera",
            url = "rtsp://192.168.1.100:554/stream",
            model = "Test Model",
            manufacturer = "Test Manufacturer",
            ipAddress = "192.168.1.100",
            port = 554
        )

        // Act
        val result = useCase(discoveredCamera)

        // Assert
        assertTrue(result.isSuccess)
        val camera = result.getOrNull()
        assertNotNull(camera)
        assertEquals("Discovered Camera", camera.name)
        assertEquals("rtsp://192.168.1.100:554/stream", camera.url)
        assertEquals("Test Model", camera.model)
    }

    @Test
    fun `test add discovered camera with credentials`() = runTest {
        // Arrange
        val cameraRepository = MockCameraRepository()
        val addCameraUseCase = AddCameraUseCase(cameraRepository)
        val useCase = AddDiscoveredCameraUseCase(cameraRepository, addCameraUseCase)
        val discoveredCamera = DiscoveredCamera(
            name = "Discovered Camera",
            url = "rtsp://192.168.1.100:554/stream",
            model = "Test Model",
            manufacturer = "Test Manufacturer",
            ipAddress = "192.168.1.100",
            port = 554
        )

        // Act
        val result = useCase(
            discoveredCamera = discoveredCamera,
            username = "admin",
            password = "password"
        )

        // Assert
        assertTrue(result.isSuccess)
        val camera = result.getOrNull()
        assertNotNull(camera)
        assertEquals("admin", camera.username)
        assertEquals("password", camera.password)
    }

    @Test
    fun `test add discovered camera without credentials`() = runTest {
        // Arrange
        val cameraRepository = MockCameraRepository()
        val addCameraUseCase = AddCameraUseCase(cameraRepository)
        val useCase = AddDiscoveredCameraUseCase(cameraRepository, addCameraUseCase)
        val discoveredCamera = DiscoveredCamera(
            name = "Discovered Camera",
            url = "rtsp://192.168.1.100:554/stream",
            model = "Test Model",
            manufacturer = "Test Manufacturer",
            ipAddress = "192.168.1.100",
            port = 554
        )

        // Act
        val result = useCase(discoveredCamera)

        // Assert
        assertTrue(result.isSuccess)
        val camera = result.getOrNull()
        assertNotNull(camera)
        assertNull(camera.username)
        assertNull(camera.password)
    }

    @Test
    fun `test add discovered camera with minimal data`() = runTest {
        // Arrange
        val cameraRepository = MockCameraRepository()
        val addCameraUseCase = AddCameraUseCase(cameraRepository)
        val useCase = AddDiscoveredCameraUseCase(cameraRepository, addCameraUseCase)
        val discoveredCamera = DiscoveredCamera(
            name = "Minimal Camera",
            url = "rtsp://192.168.1.100:554/stream",
            model = null,
            manufacturer = null,
            ipAddress = "192.168.1.100",
            port = 554
        )

        // Act
        val result = useCase(discoveredCamera)

        // Assert
        assertTrue(result.isSuccess)
        val camera = result.getOrNull()
        assertNotNull(camera)
        assertEquals("Minimal Camera", camera.name)
        assertNull(camera.model)
    }

    @Test
    fun `test add discovered camera repository failure`() = runTest {
        // Arrange
        val cameraRepository = MockCameraRepository()
        cameraRepository.shouldFailOnAdd = true
        cameraRepository.addError = Exception("Database error")
        val addCameraUseCase = AddCameraUseCase(cameraRepository)
        val useCase = AddDiscoveredCameraUseCase(cameraRepository, addCameraUseCase)
        val discoveredCamera = DiscoveredCamera(
            name = "Discovered Camera",
            url = "rtsp://192.168.1.100:554/stream",
            ipAddress = "192.168.1.100"
        )

        // Act
        val result = useCase(discoveredCamera)

        // Assert
        assertTrue(result.isFailure)
        assertEquals("Database error", result.exceptionOrNull()?.message)
    }
}

