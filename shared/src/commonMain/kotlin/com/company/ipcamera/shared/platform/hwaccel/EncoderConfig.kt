package com.company.ipcamera.shared.platform.hwaccel

/**
 * Конфигурация аппаратного энкодера
 */
data class EncoderConfig(
    val name: String,
    val format: String,
    val ffmpegEncoderName: String,
    val ffmpegArgs: List<String>,
    val maxResolution: com.company.ipcamera.core.common.model.Resolution,
    val supportedCodecs: List<VideoCodec>,
)