package com.company.ipcamera.shared.test

import com.company.ipcamera.shared.data.datasource.local.RecordingLocalDataSource
import com.company.ipcamera.shared.domain.model.Recording

/**
 * Mock реализация RecordingLocalDataSource для тестов
 */
class MockRecordingLocalDataSource(
    private var recordings: MutableList<Recording> = mutableListOf(),
) : RecordingLocalDataSource {
    var shouldFailOnSave: Boolean = false
    var shouldFailOnUpdate: Boolean = false
    var shouldFailOnDelete: Boolean = false

    override suspend fun getRecordings(): List<Recording> = recordings.toList()

    override suspend fun getRecordingById(id: String): Recording? = recordings.find { it.id == id }

    override suspend fun getRecordingsByCameraId(cameraId: String): List<Recording> {
        return recordings.filter { it.cameraId == cameraId }
    }

    override suspend fun getRecordingsByDateRange(
        startTime: Long,
        endTime: Long,
    ): List<Recording> {
        return recordings.filter { it.startTime >= startTime && (it.endTime ?: Long.MAX_VALUE) <= endTime }
    }

    override suspend fun getRecordingsByStatus(status: String): List<Recording> {
        return recordings.filter { it.status.name == status }
    }

    override suspend fun saveRecording(recording: Recording): Result<Recording> {
        return if (shouldFailOnSave) {
            Result.failure(Exception("Mock save failure"))
        } else {
            recordings.add(recording)
            Result.success(recording)
        }
    }

    override suspend fun saveRecordings(recordings: List<Recording>): Result<List<Recording>> {
        return if (shouldFailOnSave) {
            Result.failure(Exception("Mock save failure"))
        } else {
            this.recordings.addAll(recordings)
            Result.success(recordings)
        }
    }

    override suspend fun updateRecording(recording: Recording): Result<Recording> {
        return if (shouldFailOnUpdate) {
            Result.failure(Exception("Mock update failure"))
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

    override suspend fun updateRecordingStatus(
        id: String,
        status: String,
    ): Result<Unit> {
        val recording = recordings.find { it.id == id }
        return if (recording != null) {
            val updated =
                recording.copy(
                    status = com.company.ipcamera.shared.domain.model.RecordingStatus.valueOf(status),
                )
            val index = recordings.indexOfFirst { it.id == id }
            recordings[index] = updated
            Result.success(Unit)
        } else {
            Result.failure(Exception("Recording not found: $id"))
        }
    }

    override suspend fun deleteRecording(id: String): Result<Unit> {
        return if (shouldFailOnDelete) {
            Result.failure(Exception("Mock delete failure"))
        } else {
            recordings.removeIf { it.id == id }
            Result.success(Unit)
        }
    }

    override suspend fun deleteRecordingsByCameraId(cameraId: String): Result<Unit> {
        recordings.removeIf { it.cameraId == cameraId }
        return Result.success(Unit)
    }

    override suspend fun deleteAllRecordings(): Result<Unit> {
        recordings.clear()
        return Result.success(Unit)
    }

    override suspend fun recordingExists(id: String): Boolean = recordings.any { it.id == id }

    fun clear() {
        recordings.clear()
    }

    fun addRecordingDirectly(recording: Recording) {
        recordings.add(recording)
    }
}
