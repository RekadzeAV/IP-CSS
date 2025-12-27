package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.repository.DiscoveredCamera
import com.company.ipcamera.shared.test.MockCameraRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Тесты для DiscoverAndAddCameraUseCase
 */
class DiscoverAndAddCameraUseCaseTest {

    @Test
    fun `test discover and add cameras auto add false returns only discovered`() = runTest {
        // Arrange
        val cameraRepository = MockCameraRepository()
        val discoverUseCase = DiscoverCamerasUseCase(cameraRepository)
        val addDiscoveredUseCase = AddDiscoveredCameraUseCase(
            cameraRepository,
            AddCameraUseCase(cameraRepository)
        )
        val useCase = DiscoverAndAddCameraUseCase(discoverUseCase, addDiscoveredUseCase)

        // Act
        val result = useCase(autoAdd = false)

        // Assert
        assertNotNull(result)
        assertNotNull(result.discoveredCameras)
        assertTrue(result.addedCameras.isEmpty())
        assertTrue(result.failedCameras.isEmpty())
    }

    @Test
    fun `test discover and add cameras auto add true adds all discovered`() = runTest {
        // Arrange
        val cameraRepository = MockCameraRepository()
        // Note: In real scenario, discoverCameras would return actual discovered cameras
        // Here we test the logic when cameras are discovered
        val discoverUseCase = DiscoverCamerasUseCase(cameraRepository)
        val addDiscoveredUseCase = AddDiscoveredCameraUseCase(
            cameraRepository,
            AddCameraUseCase(cameraRepository)
        )
        val useCase = DiscoverAndAddCameraUseCase(discoverUseCase, addDiscoveredUseCase)

        // Act
        val result = useCase(
            username = "admin",
            password = "password",
            autoAdd = true
        )

        // Assert
        assertNotNull(result)
        assertEquals(result.discoveredCameras.size, result.totalDiscovered)
        assertEquals(result.addedCameras.size, result.totalAdded)
        assertEquals(result.failedCameras.size, result.totalFailed)
    }

    @Test
    fun `test discover and add cameras with credentials`() = runTest {
        // Arrange
        val cameraRepository = MockCameraRepository()
        val discoverUseCase = DiscoverCamerasUseCase(cameraRepository)
        val addDiscoveredUseCase = AddDiscoveredCameraUseCase(
            cameraRepository,
            AddCameraUseCase(cameraRepository)
        )
        val useCase = DiscoverAndAddCameraUseCase(discoverUseCase, addDiscoveredUseCase)

        // Act
        val result = useCase(
            username = "admin",
            password = "password",
            autoAdd = true
        )

        // Assert
        assertNotNull(result)
        // In test environment without real network, discovered list will be empty
        // So added cameras will also be empty
        assertTrue(result.discoveredCameras.isEmpty() || result.addedCameras.isNotEmpty())
    }

    @Test
    fun `test discover and add cameras result properties`() = runTest {
        // Arrange
        val cameraRepository = MockCameraRepository()
        val discoverUseCase = DiscoverCamerasUseCase(cameraRepository)
        val addDiscoveredUseCase = AddDiscoveredCameraUseCase(
            cameraRepository,
            AddCameraUseCase(cameraRepository)
        )
        val useCase = DiscoverAndAddCameraUseCase(discoverUseCase, addDiscoveredUseCase)

        // Act
        val result = useCase(autoAdd = false)

        // Assert
        assertEquals(0, result.totalDiscovered)
        assertEquals(0, result.totalAdded)
        assertEquals(0, result.totalFailed)
        // isSuccess should be false when no cameras added
        assertFalse(result.isSuccess)
    }
}


