package com.company.ipcamera.shared.data.datasource.remote.impl

import com.company.ipcamera.core.network.ApiResult
import com.company.ipcamera.core.network.api.RecordingApiService
import com.company.ipcamera.core.network.dto.*
import com.company.ipcamera.shared.data.datasource.remote.RecordingRemoteDataSource
import com.company.ipcamera.shared.domain.model.Recording
import com.company.ipcamera.shared.domain.model.Quality
import com.company.ipcamera.shared.domain.model.RecordingFormat
import com.company.ipcamera.shared.domain.model.RecordingStatus
import com.company.ipcamera.shared.domain.repository.PaginatedResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Реализация RecordingRemoteDataSource с использованием RecordingApiService
 */
class RecordingRemoteDataSourceImpl(
    private val recordingApiService: RecordingApiService
) : RecordingRemoteDataSource {

    override suspend fun getRecordings(
        cameraId: String?,
        startTime: Long?,
        endTime: Long?,
        page: Int,
        limit: Int
    ): ApiResult<PaginatedResult<Recording>> = withContext(Dispatchers.IO) {
        try {
            // RecordingApiService возвращает PaginatedResponse напрямую (без ApiResult)
            // Поэтому нужно обернуть в try-catch и вернуть ApiResult
            val paginatedResponse = recordingApiService.getRecordings(cameraId, startTime, endTime, page, limit)

            val recordings = paginatedResponse.items.map { it.toDomain() }
            val paginatedResult = PaginatedResult(
                items = recordings,
                total = paginatedResponse.total,
                page = paginatedResponse.page,
                limit = paginatedResponse.limit,
                hasMore = paginatedResponse.hasMore
            )

            ApiResult.Success(paginatedResult)
        } catch (e: Exception) {
            logger.error(e) { "Error getting recordings from remote API" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    override suspend fun getRecordingById(id: String): ApiResult<Recording> = withContext(Dispatchers.IO) {
        try {
            val response = recordingApiService.getRecordingById(id)
            ApiResult.Success(response.toDomain())
        } catch (e: Exception) {
            logger.error(e) { "Error getting recording by id from remote API: $id" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    override suspend fun deleteRecording(id: String): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            recordingApiService.deleteRecording(id)
            ApiResult.Success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error deleting recording from remote API: $id" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    override suspend fun getDownloadUrl(id: String): ApiResult<String> = withContext(Dispatchers.IO) {
        try {
            val response = recordingApiService.getDownloadUrl(id)
            val url = response.data?.get("url")
                ?: throw IllegalArgumentException("Download URL not found in response")
            ApiResult.Success(url)
        } catch (e: Exception) {
            logger.error(e) { "Error getting download URL for recording from remote API: $id" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    override suspend fun exportRecording(id: String, format: String, quality: String): ApiResult<String> = withContext(Dispatchers.IO) {
        try {
            val request = ExportRecordingRequest(format = format, quality = quality)
            val response = recordingApiService.exportRecording(id, request)
            ApiResult.Success(response.downloadUrl)
        } catch (e: Exception) {
            logger.error(e) { "Error exporting recording from remote API: $id" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    /**
     * Маппинг RecordingResponse в Domain модель Recording
     */
    private fun RecordingResponse.toDomain(): Recording {
        return Recording(
            id = id,
            cameraId = cameraId,
            cameraName = cameraName,
            startTime = startTime,
            endTime = endTime,
            duration = duration,
            filePath = filePath,
            fileSize = fileSize,
            format = RecordingFormat.valueOf(format.uppercase()),
            quality = Quality.valueOf(quality.uppercase()),
            status = RecordingStatus.valueOf(status.uppercase()),
            thumbnailUrl = thumbnailUrl,
            createdAt = createdAt
        )
    }
}

