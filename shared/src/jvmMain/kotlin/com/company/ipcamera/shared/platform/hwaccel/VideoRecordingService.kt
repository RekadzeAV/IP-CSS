package com.company.ipcamera.shared.platform.hwaccel

import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.model.Recording
import com.company.ipcamera.shared.domain.model.RecordingFormat
import com.company.ipcamera.shared.domain.model.RecordingStatus
import com.company.ipcamera.shared.domain.repository.RecordingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * JVM реализация VideoRecordingService
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
    ): Result<Recording> = withContext(Dispatchers.IO) {
        try {
            val recordingId = com.company.ipcamera.shared.common.newRandomId()
            val startTime = com.company.ipcamera.shared.common.nowMillis()

            val outputPath = buildOutputPath(camera, recordingId, format)

            val recording =
                Recording(
                    id = recordingId,
                    cameraId = camera.id,
                    cameraName = camera.name,
                    startTime = startTime,
                    endTime = null,
                    duration = duration ?: 0,
                    filePath = outputPath,
                    fileSize = null,
                    format = format,
                    quality = com.company.ipcamera.shared.domain.model.Quality.HIGH,
                    status = RecordingStatus.ACTIVE,
                    thumbnailUrl = null,
                    createdAt = startTime,
                )

            val codec = com.company.ipcamera.shared.platform.hwaccel.VideoCodec.fromString(camera.codec)
            val encoderResult = videoEncoder.startEncoding(
                inputSource = camera.url,
                outputPath = outputPath,
                codec = codec,
                encoder = null,
            )

            if (encoderResult.isSuccess) {
                activeRecordings[recordingId] = recording
                recordingRepository.addRecording(recording)
                logger.info { "Recording started: $recordingId -> $outputPath" }
                Result.success(recording)
            } else {
                logger.error { "Failed to start encoding for recording $recordingId" }
                Result.failure(Exception("Failed to start video encoder"))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error starting recording" }
            Result.failure(e)
        }
    }

    actual suspend fun stopRecording(recordingId: String): Result<Recording> = withContext(Dispatchers.IO) {
        try {
            val recording = activeRecordings[recordingId]
                ?: return@withContext Result.failure(IllegalArgumentException("Recording not found: $recordingId"))

            val stopResult = videoEncoder.stopEncoding()
            if (stopResult.isSuccess) {
                val endTime = com.company.ipcamera.shared.common.nowMillis()
                val updatedRecording =
                    recording.copy(
                        endTime = endTime,
                        duration = endTime - recording.startTime,
                        status = RecordingStatus.COMPLETED,
                    )

                activeRecordings.remove(recordingId)
                recordingRepository.updateRecording(updatedRecording)
                logger.info { "Recording stopped: $recordingId" }
                Result.success(updatedRecording)
            } else {
                logger.error { "Failed to stop encoding for recording $recordingId" }
                Result.failure(Exception("Failed to stop video encoder"))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error stopping recording" }
            Result.failure(e)
        }
    }

    actual suspend fun pauseRecording(recordingId: String): Result<Recording> = withContext(Dispatchers.IO) {
        try {
            val recording = activeRecordings[recordingId]
                ?: return@withContext Result.failure(IllegalArgumentException("Recording not found: $recordingId"))

            val pauseResult = videoEncoder.pauseEncoding()
            if (pauseResult.isSuccess) {
                val updatedRecording = recording.copy(status = RecordingStatus.PAUSED)
                activeRecordings[recordingId] = updatedRecording
                recordingRepository.updateRecording(updatedRecording)
                logger.info { "Recording paused: $recordingId" }
                Result.success(updatedRecording)
            } else {
                Result.failure(Exception("Failed to pause video encoder"))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error pausing recording" }
            Result.failure(e)
        }
    }

    actual suspend fun resumeRecording(recordingId: String): Result<Recording> = withContext(Dispatchers.IO) {
        try {
            val recording = activeRecordings[recordingId]
                ?: return@withContext Result.failure(IllegalArgumentException("Recording not found: $recordingId"))

            val resumeResult = videoEncoder.resumeEncoding()
            if (resumeResult.isSuccess) {
                val updatedRecording = recording.copy(status = RecordingStatus.ACTIVE)
                activeRecordings[recordingId] = updatedRecording
                recordingRepository.updateRecording(updatedRecording)
                logger.info { "Recording resumed: $recordingId" }
                Result.success(updatedRecording)
            } else {
                Result.failure(Exception("Failed to resume video encoder"))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error resuming recording" }
            Result.failure(e)
        }
    }

    actual fun getActiveRecording(recordingId: String): Recording? {
        return activeRecordings[recordingId]
    }

    actual fun isRecordingActive(recordingId: String): Boolean {
        return activeRecordings.containsKey(recordingId) && videoEncoder.isEncoding()
    }

    private fun buildOutputPath(
        camera: Camera,
        recordingId: String,
        format: RecordingFormat,
    ): String {
        val timestamp = com.company.ipcamera.shared.common.nowMillis()
        val extension = when (format) {
            RecordingFormat.MP4 -> "mp4"
            RecordingFormat.MKV -> "mkv"
            RecordingFormat.AVI -> "avi"
            RecordingFormat.MOV -> "mov"
            RecordingFormat.FLV -> "flv"
        }
        val filename = "${camera.id}_${timestamp}_$recordingId.$extension"
        return "/recordings/$filename"
    }
}