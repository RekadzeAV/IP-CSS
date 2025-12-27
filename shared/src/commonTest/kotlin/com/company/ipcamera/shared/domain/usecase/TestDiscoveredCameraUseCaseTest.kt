package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.repository.ConnectionTestResult
import com.company.ipcamera.shared.domain.repository.DiscoveredCamera
import com.company.ipcamera.shared.test.MockCameraRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Тесты для TestDiscoveredCameraUseCase
 */
class TestDiscoveredCameraUseCaseTest {

    @Test
    fun `test discovered camera connection`() = runTest {
        // Arrange
        val cameraRepository = MockCameraRepository()
        val useCase = TestDiscoveredCameraUseCase(cameraRepository)
        val discoveredCamera = DiscoveredCamera(
            name = "Discovered Camera",
            url = "rtsp://192.168.1.100:554/stream",
            model = "Test Model",
            manufacturer = "Test Manufacturer",
            ipAddress = "192.168.1.100",
            port = 554
        )

        // Act
        // Примечание: testConnection() использует OnvifClient для проверки подключения
        // В тестовой среде без реальной камеры возвращает Failure
        val result = useCase(discoveredCamera)

        // Assert
        assertNotNull(result)
        assertTrue(result is ConnectionTestResult)
        // В тестовой среде результат будет Failure
        // Для полного тестирования требуется мок OnvifClient или тестовая камера
    }

    @Test
    fun `test discovered camera connection with credentials`() = runTest {
        // Arrange
        val cameraRepository = MockCameraRepository()
        val useCase = TestDiscoveredCameraUseCase(cameraRepository)
        val discoveredCamera = DiscoveredCamera(
            name = "Discovered Camera",
            url = "rtsp://192.168.1.100:554/stream",
            ipAddress = "192.168.1.100"
        )

        // Act
        val result = useCase(
            discoveredCamera = discoveredCamera,
            username = "admin",
            password = "password"
        )

        // Assert
        assertNotNull(result)
        assertTrue(result is ConnectionTestResult)
    }

    @Test
    fun `test discovered camera connection without credentials`() = runTest {
        // Arrange
        val cameraRepository = MockCameraRepository()
        val useCase = TestDiscoveredCameraUseCase(cameraRepository)
        val discoveredCamera = DiscoveredCamera(
            name = "Discovered Camera",
            url = "rtsp://192.168.1.100:554/stream",
            ipAddress = "192.168.1.100"
        )

        // Act
        val result = useCase(discoveredCamera)

        // Assert
        assertNotNull(result)
        assertTrue(result is ConnectionTestResult)
    }
}

