package com.company.ipcamera.shared.platform.hwaccel

import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.model.Recording
import com.company.ipcamera.shared.domain.model.RecordingFormat

/**
 * Сервис для управления видеозаписью с поддержкой аппаратного ускорения
 */
expect class VideoRecordingService(
    recordingRepository: com.company.ipcamera.shared.domain.repository.RecordingRepository,
    videoEncoder: VideoEncoder,
) {
    val recordingRepository: com.company.ipcamera.shared.domain.repository.RecordingRepository
    val videoEncoder: VideoEncoder

    /**
     * Начать запись с камеры
     */
    suspend fun startRecording(
        camera: Camera,
        format: RecordingFormat,
        duration: Long?,
    ): Result<Recording>

    /**
     * Остановить запись
     */
    suspend fun stopRecording(recordingId: String): Result<Recording>

    /**
     * Приостановить запись
     */
    suspend fun pauseRecording(recordingId: String): Result<Recording>

    /**
     * Возобновить запись
     */
    suspend fun resumeRecording(recordingId: String): Result<Recording>

    /**
     * Получить статус активной записи
     */
    fun getActiveRecording(recordingId: String): Recording?

    /**
     * Проверить, активна ли запись
     */
    fun isRecordingActive(recordingId: String): Boolean
}