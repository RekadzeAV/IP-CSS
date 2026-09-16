package com.company.ipcamera.server.service

import com.company.ipcamera.shared.domain.model.*
import com.company.ipcamera.shared.domain.repository.AnalyticsRuleRepository
import com.company.ipcamera.shared.domain.repository.CameraRepository
import com.company.ipcamera.shared.domain.repository.EventRepository
import com.company.ipcamera.shared.domain.service.NotificationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import mu.KotlinLogging
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}
private const val KEY_NOTIFY_IN_APP = "__notifyInApp"
private const val KEY_NOTIFY_EMAIL = "__notifyEmail"
private const val KEY_NOTIFY_TELEGRAM = "__notifyTelegram"
private const val KEY_NOTIFY_CHANNELS = "__notifyChannels"
private const val KEY_EMAIL_SUBJECT_TEMPLATE = "__emailSubjectTemplate"
private const val KEY_EMAIL_BODY_TEMPLATE = "__emailBodyTemplate"
private const val KEY_NOTIFY_SMS = "__notifySms"
private const val KEY_NOTIFY_SMS_TO = "__notifySmsTo"
private const val KEY_NOTIFY_PUSH = "__notifyPush"
private const val KEY_NOTIFY_PUSH_TOKENS = "__notifyPushTokens"

/**
 * Сервис для управления правилами аналитики
 *
 * Управляет правилами аналитики и их применением к событиям
 */
