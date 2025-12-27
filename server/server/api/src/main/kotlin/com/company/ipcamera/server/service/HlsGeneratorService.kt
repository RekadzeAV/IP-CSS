package com.company.ipcamera.server.service

import mu.KotlinLogging
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.io.path.exists
import kotlin.io.path.name

private val logger = KotlinLogging.logger {}

/**
 * Сервис для генерации HLS потоков через FFmpeg
 * 
 * Генерирует HLS сегменты из RTSP потоков для веб-воспроизведения
 */
class HlsGeneratorService(
    private val ffmpegService: FfmpegService,
    private val hlsOutputDirectory: String = "streams/hls"
) {
    private val activeHlsProcesses = mutableMapOf<String, Process>()
    
    init {
        ensureDirectoriesExist()
    }
    
    /**
     * Начать генерацию HLS потока из RTSP источника
     * 
     * @param streamId ID стрима
     * @param rtspUrl URL RTSP потока
     * @param quality Качество потока
     * @return путь к HLS плейлисту или null при ошибке
     */
    fun startHlsGeneration(
        streamId: String,
        rtspUrl: String,
        quality: StreamQuality = StreamQuality.MEDIUM
    ): String? {
        try {
            // Проверяем, не идет ли уже генерация для этого streamId
            if (activeHlsProcesses.containsKey(streamId)) {
                logger.warn { "HLS generation already running for stream: $streamId" }
                return getPlaylistPath(streamId)
            }
            
            // Создаем директорию для HLS сегментов
            val streamDir = Paths.get(hlsOutputDirectory, streamId)
            Files.createDirectories(streamDir)
            
            val playlistPath = getPlaylistPath(streamId)
            val segmentPath = streamDir.resolve("segment_%03d.ts").toString()
            
            // Параметры FFmpeg для генерации HLS
            val args = mutableListOf<String>(
                "ffmpeg",
                "-i", rtspUrl,
                "-c:v", "libx264",
                "-c:a", "aac",
                "-hls_time", "4", // Длительность сегмента в секундах
                "-hls_list_size", "10", // Количество сегментов в плейлисте
                "-hls_flags", "delete_segments+append_list", // Удалять старые сегменты
                "-hls_segment_filename", segmentPath,
                "-hls_allow_cache", "0", // Отключить кэширование для live потоков
                "-f", "hls",
                playlistPath
            )
            
            // Настройки качества
            when (quality) {
                StreamQuality.LOW -> {
                    args.addAll(listOf("-b:v", "500k", "-s", "640x360", "-r", "15"))
                }
                StreamQuality.MEDIUM -> {
                    args.addAll(listOf("-b:v", "1500k", "-s", "1280x720", "-r", "25"))
                }
                StreamQuality.HIGH -> {
                    args.addAll(listOf("-b:v", "3000k", "-s", "1920x1080", "-r", "30"))
                }
                StreamQuality.ULTRA -> {
                    args.addAll(listOf("-b:v", "6000k", "-s", "1920x1080", "-r", "30", "-preset", "fast"))
                }
            }
            
            logger.info { "Starting HLS generation for stream: $streamId, command: ${args.joinToString(" ")}" }
            
            val process = ProcessBuilder(args)
                .directory(File(hlsOutputDirectory))
                .redirectErrorStream(true)
                .start()
            
            activeHlsProcesses[streamId] = process
            
            // Запускаем мониторинг процесса в фоне
            monitorProcess(streamId, process)
            
            // Ждем немного, чтобы убедиться, что процесс запустился
            Thread.sleep(1000)
            
            if (process.isAlive && File(playlistPath).exists()) {
                logger.info { "HLS generation started successfully for stream: $streamId" }
                return playlistPath
            } else {
                logger.error { "Failed to start HLS generation for stream: $streamId" }
                stopHlsGeneration(streamId)
                return null
            }
            
        } catch (e: Exception) {
            logger.error(e) { "Error starting HLS generation for stream: $streamId" }
            activeHlsProcesses.remove(streamId)
            return null
        }
    }
    
    /**
     * Остановить генерацию HLS потока
     */
    fun stopHlsGeneration(streamId: String) {
        try {
            val process = activeHlsProcesses.remove(streamId)
            process?.let {
                if (it.isAlive) {
                    it.destroyForcibly()
                    it.waitFor(5, TimeUnit.SECONDS)
                }
                logger.info { "Stopped HLS generation for stream: $streamId" }
            }
            
            // Удаляем директорию со старыми сегментами (опционально)
            // Можно оставить для кэширования или удалить сразу
            // val streamDir = Paths.get(hlsOutputDirectory, streamId)
            // if (Files.exists(streamDir)) {
            //     Files.walk(streamDir).sorted(Comparator.reverseOrder()).forEach { Files.delete(it) }
            // }
            
        } catch (e: Exception) {
            logger.error(e) { "Error stopping HLS generation for stream: $streamId" }
        }
    }
    
    /**
     * Получить путь к HLS плейлисту
     */
    fun getPlaylistPath(streamId: String): String {
        return Paths.get(hlsOutputDirectory, streamId, "playlist.m3u8").toString()
    }
    
    /**
     * Получить относительный URL для плейлиста
     */
    fun getPlaylistUrl(streamId: String): String {
        return "/api/v1/cameras/streams/$streamId/hls/playlist.m3u8"
    }
    
    /**
     * Проверить, активна ли генерация HLS
     */
    fun isHlsGenerationActive(streamId: String): Boolean {
        val process = activeHlsProcesses[streamId]
        return process != null && process.isAlive
    }
    
    /**
     * Мониторинг процесса FFmpeg
     */
    private fun monitorProcess(streamId: String, process: Process) {
        Thread {
            try {
                val exitCode = process.waitFor()
                activeHlsProcesses.remove(streamId)
                logger.info { "HLS generation process exited with code: $exitCode for stream: $streamId" }
            } catch (e: Exception) {
                logger.error(e) { "Error monitoring HLS generation process for stream: $streamId" }
                activeHlsProcesses.remove(streamId)
            }
        }.start()
    }
    
    /**
     * Убедиться, что директории существуют
     */
    private fun ensureDirectoriesExist() {
        try {
            Files.createDirectories(Paths.get(hlsOutputDirectory))
            logger.info { "Created HLS output directory: $hlsOutputDirectory" }
        } catch (e: Exception) {
            logger.error(e) { "Error creating HLS output directory" }
        }
    }
    
    /**
     * Очистить все активные процессы
     */
    fun cleanup() {
        activeHlsProcesses.keys.forEach { streamId ->
            stopHlsGeneration(streamId)
        }
    }
}


