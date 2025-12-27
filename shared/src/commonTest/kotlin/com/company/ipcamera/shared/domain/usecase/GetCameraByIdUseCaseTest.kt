package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.test.MockCameraRepository
import com.company.ipcamera.shared.test.TestDataFactory
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Тесты для GetCameraByIdUseCase
 */
class GetCameraByIdUseCaseTest {

    @Test
    fun `test get camera by id success`() = runTest {
        // Arrange
        val testCamera = TestDataFactory.createTestCamera(id = "camera-1")
        val repository = MockCameraRepository()
        repository.addCameraDirectly(testCamera)
        val useCase = GetCameraByIdUseCase(repository)

        // Act
        val result = useCase("camera-1")

        // Assert
        assertNotNull(result)
        assertEquals("camera-1", result?.id)
        assertEquals(testCamera, result)
    }

    @Test
    fun `test get camera by id not found`() = runTest {
        // Arrange
        val repository = MockCameraRepository()
        val useCase = GetCameraByIdUseCase(repository)

        // Act
        val result = useCase("non-existent-id")

        // Assert
        assertNull(result)
    }
}



