package com.company.ipcamera.server.service

import com.company.ipcamera.shared.domain.model.AnalyticsRule
import com.company.ipcamera.shared.domain.model.AnalyticsRuleActions
import com.company.ipcamera.shared.domain.model.AnalyticsRuleConditions
import com.company.ipcamera.shared.domain.model.AnalyticsRuleType
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.model.DetectionZone
import com.company.ipcamera.shared.domain.model.Notification
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.domain.model.NotificationType
import com.company.ipcamera.shared.domain.service.MotionZone
import com.company.ipcamera.shared.domain.repository.AnalyticsRuleRepository
import com.company.ipcamera.shared.domain.repository.CameraRepository
import com.company.ipcamera.shared.domain.repository.EventRepository
import com.company.ipcamera.shared.domain.service.NotificationService
import com.company.ipcamera.shared.domain.service.MotionDetectionResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.format.DateTimeFormatter
import java.time.LocalTime

class AnalyticsRuleServiceTest {
    private lateinit var ruleRepository: AnalyticsRuleRepository
    private lateinit var cameraRepository: CameraRepository
    private lateinit var eventRepository: EventRepository
    private lateinit var webhookService: AnalyticsWebhookService
    private lateinit var service: AnalyticsRuleService

    @BeforeEach
    fun setUp() {
        ruleRepository = mockk(relaxed = true)
        cameraRepository = mockk(relaxed = true)
        eventRepository = mockk(relaxed = true)
        webhookService = mockk(relaxed = true)
        service = AnalyticsRuleService(
            ruleRepository = ruleRepository,
            cameraRepository = cameraRepository,
            eventRepository = eventRepository,
            notificationService = null,
            eventService = null,
            analyticsWebhookService = webhookService
        )
    }

    @Test
    fun `applyRules sends webhook when rule matches`() = runBlocking {
        val rule = AnalyticsRule(
            id = "rule-1",
            name = "Motion webhook",
            analyticsType = AnalyticsRuleType.MOTION_DETECTION,
            conditions = AnalyticsRuleConditions(minConfidence = 0.6f),
            actions = AnalyticsRuleActions(
                createEvent = false,
                sendNotification = false,
                sendWebhook = true,
                webhookUrl = "https://example.test/webhook"
            ),
            enabled = true
        )

        coEvery { ruleRepository.getActiveRulesByCameraId("cam-1") } returns listOf(rule)
        coEvery { webhookService.send(any(), any(), any(), any()) } returns Result.success(Unit)

        val result = service.applyRules(
            cameraId = "cam-1",
            analyticsType = AnalyticsRuleType.MOTION_DETECTION,
            result = MotionDetectionResult(detected = true, confidence = 0.9f),
            confidence = 0.9f
        )

        assertTrue(result.isSuccess)
        assertEquals(listOf("rule-1"), result.getOrNull())
        coVerify(exactly = 1) {
            webhookService.send("https://example.test/webhook", any(), "rule-1", "cam-1")
        }
    }

    @Test
    fun `applyRules does not send webhook when confidence is below threshold`() = runBlocking {
        val rule = AnalyticsRule(
            id = "rule-2",
            name = "High confidence motion webhook",
            analyticsType = AnalyticsRuleType.MOTION_DETECTION,
            conditions = AnalyticsRuleConditions(minConfidence = 0.95f),
            actions = AnalyticsRuleActions(
                createEvent = false,
                sendNotification = false,
                sendWebhook = true,
                webhookUrl = "https://example.test/webhook"
            ),
            enabled = true
        )

        coEvery { ruleRepository.getActiveRulesByCameraId("cam-1") } returns listOf(rule)

        val result = service.applyRules(
            cameraId = "cam-1",
            analyticsType = AnalyticsRuleType.MOTION_DETECTION,
            result = MotionDetectionResult(detected = true, confidence = 0.4f),
            confidence = 0.4f
        )

        assertTrue(result.isSuccess)
        assertEquals(emptyList<String>(), result.getOrNull())
        coVerify(exactly = 0) {
            webhookService.send(any(), any(), any(), any())
        }
    }

