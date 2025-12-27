package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.test.MockCameraRepository
import com.company.ipcamera.shared.test.TestDataFactory
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Тесты для DeleteCameraUseCase
 */
class DeleteCameraUseCaseTest {

    @Test
    fun `test delete camera success`() = runTest {
        // Arrange
        val testCamera = TestDataFactory.createTestCamera(id = "camera-1")
        val repository = MockCameraRepository()
        repository.addCameraDirectly(testCamera)
        val useCase = DeleteCameraUseCase(repository)

        // Act
        val result = useCase("camera-1")

        // Assert
        assertTrue(result.isSuccess)
        // Verify camera was removed
        assertNull(repository.getCameraById("camera-1"))
    }

    @Test
    fun `test delete camera repository failure`() = runTest {
        // Arrange
        val repository = MockCameraRepository()
        repository.shouldFailOnRemove = true
        repository.removeError = Exception("Delete failed")
        val useCase = DeleteCameraUseCase(repository)

        // Act
        val result = useCase("camera-1")

        // Assert
        assertTrue(result.isFailure)
        assertEquals("Delete failed", result.exceptionOrNull()?.message)
    }
}



