package com.company.ipcamera.shared.platform.hwaccel

import com.company.ipcamera.core.common.model.Resolution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * JVM реализация детектора аппаратного ускорения через FFmpeg
 */
actual object HardwareAccelerationDetector {
    actual suspend fun detectAvailableAcceleration(): HardwareAccelerationType {
        return withContext(Dispatchers.IO) {
            // Приоритет проверки: NVIDIA > Intel QSV > AMD VAAPI > ARM Mali > None
            detectNVIDIA() ?: detectIntelQSV() ?: detectAMDVAAPI() ?: detectARMMali() ?: HardwareAccelerationType.None
        }
    }

    actual suspend fun getOptimalEncoder(
        codec: VideoCodec,
        resolution: Resolution,
    ): EncoderConfig? {
        val hwType = detectAvailableAcceleration()
        return when (hwType) {
            is HardwareAccelerationType.NVIDIA -> getNVIDIAEncoder(codec, resolution)
            is HardwareAccelerationType.IntelQSV -> getQSVEncoder(codec, resolution)
            is HardwareAccelerationType.AMDVAAPI -> getVAAPIEncoder(codec, resolution)
            is HardwareAccelerationType.ARMMali -> getV4L2Encoder(codec, resolution)
            else -> null
        }
    }

    actual suspend fun getOptimalDecoder(codec: VideoCodec): DecoderConfig? {
        val hwType = detectAvailableAcceleration()
        return when (hwType) {
            is HardwareAccelerationType.NVIDIA -> getNVIDIADecoder(codec)
            is HardwareAccelerationType.IntelQSV -> getQSVDecoder(codec)
            is HardwareAccelerationType.AMDVAAPI -> getVAAPIDecoder(codec)
            is HardwareAccelerationType.ARMMali -> getV4L2Decoder(codec)
            else -> null
        }
    }

    private fun detectNVIDIA(): HardwareAccelerationType.NVIDIA? {
        return try {
            val process = ProcessBuilder("nvidia-smi", "--query-gpu=name", "--format=csv,noheader")
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            
            if (output.isNotBlank() && !output.contains("NVIDIA-SMI has failed")) {
                val gpuName = output.trim()
                HardwareAccelerationType.NVIDIA(
                    encoder = "h264_nvenc",
                    decoder = "h264_cuvid"
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun detectIntelQSV(): HardwareAccelerationType.IntelQSV? {
        return try {
            val process = ProcessBuilder("vainfo")
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            
            if (output.contains("VAProfileH264ConstrainedBaseline") || 
                output.contains("VAProfileH264Main") ||
                output.contains("VAProfileH264High")) {
                HardwareAccelerationType.IntelQSV(generation = "unknown")
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun detectAMDVAAPI(): HardwareAccelerationType.AMDVAAPI? {
        return try {
            val process = ProcessBuilder("vainfo")
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            
            if (output.contains("VAProfileH264ConstrainedBaseline")) {
                HardwareAccelerationType.AMDVAAPI(driver = "radeonsi")
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun detectARMMali(): HardwareAccelerationType.ARMMali? {
        return try {
            val maliDevice = java.io.File("/dev/mali0")
            if (maliDevice.exists()) {
                HardwareAccelerationType.ARMMali(version = "unknown")
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getNVIDIAEncoder(codec: VideoCodec, resolution: Resolution): EncoderConfig? {
        val encoderName = when (codec) {
            VideoCodec.H264 -> "h264_nvenc"
            VideoCodec.H265 -> "hevc_nvenc"
            VideoCodec.MJPEG -> null
        } ?: return null
        
        return EncoderConfig(
            name = "NVIDIA NVENC",
            format = "hardware",
            ffmpegEncoderName = encoderName,
            ffmpegArgs = listOf("-preset", "p4", "-tune", "hq", "-rc", "vbr", "-cq", "23"),
            maxResolution = Resolution(3840, 2160),
            supportedCodecs = listOf(VideoCodec.H264, VideoCodec.H265)
        )
    }

    private fun getQSVEncoder(codec: VideoCodec, resolution: Resolution): EncoderConfig? {
        val encoderName = when (codec) {
            VideoCodec.H264 -> "h264_qsv"
            VideoCodec.H265 -> "hevc_qsv"
            VideoCodec.MJPEG -> null
        } ?: return null
        
        return EncoderConfig(
            name = "Intel QSV",
            format = "hardware",
            ffmpegEncoderName = encoderName,
            ffmpegArgs = listOf("-preset", "fast", "-global_quality", "23"),
            maxResolution = Resolution(3840, 2160),
            supportedCodecs = listOf(VideoCodec.H264, VideoCodec.H265)
        )
    }

    private fun getVAAPIEncoder(codec: VideoCodec, resolution: Resolution): EncoderConfig? {
        val encoderName = when (codec) {
            VideoCodec.H264 -> "h264_vaapi"
            VideoCodec.H265 -> "hevc_vaapi"
            VideoCodec.MJPEG -> null
        } ?: return null
        
        return EncoderConfig(
            name = "AMD VAAPI",
            format = "hardware",
            ffmpegEncoderName = encoderName,
            ffmpegArgs = listOf("-vaapi_device", "/dev/dri/renderD128", "-global_quality", "23"),
            maxResolution = Resolution(3840, 2160),
            supportedCodecs = listOf(VideoCodec.H264, VideoCodec.H265)
        )
    }

    private fun getV4L2Encoder(codec: VideoCodec, resolution: Resolution): EncoderConfig? {
        val encoderName = when (codec) {
            VideoCodec.H264 -> "h264_v4l2m2m"
            VideoCodec.H265 -> "hevc_v4l2m2m"
            VideoCodec.MJPEG -> null
        } ?: return null
        
        return EncoderConfig(
            name = "ARM Mali V4L2",
            format = "hardware",
            ffmpegEncoderName = encoderName,
            ffmpegArgs = listOf("-preset", "fast"),
            maxResolution = Resolution(1920, 1080),
            supportedCodecs = listOf(VideoCodec.H264, VideoCodec.H265)
        )
    }

    private fun getNVIDIADecoder(codec: VideoCodec): DecoderConfig? {
        val decoderName = when (codec) {
            VideoCodec.H264 -> "h264_cuvid"
            VideoCodec.H265 -> "hevc_cuvid"
            VideoCodec.MJPEG -> null
        } ?: return null
        
        return DecoderConfig(
            name = "NVIDIA NVDEC",
            format = "hardware",
            ffmpegDecoderName = decoderName,
            ffmpegArgs = emptyList(),
            supportedCodecs = listOf(VideoCodec.H264, VideoCodec.H265)
        )
    }

    private fun getQSVDecoder(codec: VideoCodec): DecoderConfig? {
        val decoderName = when (codec) {
            VideoCodec.H264 -> "h264_qsv"
            VideoCodec.H265 -> "hevc_qsv"
            VideoCodec.MJPEG -> null
        } ?: return null
        
        return DecoderConfig(
            name = "Intel QSV",
            format = "hardware",
            ffmpegDecoderName = decoderName,
            ffmpegArgs = emptyList(),
            supportedCodecs = listOf(VideoCodec.H264, VideoCodec.H265)
        )
    }

    private fun getVAAPIDecoder(codec: VideoCodec): DecoderConfig? {
        val decoderName = when (codec) {
            VideoCodec.H264 -> "h264_vaapi"
            VideoCodec.H265 -> "hevc_vaapi"
            VideoCodec.MJPEG -> null
        } ?: return null
        
        return DecoderConfig(
            name = "AMD VAAPI",
            format = "hardware",
            ffmpegDecoderName = decoderName,
            ffmpegArgs = listOf("-vaapi_device", "/dev/dri/renderD128"),
            supportedCodecs = listOf(VideoCodec.H264, VideoCodec.H265)
        )
    }

    private fun getV4L2Decoder(codec: VideoCodec): DecoderConfig? {
        val decoderName = when (codec) {
            VideoCodec.H264 -> "h264_v4l2m2m"
            VideoCodec.H265 -> "hevc_v4l2m2m"
            VideoCodec.MJPEG -> null
        } ?: return null
        
        return DecoderConfig(
            name = "ARM Mali V4L2",
            format = "hardware",
            ffmpegDecoderName = decoderName,
            ffmpegArgs = emptyList(),
            supportedCodecs = listOf(VideoCodec.H264, VideoCodec.H265)
        )
    }
}