    @Test
    fun `applyRules does not trigger when current day is not in daysOfWeek`() = runBlocking {
        val today = java.time.ZonedDateTime.now(java.time.ZoneId.systemDefault()).dayOfWeek.value
        val disallowedDay = if (today == 7) 1 else today + 1
        val rule = AnalyticsRule(
            id = "rule-day",
            name = "Day-filtered motion webhook",
            analyticsType = AnalyticsRuleType.MOTION_DETECTION,
            conditions = AnalyticsRuleConditions(
                minConfidence = 0.3f,
                daysOfWeek = listOf(disallowedDay)
            ),
            actions = AnalyticsRuleActions(
                createEvent = false,
                sendNotification = false,
                sendWebhook = true,
                webhookUrl = "https://example.test/webhook"
            ),
            enabled = true
        )
        coEvery { ruleRepository.getActiveRulesByCameraId("cam-1") } returns listOf(rule)

        val result = service.applyRules(
            cameraId = "cam-1",
            analyticsType = AnalyticsRuleType.MOTION_DETECTION,
            result = MotionDetectionResult(detected = true, confidence = 0.9f),
            confidence = 0.9f
        )

        assertTrue(result.isSuccess)
        assertEquals(emptyList<String>(), result.getOrNull())
        coVerify(exactly = 0) { webhookService.send(any(), any(), any(), any()) }
    }

    @Test
    fun `applyRules does not trigger when zone condition does not match`() = runBlocking {
        val rule = AnalyticsRule(
            id = "rule-zone-no-match",
            name = "Zone-filtered motion webhook",
            analyticsType = AnalyticsRuleType.MOTION_DETECTION,
            conditions = AnalyticsRuleConditions(
                minConfidence = 0.3f,
                zones = listOf("entrance")
            ),
            actions = AnalyticsRuleActions(
                createEvent = false,
                sendNotification = false,
                sendWebhook = true,
                webhookUrl = "https://example.test/webhook"
            ),
            enabled = true
        )
        coEvery { ruleRepository.getActiveRulesByCameraId("cam-1") } returns listOf(rule)

        val motion = MotionDetectionResult(
            detected = true,
            confidence = 0.8f,
            zones = listOf(
                MotionZone(
                    zone = DetectionZone(name = "parking", polygon = listOf(listOf(0, 0), listOf(10, 10))),
                    intensity = 0.9f
                )
            )
        )

        val result = service.applyRules(
            cameraId = "cam-1",
            analyticsType = AnalyticsRuleType.MOTION_DETECTION,
            result = motion,
            confidence = 0.8f
        )

        assertTrue(result.isSuccess)
        assertEquals(emptyList<String>(), result.getOrNull())
        coVerify(exactly = 0) { webhookService.send(any(), any(), any(), any()) }
    }

    @Test
    fun `applyRules triggers when zone and time window match`() = runBlocking {
        val now = LocalTime.now()
        val start = now.minusMinutes(5)
        val end = now.plusMinutes(5)
        val hhmm = DateTimeFormatter.ofPattern("HH:mm")
        val timeWindow = "${start.format(hhmm)}-${end.format(hhmm)}"

        val rule = AnalyticsRule(
            id = "rule-zone-time-match",
            name = "Zone and time filtered motion webhook",
            analyticsType = AnalyticsRuleType.MOTION_DETECTION,
            conditions = AnalyticsRuleConditions(
                minConfidence = 0.3f,
                zones = listOf("entrance"),
                timeWindow = timeWindow
            ),
            actions = AnalyticsRuleActions(
                createEvent = false,
                sendNotification = false,
                sendWebhook = true,
                webhookUrl = "https://example.test/webhook"
            ),
            enabled = true
        )
        coEvery { ruleRepository.getActiveRulesByCameraId("cam-1") } returns listOf(rule)
        coEvery { webhookService.send(any(), any(), any(), any()) } returns Result.success(Unit)

        val motion = MotionDetectionResult(
            detected = true,
            confidence = 0.8f,
            zones = listOf(
                MotionZone(
                    zone = DetectionZone(name = "Entrance", polygon = listOf(listOf(0, 0), listOf(10, 10))),
                    intensity = 0.9f
                )
            )
        )

        val result = service.applyRules(
            cameraId = "cam-1",
            analyticsType = AnalyticsRuleType.MOTION_DETECTION,
            result = motion,
            confidence = 0.8f
        )

        assertTrue(result.isSuccess)
        assertEquals(listOf("rule-zone-time-match"), result.getOrNull())
        coVerify(exactly = 1) { webhookService.send(any(), any(), "rule-zone-time-match", "cam-1") }
    }

