package com.company.ipcamera.shared.test

import com.company.ipcamera.shared.domain.model.Recording
import com.company.ipcamera.shared.domain.repository.PaginatedResult
import com.company.ipcamera.shared.domain.repository.RecordingRepository

/**
 * Переиспользуемый mock реализации RecordingRepository для тестов Use Cases
 */
class MockRecordingRepository(
    private var recordings: MutableList<Recording> = mutableListOf()
) : RecordingRepository {

    var shouldFailOnAdd: Boolean = false
    var shouldFailOnUpdate: Boolean = false
    var shouldFailOnDelete: Boolean = false
    var addError: Exception? = null
    var updateError: Exception? = null
    var deleteError: Exception? = null

    override suspend fun getRecordings(
        cameraId: String?,
        startTime: Long?,
        endTime: Long?,
        page: Int,
        limit: Int
    ): PaginatedResult<Recording> {
        var filtered = recordings.toList()

        // Фильтрация по cameraId
        if (cameraId != null) {
            filtered = filtered.filter { it.cameraId == cameraId }
        }

        // Фильтрация по времени
        if (startTime != null) {
            filtered = filtered.filter { it.startTime >= startTime }
        }
        if (endTime != null) {
            filtered = filtered.filter { it.endTime == null || it.endTime!! <= endTime }
        }

        val total = filtered.size
        val offset = (page - 1) * limit
        val paginatedItems = filtered.drop(offset).take(limit)

        return PaginatedResult(
            items = paginatedItems,
            total = total,
            page = page,
            limit = limit,
            hasMore = offset + paginatedItems.size < total
        )
    }

    override suspend fun getRecordingById(id: String): Recording? {
        return recordings.find { it.id == id }
    }

    override suspend fun addRecording(recording: Recording): Result<Recording> {
        return if (shouldFailOnAdd) {
            Result.failure(addError ?: Exception("Mock repository add failure"))
        } else {
            recordings.add(recording)
            Result.success(recording)
        }
    }

    override suspend fun updateRecording(recording: Recording): Result<Recording> {
        return if (shouldFailOnUpdate) {
            Result.failure(updateError ?: Exception("Mock repository update failure"))
        } else {
            val index = recordings.indexOfFirst { it.id == recording.id }
            if (index >= 0) {
                recordings[index] = recording
                Result.success(recording)
            } else {
                Result.failure(Exception("Recording not found: ${recording.id}"))
            }
        }
    }

    override suspend fun deleteRecording(id: String): Result<Unit> {
        return if (shouldFailOnDelete) {
            Result.failure(deleteError ?: Exception("Mock repository delete failure"))
        } else {
            val removed = recordings.removeIf { it.id == id }
            if (removed) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Recording not found: $id"))
            }
        }
    }

    override suspend fun getDownloadUrl(id: String): Result<String> {
        val recording = recordings.find { it.id == id }
        return if (recording != null) {
            Result.success("/api/v1/recordings/$id/download")
        } else {
            Result.failure(Exception("Recording not found: $id"))
        }
    }

    override suspend fun exportRecording(id: String, format: String, quality: String): Result<String> {
        val recording = recordings.find { it.id == id }
        return if (recording != null) {
            Result.success("/api/v1/recordings/$id/export?format=$format&quality=$quality")
        } else {
            Result.failure(Exception("Recording not found: $id"))
        }
    }

    /**
     * Очистить все записи из mock репозитория
     */
    fun clear() {
        recordings.clear()
    }

    /**
     * Добавить запись напрямую (без Result)
     */
    fun addRecordingDirectly(recording: Recording) {
        recordings.add(recording)
    }
}

