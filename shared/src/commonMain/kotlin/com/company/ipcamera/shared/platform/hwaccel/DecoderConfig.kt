package com.company.ipcamera.shared.platform.hwaccel

/**
 * Конфигурация аппаратного декодера
 */
data class DecoderConfig(
    val name: String,
    val format: String,
    val ffmpegDecoderName: String,
    val ffmpegArgs: List<String>,
    val supportedCodecs: List<VideoCodec>,
)