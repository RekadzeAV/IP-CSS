package com.company.ipcamera.server.security

import mu.KotlinLogging
import java.time.Instant

private val securityLogger = KotlinLogging.logger("SECURITY")

/**
 * Типы событий безопасности
 */
enum class SecurityEventType {
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    LOGOUT,
    UNAUTHORIZED_ACCESS,
    RATE_LIMIT_EXCEEDED,
    INVALID_TOKEN,
    TOKEN_EXPIRED,
    PASSWORD_CHANGE,
    USER_CREATED,
    USER_DELETED,
    PERMISSION_DENIED,
    SUSPICIOUS_ACTIVITY,
    DATA_ACCESS,
    CONFIGURATION_CHANGE
}

/**
 * Уровень серьезности события
 */
enum class SecurityEventSeverity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}

/**
 * Данные события безопасности
 */
data class SecurityEvent(
    val type: SecurityEventType,
    val severity: SecurityEventSeverity,
    val userId: String?,
    val username: String?,
    val ipAddress: String?,
    val userAgent: String?,
    val details: Map<String, Any?> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Логгер для событий безопасности
 */
object SecurityLogger {

    /**
     * Логирование события безопасности
     */
    fun log(event: SecurityEvent) {
        val message = buildString {
            append("[${event.type.name}]")
            if (event.username != null) {
                append(" User: ${event.username}")
            }
            if (event.userId != null) {
                append(" (ID: ${event.userId})")
            }
            if (event.ipAddress != null) {
                append(" IP: ${event.ipAddress}")
            }
            if (event.details.isNotEmpty()) {
                append(" Details: ${event.details.entries.joinToString(", ") { "${it.key}=${it.value}" }}")
            }
        }

        when (event.severity) {
            SecurityEventSeverity.INFO -> securityLogger.info { message }
            SecurityEventSeverity.WARNING -> securityLogger.warn { message }
            SecurityEventSeverity.ERROR -> securityLogger.error { message }
            SecurityEventSeverity.CRITICAL -> securityLogger.error { "[CRITICAL] $message" }
        }
    }

    /**
     * Логирование успешного входа
     */
    fun logLoginSuccess(userId: String, username: String, ipAddress: String?, userAgent: String?) {
        log(
            SecurityEvent(
                type = SecurityEventType.LOGIN_SUCCESS,
                severity = SecurityEventSeverity.INFO,
                userId = userId,
                username = username,
                ipAddress = ipAddress,
                userAgent = userAgent
            )
        )
    }

    /**
     * Логирование неудачного входа
     */
    fun logLoginFailure(username: String?, ipAddress: String?, userAgent: String?, reason: String? = null) {
        log(
            SecurityEvent(
                type = SecurityEventType.LOGIN_FAILURE,
                severity = SecurityEventSeverity.WARNING,
                userId = null,
                username = username,
                ipAddress = ipAddress,
                userAgent = userAgent,
                details = if (reason != null) mapOf("reason" to reason) else emptyMap()
            )
        )
    }

    /**
     * Логирование выхода
     */
    fun logLogout(userId: String, username: String, ipAddress: String?) {
        log(
            SecurityEvent(
                type = SecurityEventType.LOGOUT,
                severity = SecurityEventSeverity.INFO,
                userId = userId,
                username = username,
                ipAddress = ipAddress
            )
        )
    }

    /**
     * Логирование несанкционированного доступа
     */
    fun logUnauthorizedAccess(userId: String?, username: String?, ipAddress: String?, resource: String) {
        log(
            SecurityEvent(
                type = SecurityEventType.UNAUTHORIZED_ACCESS,
                severity = SecurityEventSeverity.WARNING,
                userId = userId,
                username = username,
                ipAddress = ipAddress,
                details = mapOf("resource" to resource)
            )
        )
    }

    /**
     * Логирование превышения rate limit
     */
    fun logRateLimitExceeded(identifier: String, ipAddress: String?, endpoint: String) {
        log(
            SecurityEvent(
                type = SecurityEventType.RATE_LIMIT_EXCEEDED,
                severity = SecurityEventSeverity.WARNING,
                userId = null,
                username = null,
                ipAddress = ipAddress,
                details = mapOf(
                    "identifier" to identifier,
                    "endpoint" to endpoint
                )
            )
        )
    }

    /**
     * Логирование невалидного токена
     */
    fun logInvalidToken(ipAddress: String?, reason: String? = null) {
        log(
            SecurityEvent(
                type = SecurityEventType.INVALID_TOKEN,
                severity = SecurityEventSeverity.WARNING,
                userId = null,
                username = null,
                ipAddress = ipAddress,
                details = if (reason != null) mapOf("reason" to reason) else emptyMap()
            )
        )
    }

    /**
     * Логирование истечения токена
     */
    fun logTokenExpired(userId: String?, ipAddress: String?) {
        log(
            SecurityEvent(
                type = SecurityEventType.TOKEN_EXPIRED,
                severity = SecurityEventSeverity.INFO,
                userId = userId,
                username = null,
                ipAddress = ipAddress
            )
        )
    }

    /**
     * Логирование создания пользователя
     */
    fun logUserCreated(createdBy: String, createdUserId: String, createdUsername: String) {
        log(
            SecurityEvent(
                type = SecurityEventType.USER_CREATED,
                severity = SecurityEventSeverity.INFO,
                userId = createdBy,
                username = null,
                ipAddress = null,
                details = mapOf(
                    "created_user_id" to createdUserId,
                    "created_username" to createdUsername
                )
            )
        )
    }

    /**
     * Логирование удаления пользователя
     */
    fun logUserDeleted(deletedBy: String, deletedUserId: String, deletedUsername: String) {
        log(
            SecurityEvent(
                type = SecurityEventType.USER_DELETED,
                severity = SecurityEventSeverity.WARNING,
                userId = deletedBy,
                username = null,
                ipAddress = null,
                details = mapOf(
                    "deleted_user_id" to deletedUserId,
                    "deleted_username" to deletedUsername
                )
            )
        )
    }

    /**
     * Логирование отказа в доступе
     */
    fun logPermissionDenied(userId: String, username: String, ipAddress: String?, resource: String, requiredPermission: String) {
        log(
            SecurityEvent(
                type = SecurityEventType.PERMISSION_DENIED,
                severity = SecurityEventSeverity.WARNING,
                userId = userId,
                username = username,
                ipAddress = ipAddress,
                details = mapOf(
                    "resource" to resource,
                    "required_permission" to requiredPermission
                )
            )
        )
    }

    /**
     * Логирование подозрительной активности
     */
    fun logSuspiciousActivity(description: String, userId: String?, ipAddress: String?, details: Map<String, Any?> = emptyMap()) {
        log(
            SecurityEvent(
                type = SecurityEventType.SUSPICIOUS_ACTIVITY,
                severity = SecurityEventSeverity.ERROR,
                userId = userId,
                username = null,
                ipAddress = ipAddress,
                details = details + ("description" to description)
            )
        )
    }
}


