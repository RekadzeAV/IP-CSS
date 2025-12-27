package com.company.ipcamera.shared.data.repository

import com.company.ipcamera.core.network.ApiResult
import com.company.ipcamera.core.network.api.RecordingApiService
import com.company.ipcamera.core.network.dto.*
import com.company.ipcamera.shared.domain.model.Recording
import com.company.ipcamera.shared.domain.model.RecordingFormat
import com.company.ipcamera.shared.domain.model.RecordingStatus
import com.company.ipcamera.shared.domain.model.Quality
import com.company.ipcamera.shared.domain.repository.PaginatedResult
import com.company.ipcamera.shared.domain.repository.RecordingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Реализация RecordingRepository с использованием API сервиса
 */
class RecordingRepositoryImpl(
    private val recordingApiService: RecordingApiService
) : RecordingRepository {
    
    override suspend fun getRecordings(
        cameraId: String?,
        startTime: Long?,
        endTime: Long?,
        page: Int,
        limit: Int
    ): PaginatedResult<Recording> = withContext(Dispatchers.Default) {
        try {
            val result = recordingApiService.getRecordings(cameraId, startTime, endTime, page, limit)
            result.fold(
                onSuccess = { paginatedResponse ->
                    PaginatedResult(
                        items = paginatedResponse.items.map { it.toDomain() },
                        total = paginatedResponse.total,
                        page = paginatedResponse.page,
                        limit = paginatedResponse.limit,
                        hasMore = paginatedResponse.hasMore
                    )
                },
                onError = { error ->
                    logger.error(error) { "Error getting recordings" }
                    PaginatedResult(emptyList(), 0, page, limit, false)
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error getting recordings" }
            PaginatedResult(emptyList(), 0, page, limit, false)
        }
    }
    
    override suspend fun getRecordingById(id: String): Recording? = withContext(Dispatchers.Default) {
        try {
            val result = recordingApiService.getRecordingById(id)
            result.fold(
                onSuccess = { dto -> dto.toDomain() },
                onError = { error ->
                    logger.error(error) { "Error getting recording by id: $id" }
                    null
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error getting recording by id: $id" }
            null
        }
    }
    
    override suspend fun addRecording(recording: Recording): Result<Recording> = withContext(Dispatchers.Default) {
        try {
            // Note: API doesn't have direct addRecording, usually recordings are created by starting recording
            // This would need to be implemented based on actual API
            Result.failure(UnsupportedOperationException("Adding recording directly is not supported. Use startRecording instead."))
        } catch (e: Exception) {
            logger.error(e) { "Error adding recording: ${recording.id}" }
            Result.failure(e)
        }
    }
    
    override suspend fun updateRecording(recording: Recording): Result<Recording> = withContext(Dispatchers.Default) {
        try {
            // Note: API doesn't have direct updateRecording
            // This would need to be implemented based on actual API
            Result.failure(UnsupportedOperationException("Updating recording is not supported by API"))
        } catch (e: Exception) {
            logger.error(e) { "Error updating recording: ${recording.id}" }
            Result.failure(e)
        }
    }
    
    override suspend fun deleteRecording(id: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            val result = recordingApiService.deleteRecording(id)
            result.fold(
                onSuccess = { 
                    Result.success(Unit)
                },
                onError = { error ->
                    logger.error(error) { "Error deleting recording: $id" }
                    Result.failure(Exception(error.message))
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error deleting recording: $id" }
            Result.failure(e)
        }
    }
    
    override suspend fun getDownloadUrl(id: String): Result<String> = withContext(Dispatchers.Default) {
        try {
            val result = recordingApiService.getDownloadUrl(id)
            result.fold(
                onSuccess = { response ->
                    val url = response.data?.get("url") ?: throw Exception("Download URL not found")
                    Result.success(url)
                },
                onError = { error ->
                    logger.error(error) { "Error getting download URL for recording: $id" }
                    Result.failure(Exception(error.message))
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error getting download URL for recording: $id" }
            Result.failure(e)
        }
    }
    
    override suspend fun exportRecording(id: String, format: String, quality: String): Result<String> = withContext(Dispatchers.Default) {
        try {
            val request = ExportRecordingRequest(format = format, quality = quality)
            val result = recordingApiService.exportRecording(id, request)
            result.fold(
                onSuccess = { response ->
                    Result.success(response.downloadUrl)
                },
                onError = { error ->
                    logger.error(error) { "Error exporting recording: $id" }
                    Result.failure(Exception(error.message))
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error exporting recording: $id" }
            Result.failure(e)
        }
    }
    
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



