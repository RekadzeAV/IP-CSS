package com.company.ipcamera.shared.test

import com.company.ipcamera.core.common.model.CameraStatus
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.repository.*

/**
 * Переиспользуемый mock реализации CameraRepository для тестов Use Cases
 */
class MockCameraRepository(
    private var cameras: MutableList<Camera> = mutableListOf()
) : CameraRepository {

    var shouldFailOnAdd: Boolean = false
    var shouldFailOnUpdate: Boolean = false
    var shouldFailOnRemove: Boolean = false
    var addError: Exception? = null
    var updateError: Exception? = null
    var removeError: Exception? = null

    override suspend fun getCameras(): List<Camera> = cameras.toList()

    override suspend fun getCameraById(id: String): Camera? = cameras.find { it.id == id }

    override suspend fun addCamera(camera: Camera): Result<Camera> {
        return if (shouldFailOnAdd) {
            Result.failure(addError ?: Exception("Mock repository add failure"))
        } else {
            cameras.add(camera)
            Result.success(camera)
        }
    }

    override suspend fun updateCamera(camera: Camera): Result<Camera> {
        return if (shouldFailOnUpdate) {
            Result.failure(updateError ?: Exception("Mock repository update failure"))
        } else {
            val index = cameras.indexOfFirst { it.id == camera.id }
            if (index >= 0) {
                cameras[index] = camera
                Result.success(camera)
            } else {
                Result.failure(Exception("Camera not found: ${camera.id}"))
            }
        }
    }

    override suspend fun removeCamera(id: String): Result<Unit> {
        return if (shouldFailOnRemove) {
            Result.failure(removeError ?: Exception("Mock repository remove failure"))
        } else {
            val removed = cameras.removeIf { it.id == id }
            if (removed) {
                Result.success(Unit)
            } else {
                Result.success(Unit) // SQLDelight delete doesn't fail if row doesn't exist
            }
        }
    }

    override suspend fun discoverCameras(): List<DiscoveredCamera> = emptyList()

    override suspend fun testConnection(camera: Camera): ConnectionTestResult {
        return ConnectionTestResult.Failure(
            error = "Mock repository: connection test not implemented",
            code = ErrorCode.UNKNOWN
        )
    }

    override suspend fun getCameraStatus(id: String): CameraStatus {
        return cameras.find { it.id == id }?.status ?: CameraStatus.UNKNOWN
    }

    /**
     * Очистить все камеры из mock репозитория
     */
    fun clear() {
        cameras.clear()
    }

    /**
     * Добавить камеру напрямую (без Result)
     */
    fun addCameraDirectly(camera: Camera) {
        cameras.add(camera)
    }
}