class AnalyticsRuleService(
    private val ruleRepository: AnalyticsRuleRepository,
    private val cameraRepository: CameraRepository,
    private val eventRepository: EventRepository,
    private val notificationService: NotificationService? = null,
    private val eventService: EventService? = null,
    private val analyticsWebhookService: AnalyticsWebhookService? = null
) {
    private val lastRuleTriggerAtMs = ConcurrentHashMap<String, Long>()

    data class RuleNotificationPolicy(
        val ruleId: String,
        val sendNotification: Boolean,
        val notifyInApp: Boolean,
        val notifyEmail: Boolean,
        val notifyTelegram: Boolean,
        val notificationType: NotificationType?
    )

    data class TestNotificationResult(
        val ruleId: String,
        val notificationId: String,
        val channels: List<String>
    )

    suspend fun getNotificationPolicy(ruleId: String): Result<RuleNotificationPolicy> = withContext(Dispatchers.IO) {
        try {
            val rule = ruleRepository.getRuleById(ruleId)
                ?: return@withContext Result.failure(IllegalArgumentException("Rule not found: $ruleId"))
            Result.success(
                RuleNotificationPolicy(
                    ruleId = rule.id,
                    sendNotification = rule.actions.sendNotification,
                    notifyInApp = rule.actions.notifyInApp,
                    notifyEmail = rule.actions.notifyEmail,
                    notifyTelegram = rule.actions.notifyTelegram,
                    notificationType = rule.actions.notificationType
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Error getting notification policy for rule: $ruleId" }
            Result.failure(e)
        }
    }

    suspend fun updateNotificationPolicy(
        ruleId: String,
        sendNotification: Boolean? = null,
        notifyInApp: Boolean? = null,
        notifyEmail: Boolean? = null,
        notifyTelegram: Boolean? = null,
        notificationType: NotificationType? = null
    ): Result<RuleNotificationPolicy> = withContext(Dispatchers.IO) {
        try {
            val rule = ruleRepository.getRuleById(ruleId)
                ?: return@withContext Result.failure(IllegalArgumentException("Rule not found: $ruleId"))

            val updatedActions = rule.actions.copy(
                sendNotification = sendNotification ?: rule.actions.sendNotification,
                notifyInApp = notifyInApp ?: rule.actions.notifyInApp,
                notifyEmail = notifyEmail ?: rule.actions.notifyEmail,
                notifyTelegram = notifyTelegram ?: rule.actions.notifyTelegram,
                notificationType = notificationType ?: rule.actions.notificationType
            )

            if ((updatedActions.notifyInApp || updatedActions.notifyEmail || updatedActions.notifyTelegram) &&
                !updatedActions.sendNotification
            ) {
                return@withContext Result.failure(
                    IllegalArgumentException("sendNotification must be true when any notification channel is enabled")
                )
            }

            val updatedRule = rule.copy(actions = updatedActions, updatedAt = System.currentTimeMillis())
            ruleRepository.updateRule(updatedRule).fold(
                onSuccess = {
                    Result.success(
                        RuleNotificationPolicy(
                            ruleId = updatedRule.id,
                            sendNotification = updatedActions.sendNotification,
                            notifyInApp = updatedActions.notifyInApp,
                            notifyEmail = updatedActions.notifyEmail,
                            notifyTelegram = updatedActions.notifyTelegram,
                            notificationType = updatedActions.notificationType
                        )
                    )
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error updating notification policy for rule: $ruleId" }
            Result.failure(e)
        }
    }
    /**
     * Получить все правила
     */
    suspend fun getAllRules(): Result<List<AnalyticsRule>> = withContext(Dispatchers.IO) {
        try {
            val rules = ruleRepository.getAllRules()
            Result.success(rules)
        } catch (e: Exception) {
            logger.error(e) { "Error getting all rules" }
            Result.failure(e)
        }
    }

    /**
     * Получить правило по ID
     */
    suspend fun getRuleById(id: String): Result<AnalyticsRule> = withContext(Dispatchers.IO) {
        try {
            val rule = ruleRepository.getRuleById(id)
            if (rule != null) {
                Result.success(rule)
            } else {
                Result.failure(IllegalArgumentException("Rule not found: $id"))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error getting rule: $id" }
            Result.failure(e)
        }
    }

    /**
     * Получить правила для камеры
     */
    suspend fun getRulesByCameraId(cameraId: String): Result<List<AnalyticsRule>> =
        withContext(Dispatchers.IO) {
            try {
                val rules = ruleRepository.getRulesByCameraId(cameraId)
                Result.success(rules)
            } catch (e: Exception) {
                logger.error(e) { "Error getting rules for camera: $cameraId" }
                Result.failure(e)
            }
        }

    /**
     * Создать правило
     */
    suspend fun createRule(rule: AnalyticsRule): Result<AnalyticsRule> = withContext(Dispatchers.IO) {
        try {
            // Валидация
            validateRule(rule)

            // Проверка существования камеры, если указана
            rule.cameraId?.let { cameraId ->
                val camera = cameraRepository.getCameraById(cameraId)
                if (camera == null) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Camera not found: $cameraId")
                    )
                }
            }

            val result = ruleRepository.createRule(rule)
            result.fold(
                onSuccess = { createdRule ->
                    logger.info { "Created analytics rule: ${createdRule.id} (${createdRule.name})" }
                    Result.success(createdRule)
                },
                onFailure = { error ->
                    logger.error(error) { "Failed to create rule: ${rule.id}" }
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error creating rule: ${rule.id}" }
            Result.failure(e)
        }
    }

    /**
     * Обновить правило
     */
    suspend fun updateRule(rule: AnalyticsRule): Result<AnalyticsRule> = withContext(Dispatchers.IO) {
        try {
            // Валидация
            validateRule(rule)

            // Проверка существования правила
            val existingRule = ruleRepository.getRuleById(rule.id)
            if (existingRule == null) {
                return@withContext Result.failure(
                    IllegalArgumentException("Rule not found: ${rule.id}")
                )
            }

            // Проверка существования камеры, если указана
            rule.cameraId?.let { cameraId ->
                val camera = cameraRepository.getCameraById(cameraId)
                if (camera == null) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Camera not found: $cameraId")
                    )
                }
            }

            val result = ruleRepository.updateRule(rule)
            result.fold(
                onSuccess = { updatedRule ->
                    logger.info { "Updated analytics rule: ${updatedRule.id} (${updatedRule.name})" }
                    Result.success(updatedRule)
                },
                onFailure = { error ->
                    logger.error(error) { "Failed to update rule: ${rule.id}" }
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error updating rule: ${rule.id}" }
            Result.failure(e)
        }
    }

    /**
     * Удалить правило
     */
    suspend fun deleteRule(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val result = ruleRepository.deleteRule(id)
            result.fold(
                onSuccess = {
                    logger.info { "Deleted analytics rule: $id" }
                    Result.success(Unit)
                },
                onFailure = { error ->
                    logger.error(error) { "Failed to delete rule: $id" }
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error deleting rule: $id" }
            Result.failure(e)
        }
    }

    /**
     * Включить/выключить правило
     */
    suspend fun setRuleEnabled(id: String, enabled: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val result = ruleRepository.setRuleEnabled(id, enabled)
                result.fold(
                    onSuccess = {
                        logger.info { "Set rule $id enabled: $enabled" }
                        Result.success(Unit)
                    },
                    onFailure = { error ->
                        logger.error(error) { "Failed to set rule enabled: $id" }
                        Result.failure(error)
                    }
                )
            } catch (e: Exception) {
                logger.error(e) { "Error setting rule enabled: $id" }
                Result.failure(e)
            }
        }

    suspend fun sendTestNotification(
        ruleId: String,
        userId: String? = null,
        cameraIdOverride: String? = null,
        notifyInApp: Boolean? = null,
        notifyEmail: Boolean? = null,
        notifyTelegram: Boolean? = null
    ): Result<TestNotificationResult> = withContext(Dispatchers.IO) {
        try {
            val rule = ruleRepository.getRuleById(ruleId)
                ?: return@withContext Result.failure(IllegalArgumentException("Rule not found: $ruleId"))
            val ns = notificationService
                ?: return@withContext Result.failure(IllegalStateException("Notification service is not configured"))

            val channels = resolveTestChannels(rule, notifyInApp, notifyEmail, notifyTelegram)
            val now = System.currentTimeMillis()
            val extras = buildTestExtras(rule, now)
            val priority = mapSeverityToPriority(rule.actions.eventSeverity)
            val cameraId = cameraIdOverride ?: rule.cameraId

            val result = ns.sendNotification(
                title = "Test: ${rule.name}",
                message = "Test notification for analytics rule '${rule.name}'",
                type = rule.actions.notificationType ?: NotificationType.INFO,
                priority = priority,
                userId = userId,
                cameraId = cameraId,
                eventId = null,
                recordingId = null,
                extras = extras
            )
            
            result.fold(
                onSuccess = { notification ->
                    Result.success(TestNotificationResult(rule.id, notification.id, channels))
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error sending test notification for rule: $ruleId" }
            Result.failure(e)
        }
    }

    private suspend fun resolveTestChannels(
        rule: AnalyticsRule,
        notifyInApp: Boolean?,
        notifyEmail: Boolean?,
        notifyTelegram: Boolean?
    ): List<String> {
        val inApp = notifyInApp ?: rule.actions.notifyInApp
        val email = notifyEmail ?: rule.actions.notifyEmail
        val telegram = notifyTelegram ?: rule.actions.notifyTelegram
        
        val channels = buildList {
            if (inApp) add("in-app")
            if (email) add("email")
            if (telegram) add("telegram")
        }
        
        if (channels.isEmpty()) {
            throw IllegalArgumentException("At least one notification channel must be enabled for test delivery")
        }
        return channels
    }

    private fun buildTestExtras(rule: AnalyticsRule, now: Long): Map<String, String> {
        return mapOf(
            "ruleId" to rule.id,
            "ruleName" to rule.name,
            "analyticsType" to rule.analyticsType.name,
            "isTestNotification" to "true",
            "triggeredAt" to now.toString(),
            KEY_NOTIFY_IN_APP to resolveTestBoolean(rule.actions.notifyInApp),
            KEY_NOTIFY_EMAIL to resolveTestBoolean(rule.actions.notifyEmail),
            KEY_NOTIFY_TELEGRAM to resolveTestBoolean(rule.actions.notifyTelegram),
            KEY_NOTIFY_CHANNELS to buildChannelCsv(rule.actions.notifyInApp, rule.actions.notifyEmail, rule.actions.notifyTelegram)
        )
    }

    private fun resolveTestBoolean(value: Boolean?): String = value.toString()

    private fun mapSeverityToPriority(severity: EventSeverity): NotificationPriority = when (severity) {
        EventSeverity.CRITICAL, EventSeverity.ERROR, EventSeverity.WARNING -> NotificationPriority.HIGH
        EventSeverity.INFO -> NotificationPriority.NORMAL
    }

    /**
     * Применить правила к результату аналитики
     *
     * Проверяет все активные правила и выполняет соответствующие действия
     */
    suspend fun applyRules(
        cameraId: String,
        analyticsType: AnalyticsRuleType,
        result: Any, // MotionDetectionResult, ObjectDetectionResult, etc.
        confidence: Float
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val activeRules = ruleRepository.getActiveRulesByCameraId(cameraId)
            val cameraName = cameraRepository.getCameraById(cameraId)?.name
            val triggeredRules = mutableListOf<String>()

            for (rule in activeRules) {
                // Проверяем тип аналитики
                if (rule.analyticsType != analyticsType && rule.analyticsType != AnalyticsRuleType.COMPLEX_ANALYSIS) {
                    continue
                }

                // Проверяем условия
                if (checkConditions(rule.id, rule.conditions, result, confidence, cameraName)) {
                    // Выполняем действия
                    executeActions(rule, cameraId, result)
                    lastRuleTriggerAtMs[rule.id] = System.currentTimeMillis()
                    triggeredRules.add(rule.id)
                    logger.debug { "Rule ${rule.id} triggered for camera: $cameraId" }
                }
            }

            Result.success(triggeredRules)
        } catch (e: Exception) {
            logger.error(e) { "Error applying rules for camera: $cameraId" }
            Result.failure(e)
        }
    }

    /**
     * Проверить условия правила
     */
    private fun checkConditions(
        ruleId: String,
        conditions: AnalyticsRuleConditions,
        result: Any,
        confidence: Float,
        cameraName: String?
    ): Boolean {
        // Проверка минимальной уверенности
        if (confidence < conditions.minConfidence) {
            return false
        }

        // Проверка количества объектов (для ObjectDetectionResult)
        if (result is com.company.ipcamera.shared.domain.service.ObjectDetectionResult) {
            val objectCount = result.objects.size
            if (objectCount < conditions.minObjectCount) {
                return false
            }
            val maxObjectCount = conditions.maxObjectCount
            if (maxObjectCount != null && objectCount > maxObjectCount) {
                return false
            }

            // Проверка типов объектов
            if (conditions.objectTypes.isNotEmpty()) {
                val detectedTypes = result.objects.map { it.type }.toSet()
                if (!detectedTypes.any { it in conditions.objectTypes }) {
                    return false
                }
            }
        }

        // Проверка окна времени и дней недели
        if (!matchesTimeWindow(conditions.timeWindow) || !matchesDaysOfWeek(conditions.daysOfWeek)) {
            return false
        }

        // Проверка зон (в текущей реализации поддерживается для MotionDetectionResult)
        if (conditions.zones.isNotEmpty()) {
            val triggeredZones = extractTriggeredZones(result)
            if (triggeredZones.isEmpty()) {
                return false
            }
            val expected = conditions.zones.map { it.trim().lowercase() }.toSet()
            val actual = triggeredZones.map { it.trim().lowercase() }.toSet()
            if (actual.intersect(expected).isEmpty()) {
                return false
            }
        }

        if (!matchesAdditionalConditions(ruleId, conditions.additionalConditions, cameraName)) {
            return false
        }

        return true
    }

    private fun matchesAdditionalConditions(
        ruleId: String,
        additionalConditions: Map<String, String>,
        cameraName: String?
    ): Boolean {
        if (additionalConditions.isEmpty()) return true

        val requiredCameraNameContains = additionalConditions["requiredCameraNameContains"]
            ?.takeIf { it.isNotBlank() }
        if (requiredCameraNameContains != null) {
            val actual = cameraName?.lowercase() ?: return false
            if (!actual.contains(requiredCameraNameContains.lowercase())) {
                return false
            }
        }

        val cooldownMs = additionalConditions["cooldownMs"]?.toLongOrNull()
        if (cooldownMs != null && cooldownMs > 0) {
            val last = lastRuleTriggerAtMs[ruleId]
            if (last != null && (System.currentTimeMillis() - last) < cooldownMs) {
                return false
            }
        }

        return true
    }

    private fun matchesDaysOfWeek(daysOfWeek: List<Int>): Boolean {
        if (daysOfWeek.isEmpty()) return true
        val today = ZonedDateTime.now(ZoneId.systemDefault()).dayOfWeek.toRuleDay()
        return today in daysOfWeek
    }

    private fun DayOfWeek.toRuleDay(): Int = when (this) {
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY -> 2
        DayOfWeek.WEDNESDAY -> 3
        DayOfWeek.THURSDAY -> 4
        DayOfWeek.FRIDAY -> 5
        DayOfWeek.SATURDAY -> 6
        DayOfWeek.SUNDAY -> 7
    }

    private fun matchesTimeWindow(timeWindow: String?): Boolean {
        if (timeWindow.isNullOrBlank()) return true
        val range = parseTimeWindow(timeWindow) ?: return false
        val now = LocalTime.now(ZoneId.systemDefault())
        val (start, end) = range
        return if (start <= end) {
            now >= start && now <= end
        } else {
            // Ночной диапазон через полночь, например 22:00-06:00
            now >= start || now <= end
        }
    }

    private fun parseTimeWindow(timeWindow: String): Pair<LocalTime, LocalTime>? {
        val parts = timeWindow.split("-")
        if (parts.size != 2) return null
        return runCatching {
            val start = LocalTime.parse(parts[0].trim())
            val end = LocalTime.parse(parts[1].trim())
            start to end
        }.getOrNull()
    }

    private fun extractTriggeredZones(result: Any): List<String> {
        return when (result) {
            is com.company.ipcamera.shared.domain.service.MotionDetectionResult -> {
                result.zones.map { it.zone.name }.filter { it.isNotBlank() }
            }
            else -> emptyList()
        }
    }

    /**
     * Выполнить действия правила
     */
    private suspend fun executeActions(
        rule: AnalyticsRule,
        cameraId: String,
        result: Any
    ) {
        val actions = rule.actions
        val context = buildActionContext(rule, cameraId, result)

        // Создание события
        if (actions.createEvent && eventService != null) {
            try {
                val camera = cameraRepository.getCameraById(cameraId)
                eventService.createEvent(
                    cameraId = cameraId,
                    cameraName = camera?.name,
                    type = actions.eventType,
                    severity = actions.eventSeverity,
                    description = "Analytics rule triggered: ${rule.name}",
                    metadata = context
                )
            } catch (e: Exception) {
                logger.error(e) { "Error creating event for rule: ${rule.id}" }
            }
        }

        // Отправка уведомления
        if (actions.sendNotification && notificationService != null && actions.notificationType != null) {
            try {
                val notificationType = actions.notificationType
                val notificationExtras = context + mapOf(
                    KEY_NOTIFY_IN_APP to actions.notifyInApp.toString(),
                    KEY_NOTIFY_EMAIL to actions.notifyEmail.toString(),
                    KEY_NOTIFY_TELEGRAM to actions.notifyTelegram.toString(),
                    KEY_NOTIFY_CHANNELS to buildChannelCsv(
                        inApp = actions.notifyInApp,
                        email = actions.notifyEmail,
                        telegram = actions.notifyTelegram
                    )
                ) + buildEmailTemplateExtras(actions.additionalActions) +
                    buildSmsExtras(actions.additionalActions) +
                    buildPushExtras(actions.additionalActions)
                notificationService.sendNotification(
                    type = notificationType ?: com.company.ipcamera.shared.domain.model.NotificationType.INFO,
                    title = "Analytics Rule Triggered",
                    message = "Rule '${rule.name}' triggered for camera: $cameraId",
                    userId = null, // Отправить всем пользователям
                    extras = notificationExtras
                )
            } catch (e: Exception) {
                logger.error(e) { "Error sending notification for rule: ${rule.id}" }
            }
        }

        val webhookUrl = actions.webhookUrl
        if (actions.sendWebhook && !webhookUrl.isNullOrBlank()) {
            try {
                if (analyticsWebhookService == null) {
                    logger.warn { "Webhook service is not configured, skipping webhook for rule: ${rule.id}" }
                } else {
                    val webhookPayload = buildJsonObject {
                        put(
                            "rule",
                            buildJsonObject {
                                put("id", rule.id)
                                put("name", rule.name)
                                put("analyticsType", rule.analyticsType.name)
                                put("priority", rule.priority)
                            }
                        )
                        put("cameraId", cameraId)
                        put(
                            "actions",
                            buildJsonObject {
                                put("sendWebhook", actions.sendWebhook)
                                put("sendNotification", actions.sendNotification)
                                put("createEvent", actions.createEvent)
                            }
                        )
                        put(
                            "resultContext",
                            buildJsonObject {
                                context.forEach { (key, value) ->
                                    put(key, value)
                                }
                            }
                        )
                    }
                    analyticsWebhookService.send(
                        url = webhookUrl,
                        payload = webhookPayload,
                        ruleId = rule.id,
                        cameraId = cameraId
                    ).onFailure { e ->
                        logger.error(e) { "Webhook delivery failed for rule: ${rule.id}" }
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "Error sending webhook for rule: ${rule.id}" }
            }
        }
    }

    private fun buildActionContext(
        rule: AnalyticsRule,
        cameraId: String,
        result: Any
    ): Map<String, String> {
        val now = System.currentTimeMillis()
        val base = mutableMapOf(
            "ruleId" to rule.id,
            "ruleName" to rule.name,
            "analyticsType" to rule.analyticsType.name,
            "cameraId" to cameraId,
            "triggeredAt" to now.toString()
        )
        when (result) {
            is com.company.ipcamera.shared.domain.service.MotionDetectionResult -> {
                base["confidence"] = result.confidence.toString()
                base["zonesCount"] = result.zones.size.toString()
                base["sourceTimestamp"] = result.timestamp.toString()
            }
            is com.company.ipcamera.shared.domain.service.ObjectDetectionResult -> {
                base["objectsCount"] = result.objects.size.toString()
                base["maxConfidence"] = (result.objects.maxOfOrNull { it.confidence } ?: 0f).toString()
                base["objectTypes"] = result.objects.map { it.type }.distinct().joinToString(",")
                base["sourceTimestamp"] = result.timestamp.toString()
            }
            is com.company.ipcamera.shared.domain.service.FaceDetectionResult -> {
                base["facesCount"] = result.faces.size.toString()
                base["maxConfidence"] = (result.faces.maxOfOrNull { it.confidence } ?: 0f).toString()
                base["sourceTimestamp"] = result.timestamp.toString()
            }
            is com.company.ipcamera.shared.domain.service.LicensePlateRecognitionResult -> {
                base["platesCount"] = result.plates.size.toString()
                base["maxConfidence"] = (result.plates.maxOfOrNull { it.confidence } ?: 0f).toString()
                base["plates"] = result.plates.map { it.plateNumber }.joinToString(",")
                base["sourceTimestamp"] = result.timestamp.toString()
            }
        }
        return base
    }

    private fun buildChannelCsv(inApp: Boolean, email: Boolean, telegram: Boolean): String =
        buildList {
            if (inApp) add("in-app")
            if (email) add("email")
            if (telegram) add("telegram")
        }.joinToString(",")

    private fun buildEmailTemplateExtras(additionalActions: Map<String, String>): Map<String, String> {
        val subject = additionalActions["emailSubjectTemplate"]?.takeIf { it.isNotBlank() }
        val body = additionalActions["emailBodyTemplate"]?.takeIf { it.isNotBlank() }
        if (subject == null && body == null) return emptyMap()
        return buildMap {
            subject?.let { put(KEY_EMAIL_SUBJECT_TEMPLATE, it) }
            body?.let { put(KEY_EMAIL_BODY_TEMPLATE, it) }
        }
    }

    private fun buildSmsExtras(additionalActions: Map<String, String>): Map<String, String> {
        val notifySms = additionalActions["notifySms"]?.toBooleanStrictOrNull()
        val smsTo = additionalActions["smsTo"]?.takeIf { it.isNotBlank() }
        if (notifySms == null && smsTo == null) return emptyMap()
        return buildMap {
            notifySms?.let { put(KEY_NOTIFY_SMS, it.toString()) }
            smsTo?.let { put(KEY_NOTIFY_SMS_TO, it) }
        }
    }

    private fun buildPushExtras(additionalActions: Map<String, String>): Map<String, String> {
        val notifyPush = additionalActions["notifyPush"]?.toBooleanStrictOrNull()
        val pushTokens = additionalActions["pushTokens"]?.takeIf { it.isNotBlank() }
        if (notifyPush == null && pushTokens == null) return emptyMap()
        return buildMap {
            notifyPush?.let { put(KEY_NOTIFY_PUSH, it.toString()) }
            pushTokens?.let { put(KEY_NOTIFY_PUSH_TOKENS, it) }
        }
    }

    /**
     * Валидация правила
     */
    private fun validateRule(rule: AnalyticsRule) {
        if (rule.name.isBlank()) {
            throw IllegalArgumentException("Rule name cannot be blank")
        }

        if (rule.conditions.minConfidence < 0.0f || rule.conditions.minConfidence > 1.0f) {
            throw IllegalArgumentException("Min confidence must be between 0.0 and 1.0")
        }

        if (rule.conditions.minObjectCount < 0) {
            throw IllegalArgumentException("Min object count cannot be negative")
        }

        val maxObjectCount = rule.conditions.maxObjectCount
        if (maxObjectCount != null && maxObjectCount < rule.conditions.minObjectCount) {
            throw IllegalArgumentException("Max object count cannot be less than min object count")
        }

        if (rule.actions.sendWebhook && rule.actions.webhookUrl.isNullOrBlank()) {
            throw IllegalArgumentException("Webhook URL is required when sendWebhook is true")
        }
    }
}
