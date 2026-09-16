package com.company.ipcamera.platform.nas.common.hardware.amd

import com.company.ipcamera.platform.nas.common.hardware.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * AMD VCE (Video Coding Engine) Implementation
 * Hardware acceleration using AMD GPU
 */
class AmdVCEEncoder : HardwareEncoder {
    
    private var initialized = false
    private val vceDevicePath = "/dev/dri/renderD128"
    
    override suspend fun isSupported(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check for AMD GPU device
            val driDevices = File("/dev/dri").listFiles()?.filter { 
                it.name.startsWith("renderD") 
            } ?: emptyList()
            
            if (driDevices.isEmpty()) {
                logger.debug { "No GPU devices found" }
                return@withContext false
            }
            
            // Check for AMD driver (amdgpu)
            val hasAmdgpuDriver = File("/sys/module/amdgpu").exists()
            
            if (!hasAmdgpuDriver) {
                logger.debug { "amdgpu driver not loaded" }
                return@withContext false
            }
            
            // Check VCE capability via vainfo
            val vainfoResult = checkVainfo()
            
            if (vainfoResult) {
                logger.info { "AMD VCE supported" }
                true
            } else {
                logger.debug { "VCE not available in vainfo" }
                false
            }
        } catch (e: Exception) {
            logger.warn(e) { "Error checking VCE support" }
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
                type = HardwareType.GPU_AMD,
                available = true,
                memoryBytes = memoryInfo,
                utilization = readGpuUtilization(),
                temperatureCelsius = readTemperature(),
                codecs = codecs
            )
        } catch (e: Exception) {
            logger.error(e) { "Error getting AMD hardware info" }
            HardwareInfo(
                name = "AMD GPU",
                type = HardwareType.GPU_AMD,
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
            
            // Use AMD VCE via VAAPI/FFmpeg
            val processBuilder = ProcessBuilder(
                "ffmpeg",
                "-hwaccel", "vaapi",
                "-hwaccel_device", vceDevicePath,
                "-hwaccel_output_format", "vaapi",
                "-f", "rawvideo",
                "-pixel_format", input.format.name.lowercase(),
                "-video_size", "${input.width}x${input.height}",
                "-framerate", "30",
                "-i", "pipe:0",
                "-c:v", "h264_vaapi",
                "-b:v", "${output.bitrate}",
                "-profile:v", "high",
                "-level:v", "4.1",
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
                logger.debug { "VCE encode successful" }
                Result.success(Unit)
            } else {
                val error = process.errorStream.bufferedReader().readText()
                logger.error { "VCE encode failed: $error" }
                Result.failure(Exception("Encode failed: $error"))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error encoding with VCE" }
            Result.failure(e)
        }
    }
    
    override suspend fun decode(input: EncodedFrame): Result<VideoFrame> = withContext(Dispatchers.IO) {
        try {
            if (!initialized) {
                initialize()
            }
            
            // Use AMD VCE for decoding via VAAPI
            val processBuilder = ProcessBuilder(
                "ffmpeg",
                "-hwaccel", "vaapi",
                "-hwaccel_device", vceDevicePath,
                "-hwaccel_output_format", "vaapi",
                "-c:v", "h264",
                "-i", "pipe:0",
                "-f", "rawvideo",
                "-pixel_format", "nv12",
                "-vf", "hwdownload,format=nv12",
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
                logger.debug { "VCE decode successful" }
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
            logger.error(e) { "Error decoding with VCE" }
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
                    "-hwaccel", "vaapi",
                    "-hwaccel_device", vceDevicePath,
                    "-i", input.url,
                    "-c:v", "h264_vaapi",
                    "-b:v", "${outputConfig.bitrate}",
                    "-s", "${outputConfig.width}x${outputConfig.height}",
                    "-r", "${outputConfig.framerate}",
                    "-profile:v", "high",
                    "-f", "h264",
                    "pipe:1"
                )
                
                val process = processBuilder
                    .redirectErrorStream(true)
                    .start()
                
                process.inputStream.bufferedReader().use { reader ->
                    reader.forEachLine { line ->
                        logger.trace { "VCE transcoding: $line" }
                    }
                }
                
                process.waitFor()
            } catch (e: Exception) {
                logger.error(e) { "Error transcoding with VCE" }
            }
        }
    }
    
    private suspend fun initialize() {
        logger.info { "Initializing AMD VCE encoder" }
        initialized = true
    }
    
    private suspend fun checkVainfo(): Boolean {
        return try {
            val process = ProcessBuilder("vainfo", "--display", "drm")
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            exitCode == 0 && output.contains("VAProfileH264", ignoreCase = true)
        } catch (e: Exception) {
            logger.warn(e) { "vainfo check failed" }
            false
        }
    }
    
    private suspend fun readGpuName(): String {
        return try {
            val deviceFile = File("/sys/class/drm/card0/device/device")
            if (deviceFile.exists()) {
                val deviceId = deviceFile.readText().trim()
                // Map AMD device ID to GPU name
                when (deviceId) {
                    "0x15DD" -> "AMD Radeon Vega 8"
                    "0x164C" -> "AMD Radeon RX Vega 11"
                    "0x731F" -> "AMD Radeon RX 5700 XT"
                    "0x73BF" -> "AMD Radeon RX 6800 XT"
                    else -> "AMD GPU ($deviceId)"
                }
            } else {
                "AMD GPU"
            }
        } catch (e: Exception) {
            "AMD GPU"
        }
    }
    
    private suspend fun readMemoryInfo(): Long {
        return try {
            val meminfoFile = File("/sys/kernel/debug/dri/0/amdgpu_vram_mm")
            if (meminfoFile.exists()) {
                meminfoFile.readText()
                    .lineSequence()
                    .find { it.contains("total") }
                    ?.split("\\s+".toRegex())
                    ?.getOrNull(1)
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
            // Read from hwmon or use rocm-smi
            val utilizationFile = File("/sys/class/hwmon/hwmon*/power*_input")
                .toURI()
                .toString()
                .let { path ->
                    File(path.substringAfter("file://"))
                        .parentFile
                        ?.listFiles()
                        ?.find { it.name.contains("power") && it.name.endsWith("input") }
                }
            
            if (utilizationFile != null && utilizationFile.exists()) {
                val power = utilizationFile.readText().trim().toLongOrNull() ?: 0L
                // Convert to approximate utilization (simplified)
                (power / 1000000.0).coerceIn(0.0, 100.0)
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
