package com.company.ipcamera.platform.nas.common.hardware.nvidia

import com.company.ipcamera.platform.nas.common.hardware.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * NVIDIA NVENC Implementation
 * Hardware acceleration using NVIDIA GPU
 */
class NvidiaNVENCEncoder : HardwareEncoder {
    
    private var initialized = false
    
    override suspend fun isSupported(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check for NVIDIA devices
            val nvidiaDevices = listOf(
                "/dev/nvidia0",
                "/dev/nvidiactl",
                "/dev/nvidia-uvm"
            ).all { java.io.File(it).exists() }
            
            if (!nvidiaDevices) {
                logger.debug { "No NVIDIA devices found" }
                return@withContext false
            }
            
            // Check nvidia-smi availability
            val process = ProcessBuilder("nvidia-smi", "--query-gpu=driver_version", "--format=csv,noheader")
                .redirectErrorStream(true)
                .start()
            
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                val driverVersion = process.inputStream.bufferedReader().readText().trim()
                logger.info { "NVIDIA NVENC supported (driver: $driverVersion)" }
                return@withContext true
            }
            
            logger.debug { "nvidia-smi not available" }
            false
        } catch (e: Exception) {
            logger.warn(e) { "Error checking NVENC support" }
            false
        }
    }
    
    override suspend fun getHardwareInfo(): HardwareInfo = withContext(Dispatchers.IO) {
        try {
            val gpuInfo = parseNvidiaSmi()
            
            HardwareInfo(
                name = gpuInfo.name,
                type = HardwareType.GPU_NVIDIA,
                available = true,
                memoryBytes = gpuInfo.memoryTotal,
                utilization = gpuInfo.utilization,
                temperatureCelsius = gpuInfo.temperature,
                codecs = getSupportedCodecs()
            )
        } catch (e: Exception) {
            logger.error(e) { "Error getting NVIDIA hardware info" }
            HardwareInfo(
                name = "NVIDIA GPU",
                type = HardwareType.GPU_NVIDIA,
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
            
            // Use NVIDIA NVENC via FFmpeg
            val processBuilder = ProcessBuilder(
                "ffmpeg",
                "-hwaccel", "cuda",
                "-hwaccel_output_format", "cuda",
                "-f", "rawvideo",
                "-pixel_format", input.format.name.lowercase(),
                "-video_size", "${input.width}x${input.height}",
                "-framerate", "30",
                "-i", "pipe:0",
                "-c:v", "h264_nvenc",
                "-b:v", "${output.bitrate}",
                "-preset", "p2", // Medium quality
                "-tune", "ull", // Ultra low latency
                "-f", "h264",
                "pipe:1"
            )
            
            val process = processBuilder
                .redirectInput(ProcessBuilder.Redirect.PIPE)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()
            
            process.outputStream.use { it.write(input.data) }
            val encodedData = process.inputStream.readBytes()
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                logger.debug { "NVENC encode successful" }
                Result.success(Unit)
            } else {
                val error = process.errorStream.bufferedReader().readText()
                logger.error { "NVENC encode failed: $error" }
                Result.failure(Exception("Encode failed: $error"))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error encoding with NVENC" }
            Result.failure(e)
        }
    }
    
    override suspend fun decode(input: EncodedFrame): Result<VideoFrame> = withContext(Dispatchers.IO) {
        try {
            if (!initialized) {
                initialize()
            }
            
            val processBuilder = ProcessBuilder(
                "ffmpeg",
                "-hwaccel", "cuda",
                "-hwaccel_output_format", "cuda",
                "-c:v", "h264_cuvid",
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
            
            process.outputStream.use { it.write(input.data) }
            val decodedData = process.inputStream.readBytes()
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                logger.debug { "NVENC decode successful" }
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
            logger.error(e) { "Error decoding with NVENC" }
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
                    "-hwaccel", "cuda",
                    "-i", input.url,
                    "-c:v", "h264_nvenc",
                    "-b:v", "${outputConfig.bitrate}",
                    "-s", "${outputConfig.width}x${outputConfig.height}",
                    "-r", "${outputConfig.framerate}",
                    "-preset", "p${outputConfig.preset.ordinal + 1}",
                    "-f", "h264",
                    "pipe:1"
                )
                
                val process = processBuilder
                    .redirectErrorStream(true)
                    .start()
                
                process.inputStream.bufferedReader().use { reader ->
                    reader.forEachLine { line ->
                        logger.trace { "NVENC transcoding: $line" }
                    }
                }
                
                process.waitFor()
            } catch (e: Exception) {
                logger.error(e) { "Error transcoding with NVENC" }
            }
        }
    }
    
    private suspend fun initialize() {
        logger.info { "Initializing NVIDIA NVENC encoder" }
        initialized = true
    }
    
    private suspend fun parseNvidiaSmi(): GpuInfo {
        return try {
            // Query GPU info using nvidia-smi
            val process = ProcessBuilder(
                "nvidia-smi",
                "--query-gpu=name,memory.total,utilization.gpu,temperature.gpu",
                "--format=csv,noheader,nounits"
            )
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText().trim()
            val parts = output.split(",").map { it.trim() }
            
            GpuInfo(
                name = parts.getOrNull(0) ?: "NVIDIA GPU",
                memoryTotal = (parts.getOrNull(1)?.toLongOrNull() ?: 0L) * 1024 * 1024, // Convert MB to bytes
                utilization = parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0,
                temperature = parts.getOrNull(3)?.toDoubleOrNull()
            )
        } catch (e: Exception) {
            logger.warn(e) { "Error parsing nvidia-smi output" }
            GpuInfo("NVIDIA GPU", 0, 0.0, null)
        }
    }
    
    private suspend fun getSupportedCodecs(): List<CodecInfo> {
        return listOf(
            CodecInfo(
                name = "H.264",
                type = CodecType.H264,
                encode = true,
                decode = true,
                maxResolution = Resolution(8192, 4320),
                maxFps = 60
            ),
            CodecInfo(
                name = "H.265/HEVC",
                type = CodecType.H265,
                encode = true,
                decode = true,
                maxResolution = Resolution(8192, 4320),
                maxFps = 60
            ),
            CodecInfo(
                name = "AV1",
                type = CodecType.AV1,
                encode = false,
                decode = true,
                maxResolution = Resolution(8192, 4320),
                maxFps = 60
            )
        )
    }
    
    data class GpuInfo(
        val name: String,
        val memoryTotal: Long,
        val utilization: Double,
        val temperature: Double?
    )
}
