package com.company.ipcamera.shared.platform.hwaccel

import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.model.Recording
import com.company.ipcamera.shared.domain.model.RecordingFormat
import com.company.ipcamera.shared.domain.model.RecordingStatus
import com.company.ipcamera.shared.domain.repository.RecordingRepository

/**
 * Android реализация VideoRecordingService
 * Android использует MediaCodec для HW ускорения, FFmpeg не доступен напрямую
 */
actual class VideoRecordingService actual constructor(
    actual val recordingRepository: RecordingRepository,
    actual val videoEncoder: VideoEncoder,
) {
    private val activeRecordings = mutableMapOf<String, Recording>()

    actual suspend fun startRecording(
        camera: Camera,
        format: RecordingFormat,
        duration: Long?,
    ): Result<Recording> {
        val recordingId = com.company.ipcamera.shared.common.newRandomId()
        val startTime = com.company.ipcamera.shared.common.nowMillis()

        val recording =
            Recording(
                id = recordingId,
                cameraId = camera.id,
                cameraName = camera.name,
                startTime = startTime,
                endTime = null,
                duration = duration ?: 0,
                filePath = null,
                fileSize = null,
                format = format,
                quality = com.company.ipcamera.shared.domain.model.Quality.HIGH,
                status = RecordingStatus.ACTIVE,
                thumbnailUrl = null,
                createdAt = startTime,
            )

        activeRecordings[recordingId] = recording
        recordingRepository.addRecording(recording)
        return Result.success(recording)
    }

    actual suspend fun stopRecording(recordingId: String): Result<Recording> {
        val recording = activeRecordings[recordingId]
            ?: return Result.failure(IllegalArgumentException("Recording not found: $recordingId"))

        val endTime = com.company.ipcamera.shared.common.nowMillis()
        val updatedRecording =
            recording.copy(
                endTime = endTime,
                duration = endTime - recording.startTime,
                status = RecordingStatus.COMPLETED,
            )

        activeRecordings.remove(recordingId)
        recordingRepository.updateRecording(updatedRecording)
        return Result.success(updatedRecording)
    }

    actual suspend fun pauseRecording(recordingId: String): Result<Recording> {
        val recording = activeRecordings[recordingId]
            ?: return Result.failure(IllegalArgumentException("Recording not found: $recordingId"))

        val updatedRecording = recording.copy(status = RecordingStatus.PAUSED)
        activeRecordings[recordingId] = updatedRecording
        recordingRepository.updateRecording(updatedRecording)
        return Result.success(updatedRecording)
    }

    actual suspend fun resumeRecording(recordingId: String): Result<Recording> {
        val recording = activeRecordings[recordingId]
            ?: return Result.failure(IllegalArgumentException("Recording not found: $recordingId"))

        val updatedRecording = recording.copy(status = RecordingStatus.ACTIVE)
        activeRecordings[recordingId] = updatedRecording
        recordingRepository.updateRecording(updatedRecording)
        return Result.success(updatedRecording)
    }

    actual fun getActiveRecording(recordingId: String): Recording? {
        return activeRecordings[recordingId]
    }

    actual fun isRecordingActive(recordingId: String): Boolean {
        return activeRecordings.containsKey(recordingId)
    }
}