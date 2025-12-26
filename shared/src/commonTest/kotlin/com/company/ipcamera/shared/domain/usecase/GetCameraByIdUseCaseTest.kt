package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.repository.CameraRepository
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
        val repository = object : CameraRepository {
            override suspend fun getCameras(): List<Camera> = emptyList()
            override suspend fun getCameraById(id: String): Camera? {
                return if (id == "camera-1") testCamera else null
            }
            override suspend fun addCamera(camera: Camera): Result<Camera> = Result.failure(Exception())
            override suspend fun updateCamera(camera: Camera): Result<Camera> = Result.failure(Exception())
            override suspend fun removeCamera(id: String): Result<Unit> = Result.failure(Exception())
            override suspend fun discoverCameras() = emptyList<com.company.ipcamera.shared.domain.repository.DiscoveredCamera>()
            override suspend fun testConnection(camera: Camera) = com.company.ipcamera.shared.domain.repository.ConnectionTestResult.Failure("", com.company.ipcamera.shared.domain.repository.ErrorCode.UNKNOWN)
            override suspend fun getCameraStatus(id: String) = com.company.ipcamera.shared.domain.model.CameraStatus.UNKNOWN
        }
        
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
        val repository = object : CameraRepository {
            override suspend fun getCameras(): List<Camera> = emptyList()
            override suspend fun getCameraById(id: String): Camera? = null
            override suspend fun addCamera(camera: Camera): Result<Camera> = Result.failure(Exception())
            override suspend fun updateCamera(camera: Camera): Result<Camera> = Result.failure(Exception())
            override suspend fun removeCamera(id: String): Result<Unit> = Result.failure(Exception())
            override suspend fun discoverCameras() = emptyList<com.company.ipcamera.shared.domain.repository.DiscoveredCamera>()
            override suspend fun testConnection(camera: Camera) = com.company.ipcamera.shared.domain.repository.ConnectionTestResult.Failure("", com.company.ipcamera.shared.domain.repository.ErrorCode.UNKNOWN)
            override suspend fun getCameraStatus(id: String) = com.company.ipcamera.shared.domain.model.CameraStatus.UNKNOWN
        }
        
        val useCase = GetCameraByIdUseCase(repository)
        
        // Act
        val result = useCase("non-existent-id")
        
        // Assert
        assertNull(result)
    }
}


