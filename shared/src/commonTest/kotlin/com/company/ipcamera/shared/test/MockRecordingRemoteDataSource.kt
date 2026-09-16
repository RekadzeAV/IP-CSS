package com.company.ipcamera.shared.test

import com.company.ipcamera.core.network.ApiError
import com.company.ipcamera.core.network.ApiResult
import com.company.ipcamera.shared.data.datasource.remote.RecordingRemoteDataSource
import com.company.ipcamera.shared.domain.model.Recording
import com.company.ipcamera.shared.domain.repository.PaginatedResult

/**
 * Mock реализация RecordingRemoteDataSource для тестов
 */
class MockRecordingRemoteDataSource(
    private var recordings: MutableList<Recording> = mutableListOf(),
) : RecordingRemoteDataSource {
    var shouldFailOnGet: Boolean = false
    var shouldFailOnDelete: Boolean = false
    var networkError: Exception? = null

    private fun apiErr(): ApiError = (networkError ?: Exception("Mock network error")).let { ApiError.UnknownError(it) }

    override suspend fun getRecordings(
        cameraId: String?,
        startTime: Long?,
        endTime: Long?,
        page: Int,
        limit: Int,
    ): ApiResult<PaginatedResult<Recording>> {
        return if (shouldFailOnGet) {
            ApiResult.Error(apiErr())
        } else {
            var filtered = recordings.toList()

            if (cameraId != null) {
                filtered = filtered.filter { it.cameraId == cameraId }
            }
            if (startTime != null) {
                filtered = filtered.filter { it.startTime >= startTime }
            }
            if (endTime != null) {
                filtered = filtered.filter { (it.endTime ?: Long.MAX_VALUE) <= endTime }
            }

            val total = filtered.size
            val offset = (page - 1) * limit
            val paginated = filtered.drop(offset).take(limit)

            ApiResult.Success(
                PaginatedResult(
                    items = paginated,
                    total = total,
                    page = page,
                    limit = limit,
                    hasMore = (offset + limit) < total,
                ),
            )
        }
    }

    override suspend fun getRecordingById(id: String): ApiResult<Recording> {
        return if (shouldFailOnGet) {
            ApiResult.Error(apiErr())
        } else {
            val recording = recordings.find { it.id == id }
            if (recording != null) {
                ApiResult.Success(recording)
            } else {
                ApiResult.Error(ApiError.UnknownError(Exception("Recording not found: $id")))
            }
        }
    }

    override suspend fun deleteRecording(id: String): ApiResult<Unit> {
        return if (shouldFailOnDelete) {
            ApiResult.Error(apiErr())
        } else {
            recordings.removeIf { it.id == id }
            ApiResult.Success(Unit)
        }
    }

    override suspend fun getDownloadUrl(id: String): ApiResult<String> {
        val recording = recordings.find { it.id == id }
        return if (recording != null) {
            ApiResult.Success("https://example.com/download/$id")
        } else {
            ApiResult.Error(ApiError.UnknownError(Exception("Recording not found: $id")))
        }
    }

    override suspend fun exportRecording(
        id: String,
        format: String,
        quality: String,
    ): ApiResult<String> {
        val recording = recordings.find { it.id == id }
        return if (recording != null) {
            ApiResult.Success("https://example.com/export/$id?format=$format&quality=$quality")
        } else {
            ApiResult.Error(ApiError.UnknownError(Exception("Recording not found: $id")))
        }
    }

    fun clear() {
        recordings.clear()
    }

    fun addRecordingDirectly(recording: Recording) {
        recordings.add(recording)
    }
}
