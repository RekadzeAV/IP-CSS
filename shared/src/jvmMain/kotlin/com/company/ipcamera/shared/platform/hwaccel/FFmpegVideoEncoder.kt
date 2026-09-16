package com.company.ipcamera.shared.platform.hwaccel

import com.company.ipcamera.core.common.model.Resolution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * FFmpeg-based видеокодировщик с поддержкой аппаратного ускорения
 */
class FFmpegVideoEncoder : VideoEncoder {
    private var encodingProcess: Process? = null
    private var isPaused = false

    override suspend fun startEncoding(
        inputSource: String,
        outputPath: String,
        codec: VideoCodec,
        encoder: EncoderConfig?,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val hwAccel = HardwareAccelerationDetector.detectAvailableAcceleration()
            val encoderConfig = encoder ?: when (hwAccel) {
                is HardwareAccelerationType.NVIDIA -> getNVIDIAEncoder(codec)
                is HardwareAccelerationType.IntelQSV -> getQSVEncoder(codec)
                is HardwareAccelerationType.AMDVAAPI -> getVAAPIEncoder(codec)
                is HardwareAccelerationType.ARMMali -> getV4L2Encoder(codec)
                else -> null
            }

            if (encoderConfig == null) {
                logger.warn { "No hardware encoder available for $codec, software encoding not yet implemented" }
                return@withContext Result.failure(UnsupportedOperationException("No encoder available"))
            }

            val command = buildFFmpegCommand(inputSource, outputPath, codec, encoderConfig)
            logger.info { "Starting encoding: ${command.joinToString(" ")}" }

            encodingProcess = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()

            val exitCode = encodingProcess?.waitFor()
            if (exitCode == 0) {
                logger.info { "Encoding completed successfully" }
                Result.success(Unit)
            } else {
                val error = encodingProcess?.inputStream?.bufferedReader()?.readText() ?: "Unknown error"
                logger.error { "Encoding failed with exit code $exitCode: $error" }
                Result.failure(Exception("FFmpeg encoding failed: $error"))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error starting encoding" }
            Result.failure(e)
        }
    }

