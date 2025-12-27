package com.company.ipcamera.server.service

import com.company.ipcamera.shared.domain.model.Quality
import com.company.ipcamera.shared.domain.model.RecordingFormat
import mu.KotlinLogging
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

/**
 * Сервис для работы с FFmpeg
 * 
 * Предоставляет функциональность для:
 * - Кодирования видео потоков в различные форматы
 * - Генерации thumbnail'ов
 * - Конвертации видео
 */
class FfmpegService {
    
    /**
     * Проверка доступности FFmpeg
     */
    fun isAvailable(): Boolean {
        return try {
            val process = ProcessBuilder("ffmpeg", "-version")
                .redirectErrorStream(true)
                .start()
            
            val exitCode = process.waitFor(5, TimeUnit.SECONDS)
            process.destroy()
            exitCode == 0
        } catch (e: Exception) {
            logger.debug(e) { "FFmpeg not available" }
            false
        }
    }
    
    /**
     * Кодирование RTSP потока в файл
     * 
     * @param rtspUrl URL RTSP потока
     * @param outputFile Выходной файл
     * @param format Формат записи
     * @param quality Качество записи
     * @param duration Длительность записи в секундах (null = бесконечная)
     * @param username Имя пользователя для RTSP (опционально)
     * @param password Пароль для RTSP (опционально)
     * @return Process процесс FFmpeg
     */
    fun encodeRtspToFile(
        rtspUrl: String,
        outputFile: File,
        format: RecordingFormat,
        quality: Quality,
        duration: Long? = null,
        username: String? = null,
        password: String? = null
    ): Process {
        val args = mutableListOf<String>()
        
        // FFmpeg команда
        args.add("ffmpeg")
        
        // RTSP опции
        args.add("-rtsp_transport")
        args.add("tcp") // Используем TCP для стабильности
        
        args.add("-i")
        // Добавляем credentials в URL если они есть
        val inputUrl = if (username != null && password != null) {
            val url = rtspUrl.replace("rtsp://", "")
            val hostAndPath = if (url.contains("/")) {
                val parts = url.split("/", limit = 2)
                "${parts[0]}/${parts[1]}"
            } else {
                url
            }
            "rtsp://$username:$password@$hostAndPath"
        } else {
            rtspUrl
        }
        args.add(inputUrl)
        
        // Длительность записи
        if (duration != null) {
            args.add("-t")
            args.add(duration.toString())
        }
        
        // Кодеки и качество
        when (format) {
            RecordingFormat.MP4 -> {
                args.add("-c:v")
                args.add("libx264") // H.264 кодек
                args.add("-c:a")
                args.add("aac") // AAC аудио кодек
                args.add("-preset")
                args.add("medium") // Баланс скорости и качества
                args.add("-movflags")
                args.add("+faststart") // Для веб-воспроизведения
            }
            RecordingFormat.MKV -> {
                args.add("-c:v")
                args.add("libx264")
                args.add("-c:a")
                args.add("aac")
                args.add("-preset")
                args.add("medium")
            }
            RecordingFormat.AVI -> {
                args.add("-c:v")
                args.add("libx264")
                args.add("-c:a")
                args.add("libmp3lame")
            }
            RecordingFormat.MOV -> {
                args.add("-c:v")
                args.add("libx264")
                args.add("-c:a")
                args.add("aac")
                args.add("-movflags")
                args.add("+faststart")
            }
            RecordingFormat.FLV -> {
                args.add("-c:v")
                args.add("libx264")
                args.add("-c:a")
                args.add("libmp3lame")
            }
        }
        
        // Настройки качества
        when (quality) {
            Quality.LOW -> {
                args.add("-crf")
                args.add("28") // Низкое качество, меньший размер
                args.add("-b:v")
                args.add("500k")
            }
            Quality.MEDIUM -> {
                args.add("-crf")
                args.add("23") // Среднее качество
                args.add("-b:v")
                args.add("2000k")
            }
            Quality.HIGH -> {
                args.add("-crf")
                args.add("20") // Высокое качество
                args.add("-b:v")
                args.add("4000k")
            }
            Quality.ULTRA -> {
                args.add("-crf")
                args.add("18") // Максимальное качество
                args.add("-b:v")
                args.add("8000k")
            }
        }
        
        // Аудио опции
        args.add("-ar")
        args.add("44100") // Частота дискретизации
        args.add("-ac")
        args.add("2") // Стерео
        
        // Выходной файл
        args.add("-y") // Перезаписать файл если существует
        args.add(outputFile.absolutePath)
        
        logger.info { "FFmpeg command: ${args.joinToString(" ")}" }
        
        val process = ProcessBuilder(args)
            .redirectErrorStream(true)
            .directory(outputFile.parentFile)
            .start()
        
        return process
    }
    
