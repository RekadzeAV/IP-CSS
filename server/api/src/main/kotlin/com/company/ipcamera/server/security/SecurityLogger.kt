package com.company.ipcamera.server.security

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    CONFIGURATION_CHANGE,
    CSRF_ATTACK,
    FILE_UPLOAD_REJECTED,
    PATH_TRAVERSAL_ATTEMPT,
    SSRF_ATTEMPT,
    CERTIFICATE_PINNING_FAILURE,
    INPUT_VALIDATION_FAILED
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
    val userAgent: String? = null,
    val details: Map<String, Any?> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Логгер для событий безопасности
 */
object SecurityLogger {

    @Volatile
    var auditRepository: AuditLogRepository? = null
        private set

    @Volatile
    var monitoringService: SecurityMonitoringService? = null
        private set

    /** Подключить репозиторий аудита для сохранения событий (при AUDIT_PERSIST_ENABLED) */
    fun setAuditRepository(repo: AuditLogRepository?) {
        auditRepository = repo
    }

    /** Подключить сервис мониторинга для генерации алертов при событиях */
    fun setMonitoringService(service: SecurityMonitoringService?) {
        monitoringService = service
    }

    /**
     * Логирование события безопасности
     */
    fun log(event: SecurityEvent) {
        auditRepository?.let { repo ->
            CoroutineScope(Dispatchers.IO).launch {
                try { repo.append(event) } catch (_: Exception) { }
            }
        }
        monitoringService?.let { svc ->
            CoroutineScope(Dispatchers.IO).launch {
                try { svc.onAuditEvent(event) } catch (_: Exception) { }
            }
        }
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

    /**
     * Логирование CSRF атаки
     */
    fun logCsrfAttack(ipAddress: String?, endpoint: String, reason: String) {
        log(
            SecurityEvent(
                type = SecurityEventType.CSRF_ATTACK,
                severity = SecurityEventSeverity.WARNING,
                userId = null,
                username = null,
                ipAddress = ipAddress,
                details = mapOf(
                    "endpoint" to endpoint,
                    "reason" to reason
                )
            )
        )
    }

    /**
     * Логирование изменения пароля
     */
    fun logPasswordChange(userId: String, username: String, ipAddress: String?, changedByAdmin: Boolean = false) {
        log(
            SecurityEvent(
                type = SecurityEventType.PASSWORD_CHANGE,
                severity = SecurityEventSeverity.INFO,
                userId = userId,
                username = username,
                ipAddress = ipAddress,
                details = mapOf("changed_by_admin" to changedByAdmin)
            )
        )
    }

    /**
     * Логирование создания камеры
     */
    fun logCameraCreated(userId: String, username: String, cameraId: String, cameraName: String, ipAddress: String?) {
        log(
            SecurityEvent(
                type = SecurityEventType.DATA_ACCESS,
                severity = SecurityEventSeverity.INFO,
                userId = userId,
                username = username,
                ipAddress = ipAddress,
                details = mapOf(
                    "action" to "camera_created",
                    "camera_id" to cameraId,
                    "camera_name" to cameraName
                )
            )
        )
    }

    /**
     * Логирование обновления камеры
     */
    fun logCameraUpdated(userId: String, username: String, cameraId: String, cameraName: String, ipAddress: String?, changes: Map<String, Any?> = emptyMap()) {
        log(
            SecurityEvent(
                type = SecurityEventType.DATA_ACCESS,
                severity = SecurityEventSeverity.INFO,
                userId = userId,
                username = username,
                ipAddress = ipAddress,
                details = mapOf(
                    "action" to "camera_updated",
                    "camera_id" to cameraId,
                    "camera_name" to cameraName
                ) + changes
            )
        )
    }

    /**
     * Логирование удаления камеры
     */
    fun logCameraDeleted(userId: String, username: String, cameraId: String, cameraName: String, ipAddress: String?) {
        log(
            SecurityEvent(
                type = SecurityEventType.DATA_ACCESS,
                severity = SecurityEventSeverity.WARNING,
                userId = userId,
                username = username,
                ipAddress = ipAddress,
                details = mapOf(
                    "action" to "camera_deleted",
                    "camera_id" to cameraId,
                    "camera_name" to cameraName
                )
            )
        )
    }

    /**
     * Логирование создания записи
     */
    fun logRecordingCreated(userId: String, username: String, recordingId: String, cameraId: String, ipAddress: String?) {
        log(
            SecurityEvent(
                type = SecurityEventType.DATA_ACCESS,
                severity = SecurityEventSeverity.INFO,
                userId = userId,
                username = username,
                ipAddress = ipAddress,
                details = mapOf(
                    "action" to "recording_created",
                    "recording_id" to recordingId,
                    "camera_id" to cameraId
                )
            )
        )
    }

    /**
     * Логирование удаления записи
     */
    fun logRecordingDeleted(userId: String, username: String, recordingId: String, cameraId: String, ipAddress: String?) {
        log(
            SecurityEvent(
                type = SecurityEventType.DATA_ACCESS,
                severity = SecurityEventSeverity.WARNING,
                userId = userId,
                username = username,
                ipAddress = ipAddress,
                details = mapOf(
                    "action" to "recording_deleted",
                    "recording_id" to recordingId,
                    "camera_id" to cameraId
                )
            )
        )
    }

    /**
     * Логирование создания события
     */
    fun logEventCreated(userId: String?, username: String?, eventId: String, eventType: String, cameraId: String, ipAddress: String?) {
        log(
            SecurityEvent(
                type = SecurityEventType.DATA_ACCESS,
                severity = SecurityEventSeverity.INFO,
                userId = userId,
                username = username,
                ipAddress = ipAddress,
                details = mapOf(
                    "action" to "event_created",
                    "event_id" to eventId,
                    "event_type" to eventType,
                    "camera_id" to cameraId
                )
            )
        )
    }

    /**
     * Логирование удаления события
     */
    fun logEventDeleted(userId: String, username: String, eventId: String, ipAddress: String?) {
        log(
            SecurityEvent(
                type = SecurityEventType.DATA_ACCESS,
                severity = SecurityEventSeverity.WARNING,
                userId = userId,
                username = username,
                ipAddress = ipAddress,
                details = mapOf(
                    "action" to "event_deleted",
                    "event_id" to eventId
                )
            )
        )
    }

    /**
     * Логирование изменения настроек
     */
    fun logConfigurationChange(userId: String, username: String, settingKey: String, oldValue: Any?, newValue: Any?, ipAddress: String?) {
        log(
            SecurityEvent(
                type = SecurityEventType.CONFIGURATION_CHANGE,
                severity = SecurityEventSeverity.WARNING,
                userId = userId,
                username = username,
                ipAddress = ipAddress,
                details = mapOf(
                    "setting_key" to settingKey,
                    "old_value" to oldValue?.toString(),
                    "new_value" to newValue?.toString()
                )
            )
        )
    }

    /**
     * Логирование доступа к данным (чтение)
     */
    fun logDataAccess(userId: String, username: String, resourceType: String, resourceId: String?, action: String, ipAddress: String?) {
        log(
            SecurityEvent(
                type = SecurityEventType.DATA_ACCESS,
                severity = SecurityEventSeverity.INFO,
                userId = userId,
                username = username,
                ipAddress = ipAddress,
                details = mapOf(
                    "resource_type" to resourceType,
                    "resource_id" to resourceId,
                    "action" to action
                )
            )
        )
    }

    /**
     * Логирование массовых операций
     */
    fun logBulkOperation(userId: String, username: String, operation: String, resourceType: String, count: Int, ipAddress: String?) {
        log(
            SecurityEvent(
                type = SecurityEventType.DATA_ACCESS,
                severity = SecurityEventSeverity.WARNING,
                userId = userId,
                username = username,
                ipAddress = ipAddress,
                details = mapOf(
                    "operation" to operation,
                    "resource_type" to resourceType,
                    "count" to count
                )
            )
        )
    }

    /**
     * Логирование обновления пользователя
     */
    fun logUserUpdated(updatedBy: String, updatedUserId: String, updatedUsername: String, changes: Map<String, Any?>, ipAddress: String?) {
        log(
            SecurityEvent(
                type = SecurityEventType.USER_CREATED, // Используем существующий тип, можно добавить USER_UPDATED
                severity = SecurityEventSeverity.INFO,
                userId = updatedBy,
                username = null,
                ipAddress = ipAddress,
                details = mapOf(
                    "action" to "user_updated",
                    "updated_user_id" to updatedUserId,
                    "updated_username" to updatedUsername
                ) + changes
            )
        )
    }

    /**
     * Логирование экспорта данных
     */
    fun logDataExport(userId: String, username: String, exportType: String, recordCount: Int, ipAddress: String?) {
        log(
            SecurityEvent(
                type = SecurityEventType.DATA_ACCESS,
                severity = SecurityEventSeverity.INFO,
                userId = userId,
                username = username,
                ipAddress = ipAddress,
                details = mapOf(
                    "action" to "data_export",
                    "export_type" to exportType,
                    "record_count" to recordCount
                )
            )
        )
    }

    /**
     * Логирование импорта данных
     */
    fun logDataImport(userId: String, username: String, importType: String, recordCount: Int, ipAddress: String?) {
        log(
            SecurityEvent(
                type = SecurityEventType.DATA_ACCESS,
                severity = SecurityEventSeverity.WARNING,
                userId = userId,
                username = username,
                ipAddress = ipAddress,
                details = mapOf(
                    "action" to "data_import",
                    "import_type" to importType,
                    "record_count" to recordCount
                )
            )
        )
    }

    /**
     * Логирование отклонения загрузки файла
     */
    fun logFileUploadRejected(ipAddress: String?, reason: String, fileName: String?, fileSize: Long?, maxSize: Long?) {
        log(
            SecurityEvent(
                type = SecurityEventType.FILE_UPLOAD_REJECTED,
                severity = SecurityEventSeverity.WARNING,
                userId = null,
                username = null,
                ipAddress = ipAddress,
                details = mapOf(
                    "reason" to reason,
                    "file_name" to (fileName ?: "unknown"),
                    "file_size" to (fileSize?.toString() ?: "unknown"),
                    "max_size" to (maxSize?.toString() ?: "unknown")
                )
            )
        )
    }

    /**
     * Логирование попытки Path Traversal
     */
    fun logPathTraversalAttempt(ipAddress: String?, userId: String?, username: String?, path: String, baseDirectory: String?) {
        log(
            SecurityEvent(
                type = SecurityEventType.PATH_TRAVERSAL_ATTEMPT,
                severity = SecurityEventSeverity.ERROR,
                userId = userId,
                username = username,
                ipAddress = ipAddress,
                details = mapOf(
                    "path" to path,
                    "base_directory" to (baseDirectory ?: "not_specified")
                )
            )
        )
    }

    /**
     * Логирование попытки SSRF атаки
     */
    fun logSsrfAttempt(ipAddress: String?, userId: String?, username: String?, url: String, reason: String) {
        log(
            SecurityEvent(
                type = SecurityEventType.SSRF_ATTEMPT,
                severity = SecurityEventSeverity.ERROR,
                userId = userId,
                username = username,
                ipAddress = ipAddress,
                details = mapOf(
                    "url" to url,
                    "reason" to reason
                )
            )
        )
    }

    /**
     * Логирование ошибки certificate pinning
     */
    fun logCertificatePinningFailure(ipAddress: String?, host: String, reason: String) {
        log(
            SecurityEvent(
                type = SecurityEventType.CERTIFICATE_PINNING_FAILURE,
                severity = SecurityEventSeverity.ERROR,
                userId = null,
                username = null,
                ipAddress = ipAddress,
                details = mapOf(
                    "host" to host,
                    "reason" to reason
                )
            )
        )
    }

    /**
     * Логирование ошибки валидации входных данных
     */
    fun logInputValidationFailed(ipAddress: String?, userId: String?, username: String?, field: String, reason: String, value: String? = null) {
        log(
            SecurityEvent(
                type = SecurityEventType.INPUT_VALIDATION_FAILED,
                severity = SecurityEventSeverity.WARNING,
                userId = userId,
                username = username,
                ipAddress = ipAddress,
                userAgent = null,
                details = mapOf(
                    "field" to field,
                    "reason" to reason,
                    "value" to (value?.take(100) ?: "not_provided") // Ограничиваем длину значения для безопасности
                )
            )
        )
    }
}


