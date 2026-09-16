package com.company.ipcamera.server.security

/**
 * Тип алерта безопасности.
 */
enum class SecurityAlertType {
    FAILED_LOGIN, BRUTE_FORCE, RATE_LIMIT, SUSPICIOUS_ACTIVITY,
    SESSION_ABUSE, INTRUSION, AUTOMATED_BLOCK, ANOMALY,
    RATE_LIMIT_ABUSE, SUSPICIOUS_LOGIN
}

/**
 * Модель алерта безопасности, используемая SecurityAlertRepository.
 */
data class SecurityAlert(
    val id: String,
    val type: SecurityAlertType,
    val severity: SecurityEventSeverity = SecurityEventSeverity.INFO,
    val title: String,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null,
    val isResolved: Boolean = false,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Сводка дашборда безопасности.
 */
data class SecurityDashboardSummary(
    val eventCountByType: Map<SecurityEventType, Int> = emptyMap(),
    val recentEvents: List<SecurityEvent> = emptyList(),
    val activeAlerts: List<SecurityAlert> = emptyList(),
    val activeAlertCount: Int = 0,
    val from: Long = 0,
    val to: Long = 0
)