    @Test
    fun `applyRules respects cooldownMs from additionalConditions`() = runBlocking {
        val rule = AnalyticsRule(
            id = "rule-cooldown",
            name = "Cooldown motion webhook",
            analyticsType = AnalyticsRuleType.MOTION_DETECTION,
            conditions = AnalyticsRuleConditions(
                minConfidence = 0.3f,
                additionalConditions = mapOf("cooldownMs" to "60000")
            ),
            actions = AnalyticsRuleActions(
                createEvent = false,
                sendNotification = false,
                sendWebhook = true,
                webhookUrl = "https://example.test/webhook"
            ),
            enabled = true
        )
        coEvery { ruleRepository.getActiveRulesByCameraId("cam-1") } returns listOf(rule)
        coEvery { webhookService.send(any(), any(), any(), any()) } returns Result.success(Unit)

        val first = service.applyRules(
            cameraId = "cam-1",
            analyticsType = AnalyticsRuleType.MOTION_DETECTION,
            result = MotionDetectionResult(detected = true, confidence = 0.9f),
            confidence = 0.9f
        )
        val second = service.applyRules(
            cameraId = "cam-1",
            analyticsType = AnalyticsRuleType.MOTION_DETECTION,
            result = MotionDetectionResult(detected = true, confidence = 0.9f),
            confidence = 0.9f
        )

        assertEquals(listOf("rule-cooldown"), first.getOrNull())
        assertEquals(emptyList<String>(), second.getOrNull())
        coVerify(exactly = 1) { webhookService.send(any(), any(), "rule-cooldown", "cam-1") }
    }

    @Test
    fun `applyRules respects requiredCameraNameContains additional condition`() = runBlocking {
        val rule = AnalyticsRule(
            id = "rule-camera-name",
            name = "Camera-name filtered webhook",
            analyticsType = AnalyticsRuleType.MOTION_DETECTION,
            conditions = AnalyticsRuleConditions(
                minConfidence = 0.3f,
                additionalConditions = mapOf("requiredCameraNameContains" to "lobby")
            ),
            actions = AnalyticsRuleActions(
                createEvent = false,
                sendNotification = false,
                sendWebhook = true,
                webhookUrl = "https://example.test/webhook"
            ),
            enabled = true
        )
        coEvery { ruleRepository.getActiveRulesByCameraId("cam-1") } returns listOf(rule)
        coEvery { webhookService.send(any(), any(), any(), any()) } returns Result.success(Unit)

        coEvery { cameraRepository.getCameraById("cam-1") } returns Camera(
            id = "cam-1",
            name = "Parking entrance",
            url = "rtsp://example/stream"
        )
        val first = service.applyRules(
            cameraId = "cam-1",
            analyticsType = AnalyticsRuleType.MOTION_DETECTION,
            result = MotionDetectionResult(detected = true, confidence = 0.9f),
            confidence = 0.9f
        )
        assertEquals(emptyList<String>(), first.getOrNull())

        coEvery { cameraRepository.getCameraById("cam-1") } returns Camera(
            id = "cam-1",
            name = "Main Lobby",
            url = "rtsp://example/stream"
        )
        val second = service.applyRules(
            cameraId = "cam-1",
            analyticsType = AnalyticsRuleType.MOTION_DETECTION,
            result = MotionDetectionResult(detected = true, confidence = 0.9f),
            confidence = 0.9f
        )

        assertEquals(listOf("rule-camera-name"), second.getOrNull())
        coVerify(exactly = 1) { webhookService.send(any(), any(), "rule-camera-name", "cam-1") }
    }

    @Test
    fun `sendTestNotification uses channel flags and returns notification id`() = runBlocking {
        val notificationService = mockk<NotificationService>(relaxed = true)
        val serviceWithNotifications = AnalyticsRuleService(
            ruleRepository = ruleRepository,
            cameraRepository = cameraRepository,
            eventRepository = eventRepository,
            notificationService = notificationService,
            eventService = null,
            analyticsWebhookService = webhookService
        )

        val rule = AnalyticsRule(
            id = "rule-test",
            name = "Rule test delivery",
            analyticsType = AnalyticsRuleType.MOTION_DETECTION,
            conditions = AnalyticsRuleConditions(minConfidence = 0.5f),
            actions = AnalyticsRuleActions(
                createEvent = false,
                sendNotification = true,
                notifyInApp = false,
                notifyEmail = true,
                notifyTelegram = false,
                notificationType = NotificationType.INFO
            ),
            enabled = true
        )
        coEvery { ruleRepository.getRuleById("rule-test") } returns rule
        val extrasSlot = slot<Map<String, String>>()
        coEvery {
            notificationService.sendNotification(
                title = any(),
                message = any(),
                type = any(),
                priority = any(),
                userId = any(),
                cameraId = any(),
                eventId = any(),
                recordingId = any(),
                extras = capture(extrasSlot)
            )
        } returns Result.success(
            Notification(
                id = "notification-1",
                title = "test",
                message = "test",
                type = NotificationType.INFO
            )
        )

        val result = serviceWithNotifications.sendTestNotification(ruleId = "rule-test")

        assertTrue(result.isSuccess)
        assertEquals("notification-1", result.getOrNull()?.notificationId)
        assertEquals(listOf("email"), result.getOrNull()?.channels)
        assertEquals("email", extrasSlot.captured["__notifyChannels"])
    }

