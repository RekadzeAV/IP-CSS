package com.company.ipcamera.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Запрос на экспорт записей
 */
@Serializable
data class ExportRequest(
    val recordingIds: List<String>,
    val format: String = "mp4", // mp4, mkv, avi
    val includeMetadata: Boolean = true,
    val outputName: String? = null,
    val quality: String = "high", // low, medium, high
)

/**
 * Статус экспорта
 */
@Serializable
data class ExportStatus(
    val id: String,
    val request: ExportRequest,
    val status: String, // pending, processing, completed, failed, cancelled
    val progress: Double, // 0.0 - 100.0
    val outputPath: String?,
    val error: String?,
    val createdAt: Long,
    val completedAt: Long?,
)
