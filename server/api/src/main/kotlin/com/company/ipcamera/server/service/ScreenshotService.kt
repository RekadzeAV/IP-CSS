package com.company.ipcamera.server.service

import com.company.ipcamera.core.network.RtspFrame
import com.company.ipcamera.core.network.RtspStreamType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

/**
 * Сервис для создания снимков кадров из видеопотоков
 */
class ScreenshotService(
    private val screenshotsDirectory: String = "screenshots"
) {
    
    init {
        ensureDirectoriesExist()
    }
    
    /**
     * Создать снимок из одного видеокадра (H.264 elementary stream в [RtspFrame.data]).
     * Декодирование через FFmpeg: временный `.h264` → один кадр JPEG.
     */
    suspend fun captureFrame(frame: RtspFrame, cameraId: String): String? {
        if (frame.streamType != RtspStreamType.VIDEO) {
            logger.debug { "captureFrame: skip non-video frame for camera=$cameraId" }
            return null
        }
        if (frame.data.isEmpty()) {
            logger.warn { "captureFrame: empty frame data for camera=$cameraId" }
            return null
        }
        return withContext(Dispatchers.IO) {
            try {
                val timestamp = System.currentTimeMillis()
                val fileName = "${cameraId}_${timestamp}.jpg"
                val outFile = Paths.get(screenshotsDirectory, fileName).toFile()
                val tempRaw = Files.createTempFile("rtsp-h264-${cameraId}-", ".h264").toFile()
                try {
                    tempRaw.writeBytes(frame.data)
                    val ffmpegBin = ffmpegBinary()
                    val args = listOf(
                        ffmpegBin,
                        "-hide_banner",
                        "-loglevel",
                        "error",
                        "-f",
                        "h264",
                        "-i",
                        tempRaw.absolutePath,
                        "-vframes",
                        "1",
                        "-q:v",
                        "2",
                        "-y",
                        outFile.absolutePath
                    )
                    val exit = runFfmpegProcess(args, timeoutSeconds = 15L)
                    if (exit == 0 && outFile.exists() && outFile.length() > 0L) {
                        logger.info { "captureFrame: saved ${outFile.absolutePath} (${outFile.length()} bytes)" }
                        outFile.absolutePath
                    } else {
                        logger.warn {
                            "captureFrame: ffmpeg exit=$exit, output exists=${outFile.exists()} " +
                                "size=${if (outFile.exists()) outFile.length() else 0}"
                        }
                        if (outFile.exists()) outFile.delete()
                        null
                    }
                } finally {
                    if (tempRaw.exists()) tempRaw.delete()
                }
            } catch (e: Exception) {
                logger.error(e) { "captureFrame failed for camera=$cameraId" }
                null
            }
        }
    }
    
    /**
     * Создать снимок используя FFmpeg (для RTSP потока)
     * 
     * @param rtspUrl URL RTSP потока
     * @param cameraId ID камеры
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @return путь к файлу снимка или null при ошибке
     */
    suspend fun captureFromRtsp(
        rtspUrl: String,
        cameraId: String,
        username: String? = null,
        password: String? = null
    ): String? {
        return withContext(Dispatchers.IO) {
            try {
                val urlWithAuth = if (username != null && password != null && !rtspUrl.contains("@")) {
                    val protocol = rtspUrl.substringBefore("://")
                    val rest = rtspUrl.substringAfter("://")
                    "$protocol://$username:$password@$rest"
                } else {
                    rtspUrl
                }

                val timestamp = System.currentTimeMillis()
                val fileName = "${cameraId}_${timestamp}.jpg"
                val filePath = Paths.get(screenshotsDirectory, fileName)
                val file = filePath.toFile()

                val ffmpegBin = ffmpegBinary()
                val args = listOf(
                    ffmpegBin,
                    "-rtsp_transport",
                    "tcp",
                    "-i",
                    urlWithAuth,
                    "-vframes",
                    "1",
                    "-q:v",
                    "2",
                    "-y",
                    file.absolutePath
                )
                val exitCode = runFfmpegProcess(args, timeoutSeconds = 10L)
                if (exitCode == 0 && file.exists()) {
                    logger.info { "Screenshot captured: ${file.absolutePath}" }
                    file.absolutePath
                } else {
                    logger.error { "FFmpeg failed to capture screenshot (exit code: $exitCode)" }
                    null
                }
            } catch (e: Exception) {
                logger.error(e) { "Error capturing screenshot from RTSP: $rtspUrl" }
                null
            }
        }
    }

    private fun ffmpegBinary(): String =
        System.getenv("FFMPEG_PATH")?.trim()?.takeIf { it.isNotEmpty() } ?: "ffmpeg"

    /** @return exit code, or -1 on timeout */
    private fun runFfmpegProcess(args: List<String>, timeoutSeconds: Long): Int {
        val process = ProcessBuilder(args)
            .redirectErrorStream(true)
            .start()
        val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
            return -1
        }
        return process.exitValue()
    }
    
    /**
     * Получить URL для доступа к снимку
     */
    fun getScreenshotUrl(filePath: String): String {
        val fileName = Paths.get(filePath).fileName.toString()
        return "/api/v1/screenshots/$fileName"
    }
    
    /**
     * Убедиться, что директории существуют
     */
    private fun ensureDirectoriesExist() {
        try {
            Files.createDirectories(Paths.get(screenshotsDirectory))
            logger.info { "Created screenshots directory: $screenshotsDirectory" }
        } catch (e: Exception) {
            logger.error(e) { "Error creating screenshots directory" }
        }
    }
    
    /**
     * Очистить старые снимки
     */
    fun cleanupOldScreenshots(maxAgeHours: Int = 24) {
        try {
            val cutoffTime = System.currentTimeMillis() - (maxAgeHours * 60 * 60 * 1000L)
            val dir = File(screenshotsDirectory)
            
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    if (file.lastModified() < cutoffTime) {
                        file.delete()
                        logger.debug { "Deleted old screenshot: ${file.name}" }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error cleaning up old screenshots" }
        }
    }
}