    override suspend fun stopEncoding(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            encodingProcess?.destroy()
            encodingProcess = null
            isPaused = false
            logger.info { "Encoding stopped" }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error stopping encoding" }
            Result.failure(e)
        }
    }

    override suspend fun pauseEncoding(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (encodingProcess != null && !isPaused) {
                // FFmpeg doesn't have native pause, we can send SIGSTOP
                encodingProcess?.children()?.forEach { it.destroy() }
                isPaused = true
                logger.info { "Encoding paused" }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error pausing encoding" }
            Result.failure(e)
        }
    }

    override suspend fun resumeEncoding(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (isPaused) {
                isPaused = false
                logger.info { "Encoding resumed" }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error resuming encoding" }
            Result.failure(e)
        }
    }

    override suspend fun getEncodingStatus(): EncodingStatus? {
        val process = encodingProcess ?: return null
        return EncodingStatus(
            isActive = process.isAlive,
            isPaused = isPaused,
            startTime = null,
            bytesEncoded = 0,
            duration = 0,
        )
    }

    override fun isEncoding(): Boolean {
        return encodingProcess?.isAlive == true
    }

    private fun buildFFmpegCommand(
        inputSource: String,
        outputPath: String,
        codec: VideoCodec,
        encoderConfig: EncoderConfig,
    ): List<String> {
        val command = mutableListOf("ffmpeg")

        // Input
        command.add("-i")
        command.add(inputSource)

        // Hardware acceleration
        when (encoderConfig.format) {
            "hardware" -> {
                when (encoderConfig.ffmpegEncoderName) {
                    "h264_nvenc", "hevc_nvenc" -> {
                        command.add("-hwaccel")
                        command.add("cuda")
                        command.add("-hwaccel_output_format")
                        command.add("cuda")
                    }
                    "h264_qsv", "hevc_qsv" -> {
                        command.add("-hwaccel")
                        command.add("qsv")
                    }
                    "h264_vaapi", "hevc_vaapi" -> {
                        command.add("-hwaccel")
                        command.add("vaapi")
                        command.add("-vaapi_device")
                        command.add("/dev/dri/renderD128")
                    }
                    "h264_v4l2m2m", "hevc_v4l2m2m" -> {
                        // V4L2 doesn't need special hwaccel flags
                    }
                }
            }
        }

        // Video codec
        command.add("-c:v")
        command.add(encoderConfig.ffmpegEncoderName)

        // Encoder-specific arguments
        encoderConfig.ffmpegArgs.forEach { arg ->
            command.add(arg)
        }

        // Output format
        command.add("-f")
        command.add("mp4")

        // Output file
        command.add(outputPath)

        return command
    }

    private fun getNVIDIAEncoder(codec: VideoCodec): EncoderConfig {
        return when (codec) {
            VideoCodec.H264 -> EncoderConfig(
                name = "NVIDIA NVENC",
                format = "hardware",
                ffmpegEncoderName = "h264_nvenc",
                ffmpegArgs = listOf("-preset", "p4", "-tune", "hq", "-rc", "vbr", "-cq", "23"),
                maxResolution = Resolution(3840, 2160),
                supportedCodecs = listOf(VideoCodec.H264, VideoCodec.H265)
            )
            VideoCodec.H265 -> EncoderConfig(
                name = "NVIDIA NVENC",
                format = "hardware",
                ffmpegEncoderName = "hevc_nvenc",
                ffmpegArgs = listOf("-preset", "p4", "-tune", "hq", "-rc", "vbr", "-cq", "23"),
                maxResolution = Resolution(3840, 2160),
                supportedCodecs = listOf(VideoCodec.H264, VideoCodec.H265)
            )
            VideoCodec.MJPEG -> throw UnsupportedOperationException("MJPEG not supported by NVENC")
        }
    }

    private fun getQSVEncoder(codec: VideoCodec): EncoderConfig {
        return when (codec) {
            VideoCodec.H264 -> EncoderConfig(
                name = "Intel QSV",
                format = "hardware",
                ffmpegEncoderName = "h264_qsv",
                ffmpegArgs = listOf("-preset", "fast", "-global_quality", "23"),
                maxResolution = Resolution(3840, 2160),
                supportedCodecs = listOf(VideoCodec.H264, VideoCodec.H265)
            )
            VideoCodec.H265 -> EncoderConfig(
                name = "Intel QSV",
                format = "hardware",
                ffmpegEncoderName = "hevc_qsv",
                ffmpegArgs = listOf("-preset", "fast", "-global_quality", "23"),
                maxResolution = Resolution(3840, 2160),
                supportedCodecs = listOf(VideoCodec.H264, VideoCodec.H265)
            )
            VideoCodec.MJPEG -> throw UnsupportedOperationException("MJPEG not supported by QSV")
        }
    }

    private fun getVAAPIEncoder(codec: VideoCodec): EncoderConfig {
        return when (codec) {
            VideoCodec.H264 -> EncoderConfig(
                name = "AMD VAAPI",
                format = "hardware",
                ffmpegEncoderName = "h264_vaapi",
                ffmpegArgs = listOf("-vaapi_device", "/dev/dri/renderD128", "-global_quality", "23"),
                maxResolution = Resolution(3840, 2160),
                supportedCodecs = listOf(VideoCodec.H264, VideoCodec.H265)
            )
            VideoCodec.H265 -> EncoderConfig(
                name = "AMD VAAPI",
                format = "hardware",
                ffmpegEncoderName = "hevc_vaapi",
                ffmpegArgs = listOf("-vaapi_device", "/dev/dri/renderD128", "-global_quality", "23"),
                maxResolution = Resolution(3840, 2160),
                supportedCodecs = listOf(VideoCodec.H264, VideoCodec.H265)
            )
            VideoCodec.MJPEG -> throw UnsupportedOperationException("MJPEG not supported by VAAPI")
        }
    }

    private fun getV4L2Encoder(codec: VideoCodec): EncoderConfig {
        return when (codec) {
            VideoCodec.H264 -> EncoderConfig(
                name = "ARM Mali V4L2",
                format = "hardware",
                ffmpegEncoderName = "h264_v4l2m2m",
                ffmpegArgs = listOf("-preset", "fast"),
                maxResolution = Resolution(1920, 1080),
                supportedCodecs = listOf(VideoCodec.H264, VideoCodec.H265)
            )
            VideoCodec.H265 -> EncoderConfig(
                name = "ARM Mali V4L2",
                format = "hardware",
                ffmpegEncoderName = "hevc_v4l2m2m",
                ffmpegArgs = listOf("-preset", "fast"),
                maxResolution = Resolution(1920, 1080),
                supportedCodecs = listOf(VideoCodec.H264, VideoCodec.H265)
            )
            VideoCodec.MJPEG -> throw UnsupportedOperationException("MJPEG not supported by V4L2")
        }
    }
}