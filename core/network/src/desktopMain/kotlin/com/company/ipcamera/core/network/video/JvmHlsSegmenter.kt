package com.company.ipcamera.core.network.video

import kotlinx.coroutines.*
import mu.KotlinLogging
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}

/**
 * JavaCV-based HLS сегментер для Desktop.
 *
 * Преобразует входящие видеокадры (RGB24/JPEG) в HLS сегменты
 * и генерирует плейлист (.m3u8).
 * Все операции синхронные (без корутин) для предсказуемого поведения.
 */
class JvmHlsSegmenter(
    private val config: HlsSegmenterConfig = HlsSegmenterConfig()
) : HlsSegmenter {

    private var currentPlaylistPath: String? = null
    private var currentOutputDir: String? = null

    private var segmentCallback: HlsSegmentCallback? = null
    private var playlistCallback: HlsPlaylistCallback? = null

    @Volatile
    private var active = false
    override fun isActive(): Boolean = active

    private var segmentIndex = AtomicInteger(0)
    private var sequenceNumber = AtomicInteger(0)
    private val frameBuffer = mutableListOf<FrameData>()

    private var ffmpegProcess: Process? = null
    private var ffmpegStdin: java.io.OutputStream? = null

    private data class FrameData(
        val data: ByteArray,
        val width: Int,
        val height: Int,
        val timestamp: Long
    )

    override fun getPlaylistPath(): String? = currentPlaylistPath
    override fun getOutputDir(): String? = currentOutputDir
    override fun setSegmentCallback(callback: HlsSegmentCallback?) { segmentCallback = callback }
    override fun setPlaylistCallback(callback: HlsPlaylistCallback?) { playlistCallback = callback }

    override suspend fun start(streamId: String): Boolean {
        // РЎРёРЅС…СЂРѕРЅРЅР°СЏ РѕРїРµСЂР°С†РёСЏ вЂ" IO РЅРµ РЅСѓР¶РµРЅ
        if (active) {
            return false
        }
        return try {
            val outputDir = File(config.outputDir, streamId).apply { mkdirs() }
            currentOutputDir = outputDir.absolutePath
            currentPlaylistPath = File(outputDir, "playlist.m3u8").absolutePath
            segmentIndex.set(0); sequenceNumber.set(0); frameBuffer.clear()
            ffmpegProcess = tryStartFfmpegPipe(outputDir)
            active = true
            logger.info { "HLS segmenter started for: $streamId (ffmpeg=${ffmpegProcess != null})" }
            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to start segmenter" }
            false
        }
    }

    override suspend fun stop() {
        flushBuffer()
        ffmpegStdin?.close()
        ffmpegProcess?.let { p ->
            if (p.isAlive) { p.destroyForcibly(); p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS) }
        }
        ffmpegProcess = null; ffmpegStdin = null
        if (segmentIndex.get() > 0) finalizePlaylist()
        active = false
        frameBuffer.clear()
    }

    override suspend fun feedFrame(data: ByteArray, width: Int, height: Int, timestamp: Long) {
        if (!active || data.isEmpty()) return
        if (ffmpegProcess != null && ffmpegStdin != null) {
            writeFrameToFfmpeg(data)
        } else {
            frameBuffer.add(FrameData(data, width, height, timestamp))
            tryGenerateSegment()
        }
    }

    /** Пытается создать сегмент если накоплено достаточно кадров */
    private fun tryGenerateSegment() {
        val segmentFrames = config.segmentDurationSec * config.fps
        if (frameBuffer.size >= segmentFrames) {
            val frames = mutableListOf<FrameData>()
            repeat(segmentFrames) { frames.add(frameBuffer.removeAt(0)) }
            generateSegment(frames)
        }
    }

    /** Принудительная генерация всех накопленных кадров (вызывается перед stop) */
    private fun flushBuffer() {
        val segmentFrames = config.segmentDurationSec * config.fps
        while (frameBuffer.size >= segmentFrames) {
            val frames = mutableListOf<FrameData>()
            repeat(segmentFrames) { frames.add(frameBuffer.removeAt(0)) }
            generateSegment(frames)
        }
    }

    private fun tryStartFfmpegPipe(outputDir: File): Process? {
        val ffmpegPath = findFfmpeg()
        if (ffmpegPath == null) {
            return null
        }
        return try {
            val playlistPath = File(outputDir, "playlist.m3u8").absolutePath
            val args = mutableListOf(
                ffmpegPath, "-y",
                "-f", "rawvideo", "-pixel_format", "rgb24",
                "-video_size", "320x240", "-framerate", config.fps.toString(),
                "-i", "pipe:0",
                "-c:v", "libx264", "-preset", "ultrafast", "-tune", "zerolatency",
                "-g", "1", "-sc_threshold", "0",
                "-b:v", config.videoBitrate.toString(),
                "-f", config.hlsFormat,
                "-hls_time", config.segmentDurationSec.toString(),
                "-hls_list_size", config.playlistSize.toString(),
                "-hls_flags", config.hlsFlags,
                "-hls_segment_filename", File(outputDir, "segment_%03d${config.segmentExtension}").absolutePath,
                "-hls_allow_cache", "0", playlistPath
            )
            val pb = ProcessBuilder(args).directory(outputDir).redirectErrorStream(true)
            val process = pb.start()
            ffmpegStdin = process.outputStream
            process
        } catch (e: Exception) {
            logger.warn(e) { "FFmpeg pipe not available" }
            null
        }
    }

    private fun writeFrameToFfmpeg(data: ByteArray) {
        try { ffmpegStdin?.write(data); ffmpegStdin?.flush() } catch (e: Exception) { ffmpegProcess?.destroyForcibly(); ffmpegProcess = null; ffmpegStdin = null }
    }

    private fun generateSegment(frames: List<FrameData>) {
        val index = segmentIndex.getAndIncrement()
        val seq = sequenceNumber.getAndIncrement()
        val dir = currentOutputDir?.let { File(it) } ?: return
        try {
            val firstFrame = frames.firstOrNull() ?: return
            val segmentFile = File(dir, "segment_$index${config.segmentExtension}")
            segmentFile.writeBytes(firstFrame.data)
            updatePlaylist(seq)
            segmentCallback?.invoke(
                HlsSegment(
                    index,
                    segmentFile.name,
                    config.segmentDurationSec * 1000L,
                    segmentFile.length(),
                    index == 0
                )
            )
        } catch (e: Exception) { logger.error(e) { "Failed to generate segment $index" } }
    }

    private fun updatePlaylist(sequence: Int) {
        val path = currentPlaylistPath ?: return
        val startIdx = maxOf(0, segmentIndex.get() - config.playlistSize)
        val sb = StringBuilder()
        sb.appendLine("#EXTM3U")
        sb.appendLine("#EXT-X-VERSION:${if (config.useFmp4) 7 else 3}")
        sb.appendLine("#EXT-X-TARGETDURATION:${config.segmentDurationSec}")
        sb.appendLine("#EXT-X-MEDIA-SEQUENCE:$sequence")
        for (i in startIdx until segmentIndex.get()) {
            sb.appendLine("#EXTINF:${config.segmentDurationSec}.000,")
            sb.appendLine("segment_$i${config.segmentExtension}")
        }
        File(path).writeText(sb.toString())
        playlistCallback?.invoke(
            HlsPlaylist(
                version = if (config.useFmp4) 7 else 3,
                targetDuration = config.segmentDurationSec,
                sequenceNumber = sequence,
                segments = (startIdx until segmentIndex.get()).map { i ->
                    HlsSegment(
                        i,
                        "segment_$i${config.segmentExtension}",
                        config.segmentDurationSec * 1000L,
                        0L,
                        i == 0
                    )
                },
                isLive = true,
                path = path
            )
        )
    }

    private fun finalizePlaylist() {
        val path = currentPlaylistPath ?: return
        File(path).writeText(File(path).readText() + "#EXT-X-ENDLIST\n")
    }

    private fun findFfmpeg(): String? {
        for (name in listOf("ffmpeg", "ffmpeg.exe")) {
            try {
                val p = ProcessBuilder(name, "-version").redirectErrorStream(true).start()
                val finished = p.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)
                if (finished && p.exitValue() == 0) return name
                p.destroy()
            } catch (_: Exception) { }
        }
        return null
    }
}
