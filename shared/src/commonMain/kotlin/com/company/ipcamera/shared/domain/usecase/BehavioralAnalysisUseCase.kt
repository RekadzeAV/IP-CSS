package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.common.nowMillis
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.domain.model.analytics.Anomaly
import com.company.ipcamera.shared.domain.model.analytics.AnomalyType
import com.company.ipcamera.shared.domain.model.analytics.BehavioralReport
import com.company.ipcamera.shared.domain.model.analytics.HistoricalDataPoint
import com.company.ipcamera.shared.domain.model.analytics.Pattern
import com.company.ipcamera.shared.domain.model.analytics.PatternType
import com.company.ipcamera.shared.domain.model.analytics.Severity
import com.company.ipcamera.shared.domain.model.analytics.TimeRange
import com.company.ipcamera.shared.domain.repository.EventRepository
import com.company.ipcamera.shared.domain.repository.PaginatedResult
import kotlin.math.abs

/**
 * Use case для поведенческого анализа активности камер
 * Детекция аномалий, паттернов и генерация отчётов
 */
class BehavioralAnalysisUseCase(
    private val eventRepository: EventRepository,
) {
    /**
     * Сгенерировать поведенческий отчёт для камеры
     */
    suspend operator fun invoke(
        cameraId: String,
        days: Int = 7,
    ): Result<BehavioralReport> {
        if (cameraId.isBlank()) {
            return Result.failure(IllegalArgumentException("Camera ID cannot be blank"))
        }

        val endTime = nowMillis()
        val startTime = endTime - (days * 24 * 60 * 60 * 1000L)

        return try {
            val events: PaginatedResult<com.company.ipcamera.shared.domain.model.Event> = eventRepository.getEvents(
                cameraId = cameraId,
                startTime = startTime,
                endTime = endTime,
                limit = 10000,
            )

            val historicalData = events.items.map { event ->
                HistoricalDataPoint(
                    cameraId = event.cameraId,
                    timestamp = event.timestamp,
                    hour = (event.timestamp / (1000 * 60 * 60) % 24).toInt(),
                    dayOfWeek = ((event.timestamp / (1000 * 60 * 60 * 24)) % 7 + 1).toInt(),
                    activityLevel = when (event.severity) {
                        EventSeverity.INFO -> 0.3
                        EventSeverity.WARNING -> 0.5
                        EventSeverity.ERROR -> 0.7
                        EventSeverity.CRITICAL -> 1.0
                    },
                    motionCount = if (event.type == EventType.MOTION_DETECTION) 1 else 0,
                    eventCount = 1,
                    crowdDensity = null,
                )
            }

            val report = analyzeBehavior(cameraId, historicalData, startTime, endTime)
            Result.success(report)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun analyzeBehavior(
        cameraId: String,
        data: List<HistoricalDataPoint>,
        startTime: Long,
        endTime: Long,
    ): BehavioralReport {
        if (data.isEmpty()) {
            return BehavioralReport(
                cameraId = cameraId,
                timeRange = TimeRange(startTime, endTime),
                averageActivity = 0.0,
                peakHours = emptyList(),
                lowActivityHours = emptyList(),
                commonPatterns = emptyList(),
                anomalies = emptyList(),
            )
        }

        val averageActivity = data.sumOf { it.activityLevel } / data.size
        val hourlyActivity = (0..23).map { hour ->
            data.filter { it.hour == hour }.sumOf { it.activityLevel } /
                maxOf(1, data.count { it.hour == hour }).toDouble()
        }

        val peakHours = hourlyActivity
            .withIndex()
            .filter { it.value > averageActivity * 1.5 }
            .map { it.index }

        val lowActivityHours = hourlyActivity
            .withIndex()
            .filter { it.value < averageActivity * 0.5 }
            .map { it.index }

        val patterns = detectPatterns(data)
        val anomalies = detectAnomalies(data, averageActivity)

        return BehavioralReport(
            cameraId = cameraId,
            timeRange = TimeRange(startTime, endTime),
            averageActivity = averageActivity,
            peakHours = peakHours,
            lowActivityHours = lowActivityHours,
            commonPatterns = patterns,
            anomalies = anomalies,
        )
    }

    private fun detectPatterns(data: List<HistoricalDataPoint>): List<Pattern> {
        val patterns = mutableListOf<Pattern>()

        // Daily peak pattern
        val hourlyActivity = (0..23).map { hour ->
            data.filter { it.hour == hour }.sumOf { it.activityLevel }
        }
        val maxHour = hourlyActivity.indexOfFirst { it == hourlyActivity.maxOrNull() }
        if (maxHour >= 0 && hourlyActivity[maxHour] > 0) {
            patterns.add(
                Pattern(
                    type = PatternType.DAILY_PEAK,
                    description = "Peak activity at hour $maxHour",
                    confidence = 0.8,
                ),
            )
        }

        // Weekly pattern
        val weeklyActivity = (1..7).map { day ->
            data.filter { it.dayOfWeek == day }.sumOf { it.activityLevel }
        }
        val maxDay = weeklyActivity.indexOfFirst { it == weeklyActivity.maxOrNull() }
        if (maxDay >= 0 && weeklyActivity[maxDay] > 0) {
            patterns.add(
                Pattern(
                    type = PatternType.WEEKLY_PATTERN,
                    description = "Peak activity on day $maxDay",
                    confidence = 0.7,
                ),
            )
        }

        return patterns
    }

    private fun detectAnomalies(
        data: List<HistoricalDataPoint>,
        averageActivity: Double,
    ): List<Anomaly> {
        val anomalies = mutableListOf<Anomaly>()

        for (point in data) {
            // Unusual time activity
            if (point.activityLevel > averageActivity * 2.0) {
                anomalies.add(
                    Anomaly(
                        type = AnomalyType.UNUSUAL_TIME_ACTIVITY,
                        severity = if (point.activityLevel > 0.8) Severity.HIGH else Severity.MEDIUM,
                        description = "Unusual activity level: ${point.activityLevel}",
                        timestamp = point.timestamp,
                        confidence = 0.75,
                    ),
                )
            }

            // Loitering detection (based on multiple events in short time)
            val eventsInWindow = data.count {
                abs(it.timestamp - point.timestamp) < 300000 && // 5 minutes
                    it.cameraId == point.cameraId
            }
            if (eventsInWindow > 5) {
                anomalies.add(
                    Anomaly(
                        type = AnomalyType.LOITERING,
                        severity = Severity.MEDIUM,
                        description = "Potential loitering detected ($eventsInWindow events in 5 min)",
                        timestamp = point.timestamp,
                        confidence = 0.65,
                    ),
                )
            }
        }

        return anomalies.distinctBy { it.type to it.timestamp / 3600000 } // Group by hour
    }
}