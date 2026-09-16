package com.company.ipcamera.shared.domain.model.analytics

import kotlinx.serialization.Serializable

/**
 * Behavioral analysis report
 */
@Serializable
data class BehavioralReport(
    val cameraId: String,
    val timeRange: TimeRange,
    val averageActivity: Double, // 0.0 - 1.0
    val peakHours: List<Int>, // Hours (0-23)
    val lowActivityHours: List<Int>,
    val commonPatterns: List<Pattern>,
    val anomalies: List<Anomaly>,
)

/**
 * Detected pattern
 */
@Serializable
data class Pattern(
    val type: PatternType,
    val description: String,
    val confidence: Double,
)

@Serializable
enum class PatternType {
    DAILY_PEAK,
    WEEKLY_PATTERN,
    SEASONAL_VARIATION,
    EVENT_CORRELATION,
    MOTION_FLOW,
}

/**
 * Detected anomaly
 */
@Serializable
data class Anomaly(
    val type: AnomalyType,
    val severity: Severity,
    val description: String,
    val timestamp: Long,
    val confidence: Double,
)

@Serializable
enum class AnomalyType {
    UNUSUAL_TIME_ACTIVITY,
    CROWD_FORMATION,
    LOITERING,
    ABANDONED_OBJECT,
    RESTRICTED_AREA_ENTRY,
    SPEED_ANOMALY,
    DIRECTION_ANOMALY,
    MISSING_OBJECT,
}

@Serializable
enum class Severity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}

/**
 * Heatmap data
 */
@Serializable
data class HeatmapData(
    val cameraId: String,
    val timeRange: TimeRange,
    val grid: List<List<Double>>, // 2D array of intensities (0.0 - 1.0)
    val maxIntensity: Double,
    val resolution: Int,
)

/**
 * Activity prediction
 */
@Serializable
data class ActivityPrediction(
    val cameraId: String,
    val predictedTime: Long,
    val predictedActivityLevel: Double, // 0.0 - 1.0
    val confidence: Double, // 0.0 - 1.0
    val peakProbability: Double, // 0.0 - 1.0
)

/**
 * Time range
 */
@Serializable
data class TimeRange(
    val startTime: Long,
    val endTime: Long,
)

/**
 * Historical data point
 */
@Serializable
data class HistoricalDataPoint(
    val cameraId: String,
    val timestamp: Long,
    val hour: Int,
    val dayOfWeek: Int, // 1-7 (Monday-Sunday)
    val activityLevel: Double,
    val motionCount: Int,
    val eventCount: Int,
    val crowdDensity: Double?,
)

/**
 * Event data for real-time analysis
 */
@Serializable
data class EventData(
    val cameraId: String,
    val timestamp: Long,
    val activityLevel: Double,
    val crowdDensity: Double,
    val loiteringTime: Long, // Seconds
    val direction: Direction?,
    val speed: Double?,
    val expectedSpeed: Double?,
)

@Serializable
enum class Direction {
    ENTERING,
    EXITING,
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT,
    RESTRICTED,
    UNKNOWN,
}

/**
 * Motion event
 */
@Serializable
data class MotionEvent(
    val id: String,
    val cameraId: String,
    val timestamp: Long,
    val zoneId: String,
    val intensity: Double,
    val duration: Long,
    val boundingBox: BoundingBox?,
)

@Serializable
data class BoundingBox(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)

/**
 * Prediction result for activity prediction
 */
@Serializable
data class PredictionResult(
    val cameraId: String,
    val timeRange: TimeRange,
    val predictions: List<ActivityPrediction>,
)

/**
 * Analytics statistics
 */
@Serializable
data class AnalyticsStatistics(
    val cameraId: String,
    val timeRange: TimeRange,
    val totalEvents: Int,
    val totalAnomalies: Int,
    val averageActivity: Double,
    val peakActivityTime: Long?,
    val lowActivityTime: Long?,
    val heatmapAvailable: Boolean,
)
