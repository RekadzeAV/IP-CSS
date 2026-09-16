package com.company.ipcamera.server.security

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

private val logger = KotlinLogging.logger {}

/**
 * Сервис расширенной защиты от угроз (4.3.2).
 *
 * Включает:
 * - Intrusion Detection (IDS) — обнаружение подозрительной активности
 * - Anomaly Detection — обнаружение аномалий в поведении
 * - Real-time monitoring — мониторинг в реальном времени
 * - Automated response — автоматическая реакция на угрозы
 */
class AdvancedThreatProtectionService(
    private val securityAlertRepository: SecurityAlertRepository,
    private val monitoringService: SecurityMonitoringService? = null
) {
    // === Конфигурация ===
    data class ThreatConfig(
        val maxFailedLogins: Int = 10,          // Максимум неудачных логинов за окно
        val maxRequestsPerMinute: Int = 300,     // Максимум запросов в минуту
        val maxConcurrentSessionsPerUser: Int = 5, // Максимум одновременных сессий
        val windowDurationMs: Long = 60_000,      // Длительность окна мониторинга (1 минута)
        val blockDurationMs: Long = 300_000,      // Длительность блокировки (5 минут)
        val enableAutoBlock: Boolean = true,       // Автоматическая блокировка
        val enableNotifications: Boolean = true     // Отправка уведомлений
    )

    private val config = ThreatConfig()
    private val mutex = Mutex()

    // === Хранилища для мониторинга ===

    // Счётчики неудачных попыток по IP
    private val failedLoginCounters = ConcurrentHashMap<String, MutableList<Long>>()

    // Счётчики запросов по IP
    private val requestCounters = ConcurrentHashMap<String, MutableList<Long>>()

    // Заблокированные IP
    private val blockedIps = ConcurrentHashMap<String, Long>()

    // Активные сессии пользователей
    private val activeSessions = ConcurrentHashMap<String, MutableSet<String>>()

    // Лог событий безопасности
    private val securityEventLog = ConcurrentLinkedQueue<SecurityEvent>()

    /**
     * Событие безопасности.
     */
    data class SecurityEvent(
        val type: EventType,
        val ip: String,
        val userId: String? = null,
        val description: String,
        val timestamp: Long = System.currentTimeMillis(),
        val severity: Severity = Severity.MEDIUM
    )

    enum class EventType {
        FAILED_LOGIN,
        BRUTE_FORCE_DETECTED,
        RATE_LIMIT_EXCEEDED,
        SUSPICIOUS_ACTIVITY,
        SESSION_ABUSE,
        INTRUSION_DETECTED,
        AUTOMATED_BLOCK,
        AUTOMATED_UNBLOCK,
        ANOMALY_DETECTED
    }

    enum class Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    /**
     * Результат проверки запроса.
     */
    data class ThreatAssessment(
        val isBlocked: Boolean = false,
        val riskLevel: RiskLevel = RiskLevel.LOW,
        val events: List<SecurityEvent> = emptyList(),
        val requiredAction: Action = Action.ALLOW
    )

    enum class RiskLevel { LOW, MEDIUM, HIGH, CRITICAL }
    enum class Action { ALLOW, CHALLENGE, BLOCK, ISOLATE }

    // === Основные методы ===

    /**
     * Проверить запрос на угрозы.
     * Вызывается для каждого входящего запроса.
     */
    suspend fun assessRequest(
        ip: String,
        userId: String? = null,
        path: String? = null
    ): ThreatAssessment = mutex.withLock {
        // Проверка на блокировку
        if (isIpBlocked(ip)) {
            logger.warn { "Blocked request from IP: $ip (userId=$userId, path=$path)" }
            return ThreatAssessment(
                isBlocked = true,
                riskLevel = RiskLevel.CRITICAL,
                requiredAction = Action.BLOCK
            )
        }

        val events = mutableListOf<SecurityEvent>()

        // 1. Rate limiting — проверка частоты запросов
        val requestCount = incrementAndCheckCounter(requestCounters, ip)
        if (requestCount > config.maxRequestsPerMinute) {
            val event = SecurityEvent(
                type = EventType.RATE_LIMIT_EXCEEDED,
                ip = ip,
                userId = userId,
                description = "Rate limit exceeded: $requestCount requests/min (max: ${config.maxRequestsPerMinute})",
                severity = Severity.HIGH
            )
            events.add(event)
            logSecurityEvent(event)

            // Автоматическая блокировка при превышении
            if (config.enableAutoBlock) {
                blockIp(ip)
                events.add(
                    SecurityEvent(
                        type = EventType.AUTOMATED_BLOCK,
                        ip = ip,
                        description = "IP auto-blocked: rate limit exceeded",
                        severity = Severity.CRITICAL
                    )
                )
            }

            return ThreatAssessment(
                riskLevel = RiskLevel.HIGH,
                events = events,
                requiredAction = Action.CHALLENGE
            )
        }

        // 2. Проверка сессий пользователя
        if (userId != null) {
            val sessions = activeSessions.getOrPut(userId) { ConcurrentHashMap.newKeySet() }
            if (sessions.size > config.maxConcurrentSessionsPerUser) {
                events.add(
                    SecurityEvent(
                        type = EventType.SESSION_ABUSE,
                        ip = ip,
                        userId = userId,
                        description = "Concurrent sessions exceeded: ${sessions.size} (max: ${config.maxConcurrentSessionsPerUser})",
                        severity = Severity.MEDIUM
                    )
                )
            }
        }

        // 3. Определяем уровень риска
        val riskLevel = when {
            events.isEmpty() -> RiskLevel.LOW
            events.any { it.severity == Severity.CRITICAL } -> RiskLevel.CRITICAL
            events.any { it.severity == Severity.HIGH } -> RiskLevel.HIGH
            else -> RiskLevel.MEDIUM
        }

        return ThreatAssessment(
            riskLevel = riskLevel,
            events = events,
            requiredAction = if (riskLevel == RiskLevel.CRITICAL) Action.BLOCK else Action.ALLOW
        )
    }

    /**
     * Зафиксировать неудачную попытку входа.
     */
    suspend fun reportFailedLogin(ip: String, username: String? = null) {
        val count = incrementAndCheckCounter(failedLoginCounters, ip)

        val event = SecurityEvent(
            type = EventType.FAILED_LOGIN,
            ip = ip,
            userId = username,
            description = "Failed login attempt #$count for user: ${username ?: "unknown"}",
            severity = if (count > config.maxFailedLogins / 2) Severity.HIGH else Severity.LOW
        )
        logSecurityEvent(event)

        // Обнаружение brute force
        if (count >= config.maxFailedLogins) {
            val bruteForceEvent = SecurityEvent(
                type = EventType.BRUTE_FORCE_DETECTED,
                ip = ip,
                description = "Brute force detected: $count failed attempts",
                severity = Severity.CRITICAL
            )
            logSecurityEvent(bruteForceEvent)
            logger.warn { "Brute force attack detected from IP: $ip ($count attempts)" }

            // Автоматическая блокировка
            if (config.enableAutoBlock) {
                blockIp(ip)
            }
        }
    }

    /**
     * Зафиксировать успешный вход.
     */
    suspend fun reportSuccessfulLogin(ip: String, userId: String, sessionId: String) {
        // Очищаем счётчик неудачных попыток
        failedLoginCounters.remove(ip)

        // Регистрируем сессию
        activeSessions.getOrPut(userId) { ConcurrentHashMap.newKeySet() }.add(sessionId)

        // Снимаем блокировку если была
        if (config.enableAutoBlock) {
            unblockIp(ip)
        }

        logger.info { "Successful login: userId=$userId from IP=$ip" }
    }

    /**
     * Завершить сессию пользователя.
     */
    suspend fun reportLogout(userId: String, sessionId: String) {
        activeSessions[userId]?.remove(sessionId)
    }

    /**
     * Обнаружение аномалий.
     */
    fun reportAnomaly(
        description: String,
        ip: String? = null,
        userId: String? = null,
        details: Map<String, Any> = emptyMap()
    ) {
        val event = SecurityEvent(
            type = EventType.ANOMALY_DETECTED,
            ip = ip ?: "unknown",
            userId = userId,
            description = description,
            severity = Severity.MEDIUM
        )
        logSecurityEvent(event)
        logger.warn { "Anomaly detected: $description (ip=$ip, userId=$userId)" }
    }

    /**
     * Обнаружение вторжения.
     */
    suspend fun reportIntrusion(
        description: String,
        ip: String,
        severity: Severity = Severity.HIGH
    ) {
        val event = SecurityEvent(
            type = EventType.INTRUSION_DETECTED,
            ip = ip,
            description = description,
            severity = severity
        )
        logSecurityEvent(event)

        // Автоматическая блокировка при CRITICAL
        if (severity == Severity.CRITICAL && config.enableAutoBlock) {
            blockIp(ip)
        }

        logger.error { "Intrusion detected: $description (ip=$ip, severity=$severity)" }
    }

    // === Управление блокировками ===

    /**
     * Проверить, заблокирован ли IP.
     */
    fun isIpBlocked(ip: String): Boolean {
        val blockedUntil = blockedIps[ip] ?: return false
        if (System.currentTimeMillis() > blockedUntil) {
            // Блокировка истекла
            blockedIps.remove(ip)
            logger.info { "IP unblocked automatically: $ip" }
            return false
        }
        return true
    }

    /**
     * Заблокировать IP.
     */
    fun blockIp(ip: String, durationMs: Long = config.blockDurationMs) {
        val blockedUntil = System.currentTimeMillis() + durationMs
        blockedIps[ip] = blockedUntil
        logger.warn { "IP blocked: $ip until ${Instant.ofEpochMilli(blockedUntil)}" }
        logger.warn { "IP blocked: $ip for ${durationMs / 1000}s" }
    }

    /**
     * Разблокировать IP.
     */
    fun unblockIp(ip: String) {
        blockedIps.remove(ip)
        logger.info { "IP unblocked: $ip" }
        logger.info { "IP unblocked: $ip" }
    }

    // === Мониторинг ===

    /**
     * Получить текущую статистику.
     */
    suspend fun getThreatStats(): ThreatStats = mutex.withLock {
        ThreatStats(
            blockedIpsCount = blockedIps.size,
            activeThreats = securityEventLog.size,
            recentEvents = securityEventLog.toList().takeLast(50),
            currentRiskLevel = calculateOverallRiskLevel()
        )
    }

    data class ThreatStats(
        val blockedIpsCount: Int,
        val activeThreats: Int,
        val recentEvents: List<SecurityEvent>,
        val currentRiskLevel: RiskLevel
    )

    // === Приватные методы ===

    private fun incrementAndCheckCounter(
        counters: ConcurrentHashMap<String, MutableList<Long>>,
        key: String
    ): Int {
        val now = System.currentTimeMillis()
        val timestamps = counters.getOrPut(key) { mutableListOf() }

        // Удаляем устаревшие записи
        timestamps.removeAll { now - it > config.windowDurationMs }

        // Добавляем текущий timestamp
        timestamps.add(now)

        return timestamps.size
    }

    private fun logSecurityEvent(event: SecurityEvent) {
        securityEventLog.add(event)

        // Асинхронно сохраняем в репозиторий
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alertType = mapToSecurityAlertType(event.type)
                val alertSeverity = mapToSecurityEventSeverity(event.severity)
                securityAlertRepository.save(
                    SecurityAlert(
                        id = "threat-${System.currentTimeMillis()}",
                        type = alertType,
                        title = event.description,
                        description = event.description,
                        severity = alertSeverity,
                        createdAt = event.timestamp
                    )
                )
            } catch (e: Exception) {
                logger.error(e) { "Failed to save threat event" }
            }
        }
    }

    private fun calculateOverallRiskLevel(): RiskLevel {
        val recentEvents = securityEventLog.toList().takeLast(100)
        return when {
            recentEvents.any { it.severity == Severity.CRITICAL } -> RiskLevel.CRITICAL
            recentEvents.any { it.severity == Severity.HIGH } -> RiskLevel.HIGH
            recentEvents.any { it.severity == Severity.MEDIUM } -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
    }

    /**
     * Очистить устаревшие события.
     */
    fun cleanup() {
        val cutoff = System.currentTimeMillis() - 3_600_000 // 1 час
        securityEventLog.removeAll { it.timestamp < cutoff }
    }

    /**
     * Получить конфигурацию.
     */
    fun getConfig(): ThreatConfig = config

    private fun mapToSecurityAlertType(eventType: EventType): SecurityAlertType {
        return when (eventType) {
            EventType.FAILED_LOGIN -> SecurityAlertType.FAILED_LOGIN
            EventType.BRUTE_FORCE_DETECTED -> SecurityAlertType.BRUTE_FORCE
            EventType.RATE_LIMIT_EXCEEDED -> SecurityAlertType.RATE_LIMIT
            EventType.SUSPICIOUS_ACTIVITY -> SecurityAlertType.SUSPICIOUS_ACTIVITY
            EventType.SESSION_ABUSE -> SecurityAlertType.SESSION_ABUSE
            EventType.INTRUSION_DETECTED -> SecurityAlertType.INTRUSION
            EventType.AUTOMATED_BLOCK, EventType.AUTOMATED_UNBLOCK -> SecurityAlertType.AUTOMATED_BLOCK
            EventType.ANOMALY_DETECTED -> SecurityAlertType.ANOMALY
        }
    }

    private fun mapToSecurityEventSeverity(severity: Severity): com.company.ipcamera.server.security.SecurityEventSeverity {
        return when (severity) {
            Severity.LOW -> com.company.ipcamera.server.security.SecurityEventSeverity.INFO
            Severity.MEDIUM -> com.company.ipcamera.server.security.SecurityEventSeverity.WARNING
            Severity.HIGH -> com.company.ipcamera.server.security.SecurityEventSeverity.ERROR
            Severity.CRITICAL -> com.company.ipcamera.server.security.SecurityEventSeverity.CRITICAL
        }
    }
}
