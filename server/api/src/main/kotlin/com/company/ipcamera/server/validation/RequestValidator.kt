package com.company.ipcamera.server.validation

import com.company.ipcamera.server.dto.*
import com.company.ipcamera.shared.domain.model.*
import java.net.URI
import java.net.URL
import java.util.regex.Pattern

/**
 * Результат валидации
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String, val field: String? = null) : ValidationResult()
}

/**
 * Валидатор для входных данных запросов
 */
object RequestValidator {

    // Паттерн для email
    private val EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"
    )

    // Паттерн для username (буквы, цифры, подчеркивания, 3-30 символов)
    private val USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,30}\$")

    /**
     * Валидация запроса на вход
     */
    fun validateLoginRequest(request: LoginRequest): ValidationResult {
        if (request.username.isBlank()) {
            return ValidationResult.Error("Username is required", "username")
        }
        if (request.username.length < 3) {
            return ValidationResult.Error("Username must be at least 3 characters", "username")
        }
        if (request.username.length > 30) {
            return ValidationResult.Error("Username must be at most 30 characters", "username")
        }
        if (request.password.isBlank()) {
            return ValidationResult.Error("Password is required", "password")
        }
        if (request.password.length < 6) {
            return ValidationResult.Error("Password must be at least 6 characters", "password")
        }
        return ValidationResult.Success
    }

    /**
     * Валидация запроса на создание пользователя
     */
    fun validateCreateUserRequest(request: CreateUserRequest): ValidationResult {
        // Валидация username
        if (request.username.isBlank()) {
            return ValidationResult.Error("Username is required", "username")
        }
        if (!USERNAME_PATTERN.matcher(request.username).matches()) {
            return ValidationResult.Error(
                "Username must contain only letters, numbers, and underscores (3-30 characters)",
                "username"
            )
        }

        // Валидация email (если указан)
        if (request.email != null && request.email.isNotBlank()) {
            if (!EMAIL_PATTERN.matcher(request.email).matches()) {
                return ValidationResult.Error("Invalid email format", "email")
            }
        }

        // Валидация пароля
        if (request.password.isBlank()) {
            return ValidationResult.Error("Password is required", "password")
        }
        if (request.password.length < 8) {
            return ValidationResult.Error("Password must be at least 8 characters", "password")
        }
        if (request.password.length > 128) {
            return ValidationResult.Error("Password must be at most 128 characters", "password")
        }

        // Валидация роли
        val validRoles = listOf("VIEWER", "OPERATOR", "ADMIN")
        if (!validRoles.contains(request.role.uppercase())) {
            return ValidationResult.Error(
                "Invalid role. Must be one of: ${validRoles.joinToString(", ")}",
                "role"
            )
        }

        return ValidationResult.Success
    }

    /**
     * Валидация запроса на обновление пользователя
     */
    fun validateUpdateUserRequest(request: UpdateUserRequest): ValidationResult {
        // Валидация email (если указан)
        if (request.email != null && request.email.isNotBlank()) {
            if (!EMAIL_PATTERN.matcher(request.email).matches()) {
                return ValidationResult.Error("Invalid email format", "email")
            }
        }

        // Валидация роли (если указана)
        if (request.role != null) {
            val validRoles = listOf("VIEWER", "OPERATOR", "ADMIN")
            if (!validRoles.contains(request.role.uppercase())) {
                return ValidationResult.Error(
                    "Invalid role. Must be one of: ${validRoles.joinToString(", ")}",
                    "role"
                )
            }
        }

        return ValidationResult.Success
    }

    /**
     * Валидация запроса на создание камеры
     */
    fun validateCreateCameraRequest(request: CreateCameraRequest): ValidationResult {
        // Валидация имени
        if (request.name.isBlank()) {
            return ValidationResult.Error("Camera name is required", "name")
        }
        if (request.name.length > 100) {
            return ValidationResult.Error("Camera name must be at most 100 characters", "name")
        }

        // Валидация URL
        if (request.url.isBlank()) {
            return ValidationResult.Error("Camera URL is required", "url")
        }
        val urlValidation = validateUrl(request.url)
        if (urlValidation is ValidationResult.Error) {
            return ValidationResult.Error("Invalid camera URL: ${urlValidation.message}", "url")
        }

        // Валидация username (если указан)
        if (request.username != null && request.username.isNotBlank()) {
            if (request.username.length > 100) {
                return ValidationResult.Error(
                    "Username must be at most 100 characters",
                    "username"
                )
            }
        }

        // Валидация password (если указан)
        if (request.password != null && request.password.length > 128) {
            return ValidationResult.Error(
                "Password must be at most 128 characters",
                "password"
            )
        }

        return ValidationResult.Success
    }

    /**
     * Валидация запроса на обновление камеры
     */
    fun validateUpdateCameraRequest(request: UpdateCameraRequest): ValidationResult {
        // Валидация имени (если указано)
        if (request.name != null) {
            if (request.name.isBlank()) {
                return ValidationResult.Error("Camera name cannot be blank", "name")
            }
            if (request.name.length > 100) {
                return ValidationResult.Error(
                    "Camera name must be at most 100 characters",
                    "name"
                )
            }
        }

        // Валидация URL (если указан)
        if (request.url != null) {
            if (request.url.isBlank()) {
                return ValidationResult.Error("Camera URL cannot be blank", "url")
            }
            val urlValidation = validateUrl(request.url)
            if (urlValidation is ValidationResult.Error) {
                return ValidationResult.Error(
                    "Invalid camera URL: ${urlValidation.message}",
                    "url"
                )
            }
        }

        // Валидация username (если указан)
        if (request.username != null && request.username.isNotBlank()) {
            if (request.username.length > 100) {
                return ValidationResult.Error(
                    "Username must be at most 100 characters",
                    "username"
                )
            }
        }

        // Валидация password (если указан)
        if (request.password != null && request.password.length > 128) {
            return ValidationResult.Error(
                "Password must be at most 128 characters",
                "password"
            )
        }

        return ValidationResult.Success
    }

    /**
     * Валидация URL
     * Включает проверку на SSRF уязвимости
     */
    private fun validateUrl(urlString: String): ValidationResult {
        return try {
            val uri = URI(urlString)
            val protocol = uri.scheme?.lowercase()
                ?: return ValidationResult.Error("URL must contain a scheme")
            if (protocol !in listOf("http", "https", "rtsp", "rtmp")) {
                return ValidationResult.Error(
                    "URL protocol must be one of: http, https, rtsp, rtmp"
                )
            }
            if (uri.host.isNullOrBlank()) {
                return ValidationResult.Error("URL must contain a host")
            }

            // SSRF защита - проверка на внутренние IP адреса и метаданные сервисы
            val ssrfValidation = com.company.ipcamera.server.security.SsrfProtection.validateUrlForSsrf(urlString)
            if (ssrfValidation is ValidationResult.Error) {
                return ValidationResult.Error(
                    "SSRF protection: ${ssrfValidation.message}"
                )
            }

            // Дополнительная валидация для RTSP URL
            if (protocol == "rtsp") {
                // RTSP URL должен иметь формат: rtsp://host[:port]/path
                val port = uri.port
                if (port != -1 && (port < 1 || port > 65535)) {
                    return ValidationResult.Error("RTSP port must be between 1 and 65535")
                }
                // Проверяем, что путь не пустой (обычно RTSP потоки имеют путь)
                if (uri.path.isNullOrBlank() || uri.path == "/") {
                    return ValidationResult.Error("RTSP URL must contain a path (e.g., /stream1)")
                }
            }

            ValidationResult.Success
        } catch (e: Exception) {
            ValidationResult.Error("Invalid URL format: ${e.message}")
        }
    }

    /**
     * Валидация запроса на обновление настроек
     */
    fun validateUpdateSettingsRequest(request: UpdateSettingsRequest): ValidationResult {
        if (request.settings.isEmpty()) {
            return ValidationResult.Error("At least one setting must be provided", "settings")
        }

        // Валидация каждого параметра настройки
        for ((key, value) in request.settings) {
            if (key.isBlank()) {
                return ValidationResult.Error("Setting key cannot be blank", "settings")
            }
            if (key.length > 100) {
                return ValidationResult.Error("Setting key must be at most 100 characters", "settings")
            }
            // Значение может быть любым строковым, но ограничим длину
            if (value.length > 10000) {
                return ValidationResult.Error("Setting value must be at most 10000 characters", "settings")
            }
        }

        return ValidationResult.Success
    }

    /**
     * Валидация запроса на подтверждение событий
     */
    fun validateAcknowledgeEventsRequest(request: AcknowledgeEventsRequest): ValidationResult {
        if (request.ids.isEmpty()) {
            return ValidationResult.Error("At least one event ID must be provided", "ids")
        }
        if (request.ids.size > 100) {
            return ValidationResult.Error("Cannot acknowledge more than 100 events at once", "ids")
        }

        // Проверяем формат ID (UUID или другой допустимый формат)
        for (id in request.ids) {
            if (id.isBlank()) {
                return ValidationResult.Error("Event ID cannot be blank", "ids")
            }
            // Проверяем базовый формат (не обязательно UUID, но должен быть непустой строкой)
            if (id.length > 100) {
                return ValidationResult.Error("Event ID must be at most 100 characters", "ids")
            }
        }

        return ValidationResult.Success
    }

    /**
     * Валидация параметров пагинации
     */
    fun validatePagination(page: Int?, limit: Int?): ValidationResult {
        val pageValue = page ?: 1
        val limitValue = limit ?: 20

        if (pageValue < 1) {
            return ValidationResult.Error("Page must be at least 1", "page")
        }
        if (limitValue < 1) {
            return ValidationResult.Error("Limit must be at least 1", "limit")
        }
        if (limitValue > 100) {
            return ValidationResult.Error("Limit must be at most 100", "limit")
        }

        return ValidationResult.Success
    }

    /**
     * Валидация запроса на изменение качества потока
     */
    fun validateSetStreamQualityRequest(request: com.company.ipcamera.server.dto.SetStreamQualityRequest): ValidationResult {
        if (request.quality.isBlank()) {
            return ValidationResult.Error("Quality is required", "quality")
        }

        val validQualities = com.company.ipcamera.server.service.allStreamQualityApiValues()
        val qualityLower = request.quality.lowercase()
        if (!validQualities.contains(qualityLower)) {
            return ValidationResult.Error(
                "Invalid quality. Must be one of: ${validQualities.joinToString(", ")}",
                "quality"
            )
        }

        return ValidationResult.Success
    }

    /**
     * Валидация WebRTC offer запроса
     */
    fun validateWebRtcOfferRequest(request: com.company.ipcamera.server.dto.WebRtcOfferRequest): ValidationResult {
        val offer = request.offer

        if (offer.type.isBlank()) {
            return ValidationResult.Error("Offer type is required", "offer.type")
        }

        if (offer.type.lowercase() != "offer") {
            return ValidationResult.Error("Offer type must be 'offer'", "offer.type")
        }

        if (offer.sdp.isBlank()) {
            return ValidationResult.Error("Offer SDP is required", "offer.sdp")
        }

        // SDP должен содержать базовые строки
        if (offer.sdp.length < 50) {
            return ValidationResult.Error("Offer SDP is too short", "offer.sdp")
        }

        if (offer.sdp.length > 10000) {
            return ValidationResult.Error("Offer SDP is too long (max 10000 characters)", "offer.sdp")
        }

        // Проверяем, что SDP содержит базовые ключевые слова
        val sdpLower = offer.sdp.lowercase()
        if (!sdpLower.contains("v=") || !sdpLower.contains("m=")) {
            return ValidationResult.Error("Invalid SDP format", "offer.sdp")
        }

        return ValidationResult.Success
    }

    /**
     * Валидация запроса на массовое подтверждение уведомлений
     */
    fun validateMarkNotificationsAsReadRequest(request: com.company.ipcamera.server.routing.MarkNotificationsAsReadRequest): ValidationResult {
        if (request.ids.isEmpty()) {
            return ValidationResult.Error("IDs list cannot be empty", "ids")
        }

        if (request.ids.size > 100) {
            return ValidationResult.Error("Cannot mark more than 100 notifications at once", "ids")
        }

        // Проверяем формат ID
        for (id in request.ids) {
            if (id.isBlank()) {
                return ValidationResult.Error("Notification ID cannot be blank", "ids")
            }
            if (id.length > 100) {
                return ValidationResult.Error("Notification ID must be at most 100 characters", "ids")
            }
        }

        return ValidationResult.Success
    }

    /**
     * Валидация camera ID из path параметра
     */
    fun validateCameraId(cameraId: String?): ValidationResult {
        if (cameraId.isNullOrBlank()) {
            return ValidationResult.Error("Camera ID is required", "id")
        }
        if (cameraId.length > 100) {
            return ValidationResult.Error("Camera ID must be at most 100 characters", "id")
        }
        return ValidationResult.Success
    }

    /**
     * Валидация фильтров из query параметров
     */
    fun validateFilters(
        type: String?,
        priority: String?,
        status: String?,
        validTypes: List<String>? = null,
        validPriorities: List<String>? = null,
        validStatuses: List<String>? = null
    ): ValidationResult {
        if (type != null && validTypes != null) {
            if (!validTypes.contains(type.uppercase())) {
                return ValidationResult.Error(
                    "Invalid type. Must be one of: ${validTypes.joinToString(", ")}",
                    "type"
                )
            }
        }

        if (priority != null && validPriorities != null) {
            if (!validPriorities.contains(priority.uppercase())) {
                return ValidationResult.Error(
                    "Invalid priority. Must be one of: ${validPriorities.joinToString(", ")}",
                    "priority"
                )
            }
        }

        if (status != null && validStatuses != null) {
            if (!validStatuses.contains(status.uppercase())) {
                return ValidationResult.Error(
                    "Invalid status. Must be one of: ${validStatuses.joinToString(", ")}",
                    "status"
                )
            }
        }

        return ValidationResult.Success
    }

    /**
     * Валидация date range параметров
     */
    fun validateDateRange(startDateStr: String?, endDateStr: String?): ValidationResult {
        if (startDateStr == null && endDateStr == null) {
            return ValidationResult.Success // Оба параметра опциональны
        }

        val startDate = startDateStr?.toLongOrNull()
        val endDate = endDateStr?.toLongOrNull()

        if (startDateStr != null && startDate == null) {
            return ValidationResult.Error("Invalid startDate format. Expected timestamp in milliseconds", "startDate")
        }

        if (endDateStr != null && endDate == null) {
            return ValidationResult.Error("Invalid endDate format. Expected timestamp in milliseconds", "endDate")
        }

        if (startDate != null && endDate != null) {
            if (startDate > endDate) {
                return ValidationResult.Error("startDate must be before or equal to endDate", "dateRange")
            }

            // Проверяем разумный диапазон (не более 1 года)
            val oneYearInMillis = 365L * 24 * 60 * 60 * 1000
            if (endDate - startDate > oneYearInMillis) {
                return ValidationResult.Error("Date range cannot exceed 1 year", "dateRange")
            }
        }

        return ValidationResult.Success
    }

    /**
     * Валидация параметров сортировки
     */
    fun validateSorting(
        sortBy: String,
        sortOrder: String,
        validFields: List<String>
    ): ValidationResult {
        if (sortBy.isBlank()) {
            return ValidationResult.Error("sortBy cannot be blank", "sortBy")
        }

        if (!validFields.contains(sortBy.lowercase())) {
            return ValidationResult.Error(
                "Invalid sortBy field. Must be one of: ${validFields.joinToString(", ")}",
                "sortBy"
            )
        }

        val orderLower = sortOrder.lowercase()
        if (orderLower != "asc" && orderLower != "desc") {
            return ValidationResult.Error("sortOrder must be 'asc' or 'desc'", "sortOrder")
        }

        return ValidationResult.Success
    }

    /**
     * Валидация системных настроек
     */
    fun validateSystemSettings(settings: SystemSettings): ValidationResult {
        return validateRecordingSettings(settings.recording)
            ?: validateStorageSettings(settings.storage)
            ?: validateSecuritySettings(settings.security)
            ?: validateNetworkSettings(settings.network)
            ?: validateNotificationSettings(settings.notifications)
            ?: ValidationResult.Success
    }

    private fun validateRecordingSettings(recording: RecordingSystemSettings?): ValidationResult? {
        recording ?: return null
        if (recording.maxDuration < 0) return error("recording.maxDuration", "Max duration must be non-negative")
        if (recording.maxDuration > 86400) return error("recording.maxDuration", "Max duration must be at most 86400 seconds (24 hours)")
        if (recording.retentionDays < 0) return error("recording.retentionDays", "Retention days must be non-negative")
        if (recording.retentionDays > 3650) return error("recording.retentionDays", "Retention days must be at most 3650 days (10 years)")
        return null
    }

    private fun validateStorageSettings(storage: StorageSettings?): ValidationResult? {
        storage ?: return null
        if (storage.maxStorageSize < 0) return error("storage.maxStorageSize", "Max storage size must be non-negative")
        if (storage.maxStorageSize > 1099511627776L) return error("storage.maxStorageSize", "Max storage size must be at most 1 TB")
        if (storage.storagePath.isBlank()) return error("storage.storagePath", "Storage path cannot be blank")
        if (storage.storagePath.length > 500) return error("storage.storagePath", "Storage path must be at most 500 characters")
        return null
    }

    private fun validateSecuritySettings(security: SecuritySettings?): ValidationResult? {
        security ?: return null
        if (security.sessionTimeout < 60) return error("security.sessionTimeout", "Session timeout must be at least 60 seconds")
        if (security.sessionTimeout > 86400) return error("security.sessionTimeout", "Session timeout must be at most 86400 seconds (24 hours)")
        security.passwordPolicy?.let { policy ->
            if (policy.minLength < 4) return error("security.passwordPolicy.minLength", "Password minimum length must be at least 4")
            if (policy.minLength > 128) return error("security.passwordPolicy.minLength", "Password minimum length must be at most 128")
        }
        return null
    }

    private fun validateNetworkSettings(network: NetworkSettings?): ValidationResult? {
        network ?: return null
        if (network.apiPort !in 1..65535) return error("network.apiPort", "API port must be between 1 and 65535")
        if (network.websocketPort !in 1..65535) return error("network.websocketPort", "WebSocket port must be between 1 and 65535")
        if (network.apiPort == network.websocketPort) return error("network.websocketPort", "API port and WebSocket port must be different")
        return null
    }

    private fun validateNotificationSettings(notifications: NotificationSystemSettings?): ValidationResult? {
        notifications ?: return null
        notifications.webhookUrl?.let { webhookUrl ->
            if (webhookUrl.isNotBlank()) {
                try {
                    URL(webhookUrl)
                } catch (e: Exception) {
                    return error("notifications.webhookUrl", "Invalid webhook URL format")
                }
                if (webhookUrl.length > 500) return error("notifications.webhookUrl", "Webhook URL must be at most 500 characters")
            }
        }
        return null
    }

    private fun error(path: String, message: String) = ValidationResult.Error(message, path)

    /**
     * Валидация запроса массового удаления записей
     */
    fun validateBulkDeleteRecordingsRequest(request: BulkDeleteRecordingsRequest): ValidationResult {
        if (request.ids.isEmpty()) {
            return ValidationResult.Error("At least one recording ID is required", "ids")
        }
        if (request.ids.size > 100) {
            return ValidationResult.Error("Cannot delete more than 100 recordings at once", "ids")
        }
        if (request.ids.any { it.isBlank() }) {
            return ValidationResult.Error("Recording IDs cannot be blank", "ids")
        }
        // Проверка на дубликаты
        if (request.ids.distinct().size != request.ids.size) {
            return ValidationResult.Error("Duplicate recording IDs are not allowed", "ids")
        }
        return ValidationResult.Success
    }

    /**
     * Валидация запроса массового экспорта записей
     */
    fun validateBulkExportRecordingsRequest(request: BulkExportRecordingsRequest): ValidationResult {
        if (request.ids.isEmpty()) {
            return ValidationResult.Error("At least one recording ID is required", "ids")
        }
        if (request.ids.size > 50) {
            return ValidationResult.Error("Cannot export more than 50 recordings at once", "ids")
        }
        if (request.ids.any { it.isBlank() }) {
            return ValidationResult.Error("Recording IDs cannot be blank", "ids")
        }
        // Проверка на дубликаты
        if (request.ids.distinct().size != request.ids.size) {
            return ValidationResult.Error("Duplicate recording IDs are not allowed", "ids")
        }
        // Валидация формата
        request.format?.let { format ->
            val validFormats = listOf("mp4", "mkv", "avi", "mov", "flv")
            if (!validFormats.contains(format.lowercase())) {
                return ValidationResult.Error("Invalid format. Allowed formats: ${validFormats.joinToString(", ")}", "format")
            }
        }
        // Валидация качества
        request.quality?.let { quality ->
            val validQualities = listOf("low", "medium", "high", "ultra")
            if (!validQualities.contains(quality.lowercase())) {
                return ValidationResult.Error("Invalid quality. Allowed qualities: ${validQualities.joinToString(", ")}", "quality")
            }
        }
        // Валидация временных меток
        if (request.startTime != null && request.endTime != null) {
            if (request.startTime >= request.endTime) {
                return ValidationResult.Error("Start time must be before end time", "startTime")
            }
            if (request.endTime - request.startTime > 86400000L) { // Максимум 24 часа
                return ValidationResult.Error("Export duration cannot exceed 24 hours", "endTime")
            }
        }
        return ValidationResult.Success
    }

    /**
     * Валидация запроса массового удаления камер
     */
    fun validateBulkDeleteCamerasRequest(request: BulkDeleteCamerasRequest): ValidationResult {
        if (request.ids.isEmpty()) {
            return ValidationResult.Error("At least one camera ID is required", "ids")
        }
        if (request.ids.size > 50) {
            return ValidationResult.Error("Cannot delete more than 50 cameras at once", "ids")
        }
        if (request.ids.any { it.isBlank() }) {
            return ValidationResult.Error("Camera IDs cannot be blank", "ids")
        }
        // Проверка на дубликаты
        if (request.ids.distinct().size != request.ids.size) {
            return ValidationResult.Error("Duplicate camera IDs are not allowed", "ids")
        }
        return ValidationResult.Success
    }

    /**
     * Валидация правила аналитики
     */
    fun validateAnalyticsRule(request: CreateAnalyticsRuleRequest): ValidationResult {
        return validateRuleName(request)
            ?: validateAnalyticsType(request)
            ?: validateConditions(request)
            ?: validateActions(request)
            ?: ValidationResult.Success
    }

    private fun validateRuleName(request: CreateAnalyticsRuleRequest): ValidationResult? {
        if (request.name.isBlank()) return error("name", "Rule name is required")
        if (request.name.length > 100) return error("name", "Rule name must be at most 100 characters")
        return null
    }

    private fun validateAnalyticsType(request: CreateAnalyticsRuleRequest): ValidationResult? {
        val validTypes = listOf("MOTION_DETECTION", "OBJECT_DETECTION", "FACE_DETECTION", "LICENSE_PLATE_RECOGNITION", "COMPLEX_ANALYSIS")
        if (validTypes.contains(request.analyticsType)) return null
        return error("analyticsType", "Invalid analytics type. Must be one of: ${validTypes.joinToString(", ")}")
    }

    private fun validateConditions(request: CreateAnalyticsRuleRequest): ValidationResult? {
        val c = request.conditions
        if (c.minConfidence !in 0.0f..1.0f) return error("conditions.minConfidence", "Min confidence must be between 0.0 and 1.0")
        if (c.minObjectCount < 0) return error("conditions.minObjectCount", "Min object count cannot be negative")
        if (c.maxObjectCount != null && c.maxObjectCount < c.minObjectCount) return error("conditions.maxObjectCount", "Max object count cannot be less than min object count")
        return null
    }

    private fun validateActions(request: CreateAnalyticsRuleRequest): ValidationResult? {
        val a = request.actions
        
        if (a.sendWebhook && a.webhookUrl.isNullOrBlank()) return error("actions.webhookUrl", "Webhook URL is required when sendWebhook is true")
        
        a.webhookUrl?.let { url ->
            if (url.isNotBlank()) {
                try { URL(url) } catch (e: Exception) { return error("actions.webhookUrl", "Invalid webhook URL format") }
            }
        }

        val validEventTypes = listOf("MOTION_DETECTION", "OBJECT_DETECTION", "FACE_DETECTION", "LICENSE_PLATE_RECOGNITION", "TAMPERING", "OFFLINE", "ONLINE")
        if (!validEventTypes.contains(a.eventType)) return error("actions.eventType", "Invalid event type. Must be one of: ${validEventTypes.joinToString(", ")}")

        val validSeverities = listOf("INFO", "WARNING", "ERROR", "CRITICAL")
        if (!validSeverities.contains(a.eventSeverity)) return error("actions.eventSeverity", "Invalid event severity. Must be one of: ${validSeverities.joinToString(", ")}")

        return null
    }

    /**
     * Валидация конфигурации аналитики
     */
    fun validateAnalyticsConfig(config: AnalyticsConfigDto): ValidationResult {
        return validateObjectDetection(config)
            ?: validateZones(config)
            ?: validateMotionDetection(config)
            ?: validateANPR(config)
            ?: validateFaceRecognition(config)
            ?: ValidationResult.Success
    }

    private fun validateObjectDetection(config: AnalyticsConfigDto): ValidationResult? {
        if (config.objectDetectionConfidenceThreshold < 0.0f || config.objectDetectionConfidenceThreshold > 1.0f) {
            return ValidationResult.Error("Object detection confidence threshold must be between 0.0 and 1.0", "objectDetectionConfidenceThreshold")
        }
        if (config.objectDetectionMaxObjects !in 1..100) {
            return ValidationResult.Error("Object detection max objects must be between 1 and 100", "objectDetectionMaxObjects")
        }
        config.objectTypes.forEachIndexed { index, type ->
            if (type.isBlank()) return ValidationResult.Error("Object type at index $index cannot be blank", "objectTypes")
            if (type.length > 50) return ValidationResult.Error("Object type at index $index must be at most 50 characters", "objectTypes")
        }
        if (config.objectDetectionInputSize !in 64..1280) {
            return ValidationResult.Error("Object detection input size must be between 64 and 1280", "objectDetectionInputSize")
        }
        config.objectDetectionModelPath?.let { path ->
            if (path.isBlank()) return ValidationResult.Error("Object detection model path cannot be blank", "objectDetectionModelPath")
            if (path.length > 500) return ValidationResult.Error("Object detection model path must be at most 500 characters", "objectDetectionModelPath")
        }
        return null
    }

    private fun validateZones(config: AnalyticsConfigDto): ValidationResult? {
        config.zones.forEachIndexed { index, zone ->
            if (zone.name.isBlank()) return ValidationResult.Error("Zone at index $index name cannot be blank", "zones")
            if (zone.name.length > 100) return ValidationResult.Error("Zone at index $index name must be at most 100 characters", "zones")
            if (zone.polygon.isEmpty()) return ValidationResult.Error("Zone at index $index polygon must contain at least one point", "zones")
            if (zone.polygon.size < 3) return ValidationResult.Error("Zone at index $index polygon must contain at least 3 points", "zones")
            zone.polygon.forEachIndexed { pointIndex, point ->
                if (point.size != 2) return ValidationResult.Error("Zone at index $index point $pointIndex must contain exactly 2 coordinates", "zones")
                if (point.any { it < 0 }) return ValidationResult.Error("Zone at index $index coordinates must be non-negative", "zones")
            }
            if (zone.sensitivity !in 0..100) return ValidationResult.Error("Zone at index $index sensitivity must be between 0 and 100", "zones")
        }
        return null
    }

    private fun validateMotionDetection(config: AnalyticsConfigDto): ValidationResult? {
        if (config.motionThreshold !in 0.0f..1.0f) {
            return ValidationResult.Error("Motion threshold must be between 0.0 and 1.0", "motionThreshold")
        }
        if (config.motionMinArea != null && config.motionMinArea < 0) {
            return ValidationResult.Error("Motion min area must be non-negative", "motionMinArea")
        }
        if (config.motionEventCooldownMs < 0L) {
            return ValidationResult.Error("Motion event cooldown must be non-negative", "motionEventCooldownMs")
        }
        return null
    }

    private fun validateANPR(config: AnalyticsConfigDto): ValidationResult? {
        if (config.anprConfidenceThreshold !in 0.0f..1.0f) {
            return ValidationResult.Error("ANPR confidence threshold must be between 0.0 and 1.0", "anprConfidenceThreshold")
        }
        return null
    }

    private fun validateFaceRecognition(config: AnalyticsConfigDto): ValidationResult? {
        if (config.faceRecognitionConfidenceThreshold !in 0.0f..1.0f) {
            return ValidationResult.Error("Face recognition confidence threshold must be between 0.0 and 1.0", "faceRecognitionConfidenceThreshold")
        }
        return null
    }
}


