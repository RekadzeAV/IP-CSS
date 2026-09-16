package com.company.ipcamera.server.stub.javacv

// Frame stub
class Frame {
    var image: Any? = null
    var timestamp: Long = 0
}

// OpenCVFrameConverter stub
class OpenCVFrameConverter {
    class ToMat {
        fun convert(src: Any?): Any? = null
        fun convertTo(src: Any?, dest: Any?) {}
    }
    class FromMat {
        fun convert(src: Any?): Any? = null
    }
}

// FFmpegFrameRecorder stub
class FFmpegFrameRecorder {
    constructor(output: Any?, width: Int, height: Int) {}
    constructor(output: String, width: Int, height: Int) : this(output as Any?, width, height)
    var pixelFormat: Int = 0
    var audioSampleFormat: Int = 0
    var sampleRate: Int = 0
    var audioChannels: Int = 0
    var audioBitrate: Int = 0
    var videoBitrate: Int = 0
    var framerate: Double = 0.0
    var gopSize: Int = 0
    var maxBFrames: Int = 0
    var hwAccelerate: Boolean = false
    var videoCodec: Int = 0
    var audioCodec: Int = 0
    var format: String = ""
    var metadata: Map<String, String> = emptyMap()
    var frameSync: Boolean = false
    var audioOptions: Map<String, String> = emptyMap()
    var videoOptions: Map<String, String> = emptyMap()
    var channels: Int = 0
    var channelMapping: List<Double> = emptyList()
    var audioQuality: Int = 0
    var videoQuality: Int = 0
    var threading: Int = 0
    var threadCount: Int = 0
    var threads: Int = 0
    var flags: Int = 0
    var options: Map<String, String> = emptyMap()
    var frame: Frame? = null
    var images: Array<Frame> = arrayOf()
    var timestamps: LongArray = longArrayOf()
    var duration: Double = 0.0
    var totalBytes: Long = 0
    var totalFrames: Long = 0
    fun start() {}
    fun stop() {}
    fun record(frame: Frame?) {}
    fun record(image: Any?) {}
    fun record(images: Array<Frame?>?) {}
    fun record(images: Array<Any?>?) {}
    fun recordPacket(packet: Any?) {}
    fun release() {}
    fun close() {}
    fun isStarted(): Boolean = false
    fun isOpened(): Boolean = false
    fun isRecording(): Boolean = false
    fun getVideoWidth(): Int = 0
    fun getVideoHeight(): Int = 0
    fun getFrameRate(): Double = 0.0
    fun getDurationNs(): Long = 0
    fun getDurationUs(): Long = 0
    fun getDurationMs(): Long = 0
    fun getDurationSec(): Double = 0.0
    fun getFrameCount(): Long = 0
    fun getAudioFrameCount(): Long = 0
    fun getVideoFrameCount(): Long = 0
    fun getSampleRateHz(): Int = 0
    fun getChannelLayout(): Long = 0
    fun getTimeBase(): Double = 0.0
    fun getVideoTimeBase(): Double = 0.0
    fun getAudioTimeBase(): Double = 0.0
    fun getAudioQualityRatio(): Double = 0.0
    fun getVideoQualityRatio(): Double = 0.0
    fun getHardwareAcceleration(): Boolean = false
    fun getHardwareDevice(): String? = null
    fun getHardwareContext(): Any? = null
    fun getHardwareFormat(): Int = 0
    fun getHardwareTransferMode(): Int = 0
    fun getHardwareUpload(): Boolean = false
    fun getHardwareDownload(): Boolean = false
    fun getHardwareConvert(): Boolean = false
    fun getHardwareCopy(): Boolean = false
    fun getHardwareDecompress(): Boolean = false
    fun getHardwareEncode(): Boolean = false
    fun getHardwareDecode(): Boolean = false
}

// FFmpegFrameGrabber stub
class FFmpegFrameGrabber {
    constructor(input: Any?) {}
    constructor(input: String) : this(input as Any?)
    var pixelFormat: Int = 0
    var audioSampleFormat: Int = 0
    var sampleRate: Int = 0
    var audioChannels: Int = 0
    var audioBitrate: Int = 0
    var videoBitrate: Int = 0
    var frameRate: Double = 0.0
    var gopSize: Int = 0
    var maxBFrames: Int = 0
    var hwAccelerate: Boolean = false
    var videoCodec: Int = 0
    var audioCodec: Int = 0
    var format: String = ""
    var metadata: Map<String, String> = emptyMap()
    var frameSync: Boolean = false
    var audioOptions: Map<String, String> = emptyMap()
    var videoOptions: Map<String, String> = emptyMap()
    var channels: Int = 0
    var channelMapping: List<Double> = emptyList()
    var audioQuality: Int = 0
    var videoQuality: Int = 0
    var threading: Int = 0
    var threadCount: Int = 0
    var threads: Int = 0
    var flags: Int = 0
    var options: Map<String, String> = emptyMap()
    var frame: Frame? = null
    var images: Array<Frame> = arrayOf()
    var timestamps: LongArray = longArrayOf()
    var duration: Double = 0.0
    var totalBytes: Long = 0
    var totalFrames: Long = 0
    var imageWidth: Int = 0
    var imageHeight: Int = 0
    var videoStream: Int = -1
    var audioStream: Int = -1
    var subtitleStream: Int = -1
    var videoCodecId: Int = 0
    var audioCodecId: Int = 0
    var subtitleCodecId: Int = 0
    var videoDelay: Int = 0
    var audioDelay: Int = 0
    var subtitleDelay: Int = 0
    var videoPts: Long = 0L
    var audioPts: Long = 0L
    var subtitlePts: Long = 0L
    fun start() {}
    fun stop() {}
    fun grab(): Frame = Frame()
    fun grabImage(): Frame = Frame()
    fun grabFrame(): Frame = Frame()
    fun grabAudio(): Any? = null
    fun grabSamples(): Any? = null
    fun grabPacket(): Any? = null
    fun release() {}
    fun close() {}
    fun isStarted(): Boolean = false
    fun isOpened(): Boolean = false
    fun isGrabbing(): Boolean = false
    fun getVideoWidth(): Int = 0
    fun getVideoHeight(): Int = 0
    fun getDurationNs(): Long = 0
    fun getDurationUs(): Long = 0
    fun getDurationMs(): Long = 0
    fun getDurationSec(): Double = 0.0
    fun getFrameCount(): Long = 0
    fun getAudioFrameCount(): Long = 0
    fun getVideoFrameCount(): Long = 0
    fun getSampleRateHz(): Int = 0
    fun getChannelLayout(): Long = 0
    fun getTimeBase(): Double = 0.0
    fun getVideoTimeBase(): Double = 0.0
    fun getAudioTimeBase(): Double = 0.0
    fun getAudioQualityRatio(): Double = 0.0
    fun getVideoQualityRatio(): Double = 0.0
    fun getHardwareAcceleration(): Boolean = false
    fun getHardwareDevice(): String? = null
    fun getHardwareContext(): Any? = null
    fun getHardwareFormat(): Int = 0
    fun getHardwareTransferMode(): Int = 0
    fun getHardwareUpload(): Boolean = false
    fun getHardwareDownload(): Boolean = false
    fun getHardwareConvert(): Boolean = false
    fun getHardwareCopy(): Boolean = false
    fun getHardwareDecompress(): Boolean = false
    fun getHardwareEncode(): Boolean = false
    fun getHardwareDecode(): Boolean = false
    fun getAudioChannelsCount(): Int = 0
    fun getSampleRateHzCount(): Int = 0
    fun getLengthInFrames(): Long = 0
    fun getLengthInTime(): Long = 0
}