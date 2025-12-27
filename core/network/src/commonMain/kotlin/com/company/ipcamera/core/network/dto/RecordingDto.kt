package com.company.ipcamera.core.network.dto

import kotlinx.serialization.Serializable

/**
 * DTO для записей
 */
@Serializable
data class RecordingResponse(
    val id: String,
    val cameraId: String,
    val cameraName: String? = null,
    val startTime: Long,
    val endTime: Long? = null,
    val duration: Long,
    val filePath: String? = null,
    val fileSize: Long? = null,
    val format: String = "mp4",
    val quality: String = "HIGH",
    val status: String,
    val thumbnailUrl: String? = null,
    val createdAt: Long
)

@Serializable
data class StartRecordingRequest(
    val cameraId: String,
    val duration: Long? = null,
    val quality: String? = "HIGH",
    val format: String? = "mp4"
)

@Serializable
data class StartRecordingResponse(
    val recordingId: String,
    val cameraId: String,
    val startTime: Long,
    val estimatedEndTime: Long? = null
)

@Serializable
data class ExportRecordingRequest(
    val format: String = "mp4",
    val quality: String = "medium",
    val startTime: Long? = null,
    val endTime: Long? = null
)

@Serializable
data class ExportRecordingResponse(
    val exportId: String,
    val downloadUrl: String,
    val expiresAt: Long
)



