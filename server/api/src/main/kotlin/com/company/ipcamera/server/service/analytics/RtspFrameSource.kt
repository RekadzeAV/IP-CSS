package com.company.ipcamera.server.service.analytics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.io.File
import java.io.InputStream
import java.nio.file.Paths
import kotlin.coroutines.coroutineContext

private val logger = KotlinLogging.logger {}

/**
 * Источник декодированных кадров из RTSP-потока для серверной аналитики.
 *
 * ffmpeg-конвейер `-f rawvideo -pix_fmt rgb24 -vf scale=WxH pipe:1` выдаёт RGB24-кадры
 * фиксированного размера (W*H*3 байт), совместимые с JNI-детекторами
 * [com.company.ipcamera.core.network.analytics.NativeAnalytics] — детектор создаётся
 * на то же W×H, что и источник. Аналитическое разрешение по умолчанию 640x360.
 *
 * Graceful degradation: если ffmpeg недоступен — [isAvailable]=false, [frames] — пустой поток.
 */
class RtspFrameSource(
    private val ffmpegPath: String? = System.getenv("FFMPEG_PATH"),
    val analyticsWidth: Int = DEFAULT_ANALYTICS_WIDTH,
    val analyticsHeight: Int = DEFAULT_ANALYTICS_HEIGHT,
) {
    companion object {
        const val DEFAULT_ANALYTICS_WIDTH = 640
        const val DEFAULT_ANALYTICS_HEIGHT = 360
        const val BYTES_PER_PIXEL = 3
        private const val CONNECT_TIMEOUT_SECONDS = 10L
    }

    /** Размер кадра в байтах: width * height * 3 (RGB24). */
    val frameSizeBytes: Int = analyticsWidth * analyticsHeight * BYTES_PER_PIXEL

    private val ffmpegBinary: String by lazy { ffmpegPath ?: findOnPath("ffmpeg") ?: "ffmpeg" }

    fun isAvailable(): Boolean = ffmpegPath != null || findOnPath("ffmpeg") != null

    /** Поток RGB-кадров из RTSP-потока. */
    fun frames(
        rtspUrl: String,
        username: String? = null,
        password: String? = null,
    ): Flow<RtspAnalyticsFrame> = callbackFlow {
        if (!isAvailable()) {
            logger.warn { "RtspFrameSource: ffmpeg not available — no frames for $rtspUrl" }
            close()
            return@callbackFlow
        }
        val command = listOf(
            ffmpegBinary,
            "-hide_banner", "-loglevel", "error",
            "-rtsp_transport", "tcp",
            "-stimeout", (CONNECT_TIMEOUT_SECONDS * 1_000_000).toString(),
            "-i", insertCredentials(rtspUrl, username, password),
            "-vf", "scale=$analyticsWidth:$analyticsHeight",
            "-pix_fmt", "rgb24",
            "-f", "rawvideo",
            "pipe:1",
        )

        var process: Process? = null
        val reader = launch(Dispatchers.IO) {
            try {
                val proc = ProcessBuilder(command).redirectErrorStream(false).start()
                process = proc
                logger.info { "RtspFrameSource: ffmpeg started for $rtspUrl (${analyticsWidth}x$analyticsHeight RGB24)" }
                val buffer = ByteArray(frameSizeBytes)
                while (isActive && proc.isAlive) {
                    if (readFully(proc.inputStream, buffer) < frameSizeBytes) break
                    trySend(
                        RtspAnalyticsFrame(
                            data = buffer.copyOf(),
                            width = analyticsWidth,
                            height = analyticsHeight,
                            timestamp = System.currentTimeMillis(),
                        )
                    )
                }
            } catch (e: Exception) {
                logger.warn(e) { "RtspFrameSource: reader error for $rtspUrl: ${e.message}" }
            } finally {
                close()
            }
        }

        awaitClose {
            reader.cancel()
            process?.let { p ->
                try {
                    p.destroy()
                    if (!p.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)) p.destroyForcibly()
                } catch (_: Exception) {
                }
            }
            logger.debug { "RtspFrameSource: ffmpeg stopped for $rtspUrl" }
        }
    }

    /** Полное чтение буфера из потока; возвращает число прочитанных байт. */
    private suspend fun readFully(input: InputStream, buffer: ByteArray): Int =
        withContext(Dispatchers.IO) {
            var total = 0
            while (total < buffer.size && coroutineContext.isActive) {
                val read = input.read(buffer, total, buffer.size - total)
                if (read < 0) break
                total += read
            }
            total
        }

    /** Прямой захват одиночного кадра как JPEG (для снапшотов). */
    suspend fun captureJpeg(
        rtspUrl: String,
        username: String? = null,
        password: String? = null,
        outputFile: File,
        timeoutSeconds: Long = 15,
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isAvailable()) return@withContext false
        try {
            val command = listOf(
                ffmpegBinary,
                "-hide_banner", "-loglevel", "error",
                "-rtsp_transport", "tcp",
                "-stimeout", (CONNECT_TIMEOUT_SECONDS * 1_000_000).toString(),
                "-i", insertCredentials(rtspUrl, username, password),
                "-vframes", "1",
                "-q:v", "2",
                "-y", outputFile.absolutePath,
            )
            val proc = ProcessBuilder(command).start()
            if (!proc.waitFor(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS)) {
                proc.destroyForcibly()
                return@withContext false
            }
            proc.exitValue() == 0 && outputFile.exists() && outputFile.length() > 0
        } catch (e: Exception) {
            logger.warn(e) { "RtspFrameSource.captureJpeg failed for $rtspUrl: ${e.message}" }
            false
        }
    }

    private fun insertCredentials(url: String, username: String?, password: String?): String {
        if (username.isNullOrBlank() || url.contains("@")) return url
        val scheme = url.substringBefore("://")
        val rest = url.substringAfter("://")
        return if (password != null) "$scheme://$username:$password@$rest" else "$scheme://$username@$rest"
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
}

/** Декодированный аналитический кадр (RGB24). */
data class RtspAnalyticsFrame(
    val data: ByteArray,
    val width: Int,
    val height: Int,
    val timestamp: Long,
)
