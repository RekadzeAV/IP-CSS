package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.test.MockCameraRepository
import com.company.ipcamera.shared.test.TestDataFactory
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Тесты для GetCamerasUseCase
 */
class GetCamerasUseCaseTest {

    @Test
    fun `test get cameras success`() = runTest {
        // Arrange
        val testCameras = TestDataFactory.createTestCameras(3)
        val repository = MockCameraRepository()
        testCameras.forEach { repository.addCameraDirectly(it) }
        val useCase = GetCamerasUseCase(repository)

        // Act
        val result = useCase()

        // Assert
        assertEquals(3, result.size)
        assertEquals(testCameras, result)
    }

    @Test
    fun `test get cameras empty list`() = runTest {
        // Arrange
        val repository = MockCameraRepository()
        val useCase = GetCamerasUseCase(repository)

        // Act
        val result = useCase()

        // Assert
        assertTrue(result.isEmpty())
    }
}



