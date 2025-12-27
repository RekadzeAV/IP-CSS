package com.company.ipcamera.server.service

import com.company.ipcamera.shared.domain.model.Quality
import com.company.ipcamera.shared.domain.model.RecordingFormat
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mu.KotlinLogging
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}
private val json = Json { ignoreUnknownKeys = true }

/**
 * Информация о видео из FFprobe
 */
@Serializable
data class VideoInfo(
    val duration: String? = null,
    val size: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val bitrate: String? = null,
    val codec: String? = null,
    val fps: String? = null
)

/**
 * Тип аппаратного ускорения
 */
enum class HardwareAcceleration {
    NONE,
    NVIDIA,  // h264_nvenc, hevc_nvenc
    INTEL,   // h264_qsv, hevc_qsv
    AMD      // h264_amf, hevc_amf
}

/**
 * Сервис для работы с FFmpeg
 *
 * Предоставляет функциональность для:
 * - Кодирования видео потоков в различные форматы
 * - Генерации thumbnail'ов
 * - Конвертации видео
 * - Аппаратного ускорения (NVIDIA/Intel/AMD)
 * - Поддержки H.265/HEVC
 */
class FfmpegService {

    private var cachedHardwareAcceleration: HardwareAcceleration? = null

    /**
     * Обнаружение доступного аппаратного ускорения
     */
    fun detectHardwareAcceleration(): HardwareAcceleration {
        if (cachedHardwareAcceleration != null) {
            return cachedHardwareAcceleration!!
        }

        if (!isAvailable()) {
            cachedHardwareAcceleration = HardwareAcceleration.NONE
            return HardwareAcceleration.NONE
        }

        // Проверяем NVIDIA (h264_nvenc)
        if (checkEncoderAvailable("h264_nvenc")) {
            logger.info { "NVIDIA hardware acceleration detected (h264_nvenc)" }
            cachedHardwareAcceleration = HardwareAcceleration.NVIDIA
            return HardwareAcceleration.NVIDIA
        }

        // Проверяем Intel (h264_qsv)
        if (checkEncoderAvailable("h264_qsv")) {
            logger.info { "Intel hardware acceleration detected (h264_qsv)" }
            cachedHardwareAcceleration = HardwareAcceleration.INTEL
            return HardwareAcceleration.INTEL
        }

        // Проверяем AMD (h264_amf)
        if (checkEncoderAvailable("h264_amf")) {
            logger.info { "AMD hardware acceleration detected (h264_amf)" }
            cachedHardwareAcceleration = HardwareAcceleration.AMD
            return HardwareAcceleration.AMD
        }

        logger.info { "No hardware acceleration detected, using software encoding" }
        cachedHardwareAcceleration = HardwareAcceleration.NONE
        return HardwareAcceleration.NONE
    }

    /**
     * Проверка доступности кодера
     */
    private fun checkEncoderAvailable(encoder: String): Boolean {
        return try {
            val process = ProcessBuilder("ffmpeg", "-hide_banner", "-encoders")
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor(5, TimeUnit.SECONDS)
            process.destroy()

            exitCode == 0 && output.contains(encoder)
        } catch (e: Exception) {
            logger.debug(e) { "Error checking encoder: $encoder" }
            false
        }
    }

