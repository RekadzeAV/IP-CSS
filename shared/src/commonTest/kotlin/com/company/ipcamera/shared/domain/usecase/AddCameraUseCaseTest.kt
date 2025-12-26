package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.repository.CameraRepository
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
        val repository = object : CameraRepository {
            private val cameras = mutableListOf<Camera>()
            
            override suspend fun getCameras(): List<Camera> = cameras.toList()
            override suspend fun getCameraById(id: String): Camera? = cameras.find { it.id == id }
            override suspend fun addCamera(camera: Camera): Result<Camera> {
                cameras.add(camera)
                return Result.success(camera)
            }
            override suspend fun updateCamera(camera: Camera): Result<Camera> = Result.failure(Exception())
            override suspend fun removeCamera(id: String): Result<Unit> = Result.failure(Exception())
            override suspend fun discoverCameras() = emptyList<com.company.ipcamera.shared.domain.repository.DiscoveredCamera>()
            override suspend fun testConnection(camera: Camera) = com.company.ipcamera.shared.domain.repository.ConnectionTestResult.Failure("", com.company.ipcamera.shared.domain.repository.ErrorCode.UNKNOWN)
            override suspend fun getCameraStatus(id: String) = com.company.ipcamera.shared.domain.model.CameraStatus.UNKNOWN
        }
        
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
        val repository = object : CameraRepository {
            private val cameras = mutableListOf<Camera>()
            
            override suspend fun getCameras(): List<Camera> = cameras.toList()
            override suspend fun getCameraById(id: String): Camera? = cameras.find { it.id == id }
            override suspend fun addCamera(camera: Camera): Result<Camera> {
                cameras.add(camera)
                return Result.success(camera)
            }
            override suspend fun updateCamera(camera: Camera): Result<Camera> = Result.failure(Exception())
            override suspend fun removeCamera(id: String): Result<Unit> = Result.failure(Exception())
            override suspend fun discoverCameras() = emptyList<com.company.ipcamera.shared.domain.repository.DiscoveredCamera>()
            override suspend fun testConnection(camera: Camera) = com.company.ipcamera.shared.domain.repository.ConnectionTestResult.Failure("", com.company.ipcamera.shared.domain.repository.ErrorCode.UNKNOWN)
            override suspend fun getCameraStatus(id: String) = com.company.ipcamera.shared.domain.model.CameraStatus.UNKNOWN
        }
        
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
        val repository = object : CameraRepository {
            override suspend fun getCameras(): List<Camera> = emptyList()
            override suspend fun getCameraById(id: String): Camera? = null
            override suspend fun addCamera(camera: Camera): Result<Camera> {
                return Result.failure(Exception("Database error"))
            }
            override suspend fun updateCamera(camera: Camera): Result<Camera> = Result.failure(Exception())
            override suspend fun removeCamera(id: String): Result<Unit> = Result.failure(Exception())
            override suspend fun discoverCameras() = emptyList<com.company.ipcamera.shared.domain.repository.DiscoveredCamera>()
            override suspend fun testConnection(camera: Camera) = com.company.ipcamera.shared.domain.repository.ConnectionTestResult.Failure("", com.company.ipcamera.shared.domain.repository.ErrorCode.UNKNOWN)
            override suspend fun getCameraStatus(id: String) = com.company.ipcamera.shared.domain.model.CameraStatus.UNKNOWN
        }
        
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


