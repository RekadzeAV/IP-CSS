package com.company.ipcamera.server.service

/**
 * Качество потока для HLS и видеопотоков
 */
enum class StreamQuality {
    LOW,       // 640x360, 500kbps, 15fps
    MEDIUM,    // 1280x720, 1500kbps, 25fps
    HIGH,      // 1920x1080, 3000kbps, 30fps
    ULTRA,     // 1920x1080, 6000kbps, 30fps, fast preset
    QHD1440,   // 2560x1440 HEVC при HLS_HEVC_FOR_2K4K, иначе H.264 — см. docs/planning/HLS_TRANSCODE_PROFILES.md
    UHD4K,     // 3840x2160
    /** Тот же рендий 2K, всегда H.264 (добавляется в adaptive при HEVC для клиентов без HEVC). */
    QHD1440_H264,
    /** Тот же рендий 4K, всегда H.264. */
    UHD4K_H264
}

/** Строка для API (`low`, `qhd1440`, …) — совпадает с [StreamQuality.name].lowercase(). */
fun StreamQuality.toApiQualityString(): String = name.lowercase()

fun allStreamQualityApiValues(): List<String> = StreamQuality.entries.map { it.name.lowercase() }

