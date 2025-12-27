package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.test.MockCameraRepository
import com.company.ipcamera.shared.test.TestDataFactory
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Тесты для UpdateCameraUseCase
 */
class UpdateCameraUseCaseTest {

    @Test
    fun `test update camera success`() = runTest {
        // Arrange
        val originalCamera = TestDataFactory.createTestCamera(
            id = "camera-1",
            name = "Original Name"
        )
        val repository = MockCameraRepository()
        repository.addCameraDirectly(originalCamera)
        val useCase = UpdateCameraUseCase(repository)
        val cameraToUpdate = originalCamera.copy(name = "Updated Name")

        // Act
        val result = useCase(cameraToUpdate)

        // Assert
        assertTrue(result.isSuccess)
        val updated = result.getOrNull()
        assertNotNull(updated)
        assertEquals("Updated Name", updated.name)
        assertEquals("camera-1", updated.id)
        assertTrue(updated.updatedAt > originalCamera.updatedAt)
    }

    @Test
    fun `test update camera repository failure`() = runTest {
        // Arrange
        val camera = TestDataFactory.createTestCamera()
        val repository = MockCameraRepository()
        repository.shouldFailOnUpdate = true
        repository.updateError = Exception("Update failed")
        val useCase = UpdateCameraUseCase(repository)

        // Act
        val result = useCase(camera)

        // Assert
        assertTrue(result.isFailure)
        assertEquals("Update failed", result.exceptionOrNull()?.message)
    }
}



