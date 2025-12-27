package com.company.ipcamera.core.network.dto

import kotlinx.serialization.Serializable

/**
 * DTO для уведомлений
 */
@Serializable
data class NotificationResponse(
    val id: String,
    val title: String,
    val message: String,
    val type: String,
    val priority: String,
    val cameraId: String? = null,
    val eventId: String? = null,
    val recordingId: String? = null,
    val channelId: String? = null,
    val icon: String? = null,
    val sound: Boolean = true,
    val vibration: Boolean = false,
    val read: Boolean = false,
    val readAt: Long? = null,
    val timestamp: Long,
    val extras: Map<String, String> = emptyMap()
)

