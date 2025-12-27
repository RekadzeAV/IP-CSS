package com.company.ipcamera.server.service

/**
 * Качество потока для HLS и видеопотоков
 */
enum class StreamQuality {
    LOW,      // 640x360, 500kbps, 15fps
    MEDIUM,   // 1280x720, 1500kbps, 25fps
    HIGH,     // 1920x1080, 3000kbps, 30fps
    ULTRA     // 1920x1080, 6000kbps, 30fps, fast preset
}

