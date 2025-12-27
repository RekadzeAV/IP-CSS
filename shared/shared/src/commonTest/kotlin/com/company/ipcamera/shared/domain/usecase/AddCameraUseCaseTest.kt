package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.test.MockCameraRepository
import com.company.ipcamera.shared.test.TestDataFactory
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Тесты для AddCameraUseCase
 */
class AddCameraUseCaseTest {

    @Test
    fun `test add camera success`() = runTest {
        // Arrange
        val repository = MockCameraRepository()
        val useCase = AddCameraUseCase(repository)

        // Act
        val result = useCase(
            name = "Test Camera",
            url = "rtsp://192.168.1.100:554/stream",
            username = "admin",
            password = "password",
            model = "Test Model"
        )

        // Assert
        assertTrue(result.isSuccess)
        val camera = result.getOrNull()
        assertNotNull(camera)
        assertEquals("Test Camera", camera.name)
        assertEquals("rtsp://192.168.1.100:554/stream", camera.url)
        assertEquals("admin", camera.username)
        assertEquals("password", camera.password)
        assertEquals("Test Model", camera.model)
        assertNotNull(camera.id)
    }

    @Test
    fun `test add camera with minimal data`() = runTest {
        // Arrange
        val repository = MockCameraRepository()
        val useCase = AddCameraUseCase(repository)

        // Act
        val result = useCase(
            name = "Minimal Camera",
            url = "rtsp://192.168.1.100:554/stream"
        )

        // Assert
        assertTrue(result.isSuccess)
        val camera = result.getOrNull()
        assertNotNull(camera)
        assertEquals("Minimal Camera", camera.name)
        assertNull(camera.username)
        assertNull(camera.password)
        assertNull(camera.model)
    }

    @Test
    fun `test add camera repository failure`() = runTest {
        // Arrange
        val repository = MockCameraRepository()
        repository.shouldFailOnAdd = true
        repository.addError = Exception("Database error")
        val useCase = AddCameraUseCase(repository)

        // Act
        val result = useCase(
            name = "Test Camera",
            url = "rtsp://192.168.1.100:554/stream"
        )

        // Assert
        assertTrue(result.isFailure)
        assertEquals("Database error", result.exceptionOrNull()?.message)
    }
}



