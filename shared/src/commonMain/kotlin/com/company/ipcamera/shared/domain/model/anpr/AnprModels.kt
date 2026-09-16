package com.company.ipcamera.shared.domain.model.anpr

import kotlinx.serialization.Serializable

/**
 * License plate recognition result
 */
@Serializable
data class PlateResult(
    val plateNumber: String,
    val confidence: Double, // 0.0 - 1.0
    val region: String?, // Region/state code
    val regionConfidence: Double,
    val vehicleType: VehicleType?,
    val boundingBox: BoundingBox,
    val processingTimeMs: Long,
    val coordinates: List<Point>, // Polygon coordinates
)

/**
 * Vehicle type classification
 */
@Serializable
enum class VehicleType {
    SEDAN,
    SUV,
    TRUCK,
    MOTORCYCLE,
    BUS,
    VAN,
    PICKUP,
    COUPE,
    CONVERTIBLE,
    HATCHBACK,
    STATION_WAGON,
    MINIVAN,
    UNKNOWN,
}

/**
 * Bounding box for plate detection
 */
@Serializable
data class BoundingBox(
    val x: Int, // Top-left x
    val y: Int, // Top-left y
    val width: Int,
    val height: Int,
)

/**
 * Point coordinates
 */
@Serializable
data class Point(
    val x: Int,
    val y: Int,
)

/**
 * License plate database entry
 */
@Serializable
data class LicensePlate(
    val id: String,
    val plateNumber: String,
    val region: String?,
    val vehicleType: VehicleType?,
    val ownerName: String?,
    val ownerContact: String?,
    val notes: String?,
    val isBlacklisted: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * Plate detection event
 */
@Serializable
data class PlateDetectionEvent(
    val id: String,
    val cameraId: String,
    val timestamp: Long,
    val plateNumber: String,
    val confidence: Double,
    val plateId: String?, // Linked plate from database
    val snapshotPath: String?,
    val direction: PlateDirection?,
    val processed: Boolean = false,
)

/**
 * Direction of vehicle movement
 */
@Serializable
enum class PlateDirection {
    ENTERING,
    EXITING,
    UNKNOWN,
}

/**
 * ANPR statistics
 */
@Serializable
data class AnprStatistics(
    val cameraId: String,
    val timeRange: TimeRange,
    val totalDetections: Int,
    val uniquePlates: Int,
    val blacklistedDetections: Int,
    val averageConfidence: Double,
    val detectionsByHour: Map<Int, Int>,
    val topPlates: List<PlateFrequency>,
)

/**
 * Time range for statistics
 */
@Serializable
data class TimeRange(
    val startTime: Long,
    val endTime: Long,
)

/**
 * Plate frequency data
 */
@Serializable
data class PlateFrequency(
    val plateNumber: String,
    val count: Int,
    val lastSeen: Long,
)

/**
 * ANPR configuration
 */
@Serializable
data class AnprConfig(
    val enabled: Boolean,
    val region: String,
    val countries: List<String>,
    val minPlateSize: Int,
    val maxPlateSize: Int,
    val confidenceThreshold: Double,
    val processingTimeout: Long,
    val blacklistedPlates: List<String>,
    val alertOnBlacklist: Boolean,
)
