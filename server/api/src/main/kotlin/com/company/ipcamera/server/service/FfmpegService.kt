package com.company.ipcamera.server.service

import com.company.ipcamera.shared.domain.model.Quality
import com.company.ipcamera.shared.domain.model.RecordingFormat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.contentOrNull
import mu.KotlinLogging
import java.io.File
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

/**
 * Реальная реализация FfmpegService: вызывает внешние бинарники `ffmpeg`/`ffprobe`
 * через ProcessBuilder. Детект бинарников: env `FFMPEG_PATH`/`FFPROBE_PATH` → PATH.
 *
 * При недоступном ffmpeg: `isAvailable()=false`, операции возвращают `false`,
 * `getVideoInfo` возвращает null — роуты корректно это отрабатывают
 * (`if (success && outputFile.exists())`).
 *
 * Заменяет прежний стаб (echo-процессы, фейковые 1920x1080).
 */
class FfmpegService(
    private val ffmpegPath: String? = System.getenv("FFMPEG_PATH"),
    private val ffprobePath: String? = System.getenv("FFPROBE_PATH"),
) {

    private val ffmpegBinary: String by lazy {
        ffmpegPath ?: findOnPath("ffmpeg") ?: "ffmpeg"
    }
    private val ffprobeBinary: String by lazy {
        ffprobePath ?: findOnPath("ffprobe") ?: "ffprobe"
    }

    /** Качество → целевой битрейт (kbps). */
    private fun bitrateFor(quality: Quality): String = when (quality) {
        Quality.LOW -> "1000k"
        Quality.MEDIUM -> "2500k"
        Quality.HIGH -> "4000k"
        Quality.ULTRA -> "8000k"
    }

    private fun formatName(format: RecordingFormat): String = format.name.lowercase()

    private fun codecArgs(useH265: Boolean): List<String> =
        if (useH265) listOf("-c:v", "libx265") else listOf("-c:v", "libx264")

    fun encodeRtspToFile(
        rtspUrl: String,
        outputFile: File,
        format: RecordingFormat,
        quality: Quality,
        duration: Long?,
        username: String? = null,
        password: String? = null,
        useH265: Boolean = false,
    ): Process {
        val creds = if (!username.isNullOrBlank()) {
            listOf("-rtsp_transport", "tcp", "-i", rtspUrl.insertCredentials(username, password))
        } else {
            listOf("-rtsp_transport", "tcp", "-i", rtspUrl)
        }
        val command = listOf(
            ffmpegBinary,
            "-y",
            "-hide_banner",
            "-loglevel", "error",
            *creds.toTypedArray(),
            "-t", duration?.let { (it / 1000.0).toString() } ?: "2147483647",
            *codecArgs(useH265).toTypedArray(),
            "-b:v", bitrateFor(quality),
            "-f", formatName(format),
            outputFile.absolutePath
        )
        logger.info { "FFmpeg encode start: ${command.joinToString(" ")}" }
        return ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
    }


    fun exportVideo(
        inputFile: File,
        outputFile: File,
        format: RecordingFormat,
        quality: Quality,
        startTime: Double?,
        endTime: Double?,
        useH265: Boolean,
    ): Boolean {
        if (!isAvailable()) {
            logger.warn { "FFmpeg binary not available — exportVideo skipped" }
            return false
        }
        if (!inputFile.exists()) {
            logger.warn { "FFmpeg export: input file does not exist: $inputFile" }
            return false
        }
        if (endTime != null && startTime != null && endTime <= startTime) {
            logger.warn { "FFmpeg export: invalid time range [$startTime, $endTime]" }
            return false
        }

        val command = mutableListOf(
            ffmpegBinary, "-y", "-hide_banner", "-loglevel", "error"
        )
        if (startTime != null) {
            command += listOf("-ss", startTime.toString())
        }
        command += listOf("-i", inputFile.absolutePath)
        if (endTime != null && startTime != null) {
            command += listOf("-t", (endTime - startTime).toString())
        }
        command += listOf(
            *codecArgs(useH265).toTypedArray(),
            "-b:v", bitrateFor(quality),
            "-f", formatName(format),
            outputFile.absolutePath
        )

        return runFfmpeg(command, "export") && outputFile.exists() && outputFile.length() > 0
    }

    fun transcodeVideo(
        inputFile: File,
        outputFile: File,
        format: RecordingFormat,
        quality: Quality,
        width: Int?,
        height: Int?,
        fps: Int?,
    ): Boolean {
        if (!isAvailable()) {
            logger.warn { "FFmpeg binary not available — transcodeVideo skipped" }
            return false
        }
        if (!inputFile.exists()) {
            logger.warn { "FFmpeg transcode: input file does not exist: $inputFile" }
            return false
        }

        val command = mutableListOf(
            ffmpegBinary, "-y", "-hide_banner", "-loglevel", "error",
            "-i", inputFile.absolutePath
        )
        if (width != null && height != null) {
            command += listOf("-vf", "scale=$width:$height")
        }
        if (fps != null) {
            command += listOf("-r", fps.toString())
        }
        command += listOf(
            "-c:v", "libx264",
            "-b:v", bitrateFor(quality),
            "-f", formatName(format),
            outputFile.absolutePath
        )

        return runFfmpeg(command, "transcode") && outputFile.exists() && outputFile.length() > 0
    }

    fun pipeToRtsp(
        inputFile: File,
        rtspServerUrl: String,
        quality: Quality,
        width: Int?,
        height: Int?,
        fps: Int?,
    ): Process {
        val command = listOf(
            ffmpegBinary, "-y", "-hide_banner", "-loglevel", "error",
            "-re", "-i", inputFile.absolutePath,
            "-c:v", "libx264",
            "-b:v", bitrateFor(quality),
            "-f", "rtsp",
            rtspServerUrl
        )
        logger.info { "FFmpeg pipe start: ${command.joinToString(" ")}" }
        return ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
    }

    fun isAvailable(): Boolean = ffmpegPath != null || findOnPath("ffmpeg") != null

    /**
     * Информация о видео через ffprobe (JSON).
     * Возвращает null, если файл не существует / ffprobe недоступен / файл не медиа.
     */
    fun getVideoInfo(file: File): Map<String, Any?>? {
        if (!file.exists()) return null
        val ffprobe = ffprobePath ?: findOnPath("ffprobe") ?: return null

        return try {
            val command = listOf(
                ffprobe, "-v", "error",
                "-select_streams", "v:0",
                "-show_entries",
                "stream=codec_name,width,height,bit_rate:format=duration",
                "-of", "json",
                file.absolutePath
            )
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            val exit = process.waitFor()
            if (exit != 0) {
                logger.warn { "ffprobe exited with code $exit for ${file.name}" }
                return null
            }

            val json = Json.parseToJsonElement(output).jsonObject
            val stream = json["streams"]?.jsonArray?.firstOrNull()?.jsonObject
            val formatObj = json["format"]?.jsonObject

            buildMap {
                put("codec", stream?.get("codec_name")?.jsonPrimitive?.contentOrNull)
                put("width", stream?.get("width")?.jsonPrimitive?.intOrNull)
                put("height", stream?.get("height")?.jsonPrimitive?.intOrNull)
                put("duration", formatObj?.get("duration")?.jsonPrimitive?.doubleOrNull ?: 0.0)
                put(
                    "bitrate",
                    (stream?.get("bit_rate")?.jsonPrimitive?.contentOrNull
                        ?: formatObj?.get("bit_rate")?.jsonPrimitive?.contentOrNull)?.toDoubleOrNull()
                )
            }
        } catch (e: Exception) {
            logger.warn(e) { "ffprobe failed for ${file.name}: ${e.message}" }
            null
        }
    }

    private fun runFfmpeg(command: List<String>, operation: String): Boolean {
        logger.info { "FFmpeg $operation: ${command.joinToString(" ")}" }
        return try {
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            val exit = process.waitFor()
            if (exit != 0) {
                logger.warn { "FFmpeg $operation exited with $exit: ${output.take(500)}" }
                false
            } else {
                logger.info { "FFmpeg $operation completed" }
                true
            }
        } catch (e: Exception) {
            logger.error(e) { "FFmpeg $operation failed: ${e.message}" }
            false
        }
    }

    private fun findOnPath(name: String): String? {
        val ext = if (System.getProperty("os.name").lowercase().contains("win")) {
            listOf(".exe", ".cmd", ".bat", "")
        } else {
            listOf("")
        }
        val pathDirs = System.getenv("PATH")?.split(File.pathSeparator) ?: return null
        for (dir in pathDirs) {
            if (dir.isBlank()) continue
            for (suffix in ext) {
                val candidate = Paths.get(dir, name + suffix).toFile()
                if (candidate.isFile) return candidate.absolutePath
            }
        }
        return null
    }

    private fun String.insertCredentials(username: String?, password: String?): String {
        if (username == null) return this
        val scheme = substringBefore("://")
        val rest = substringAfter("://")
        return if (password != null) {
            "$scheme://$username:$password@$rest"
        } else {
            "$scheme://$username@$rest"
        }
    }
}
