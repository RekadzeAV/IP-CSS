package com.company.ipcamera.shared.test

import com.company.ipcamera.shared.data.datasource.local.CameraLocalDataSource
import com.company.ipcamera.shared.domain.model.Camera

/**
 * Mock реализация CameraLocalDataSource для тестов
 */
class MockCameraLocalDataSource(
    private var cameras: MutableList<Camera> = mutableListOf(),
) : CameraLocalDataSource {
    var shouldFailOnSave: Boolean = false
    var shouldFailOnUpdate: Boolean = false
    var shouldFailOnDelete: Boolean = false

    override suspend fun getCameras(): List<Camera> = cameras.toList()

    override suspend fun getCameraById(id: String): Camera? = cameras.find { it.id == id }

    override suspend fun saveCamera(camera: Camera): Result<Camera> {
        return if (shouldFailOnSave) {
            Result.failure(Exception("Mock save failure"))
        } else {
            cameras.add(camera)
            Result.success(camera)
        }
    }

    override suspend fun saveCameras(cameras: List<Camera>): Result<List<Camera>> {
        return if (shouldFailOnSave) {
            Result.failure(Exception("Mock save failure"))
        } else {
            this.cameras.addAll(cameras)
            Result.success(cameras)
        }
    }

    override suspend fun updateCamera(camera: Camera): Result<Camera> {
        return if (shouldFailOnUpdate) {
            Result.failure(Exception("Mock update failure"))
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

    override suspend fun deleteCamera(id: String): Result<Unit> {
        return if (shouldFailOnDelete) {
            Result.failure(Exception("Mock delete failure"))
        } else {
            cameras.removeIf { it.id == id }
            Result.success(Unit)
        }
    }

    override suspend fun deleteAllCameras(): Result<Unit> {
        cameras.clear()
        return Result.success(Unit)
    }

    override suspend fun updateCameraStatus(
        id: String,
        status: String,
        lastSeen: Long?,
    ): Result<Unit> {
        val camera = cameras.find { it.id == id }
        return if (camera != null) {
            val updated =
                camera.copy(
                    status = com.company.ipcamera.core.common.model.CameraStatus.valueOf(status),
                    lastSeen = lastSeen,
                )
            val index = cameras.indexOfFirst { it.id == id }
            cameras[index] = updated
            Result.success(Unit)
        } else {
            Result.failure(Exception("Camera not found: $id"))
        }
    }

    override suspend fun cameraExists(id: String): Boolean {
        return cameras.any { it.id == id }
    }

    fun clear() {
        cameras.clear()
    }

    fun addCameraDirectly(camera: Camera) {
        cameras.add(camera)
    }

    /** Обновить камеру в хранилище напрямую (для тестов TTL без инвалидации кэша репозитория). */
    fun updateCameraDirectly(camera: Camera) {
        val index = cameras.indexOfFirst { it.id == camera.id }
        if (index >= 0) cameras[index] = camera else cameras.add(camera)
    }
}