    /**
     * Получить кодек для видео с учетом аппаратного ускорения и формата
     */
    private fun getVideoCodec(format: RecordingFormat, useH265: Boolean = false): String {
        val hwAccel = detectHardwareAcceleration()

        if (useH265) {
            return when (hwAccel) {
                HardwareAcceleration.NVIDIA -> "hevc_nvenc"
                HardwareAcceleration.INTEL -> "hevc_qsv"
                HardwareAcceleration.AMD -> "hevc_amf"
                HardwareAcceleration.NONE -> "libx265"
            }
        }

        return when (hwAccel) {
            HardwareAcceleration.NVIDIA -> "h264_nvenc"
            HardwareAcceleration.INTEL -> "h264_qsv"
            HardwareAcceleration.AMD -> "h264_amf"
            HardwareAcceleration.NONE -> "libx264"
        }
    }

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
     * @param useH265 Использовать H.265/HEVC кодек вместо H.264 (по умолчанию false)
     * @return Process процесс FFmpeg
     */
    fun encodeRtspToFile(
        rtspUrl: String,
        outputFile: File,
        format: RecordingFormat,
        quality: Quality,
        duration: Long? = null,
        username: String? = null,
        password: String? = null,
        useH265: Boolean = false
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

        // Определяем кодек (H.264 или H.265)
        val videoCodec = getVideoCodec(format, useH265 = useH265)
        val hwAccel = detectHardwareAcceleration()

        // Кодеки и качество
        when (format) {
            RecordingFormat.MP4 -> {
                args.add("-c:v")
                args.add(videoCodec)

                // Настройки для аппаратного ускорения
                if (hwAccel != HardwareAcceleration.NONE) {
                    when (hwAccel) {
                        HardwareAcceleration.NVIDIA -> {
                            args.add("-preset")
                            args.add("p4") // NVIDIA preset (p1-p7, p4 = balanced)
                            args.add("-rc")
                            args.add("vbr") // Variable bitrate
                        }
                        HardwareAcceleration.INTEL -> {
                            args.add("-preset")
                            args.add("medium")
                        }
                        HardwareAcceleration.AMD -> {
                            args.add("-quality")
                            args.add("balanced")
                        }
                        else -> {}
                    }
                } else {
                    args.add("-preset")
                    args.add("medium") // Баланс скорости и качества
                    args.add("-tune")
                    args.add("zerolatency") // Минимальная задержка
                }

                args.add("-c:a")
                args.add("aac") // AAC аудио кодек
                args.add("-movflags")
                args.add("+faststart") // Для веб-воспроизведения
            }
            RecordingFormat.MKV -> {
                args.add("-c:v")
                args.add(videoCodec)
                if (hwAccel == HardwareAcceleration.NONE) {
                    args.add("-preset")
                    args.add("medium")
                }
                args.add("-c:a")
                args.add("aac")
            }
            RecordingFormat.AVI -> {
                args.add("-c:v")
                args.add(videoCodec)
                if (hwAccel == HardwareAcceleration.NONE) {
                    args.add("-preset")
                    args.add("medium")
                }
                args.add("-c:a")
                args.add("libmp3lame")
            }
            RecordingFormat.MOV -> {
                args.add("-c:v")
                args.add(videoCodec)
                if (hwAccel == HardwareAcceleration.NONE) {
                    args.add("-preset")
                    args.add("medium")
                }
                args.add("-c:a")
                args.add("aac")
                args.add("-movflags")
                args.add("+faststart")
            }
            RecordingFormat.FLV -> {
                args.add("-c:v")
                args.add(videoCodec)
                if (hwAccel == HardwareAcceleration.NONE) {
                    args.add("-preset")
                    args.add("medium")
                }
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
     * @return VideoInfo с информацией о видео или null при ошибке
     */
    fun getVideoInfo(videoFile: File): VideoInfo? {
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
                // Парсим JSON используя kotlinx.serialization
                val jsonObject = json.parseToJsonElement(output).jsonObject

                // Получаем информацию о формате
                val format = jsonObject["format"]?.jsonObject
                val duration = format?.get("duration")?.jsonPrimitive?.content
                val size = format?.get("size")?.jsonPrimitive?.content
                val bitrate = format?.get("bit_rate")?.jsonPrimitive?.content

                // Получаем информацию о видеопотоке
                val streamsArray = jsonObject["streams"]?.jsonArray
                var width: Int? = null
                var height: Int? = null
                var codec: String? = null
                var fps: String? = null

                streamsArray?.forEach { streamElement ->
                    val stream = streamElement.jsonObject
                    val codecType = stream["codec_type"]?.jsonPrimitive?.content

                    if (codecType == "video") {
                        width = stream["width"]?.jsonPrimitive?.content?.toIntOrNull()
                        height = stream["height"]?.jsonPrimitive?.content?.toIntOrNull()
                        codec = stream["codec_name"]?.jsonPrimitive?.content

                        // Вычисляем FPS
                        val rFrameRate = stream["r_frame_rate"]?.jsonPrimitive?.content
                        rFrameRate?.let {
                            val parts = it.split("/")
                            if (parts.size == 2) {
                                val num = parts[0].toDoubleOrNull()
                                val den = parts[1].toDoubleOrNull()
                                if (num != null && den != null && den != 0.0) {
                                    fps = String.format("%.2f", num / den)
                                }
                            }
                        }
                    }
                }

                VideoInfo(
                    duration = duration,
                    size = size,
                    width = width,
                    height = height,
                    bitrate = bitrate,
                    codec = codec,
                    fps = fps
                )
            } else {
                null
            }
        } catch (e: Exception) {
            logger.error(e) { "Error getting video info" }
            null
        }
    }

    /**
     * Получить информацию о видео файле (старый формат для обратной совместимости)
     */
    fun getVideoInfoMap(videoFile: File): Map<String, String>? {
        val info = getVideoInfo(videoFile) ?: return null
        return buildMap {
            info.duration?.let { put("duration", it) }
            info.size?.let { put("size", it) }
            info.width?.let { put("width", it.toString()) }
            info.height?.let { put("height", it.toString()) }
            info.bitrate?.let { put("bitrate", it) }
            info.codec?.let { put("codec", it) }
            info.fps?.let { put("fps", it) }
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

        // Определяем кодек с учетом аппаратного ускорения
        val videoCodec = getVideoCodec(outputFormat, useH265 = false)
        val hwAccel = detectHardwareAcceleration()

        // Настройки кодеков в зависимости от формата
        when (outputFormat) {
            RecordingFormat.MP4 -> {
                args.add("-c:v")
                args.add(videoCodec)
                if (hwAccel == HardwareAcceleration.NONE) {
                    args.add("-preset")
                    args.add("medium")
                }
                args.add("-c:a")
                args.add("aac")
                args.add("-movflags")
                args.add("+faststart")
            }
            RecordingFormat.MKV -> {
                args.add("-c:v")
                args.add(videoCodec)
                if (hwAccel == HardwareAcceleration.NONE) {
                    args.add("-preset")
                    args.add("medium")
                }
                args.add("-c:a")
                args.add("aac")
            }
            RecordingFormat.AVI -> {
                args.add("-c:v")
                args.add(videoCodec)
                if (hwAccel == HardwareAcceleration.NONE) {
                    args.add("-preset")
                    args.add("medium")
                }
                args.add("-c:a")
                args.add("libmp3lame")
            }
            RecordingFormat.MOV -> {
                args.add("-c:v")
                args.add(videoCodec)
                if (hwAccel == HardwareAcceleration.NONE) {
                    args.add("-preset")
                    args.add("medium")
                }
                args.add("-c:a")
                args.add("aac")
            }
            RecordingFormat.FLV -> {
                args.add("-c:v")
                args.add(videoCodec)
                if (hwAccel == HardwareAcceleration.NONE) {
                    args.add("-preset")
                    args.add("medium")
                }
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

