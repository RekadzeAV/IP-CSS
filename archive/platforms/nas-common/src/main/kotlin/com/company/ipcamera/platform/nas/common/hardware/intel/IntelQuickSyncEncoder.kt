package com.company.ipcamera.platform.nas.common.hardware.intel

import com.company.ipcamera.platform.nas.common.hardware.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * Intel Quick Sync Video Implementation
 * Hardware acceleration using Intel integrated graphics
 */
class IntelQuickSyncEncoder : HardwareEncoder {
    
    private var initialized = false
    
    override suspend fun isSupported(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check for Intel GPU device
            val driDevices = File("/dev/dri").listFiles()?.filter { 
                it.name.startsWith("renderD") 
            } ?: emptyList()
            
            if (driDevices.isEmpty()) {
                logger.debug { "No Intel GPU devices found" }
                return@withContext false
            }
            
            // Check for i915 driver
            val hasI915Driver = File("/sys/module/i915").exists()
            
            if (!hasI915Driver) {
                logger.debug { "i915 driver not loaded" }
                return@withContext false
            }
            
            logger.info { "Intel QuickSync supported (${driDevices.size} devices)" }
            true
        } catch (e: Exception) {
            logger.warn(e) { "Error checking QuickSync support" }
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
                type = HardwareType.GPU_INTEL,
                available = true,
                memoryBytes = memoryInfo,
                utilization = readGpuUtilization(),
                temperatureCelsius = readTemperature(),
                codecs = codecs
            )
        } catch (e: Exception) {
            logger.error(e) { "Error getting hardware info" }
            HardwareInfo(
                name = "Intel GPU",
                type = HardwareType.GPU_INTEL,
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
            
            // Use Intel Media SDK or VAAPI for encoding
            val processBuilder = ProcessBuilder(
                "ffmpeg",
                "-hwaccel", "qsv",
                "-qsv_device", "/dev/dri/renderD128",
                "-f", "rawvideo",
                "-pixel_format", input.format.name.lowercase(),
                "-video_size", "${input.width}x${input.height}",
                "-framerate", "30",
                "-i", "pipe:0",
                "-c:v", "h264_qsv",
                "-b:v", "${output.bitrate}",
                "-preset", "fast",
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
                logger.debug { "QuickSync encode successful" }
                Result.success(Unit)
            } else {
                val error = process.errorStream.bufferedReader().readText()
                logger.error { "QuickSync encode failed: $error" }
                Result.failure(Exception("Encode failed: $error"))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error encoding with QuickSync" }
            Result.failure(e)
        }
    }
    
    override suspend fun decode(input: EncodedFrame): Result<VideoFrame> = withContext(Dispatchers.IO) {
        try {
            if (!initialized) {
                initialize()
            }
            
            // Use Intel QuickSync for decoding
            val processBuilder = ProcessBuilder(
                "ffmpeg",
                "-hwaccel", "qsv",
                "-qsv_device", "/dev/dri/renderD128",
                "-c:v", "h264_qsv",
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
                logger.debug { "QuickSync decode successful" }
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
            logger.error(e) { "Error decoding with QuickSync" }
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
                    "-hwaccel", "qsv",
                    "-qsv_device", "/dev/dri/renderD128",
                    "-i", input.url,
                    "-c:v", "h264_qsv",
                    "-b:v", "${outputConfig.bitrate}",
                    "-s", "${outputConfig.width}x${outputConfig.height}",
                    "-r", "${outputConfig.framerate}",
                    "-preset", outputConfig.preset.name.lowercase(),
                    "-f", "h264",
                    "pipe:1"
                )
                
                val process = processBuilder
                    .redirectErrorStream(true)
                    .start()
                
                process.inputStream.bufferedReader().use { reader ->
                    reader.forEachLine { line ->
                        // Parse ffmpeg output and emit frames
                        // Simplified - real implementation would parse actual frames
                        logger.trace { "Transcoding: $line" }
                    }
                }
                
                process.waitFor()
            } catch (e: Exception) {
                logger.error(e) { "Error transcoding with QuickSync" }
            }
        }
    }
    
    private suspend fun initialize() {
        logger.info { "Initializing Intel QuickSync encoder" }
        initialized = true
    }
    
    private suspend fun readGpuName(): String {
        return try {
            val deviceFile = File("/sys/class/drm/card0/device/device")
            if (deviceFile.exists()) {
                val deviceId = deviceFile.readText().trim()
                // Map device ID to GPU name
                when (deviceId) {
                    "0x4680" -> "Intel UHD Graphics 770"
                    "0x9A49" -> "Intel Iris Xe Graphics"
                    else -> "Intel GPU ($deviceId)"
                }
            } else {
                "Intel GPU"
            }
        } catch (e: Exception) {
            "Intel GPU"
        }
    }
    
    private suspend fun readMemoryInfo(): Long {
        return try {
            val meminfoFile = File("/sys/kernel/debug/dri/0/i915_gem_objects")
            if (meminfoFile.exists()) {
                // Parse memory info from i915 debugfs
                meminfoFile.readText()
                    .lineSequence()
                    .find { it.contains("bound") }
                    ?.split("\\s+".toRegex())
                    ?.getOrNull(0)
                    ?.toLongOrNull() ?: 0L
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }
    
    private suspend fun readGpuUtilization(): Double {
        return try {
            // Read from sysfs or use intel_gpu_top
            val utilizationFile = File("/sys/class/drm/card0/gt_rc6")
            if (utilizationFile.exists()) {
                val rc6 = utilizationFile.readText().trim().toLongOrNull() ?: 0L
                // RC6 is residency in idle state, so utilization = 100 - rc6
                (100.0 - (rc6 / 1000000.0)).coerceIn(0.0, 100.0)
            } else {
                0.0
            }
        } catch (e: Exception) {
            0.0
        }
    }
    
    private suspend fun readTemperature(): Double? {
        return try {
            val tempFile = File("/sys/class/hwmon/hwmon*/temp*_input")
                .toURI()
                .toString()
                .let { path ->
                    File(path.substringAfter("file://"))
                        .parentFile
                        ?.listFiles()
                        ?.find { it.name.contains("temp") && it.name.endsWith("input") }
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
                maxResolution = Resolution(4096, 2304),
                maxFps = 60
            ),
            CodecInfo(
                name = "H.265/HEVC",
                type = CodecType.H265,
                encode = true,
                decode = true,
                maxResolution = Resolution(4096, 2304),
                maxFps = 60
            ),
            CodecInfo(
                name = "VP9",
                type = CodecType.VP9,
                encode = false,
                decode = true,
                maxResolution = Resolution(4096, 2304),
                maxFps = 60
            )
        )
    }
}
