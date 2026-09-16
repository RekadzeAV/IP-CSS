package com.company.ipcamera.platform.nas.common.hardware.arm

import com.company.ipcamera.platform.nas.common.hardware.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * ARM Mali Video Processor Implementation
 * Hardware acceleration using ARM Mali GPU/VPU
 */
class ArmMaliEncoder : HardwareEncoder {
    
    private var initialized = false
    private val maliDevicePath = "/dev/mali0"
    private val vpuDevicePath = "/dev/video10" // V4L2 M2M device
    
    override suspend fun isSupported(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check for Mali GPU device
            val hasMaliDevice = File(maliDevicePath).exists()
            
            if (!hasMaliDevice) {
                logger.debug { "Mali GPU device not found" }
                return@withContext false
            }
            
            // Check for V4L2 M2M (Memory-to-Memory) device
            val hasVpuDevice = File(vpuDevicePath).exists()
            
            if (!hasVpuDevice) {
                logger.debug { "V4L2 M2M device not found" }
                return@withContext false
            }
            
            // Check for media device
            val hasMediaDevice = File("/dev/media0").exists()
            
            if (hasMediaDevice) {
                logger.info { "ARM Mali VPU supported" }
                true
            } else {
                logger.debug { "Media device not found" }
                false
            }
        } catch (e: Exception) {
            logger.warn(e) { "Error checking Mali VPU support" }
            false
        }
    }
    
    override suspend fun getHardwareInfo(): HardwareInfo = withContext(Dispatchers.IO) {
        try {
            val gpuName = readGpuName()
            val memoryInfo = readMemoryInfo()
            val codecs = getSupportedCodecs()
            
            HardwareInfo(
                name = gpuName,
                type = HardwareType.VPU_ARM,
                available = true,
                memoryBytes = memoryInfo,
                utilization = readGpuUtilization(),
                temperatureCelsius = readTemperature(),
                codecs = codecs
            )
        } catch (e: Exception) {
            logger.error(e) { "Error getting ARM hardware info" }
            HardwareInfo(
                name = "ARM Mali",
                type = HardwareType.VPU_ARM,
                available = false,
                memoryBytes = 0,
                utilization = 0.0,
                temperatureCelsius = null,
                codecs = emptyList()
            )
        }
    }
    
    override suspend fun encode(input: VideoFrame, output: EncodedFrame): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!initialized) {
                initialize()
            }
            
            // Use ARM Mali VPU via V4L2 M2M / FFmpeg
            val processBuilder = ProcessBuilder(
                "ffmpeg",
                "-f", "rawvideo",
                "-pixel_format", input.format.name.lowercase(),
                "-video_size", "${input.width}x${input.height}",
                "-framerate", "30",
                "-i", "pipe:0",
                "-c:v", "h264_v4l2m2m",
                "-b:v", "${output.bitrate}",
                "-profile:v", "high",
                "-f", "h264",
                "pipe:1"
            )
            
            val process = processBuilder
                .redirectInput(ProcessBuilder.Redirect.PIPE)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()
            
            // Write input data
            process.outputStream.use { it.write(input.data) }
            
            // Read encoded output
            val encodedData = process.inputStream.readBytes()
            
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                logger.debug { "Mali VPU encode successful" }
                Result.success(Unit)
            } else {
                val error = process.errorStream.bufferedReader().readText()
                logger.error { "Mali VPU encode failed: $error" }
                Result.failure(Exception("Encode failed: $error"))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error encoding with Mali VPU" }
            Result.failure(e)
        }
    }
    
    override suspend fun decode(input: EncodedFrame): Result<VideoFrame> = withContext(Dispatchers.IO) {
        try {
            if (!initialized) {
                initialize()
            }
            
            // Use ARM Mali VPU for decoding via V4L2 M2M
            val processBuilder = ProcessBuilder(
                "ffmpeg",
                "-c:v", "h264_v4l2m2m",
                "-i", "pipe:0",
                "-f", "rawvideo",
                "-pixel_format", "nv12",
                "pipe:1"
            )
            
            val process = processBuilder
                .redirectInput(ProcessBuilder.Redirect.PIPE)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()
            
            // Write encoded data
            process.outputStream.use { it.write(input.data) }
            
            // Read decoded output
            val decodedData = process.inputStream.readBytes()
            
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                logger.debug { "Mali VPU decode successful" }
                Result.success(
                    VideoFrame(
                        data = decodedData,
                        width = input.width,
                        height = input.height,
                        format = PixelFormat.NV12,
                        timestamp = input.timestamp
                    )
                )
            } else {
                val error = process.errorStream.bufferedReader().readText()
                Result.failure(Exception("Decode failed: $error"))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error decoding with Mali VPU" }
            Result.failure(e)
        }
    }
    
    override suspend fun transcode(
        input: VideoStream,
        outputConfig: EncodeConfig
    ): Flow<EncodedFrame> = withContext(Dispatchers.IO) {
        kotlinx.coroutines.flow.flow {
            try {
                val processBuilder = ProcessBuilder(
                    "ffmpeg",
                    "-i", input.url,
                    "-c:v", "h264_v4l2m2m",
                    "-b:v", "${outputConfig.bitrate}",
                    "-s", "${outputConfig.width}x${outputConfig.height}",
                    "-r", "${outputConfig.framerate}",
                    "-f", "h264",
                    "pipe:1"
                )
                
                val process = processBuilder
                    .redirectErrorStream(true)
                    .start()
                
                process.inputStream.bufferedReader().use { reader ->
                    reader.forEachLine { line ->
                        logger.trace { "Mali VPU transcoding: $line" }
                    }
                }
                
                process.waitFor()
            } catch (e: Exception) {
                logger.error(e) { "Error transcoding with Mali VPU" }
            }
        }
    }
    
    private suspend fun initialize() {
        logger.info { "Initializing ARM Mali VPU encoder" }
        initialized = true
        
        // Load necessary kernel modules
        loadKernelModules()
    }
    
    private suspend fun loadKernelModules() {
        try {
            // Load v4l2_mem2mem module if not loaded
            val modules = listOf(
                "v4l2_mem2mem",
                "videobuf2_dma_contig",
                "videobuf2_v4l2",
                "videobuf2_common"
            )
            
            modules.forEach { module ->
                val process = ProcessBuilder("modprobe", module)
                    .redirectErrorStream(true)
                    .start()
                
                val exitCode = process.waitFor()
                if (exitCode == 0) {
                    logger.debug { "Loaded kernel module: $module" }
                }
            }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to load kernel modules" }
        }
    }
    
    private suspend fun readGpuName(): String {
        return try {
            val compatibleFile = File("/proc/device-tree/compatible")
            if (compatibleFile.exists()) {
                val compatible = compatibleFile.readText()
                when {
                    compatible.contains("rockchip") -> "ARM Mali (Rockchip)"
                    compatible.contains("amlogic") -> "ARM Mali (Amlogic)"
                    compatible.contains("allwinner") -> "ARM Mali (Allwinner)"
                    compatible.contains("exynos") -> "ARM Mali (Samsung Exynos)"
                    else -> "ARM Mali"
                }
            } else {
                "ARM Mali"
            }
        } catch (e: Exception) {
            "ARM Mali"
        }
    }
    
    private suspend fun readMemoryInfo(): Long {
        return try {
            val meminfoFile = File("/proc/meminfo")
            val lines = meminfoFile.readLines()
            
            val getValue = { key: String ->
                lines.find { it.startsWith(key) }
                    ?.split("\\s+".toRegex())
                    ?.getOrNull(1)
                    ?.toLongOrNull() ?: 0L
            }
            
            // Get CMA (Contiguous Memory Allocator) size for VPU
            val cmaTotal = getValue("CmaTotal:")
            cmaTotal * 1024 // Convert KB to bytes
        } catch (e: Exception) {
            0L
        }
    }
    
    private suspend fun readGpuUtilization(): Double {
        return try {
            // Read from devfreq or similar
            val loadFile = File("/sys/class/devfreq/*/load")
                .toURI()
                .toString()
                .let { path ->
                    File(path.substringAfter("file://"))
                }
            
            if (loadFile.exists()) {
                val load = loadFile.readText().trim().toIntOrNull() ?: 0
                (load / 10.0).coerceIn(0.0, 100.0)
            } else {
                0.0
            }
        } catch (e: Exception) {
            0.0
        }
    }
    
    private suspend fun readTemperature(): Double? {
        return try {
            val tempFile = File("/sys/class/thermal/thermal_zone*/temp")
                .toURI()
                .toString()
                .let { path ->
                    File(path.substringAfter("file://"))
                        .parentFile
                        ?.listFiles()
                        ?.find { it.name.contains("temp") }
                }
            
            if (tempFile != null && tempFile.exists()) {
                val temp = tempFile.readText().trim().toLongOrNull()
                temp?.div(1000.0)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun getSupportedCodecs(): List<CodecInfo> {
        return listOf(
            CodecInfo(
                name = "H.264",
                type = CodecType.H264,
                encode = true,
                decode = true,
                maxResolution = Resolution(3840, 2160), // 4K
                maxFps = 30
            ),
            CodecInfo(
                name = "H.265/HEVC",
                type = CodecType.H265,
                encode = false,
                decode = true,
                maxResolution = Resolution(3840, 2160),
                maxFps = 30
            ),
            CodecInfo(
                name = "VP9",
                type = CodecType.VP9,
                encode = false,
                decode = true,
                maxResolution = Resolution(3840, 2160),
                maxFps = 30
            ),
            CodecInfo(
                name = "AV1",
                type = CodecType.AV1,
                encode = false,
                decode = false,
                maxResolution = Resolution(1920, 1080),
                maxFps = 30
            )
        )
    }
}
