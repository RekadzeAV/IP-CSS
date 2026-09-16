package com.company.ipcamera.shared.domain.model

/**
 * Запись о распознанном номерном знаке, сохранённая в БД (ANPR).
 */
data class StoredLicensePlate(
    val id: String,
    val cameraId: String,
    val timestamp: Long,
    val plateNumber: String,
    val confidence: Float,
    val country: String?,
    val bboxX: Int,
    val bboxY: Int,
    val bboxWidth: Int,
    val bboxHeight: Int,
    val createdAt: Long,
)
