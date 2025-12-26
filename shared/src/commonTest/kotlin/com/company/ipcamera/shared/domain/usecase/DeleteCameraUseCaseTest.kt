package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.repository.CameraRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Тесты для DeleteCameraUseCase
 */
class DeleteCameraUseCaseTest {
    
    @Test
    fun `test delete camera success`() = runTest {
        // Arrange
        var deletedId: String? = null
        val repository = object : CameraRepository {
            override suspend fun getCameras(): List<com.company.ipcamera.shared.domain.model.Camera> = emptyList()
            override suspend fun getCameraById(id: String): com.company.ipcamera.shared.domain.model.Camera? = null
            override suspend fun addCamera(camera: com.company.ipcamera.shared.domain.model.Camera): Result<com.company.ipcamera.shared.domain.model.Camera> = Result.failure(Exception())
            override suspend fun updateCamera(camera: com.company.ipcamera.shared.domain.model.Camera): Result<com.company.ipcamera.shared.domain.model.Camera> = Result.failure(Exception())
            override suspend fun removeCamera(id: String): Result<Unit> {
                deletedId = id
                return Result.success(Unit)
            }
            override suspend fun discoverCameras() = emptyList<com.company.ipcamera.shared.domain.repository.DiscoveredCamera>()
            override suspend fun testConnection(camera: com.company.ipcamera.shared.domain.model.Camera) = com.company.ipcamera.shared.domain.repository.ConnectionTestResult.Failure("", com.company.ipcamera.shared.domain.repository.ErrorCode.UNKNOWN)
            override suspend fun getCameraStatus(id: String) = com.company.ipcamera.shared.domain.model.CameraStatus.UNKNOWN
        }
        
        val useCase = DeleteCameraUseCase(repository)
        
        // Act
        val result = useCase("camera-1")
        
        // Assert
        assertTrue(result.isSuccess)
        assertEquals("camera-1", deletedId)
    }
    
    @Test
    fun `test delete camera repository failure`() = runTest {
        // Arrange
        val repository = object : CameraRepository {
            override suspend fun getCameras(): List<com.company.ipcamera.shared.domain.model.Camera> = emptyList()
            override suspend fun getCameraById(id: String): com.company.ipcamera.shared.domain.model.Camera? = null
            override suspend fun addCamera(camera: com.company.ipcamera.shared.domain.model.Camera): Result<com.company.ipcamera.shared.domain.model.Camera> = Result.failure(Exception())
            override suspend fun updateCamera(camera: com.company.ipcamera.shared.domain.model.Camera): Result<com.company.ipcamera.shared.domain.model.Camera> = Result.failure(Exception())
            override suspend fun removeCamera(id: String): Result<Unit> {
                return Result.failure(Exception("Delete failed"))
            }
            override suspend fun discoverCameras() = emptyList<com.company.ipcamera.shared.domain.repository.DiscoveredCamera>()
            override suspend fun testConnection(camera: com.company.ipcamera.shared.domain.model.Camera) = com.company.ipcamera.shared.domain.repository.ConnectionTestResult.Failure("", com.company.ipcamera.shared.domain.repository.ErrorCode.UNKNOWN)
            override suspend fun getCameraStatus(id: String) = com.company.ipcamera.shared.domain.model.CameraStatus.UNKNOWN
        }
        
        val useCase = DeleteCameraUseCase(repository)
        
        // Act
        val result = useCase("camera-1")
        
        // Assert
        assertTrue(result.isFailure)
        assertEquals("Delete failed", result.exceptionOrNull()?.message)
    }
}


