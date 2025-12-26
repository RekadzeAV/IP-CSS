package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.repository.CameraRepository
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
        val repository = object : CameraRepository {
            override suspend fun getCameras(): List<Camera> = testCameras
            override suspend fun getCameraById(id: String): Camera? = testCameras.find { it.id == id }
            override suspend fun addCamera(camera: Camera): Result<Camera> = Result.failure(Exception())
            override suspend fun updateCamera(camera: Camera): Result<Camera> = Result.failure(Exception())
            override suspend fun removeCamera(id: String): Result<Unit> = Result.failure(Exception())
            override suspend fun discoverCameras() = emptyList<com.company.ipcamera.shared.domain.repository.DiscoveredCamera>()
            override suspend fun testConnection(camera: Camera) = com.company.ipcamera.shared.domain.repository.ConnectionTestResult.Failure("", com.company.ipcamera.shared.domain.repository.ErrorCode.UNKNOWN)
            override suspend fun getCameraStatus(id: String) = com.company.ipcamera.shared.domain.model.CameraStatus.UNKNOWN
        }
        
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
        
        val useCase = GetCamerasUseCase(repository)
        
        // Act
        val result = useCase()
        
        // Assert
        assertTrue(result.isEmpty())
    }
}


