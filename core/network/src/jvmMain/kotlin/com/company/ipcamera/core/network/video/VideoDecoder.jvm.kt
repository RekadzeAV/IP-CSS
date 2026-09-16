package com.company.ipcamera.core.network.video

import com.company.ipcamera.core.network.RtspFrame

/**
 * JVM stub реализация VideoDecoder
 */
actual class VideoDecoder {
    actual constructor(codec: VideoCodec, width: Int, height: Int) {
        // Stub: не реализовано для JVM
    }

    actual fun decode(frame: RtspFrame): Boolean = false
    actual fun setCallback(callback: DecodedFrameCallback?) {}
    actual fun getInfo(): DecoderInfo? = null
    actual fun release() {}
}
