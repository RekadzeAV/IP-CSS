package com.company.ipcamera.shared.test

import com.company.ipcamera.core.network.ApiError
import com.company.ipcamera.core.network.ApiResult
import com.company.ipcamera.shared.data.datasource.remote.CameraRemoteDataSource
import com.company.ipcamera.shared.domain.model.Camera

/**
 * Mock реализация CameraRemoteDataSource для тестов
 */
class MockCameraRemoteDataSource(
    private var cameras: MutableList<Camera> = mutableListOf(),
) : CameraRemoteDataSource {
    var shouldFailOnGet: Boolean = false
    var shouldFailOnCreate: Boolean = false
    var shouldFailOnUpdate: Boolean = false
    var shouldFailOnDelete: Boolean = false
    var networkError: Exception? = null

    private fun apiErr(): ApiError = (networkError ?: Exception("Mock network error")).let { ApiError.UnknownError(it) }

    override suspend fun getCameras(
        page: Int,
        limit: Int,
        status: String?,
    ): ApiResult<List<Camera>> {
        return if (shouldFailOnGet) {
            ApiResult.Error(apiErr())
        } else {
            ApiResult.Success(cameras.toList())
        }
    }

    override suspend fun getCameraById(id: String): ApiResult<Camera> {
        return if (shouldFailOnGet) {
            ApiResult.Error(apiErr())
        } else {
            val camera = cameras.find { it.id == id }
            if (camera != null) {
                ApiResult.Success(camera)
            } else {
                ApiResult.Error(ApiError.UnknownError(Exception("Camera not found: $id")))
            }
        }
    }

    override suspend fun createCamera(camera: Camera): ApiResult<Camera> {
        return if (shouldFailOnCreate) {
            ApiResult.Error(apiErr())
        } else {
            cameras.add(camera)
            ApiResult.Success(camera)
        }
    }

    override suspend fun updateCamera(
        id: String,
        camera: Camera,
    ): ApiResult<Camera> {
        return if (shouldFailOnUpdate) {
            ApiResult.Error(apiErr())
        } else {
            val index = cameras.indexOfFirst { it.id == id }
            if (index >= 0) {
                cameras[index] = camera
                ApiResult.Success(camera)
            } else {
                ApiResult.Error(ApiError.UnknownError(Exception("Camera not found: $id")))
            }
        }
    }

    override suspend fun deleteCamera(id: String): ApiResult<Unit> {
        return if (shouldFailOnDelete) {
            ApiResult.Error(apiErr())
        } else {
            cameras.removeIf { it.id == id }
            ApiResult.Success(Unit)
        }
    }

    override suspend fun testConnection(id: String): ApiResult<Map<String, String>> {
        return ApiResult.Success(mapOf("status" to "connected"))
    }

    override suspend fun getCameraStatus(id: String): ApiResult<Map<String, String>> {
        val camera = cameras.find { it.id == id }
        return if (camera != null) {
            ApiResult.Success(mapOf("status" to camera.status.name))
        } else {
            ApiResult.Error(ApiError.UnknownError(Exception("Camera not found: $id")))
        }
    }

    fun clear() {
        cameras.clear()
    }

    fun addCameraDirectly(camera: Camera) {
        cameras.add(camera)
    }
}
