package com.company.ipcamera.core.network.dto

import kotlinx.serialization.Serializable

/**
 * DTO для статуса трансляции
 */
@Serializable
data class StreamStatusResponse(
    val active: Boolean,
    val streamId: String? = null,
    val hlsUrl: String? = null,
    val rtspUrl: String? = null
)

/**
 * DTO для RTSP URL
 */
@Serializable
data class RtspStreamUrlResponse(
    val rtspUrl: String
)

