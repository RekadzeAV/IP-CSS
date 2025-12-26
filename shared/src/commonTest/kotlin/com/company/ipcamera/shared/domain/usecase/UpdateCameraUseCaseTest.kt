package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.repository.CameraRepository
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
        var updatedCamera: Camera? = null
        
        val repository = object : CameraRepository {
            override suspend fun getCameras(): List<Camera> = emptyList()
            override suspend fun getCameraById(id: String): Camera? = null
            override suspend fun addCamera(camera: Camera): Result<Camera> = Result.failure(Exception())
            override suspend fun updateCamera(camera: Camera): Result<Camera> {
                updatedCamera = camera
                return Result.success(camera)
            }
            override suspend fun removeCamera(id: String): Result<Unit> = Result.failure(Exception())
            override suspend fun discoverCameras() = emptyList<com.company.ipcamera.shared.domain.repository.DiscoveredCamera>()
            override suspend fun testConnection(camera: Camera) = com.company.ipcamera.shared.domain.repository.ConnectionTestResult.Failure("", com.company.ipcamera.shared.domain.repository.ErrorCode.UNKNOWN)
            override suspend fun getCameraStatus(id: String) = com.company.ipcamera.shared.domain.model.CameraStatus.UNKNOWN
        }
        
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
        val repository = object : CameraRepository {
            override suspend fun getCameras(): List<Camera> = emptyList()
            override suspend fun getCameraById(id: String): Camera? = null
            override suspend fun addCamera(camera: Camera): Result<Camera> = Result.failure(Exception())
            override suspend fun updateCamera(camera: Camera): Result<Camera> {
                return Result.failure(Exception("Update failed"))
            }
            override suspend fun removeCamera(id: String): Result<Unit> = Result.failure(Exception())
            override suspend fun discoverCameras() = emptyList<com.company.ipcamera.shared.domain.repository.DiscoveredCamera>()
            override suspend fun testConnection(camera: Camera) = com.company.ipcamera.shared.domain.repository.ConnectionTestResult.Failure("", com.company.ipcamera.shared.domain.repository.ErrorCode.UNKNOWN)
            override suspend fun getCameraStatus(id: String) = com.company.ipcamera.shared.domain.model.CameraStatus.UNKNOWN
        }
        
        val useCase = UpdateCameraUseCase(repository)
        
        // Act
        val result = useCase(camera)
        
        // Assert
        assertTrue(result.isFailure)
        assertEquals("Update failed", result.exceptionOrNull()?.message)
    }
}


