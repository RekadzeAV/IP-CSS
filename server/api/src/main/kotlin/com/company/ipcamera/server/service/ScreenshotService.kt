package com.company.ipcamera.server.service

import com.company.ipcamera.core.network.RtspFrame
import mu.KotlinLogging
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import javax.imageio.ImageIO
import java.awt.image.BufferedImage

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
     * Создать снимок из видеокадра
     * 
     * @param frame RTSP кадр
     * @param cameraId ID камеры
     * @return путь к файлу снимка или null при ошибке
     */
    suspend fun captureFrame(frame: RtspFrame, cameraId: String): String? {
        return try {
            // Создаем имя файла
            val timestamp = System.currentTimeMillis()
            val fileName = "${cameraId}_${timestamp}.jpg"
            val filePath = Paths.get(screenshotsDirectory, fileName)
            val file = filePath.toFile()
            
            // Декодируем кадр в изображение (упрощенная версия)
            // В реальной реализации нужна декодирование H.264 кадра
            // Здесь используем заглушку - в production нужно использовать FFmpeg или нативную библиотеку
            
            // TODO: Реализовать декодирование H.264 кадра в BufferedImage
            // Пока возвращаем null, так как нужно декодирование видеокадра
            logger.warn { "Frame decoding not yet implemented. Frame data size: ${frame.data.size} bytes" }
            null
            
        } catch (e: Exception) {
            logger.error(e) { "Error capturing frame for camera: $cameraId" }
            null
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
        return try {
            // Формируем URL с credentials если нужно
            val urlWithAuth = if (username != null && password != null && !rtspUrl.contains("@")) {
                val protocol = rtspUrl.substringBefore("://")
                val rest = rtspUrl.substringAfter("://")
                "$protocol://$username:$password@$rest"
            } else {
                rtspUrl
            }
            
            // Создаем имя файла
            val timestamp = System.currentTimeMillis()
            val fileName = "${cameraId}_${timestamp}.jpg"
            val filePath = Paths.get(screenshotsDirectory, fileName)
            val file = filePath.toFile()
            
            // Используем FFmpeg для получения кадра
            val args = mutableListOf<String>(
                "ffmpeg",
                "-rtsp_transport", "tcp",
                "-i", urlWithAuth,
                "-vframes", "1", // Только один кадр
                "-q:v", "2", // Высокое качество JPEG
                "-y", // Перезаписать файл если существует
                file.absolutePath
            )
            
            val process = ProcessBuilder(args)
                .redirectErrorStream(true)
                .start()
            
            val exitCode = process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)
            
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

