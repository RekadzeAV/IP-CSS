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

private fun MutableList<String>.addRtspInputSource(rtspUrl: String) {
    addAll(rtspFfmpegInputArgs(rtspUrl))
}

/**
 * Сервис для генерации HLS потоков через FFmpeg
 *
 * Генерирует HLS сегменты из RTSP потоков для веб-воспроизведения
 */
class HlsGeneratorService(
    private val ffmpegService: FfmpegService,
    private val hlsOutputDirectory: String = "streams/hls",
    /** При true — рендции QHD/4K кодируются в HEVC (`libx265`), иначе H.264. См. `HLS_HEVC_FOR_2K4K`. */
    private val hlsHevcFor2K4K: Boolean =
        System.getenv("HLS_HEVC_FOR_2K4K")?.equals("true", ignoreCase = true) == true,
    /** Low-latency HLS режим: уменьшенные сегменты и partial segments (LL-HLS). Значение по умолчанию из env. */
    private val lowLatencyMode: Boolean =
        System.getenv("HLS_LOW_LATENCY_MODE")?.equals("true", ignoreCase = true) == true,
    /** Длительность сегмента в секундах (для low-latency меньше = лучше). По умолчанию 2 сек. */
    private val hlsSegmentDuration: Double =
        System.getenv("HLS_SEGMENT_DURATION")?.toDoubleOrNull() ?: 2.0,
    /** Количество сегментов в плейлисте (меньше = меньше задержка, но выше нагрузка). По умолчанию 3 для low-latency, 6 для standard. */
    private val hlsPlaylistSize: Int =
        System.getenv("HLS_PLAYLIST_SIZE")?.toIntOrNull() ?: if (lowLatencyMode) 3 else 6
) {
    private val activeHlsProcesses = mutableMapOf<String, Process>()
    /** Процессы вариантов адаптивного HLS; [MutableList] чтобы монитор мог удалять завершённые процессы. */
    private val activeAdaptiveStreams = mutableMapOf<String, MutableList<Process>>()
    private val adaptiveStreamsLock = Any()
    private val cleanupScheduler = HlsCleanupScheduler()

    init {
        ensureDirectoriesExist()
        cleanupScheduler.startCleanupScheduler(hlsOutputDirectory)
    }

    private fun useHevcFor(quality: StreamQuality): Boolean =
        hlsHevcFor2K4K && (quality == StreamQuality.QHD1440 || quality == StreamQuality.UHD4K)

    private fun StreamQuality.forcesH264ForHighTier(): Boolean =
        this == StreamQuality.QHD1440_H264 || this == StreamQuality.UHD4K_H264

    /** При HEVC для 2K/4K дублируем те же разрешения в H.264 для master ABR. */
    private fun withHighTierH264Duplicates(qualities: List<StreamQuality>): List<StreamQuality> {
        if (!hlsHevcFor2K4K) return qualities
        val extra = buildList {
            if (qualities.contains(StreamQuality.QHD1440) && !qualities.contains(StreamQuality.QHD1440_H264)) {
                add(StreamQuality.QHD1440_H264)
            }
            if (qualities.contains(StreamQuality.UHD4K) && !qualities.contains(StreamQuality.UHD4K_H264)) {
                add(StreamQuality.UHD4K_H264)
            }
        }
        return qualities + extra
    }

    private fun videoCodecAndAudio(quality: StreamQuality): List<String> {
        val v = when {
            quality.forcesH264ForHighTier() -> "libx264"
            useHevcFor(quality) -> "libx265"
            else -> "libx264"
        }
        return listOf("-c:v", v, "-c:a", "aac")
    }

    private fun masterVariantMeta(quality: StreamQuality): Triple<Int, String, String> {
        val hevc = useHevcFor(quality)
        return when (quality) {
            StreamQuality.LOW -> Triple(564000, "640x360", "avc1.42e01e,mp4a.40.2")
            StreamQuality.MEDIUM -> Triple(1644000, "1280x720", "avc1.4d001f,mp4a.40.2")
            StreamQuality.HIGH -> Triple(3144000, "1920x1080", "avc1.640028,mp4a.40.2")
            StreamQuality.ULTRA -> Triple(6144000, "1920x1080", "avc1.640028,mp4a.40.2")
            StreamQuality.QHD1440_H264 -> Triple(10_400_000, "2560x1440", "avc1.640032,mp4a.40.2")
            StreamQuality.UHD4K_H264 -> Triple(26_000_000, "3840x2160", "avc1.640033,mp4a.40.2")
            StreamQuality.QHD1440 ->
                if (hevc) Triple(8_000_000, "2560x1440", "hvc1.1.6.L93.B0,mp4a.40.2")
                else Triple(10_400_000, "2560x1440", "avc1.640032,mp4a.40.2")
            StreamQuality.UHD4K ->
                if (hevc) Triple(15_000_000, "3840x2160", "hvc1.1.6.L93.B0,mp4a.40.2")
                else Triple(26_000_000, "3840x2160", "avc1.640033,mp4a.40.2")
        }
    }

    private fun transcodeArgsForQuality(quality: StreamQuality): List<String> = when (quality) {
        StreamQuality.LOW -> listOf("-b:v", "500k", "-s", "640x360", "-r", "15")
        StreamQuality.MEDIUM -> listOf("-b:v", "1500k", "-s", "1280x720", "-r", "25")
        StreamQuality.HIGH -> listOf("-b:v", "3000k", "-s", "1920x1080", "-r", "30")
        StreamQuality.ULTRA -> listOf("-b:v", "6000k", "-s", "1920x1080", "-r", "30", "-preset", "fast")
        StreamQuality.QHD1440,
        StreamQuality.QHD1440_H264 -> listOf("-b:v", "10000k", "-s", "2560x1440", "-r", "30", "-preset", "fast")
        StreamQuality.UHD4K,
        StreamQuality.UHD4K_H264 -> listOf("-b:v", "25000k", "-s", "3840x2160", "-r", "30", "-preset", "fast")
    }

    private fun adaptiveTranscodeArgsForQuality(quality: StreamQuality): List<String> = when (quality) {
        StreamQuality.LOW -> listOf(
            "-b:v", "500k",
            "-maxrate", "500k",
            "-bufsize", "1000k",
            "-s", "640x360",
            "-r", "15",
            "-b:a", "64k"
        )
        StreamQuality.MEDIUM -> listOf(
            "-b:v", "1500k",
            "-maxrate", "1500k",
            "-bufsize", "3000k",
            "-s", "1280x720",
            "-r", "25",
            "-b:a", "128k"
        )
        StreamQuality.HIGH -> listOf(
            "-b:v", "3000k",
            "-maxrate", "3000k",
            "-bufsize", "6000k",
            "-s", "1920x1080",
            "-r", "30",
            "-b:a", "192k"
        )
        StreamQuality.ULTRA -> listOf(
            "-b:v", "6000k",
            "-maxrate", "6000k",
            "-bufsize", "12000k",
            "-s", "1920x1080",
            "-r", "30",
            "-preset", "fast",
            "-b:a", "256k"
        )
        StreamQuality.QHD1440,
        StreamQuality.QHD1440_H264 -> listOf(
            "-b:v", "10000k",
            "-maxrate", "10000k",
            "-bufsize", "20000k",
            "-s", "2560x1440",
            "-r", "30",
            "-preset", "fast",
            "-b:a", "256k"
        )
        StreamQuality.UHD4K,
        StreamQuality.UHD4K_H264 -> listOf(
            "-b:v", "25000k",
            "-maxrate", "25000k",
            "-bufsize", "50000k",
            "-s", "3840x2160",
            "-r", "30",
            "-preset", "fast",
            "-b:a", "320k"
        )
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

            // Порядок аргументов FFmpeg: бинарник → ввод → кодеки/битрейт → HLS → выходной плейлист (последним).
            val args = mutableListOf<String>()
            args.add(ffmpegExecutable())
            args.addRtspInputSource(rtspUrl)
            args.addAll(videoCodecAndAudio(quality))
            args.addAll(transcodeArgsForQuality(quality))
            
            // Low-latency HLS оптимизации
            val hlsFlags = if (lowLatencyMode) {
                // LL-HLS: partial segments, independent segments, append list
                "delete_segments+independent_segments+append_list"
            } else {
                "delete_segments+append_list"
            }

            args.addAll(
                listOf(
                    "-hls_time", hlsSegmentDuration.toString(),
                    "-hls_list_size", hlsPlaylistSize.toString(),
                    "-hls_flags", hlsFlags,
                    "-hls_segment_filename", segmentPath,
                    "-hls_allow_cache", "0",
                    "-hls_segment_type", if (lowLatencyMode) "fmp4" else "mpegts",
                    "-f", "hls",
                    playlistPath
                )
            )

            // Low-latency: additional tuning
            if (lowLatencyMode) {
                args.addAll(
                    listOf(
                        "-sc_threshold", "0", // Disable scene change detection
                        "-g", "60", // Keyframe interval (2 sec at 30fps)
                        "-keyint_min", "60"
                    )
                )
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

                        // HLS-генерация стартует ffmpeg в фоне; плейлист будет создан процессом.
            // Возвращаем путь к плейлисту сразу (consistent с адаптивным режимом),
            // даже если ffmpeg ещё не успел создать сегменты.
            logger.info { "HLS generation started for stream: $streamId (playlist: $playlistPath)" }
            return playlistPath

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
     * Используется формат /api/v1/cameras/{cameraId}/stream/hls/playlist.m3u8
     */
    fun getPlaylistUrl(cameraId: String): String {
        return "/api/v1/cameras/$cameraId/stream/hls/playlist.m3u8"
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
    private fun monitorProcess(
        trackingKey: String,
        process: Process,
        adaptiveParentStreamId: String? = null
    ) {
        fun removeAdaptiveChildIfNeeded() {
            if (adaptiveParentStreamId == null) return
            synchronized(adaptiveStreamsLock) {
                val current = activeAdaptiveStreams[adaptiveParentStreamId] ?: return@synchronized
                current.removeAll { it === process }
                if (current.isEmpty()) {
                    activeAdaptiveStreams.remove(adaptiveParentStreamId)
                }
            }
        }

        Thread {
            try {
                val exitCode = process.waitFor()
                activeHlsProcesses.remove(trackingKey)
                removeAdaptiveChildIfNeeded()
                logger.info { "HLS generation process exited with code: $exitCode for stream: $trackingKey" }
            } catch (e: Exception) {
                logger.error(e) { "Error monitoring HLS generation process for stream: $trackingKey" }
                activeHlsProcesses.remove(trackingKey)
                removeAdaptiveChildIfNeeded()
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
     * Начать генерацию HLS из записанного видео файла
     *
     * @param recordingId ID записи
     * @param videoFilePath Путь к видео файлу
     * @param quality Качество потока
     * @return путь к HLS плейлисту или null при ошибке
     */
    fun startHlsFromRecording(
        recordingId: String,
        videoFilePath: String,
        quality: StreamQuality = StreamQuality.MEDIUM
    ): String? {
        try {
            val videoFile = File(videoFilePath)
            if (!videoFile.exists()) {
                logger.error { "Video file not found: $videoFilePath" }
                return null
            }

            // Проверяем, не идет ли уже генерация для этого recordingId
            if (activeHlsProcesses.containsKey(recordingId)) {
                logger.warn { "HLS generation already running for recording: $recordingId" }
                return getRecordingPlaylistPath(recordingId)
            }

            // Создаем директорию для HLS сегментов
            val streamDir = Paths.get(hlsOutputDirectory, "recordings", recordingId)
            Files.createDirectories(streamDir)

            val playlistPath = getRecordingPlaylistPath(recordingId)
            val segmentPath = streamDir.resolve("segment_%03d.ts").toString()

            // Порядок: бинарник → ввод → кодеки → битрейт/разрешение → опции HLS → плейлист (последним).
            val args = mutableListOf<String>()
            args.add(ffmpegExecutable())
            args.addAll(ffmpegRecordingToHlsInputFlags())
            args.addAll(listOf("-i", videoFilePath))
            args.addAll(videoCodecAndAudio(quality))
            args.addAll(transcodeArgsForQuality(quality))
            args.addAll(
                listOf(
                    "-hls_time", "4",
                    "-hls_list_size", "0",
                    "-hls_flags", "delete_segments",
                    "-hls_segment_filename", segmentPath,
                    "-hls_allow_cache", "1",
                    "-f", "hls",
                    playlistPath
                )
            )

            logger.info { "Starting HLS generation from recording: $recordingId, command: ${args.joinToString(" ")}" }

            val process = ProcessBuilder(args)
                .directory(File(hlsOutputDirectory))
                .redirectErrorStream(true)
                .start()

            activeHlsProcesses[recordingId] = process

            // Запускаем мониторинг процесса в фоне
            monitorProcess(recordingId, process)

            // Ждем немного, чтобы убедиться, что процесс запустился
            Thread.sleep(2000)

                        // HLS-генерация со стартом ffmpeg в фоне; возвращаем путь к плейлисту,
            // чтобы вызывающий мог сразу обслуживать маршрут (consistent с adaptive режимом).
            logger.info { "HLS generation started for recording: $recordingId (playlist: $playlistPath)" }
            return playlistPath

        } catch (e: Exception) {
            logger.error(e) { "Error starting HLS generation for recording: $recordingId" }
            activeHlsProcesses.remove(recordingId)
            return null
        }
    }

    /**
     * Получить путь к HLS плейлисту для записи
     */
    fun getRecordingPlaylistPath(recordingId: String): String {
        return Paths.get(hlsOutputDirectory, "recordings", recordingId, "playlist.m3u8").toString()
    }

    /**
     * Получить относительный URL для плейлиста записи
     */
    fun getRecordingPlaylistUrl(recordingId: String): String {
        return "/api/v1/recordings/$recordingId/hls/playlist.m3u8"
    }

    /**
     * Начать генерацию адаптивного HLS потока с несколькими вариантами качества
     * Генерирует master playlist с вариантами: low, medium, high, ultra
     *
     * @param streamId ID стрима
     * @param rtspUrl URL RTSP потока
     * @param qualities Список качеств для генерации (по умолчанию все)
     * @param cameraId ID камеры для формирования URL в master playlist (опционально)
     * @return путь к master playlist или null при ошибке
     */
    fun startAdaptiveHlsGeneration(
        streamId: String,
        rtspUrl: String,
        qualities: List<StreamQuality> = listOf(
            StreamQuality.LOW,
            StreamQuality.MEDIUM,
            StreamQuality.HIGH,
            StreamQuality.ULTRA,
            StreamQuality.QHD1440,
            StreamQuality.UHD4K
        ),
        cameraId: String? = null
    ): String? {
        try {
            val variantQualities = withHighTierH264Duplicates(qualities)
            // Создаем директорию для HLS сегментов
            val streamDir = Paths.get(hlsOutputDirectory, streamId)
            Files.createDirectories(streamDir)

            val processes = mutableListOf<Process>()
            val variantPlaylists = mutableListOf<String>()
            synchronized(adaptiveStreamsLock) {
                if (activeAdaptiveStreams.containsKey(streamId)) {
                    logger.warn { "Adaptive HLS generation already running for stream: $streamId" }
                    return getMasterPlaylistPath(streamId)
                }
                // Регистрируем список до старта процессов, чтобы monitorProcess мог снимать детей при exit.
                activeAdaptiveStreams[streamId] = processes
            }

            // Генерируем каждый вариант качества
            for (quality in variantQualities) {
                val qualityName = quality.name.lowercase()
                val variantDir = streamDir.resolve(qualityName)
                Files.createDirectories(variantDir)

                val variantPlaylistPath = variantDir.resolve("playlist.m3u8").toString()
                val segmentPath = variantDir.resolve("segment_%03d.ts").toString()

                // Порядок: ffmpeg → RTSP-ввод → кодеки и битрейты → опции HLS → плейлист (последним).
                val args = mutableListOf<String>()
                args.add(ffmpegExecutable())
                args.addRtspInputSource(rtspUrl)
                args.addAll(videoCodecAndAudio(quality))
                args.addAll(adaptiveTranscodeArgsForQuality(quality))
                
                // Low-latency HLS оптимизации для адаптивных вариантов
                val hlsFlags = if (lowLatencyMode) {
                    "delete_segments+independent_segments+append_list"
                } else {
                    "delete_segments+append_list"
                }
                
                args.addAll(
                    listOf(
                        "-hls_time", hlsSegmentDuration.toString(),
                        "-hls_list_size", hlsPlaylistSize.toString(),
                        "-hls_flags", hlsFlags,
                        "-hls_segment_filename", segmentPath,
                        "-hls_allow_cache", "0",
                        "-hls_segment_type", if (lowLatencyMode) "fmp4" else "mpegts",
                        "-f", "hls",
                        variantPlaylistPath
                    )
                )

                // Low-latency: additional tuning для адаптивных стримов
                if (lowLatencyMode) {
                    args.addAll(
                        listOf(
                            "-sc_threshold", "0",
                            "-g", "60",
                            "-keyint_min", "60"
                        )
                    )
                }

                logger.info { "Starting HLS variant generation: $qualityName for stream: $streamId" }

                val process = ProcessBuilder(args)
                    .directory(File(hlsOutputDirectory))
                    .redirectErrorStream(true)
                    .start()

                processes.add(process)
                variantPlaylists.add(variantPlaylistPath)

                // Мониторинг процесса (удаление из activeAdaptiveStreams при exit — см. monitorProcess)
                monitorProcess("$streamId-$qualityName", process, adaptiveParentStreamId = streamId)
            }

            // Ждем немного для генерации первых сегментов
            Thread.sleep(5000)

            // Создаем master playlist с корректными URL для вариантов качества
            val masterPlaylistPath = getMasterPlaylistPath(streamId)
            createMasterPlaylist(masterPlaylistPath, streamId, variantQualities, cameraId)

            logger.info { "Adaptive HLS generation started successfully for stream: $streamId" }
            return masterPlaylistPath

        } catch (e: Exception) {
            logger.error(e) { "Error starting adaptive HLS generation for stream: $streamId" }
            stopAdaptiveHlsGeneration(streamId)
            return null
        }
    }

    /**
     * Создать master playlist с вариантами качества
     *
     * @param masterPlaylistPath Путь к master playlist файлу
     * @param streamId ID стрима
     * @param qualities Список качеств для включения в master playlist
     * @param cameraId ID камеры (для формирования правильных URL)
     */
    private fun createMasterPlaylist(
        masterPlaylistPath: String,
        streamId: String,
        qualities: List<StreamQuality>,
        cameraId: String? = null
    ) {
        val masterPlaylist = StringBuilder()
        masterPlaylist.appendLine("#EXTM3U")
        masterPlaylist.appendLine("#EXT-X-VERSION:3")

        for (quality in qualities) {
            val qualityName = quality.name.lowercase()
            // Используем cameraId если доступен, иначе используем streamId в формате streams/{streamId}
            val variantUrl = if (cameraId != null) {
                "/api/v1/cameras/$cameraId/stream/hls/$qualityName/playlist.m3u8"
            } else {
                "/api/v1/cameras/streams/$streamId/hls/$qualityName/playlist.m3u8"
            }

            val (bandwidth, resolution, codecs) = masterVariantMeta(quality)

            masterPlaylist.appendLine("#EXT-X-STREAM-INF:BANDWIDTH=$bandwidth,RESOLUTION=$resolution,CODECS=\"$codecs\"")
            masterPlaylist.appendLine(variantUrl)
        }

        File(masterPlaylistPath).writeText(masterPlaylist.toString())
        logger.info { "Created master playlist: $masterPlaylistPath" }
    }

    /**
     * Остановить генерацию адаптивного HLS потока
     */
    fun stopAdaptiveHlsGeneration(streamId: String) {
        try {
            val processes = synchronized(adaptiveStreamsLock) {
                activeAdaptiveStreams.remove(streamId)
            }
            processes?.forEach { process ->
                if (process.isAlive) {
                    process.destroyForcibly()
                    process.waitFor(5, TimeUnit.SECONDS)
                }
            }
            logger.info { "Stopped adaptive HLS generation for stream: $streamId" }
        } catch (e: Exception) {
            logger.error(e) { "Error stopping adaptive HLS generation for stream: $streamId" }
        }
    }

    /**
     * Получить путь к master playlist
     */
    fun getMasterPlaylistPath(streamId: String): String {
        return Paths.get(hlsOutputDirectory, streamId, "master.m3u8").toString()
    }

    /**
     * Получить относительный URL для master playlist
     */
    fun getMasterPlaylistUrl(cameraId: String): String {
        return "/api/v1/cameras/$cameraId/stream/hls/master.m3u8"
    }

    /**
     * Очистить все активные процессы
     */
    fun cleanup() {
        activeHlsProcesses.keys.toList().forEach { streamId ->
            stopHlsGeneration(streamId)
        }
        synchronized(adaptiveStreamsLock) {
            activeAdaptiveStreams.keys.toList()
        }.forEach { streamId ->
            stopAdaptiveHlsGeneration(streamId)
        }
        cleanupScheduler.stop()
    }
}