    /**
     * Генерация thumbnail из видео файла
     * 
     * @param videoFile Входной видео файл
     * @param thumbnailFile Выходной файл thumbnail
     * @param timeOffset Смещение времени в секундах (по умолчанию 1 секунда)
     * @param width Ширина thumbnail (по умолчанию 320)
     * @return true если успешно
     */
    fun generateThumbnail(
        videoFile: File,
        thumbnailFile: File,
        timeOffset: Double = 1.0,
        width: Int = 320
    ): Boolean {
        if (!videoFile.exists()) {
            logger.error { "Video file not found: ${videoFile.absolutePath}" }
            return false
        }
        
        // Создаем директорию для thumbnail если не существует
        thumbnailFile.parentFile?.mkdirs()
        
        val args = listOf(
            "ffmpeg",
            "-i", videoFile.absolutePath,
            "-ss", timeOffset.toString(),
            "-vframes", "1",
            "-vf", "scale=$width:-1",
            "-y",
            thumbnailFile.absolutePath
        )
        
        return try {
            val process = ProcessBuilder(args)
                .redirectErrorStream(true)
                .start()
            
            val exitCode = process.waitFor(30, TimeUnit.SECONDS)
            
            if (exitCode == 0 && thumbnailFile.exists()) {
                logger.info { "Thumbnail generated: ${thumbnailFile.absolutePath}" }
                true
            } else {
                logger.warn { "FFmpeg failed to generate thumbnail (exit code: $exitCode)" }
                false
            }
        } catch (e: Exception) {
            logger.error(e) { "Error generating thumbnail" }
            false
        }
    }
    
    /**
     * Получить информацию о видео файле
     * 
     * @param videoFile Видео файл
     * @return Map с информацией о видео или null при ошибке
     */
    fun getVideoInfo(videoFile: File): Map<String, String>? {
        if (!videoFile.exists()) {
            return null
        }
        
        val args = listOf(
            "ffprobe",
            "-v", "quiet",
            "-print_format", "json",
            "-show_format",
            "-show_streams",
            videoFile.absolutePath
        )
        
        return try {
            val process = ProcessBuilder(args)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor(10, TimeUnit.SECONDS)
            
            if (exitCode == 0) {
                // Парсим JSON (упрощенная версия)
                // В продакшене лучше использовать JSON библиотеку
                val info = mutableMapOf<String, String>()
                
                // Извлекаем базовую информацию
                val durationMatch = Regex("\"duration\":\\s*\"([^\"]+)\"").find(output)
                durationMatch?.let {
                    info["duration"] = it.groupValues[1]
                }
                
                val sizeMatch = Regex("\"size\":\\s*\"([^\"]+)\"").find(output)
                sizeMatch?.let {
                    info["size"] = it.groupValues[1]
                }
                
                val widthMatch = Regex("\"width\":\\s*(\\d+)").find(output)
                widthMatch?.let {
                    info["width"] = it.groupValues[1]
                }
                
                val heightMatch = Regex("\"height\":\\s*(\\d+)").find(output)
                heightMatch?.let {
                    info["height"] = it.groupValues[1]
                }
                
                info
            } else {
                null
            }
        } catch (e: Exception) {
            logger.error(e) { "Error getting video info" }
            null
        }
    }
    
    /**
     * Конвертация видео в другой формат
     * 
     * @param inputFile Входной файл
     * @param outputFile Выходной файл
     * @param outputFormat Целевой формат
     * @return true если успешно
     */
    fun convertVideo(
        inputFile: File,
        outputFile: File,
        outputFormat: RecordingFormat
    ): Boolean {
        if (!inputFile.exists()) {
            return false
        }
        
        outputFile.parentFile?.mkdirs()
        
        val args = mutableListOf<String>(
            "ffmpeg",
            "-i", inputFile.absolutePath
        )
        
        // Настройки кодеков в зависимости от формата
        when (outputFormat) {
            RecordingFormat.MP4 -> {
                args.add("-c:v")
                args.add("libx264")
                args.add("-c:a")
                args.add("aac")
                args.add("-movflags")
                args.add("+faststart")
            }
            RecordingFormat.MKV -> {
                args.add("-c:v")
                args.add("libx264")
                args.add("-c:a")
                args.add("aac")
            }
            RecordingFormat.AVI -> {
                args.add("-c:v")
                args.add("libx264")
                args.add("-c:a")
                args.add("libmp3lame")
            }
            RecordingFormat.MOV -> {
                args.add("-c:v")
                args.add("libx264")
                args.add("-c:a")
                args.add("aac")
            }
            RecordingFormat.FLV -> {
                args.add("-c:v")
                args.add("libx264")
                args.add("-c:a")
                args.add("libmp3lame")
            }
        }
        
        args.add("-y")
        args.add(outputFile.absolutePath)
        
        return try {
            val process = ProcessBuilder(args)
                .redirectErrorStream(true)
                .start()
            
            val exitCode = process.waitFor(300, TimeUnit.SECONDS) // 5 минут максимум
            
            exitCode == 0 && outputFile.exists()
        } catch (e: Exception) {
            logger.error(e) { "Error converting video" }
            false
        }
    }
}

