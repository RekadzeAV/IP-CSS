package com.company.ipcamera.server.security

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * Сервис мониторинга безопасности (4.3.3.2): правила и создание алертов.
 */
class SecurityMonitoringService(
    private val auditRepository: AuditLogRepository,
    private val alertRepository: SecurityAlertRepository
) {
    private val loginFailureCountByIp = ConcurrentHashMap<String, MutableList<Long>>()
    private val mutex = Mutex()

    /** Окно для подсчёта неудачных попыток входа (мс). */
    private val loginFailureWindowMs = 5 * 60 * 1000L

    /** Порог неудачных попыток для алерта BRUTE_FORCE. */
    private val loginFailureThreshold = 5

    /**
     * Вызывать при добавлении события в аудит (или из SecurityLogger).
     * Проверяет правила и создаёт алерты при срабатывании.
     */
    suspend fun onAuditEvent(event: SecurityEvent) {
        when (event.type) {
            SecurityEventType.LOGIN_FAILURE -> checkLoginFailureRule(event)
            SecurityEventType.RATE_LIMIT_EXCEEDED -> createRateLimitAlert(event)
            SecurityEventType.SUSPICIOUS_ACTIVITY -> createSuspiciousAlert(event)
            else -> { }
        }
    }

    private suspend fun checkLoginFailureRule(event: SecurityEvent) {
        val ip = event.ipAddress ?: return
        mutex.withLock {
            val list = loginFailureCountByIp.getOrPut(ip) { mutableListOf() }
            val now = System.currentTimeMillis()
            list.add(now)
            list.removeAll { it < now - loginFailureWindowMs }
            if (list.size >= loginFailureThreshold) {
                val count = list.size
                list.clear()
                createBruteForceAlert(ip, count)
            }
        }
    }

    private suspend fun createBruteForceAlert(ip: String, count: Int) {
        val id = "alert_${UUID.randomUUID().toString().take(8)}"
        val alert = SecurityAlert(
            id = id,
            type = SecurityAlertType.BRUTE_FORCE,
            severity = SecurityEventSeverity.WARNING,
            title = "Возможная атака подбора пароля",
            description = "Обнаружено $count неудачных попыток входа с IP: $ip за последние 5 минут.",
            createdAt = System.currentTimeMillis(),
            metadata = mapOf("ip" to ip, "count" to count.toString())
        )
        alertRepository.save(alert)
        logger.warn { "Security alert created: BRUTE_FORCE for IP $ip" }
    }

    private suspend fun createRateLimitAlert(event: SecurityEvent) {
        val id = "alert_${UUID.randomUUID().toString().take(8)}"
        val alert = SecurityAlert(
            id = id,
            type = SecurityAlertType.RATE_LIMIT_ABUSE,
            severity = SecurityEventSeverity.WARNING,
            title = "Превышение лимита запросов",
            description = event.details["identifier"]?.toString()?.let { "Identifier: $it" } ?: "Rate limit exceeded",
            createdAt = System.currentTimeMillis(),
            metadata = event.details.mapValues { (_, v) -> v?.toString() ?: "" }
        )
        alertRepository.save(alert)
    }

    private suspend fun createSuspiciousAlert(event: SecurityEvent) {
        val id = "alert_${UUID.randomUUID().toString().take(8)}"
        val alert = SecurityAlert(
            id = id,
            type = SecurityAlertType.SUSPICIOUS_LOGIN,
            severity = event.severity,
            title = "Подозрительная активность",
            description = event.details["description"]?.toString() ?: "Suspicious activity detected",
            createdAt = System.currentTimeMillis(),
            metadata = event.details.mapValues { (_, v) -> v?.toString() ?: "" }
        )
        alertRepository.save(alert)
    }

    /**
     * Сводка для дашборда: счётчики по типам за период, последние события, активные алерты.
     */
    suspend fun getDashboardSummary(
        fromTimestamp: Long? = null,
        toTimestamp: Long? = null,
        recentEventsLimit: Int = 20,
        activeAlertsLimit: Int = 10
    ): SecurityDashboardSummary {
        val from = fromTimestamp ?: (System.currentTimeMillis() - 24 * 60 * 60 * 1000L)
        val to = toTimestamp ?: System.currentTimeMillis()
        val events = auditRepository.getRecent(limit = 500, offset = 0, fromTimestamp = from, toTimestamp = to)
        val byType = events.groupBy { it.type }.mapValues { it.value.size }
        val recent = auditRepository.getRecent(limit = recentEventsLimit, offset = 0)
        val activeAlerts = alertRepository.getActive(limit = activeAlertsLimit)
        return SecurityDashboardSummary(
            eventCountByType = byType,
            recentEvents = recent,
            activeAlerts = activeAlerts,
            activeAlertCount = activeAlerts.size,
            from = from,
            to = to
        )
    }
}

