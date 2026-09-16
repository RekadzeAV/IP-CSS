package com.company.ipcamera.platform.nas.common.hardware

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Hardware Video Acceleration Interface
 * Abstract interface for hardware-accelerated video encoding/decoding
 */
interface HardwareEncoder {
    
    /**
     * Check if hardware acceleration is available
     */
    suspend fun isSupported(): Boolean
    
    /**
     * Get hardware information
     */
    suspend fun getHardwareInfo(): HardwareInfo
    
    /**
     * Encode video frame
     */
    suspend fun encode(input: VideoFrame, output: EncodedFrame): Result<Unit>
    
    /**
     * Decode video frame
     */
    suspend fun decode(input: EncodedFrame): Result<VideoFrame>
    
    /**
     * Transcode video stream
     */
    suspend fun transcode(
        input: VideoStream,
        outputConfig: EncodeConfig
    ): Flow<EncodedFrame>
}

/**
 * Hardware information
 */
data class HardwareInfo(
    val name: String,
    val type: HardwareType,
    val available: Boolean,
    val memoryBytes: Long,
    val utilization: Double, // 0.0 - 1.0
    val temperatureCelsius: Double?,
    val codecs: List<CodecInfo>
)

/**
 * Hardware type
 */
enum class HardwareType {
    GPU_NVIDIA,
    GPU_AMD,
    GPU_INTEL,
    VPU_ARM
}

/**
 * Codec information
 */
data class CodecInfo(
    val name: String,
    val type: CodecType,
    val encode: Boolean,
    val decode: Boolean,
    val maxResolution: Resolution,
    val maxFps: Int
)

enum class CodecType {
    H264,
    H265,
    VP8,
    VP9,
    AV1
}

data class Resolution(
    val width: Int,
    val height: Int
)

/**
 * Video frame
 */
data class VideoFrame(
    val data: ByteArray,
    val width: Int,
    val height: Int,
    val format: PixelFormat,
    val timestamp: Long
)

enum class PixelFormat {
    NV12,
    I420,
    RGB24,
    BGR24
}

/**
 * Encoded frame
 */
data class EncodedFrame(
    val data: ByteArray,
    val codec: String,
    val width: Int,
    val height: Int,
    val bitrate: Int,
    val framerate: Double,
    val keyframe: Boolean,
    val timestamp: Long
)

/**
 * Video stream
 */
data class VideoStream(
    val url: String,
    val codec: String,
    val width: Int,
    val height: Int,
    val framerate: Double
)

/**
 * Encode configuration
 */
data class EncodeConfig(
    val codec: String,
    val bitrate: Int,
    val width: Int,
    val height: Int,
    val framerate: Double,
    val quality: Int,
    val preset: EncodePreset
)

enum class EncodePreset {
    ULTRAFAST,
    FAST,
    MEDIUM,
    SLOW,
    VERYSLOW
}