    @Test
    fun `getNotificationPolicy returns policy from rule actions`() = runBlocking {
        val rule = AnalyticsRule(
            id = "rule-policy-1",
            name = "Policy read",
            analyticsType = AnalyticsRuleType.MOTION_DETECTION,
            conditions = AnalyticsRuleConditions(minConfidence = 0.3f),
            actions = AnalyticsRuleActions(
                sendNotification = true,
                notifyInApp = false,
                notifyEmail = true,
                notifyTelegram = true,
                notificationType = NotificationType.WARNING
            ),
            enabled = true
        )
        coEvery { ruleRepository.getRuleById("rule-policy-1") } returns rule

        val result = service.getNotificationPolicy("rule-policy-1")

        assertTrue(result.isSuccess)
        val policy = result.getOrNull()!!
        assertEquals("rule-policy-1", policy.ruleId)
        assertEquals(true, policy.sendNotification)
        assertEquals(false, policy.notifyInApp)
        assertEquals(true, policy.notifyEmail)
        assertEquals(true, policy.notifyTelegram)
        assertEquals(NotificationType.WARNING, policy.notificationType)
    }

    @Test
    fun `updateNotificationPolicy fails when channels enabled but sendNotification false`() = runBlocking {
        val rule = AnalyticsRule(
            id = "rule-policy-2",
            name = "Policy update",
            analyticsType = AnalyticsRuleType.MOTION_DETECTION,
            conditions = AnalyticsRuleConditions(minConfidence = 0.3f),
            actions = AnalyticsRuleActions(
                sendNotification = true,
                notifyInApp = true,
                notifyEmail = false,
                notifyTelegram = false,
                notificationType = NotificationType.INFO
            ),
            enabled = true
        )
        coEvery { ruleRepository.getRuleById("rule-policy-2") } returns rule

        val result = service.updateNotificationPolicy(
            ruleId = "rule-policy-2",
            sendNotification = false,
            notifyInApp = true
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("sendNotification must be true") == true)
    }

    @Test
    fun `applyRules forwards email templates from additionalActions into notification extras`() = runBlocking {
        val notificationService = mockk<NotificationService>(relaxed = true)
        val serviceWithNotifications = AnalyticsRuleService(
            ruleRepository = ruleRepository,
            cameraRepository = cameraRepository,
            eventRepository = eventRepository,
            notificationService = notificationService,
            eventService = null,
            analyticsWebhookService = webhookService
        )
        val rule = AnalyticsRule(
            id = "rule-email-template",
            name = "Email template rule",
            analyticsType = AnalyticsRuleType.MOTION_DETECTION,
            conditions = AnalyticsRuleConditions(minConfidence = 0.5f),
            actions = AnalyticsRuleActions(
                createEvent = false,
                eventType = EventType.MOTION_DETECTION,
                eventSeverity = EventSeverity.INFO,
                sendNotification = true,
                notifyInApp = false,
                notifyEmail = true,
                notifyTelegram = false,
                notificationType = NotificationType.INFO,
                additionalActions = mapOf(
                    "emailSubjectTemplate" to "[Custom] {{title}}",
                    "emailBodyTemplate" to "<b>{{message}}</b>"
                )
            ),
            enabled = true
        )
        coEvery { ruleRepository.getActiveRulesByCameraId("cam-1") } returns listOf(rule)
        val extrasSlot = slot<Map<String, String>>()
        coEvery {
            notificationService.sendNotification(
                title = any(),
                message = any(),
                type = any(),
                priority = any(),
                userId = any(),
                cameraId = any(),
                eventId = any(),
                recordingId = any(),
                extras = capture(extrasSlot)
            )
        } returns Result.success(
            Notification(
                id = "notification-email-template",
                title = "ok",
                message = "ok",
                type = NotificationType.INFO
            )
        )

        val result = serviceWithNotifications.applyRules(
            cameraId = "cam-1",
            analyticsType = AnalyticsRuleType.MOTION_DETECTION,
            result = MotionDetectionResult(detected = true, confidence = 0.9f),
            confidence = 0.9f
        )

        assertTrue(result.isSuccess)
        assertEquals(listOf("rule-email-template"), result.getOrNull())
        assertEquals("[Custom] {{title}}", extrasSlot.captured["__emailSubjectTemplate"])
        assertEquals("<b>{{message}}</b>", extrasSlot.captured["__emailBodyTemplate"])
    }
}
