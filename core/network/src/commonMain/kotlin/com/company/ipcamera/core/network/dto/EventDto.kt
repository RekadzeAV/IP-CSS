package com.company.ipcamera.core.network.dto

import kotlinx.serialization.Serializable

/**
 * DTO для событий
 */
@Serializable
data class EventResponse(
    val id: String,
    val cameraId: String,
    val cameraName: String? = null,
    val type: String,
    val severity: String = "INFO",
    val timestamp: Long,
    val description: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val acknowledged: Boolean = false,
    val acknowledgedAt: Long? = null,
    val acknowledgedBy: String? = null,
    val thumbnailUrl: String? = null,
    val videoUrl: String? = null
)

@Serializable
data class AcknowledgeEventResponse(
    val success: Boolean,
    val message: String? = null
)


