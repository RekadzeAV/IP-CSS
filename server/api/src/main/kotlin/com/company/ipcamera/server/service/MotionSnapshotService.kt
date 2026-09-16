package com.company.ipcamera.server.service

import com.company.ipcamera.server.repository.MotionEventRepository
import com.company.ipcamera.server.service.analytics.RtspFrameSource
import com.company.ipcamera.shared.domain.model.MotionEvent
import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * Сохранение JPEG-снапшотов событий аналитики (движение / объекты).
 * Реальная запись на диск: `snapshots/motion/<id>.jpg`, `snapshots/objects/<id>.jpg`.
 *
 * `captureFromRtsp` выполняет реальный захват кадра через [FfmpegService]
 * (ранее был стаб-нооп).
 */
class MotionSnapshotService(
    private val motionEventRepository: MotionEventRepository? = null
) {
    /** Снапшот события движения (папка snapshots/motion). */
    suspend fun saveSnapshot(event: MotionEvent, imageData: ByteArray): Result<File> {
        return try {
            val dir = File("./snapshots/motion")
            dir.mkdirs()
            val file = File(dir, "${event.id}.jpg")
            file.writeBytes(imageData)
            Result.success(file)
        } catch (e: Exception) {
            logger.error(e) { "Failed to save motion snapshot for event ${event.id}" }
            Result.failure(e)
        }
    }

    /** Снапшот события детекции объектов (папка snapshots/objects). */
    suspend fun saveObjectSnapshot(eventId: String, imageData: ByteArray): Result<File> {
        return try {
            val dir = File("./snapshots/objects")
            dir.mkdirs()
            val file = File(dir, "$eventId.jpg")
            file.writeBytes(imageData)
            Result.success(file)
        } catch (e: Exception) {
            logger.error(e) { "Failed to save object snapshot for event $eventId" }
            Result.failure(e)
        }
    }

    /** Реальный захват одиночного кадра RTSP-потока через ffmpeg. */
    suspend fun captureFromRtsp(url: String, outputFile: File): Result<Unit> {
        return try {
            val source = RtspFrameSource()
            val ok = source.captureJpeg(rtspUrl = url, outputFile = outputFile)
            if (ok) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("captureJpeg failed for $url (ffmpeg missing or timeout)"))
            }
        } catch (e: Exception) {
            logger.error(e) { "captureFromRtsp error for $url" }
            Result.failure(e)
        }
    }
}
