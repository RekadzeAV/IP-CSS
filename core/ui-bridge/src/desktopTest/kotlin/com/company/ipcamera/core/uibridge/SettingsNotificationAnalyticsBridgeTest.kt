package com.company.ipcamera.core.uibridge

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SettingsNotificationAnalyticsBridgeTest {

    @Test
    fun testGetSettingsSuccess() = runTest {
        val bridge = DesktopSettingsBridge(null, null)
        val settings = bridge.getSettings()
        assertNotNull(settings)
    }

    @Test
    fun testUpdateSettingSuccess() = runTest {
        val bridge = DesktopSettingsBridge(null, null)
        val result = bridge.updateSetting("key", "value")
        assertTrue(result.isSuccess)
    }

    @Test
    fun testUpdateProfileSuccess() = runTest {
        val bridge = DesktopSettingsBridge(null, null)
        val profile = CameraProfile("Test", "1920x1080", 30, 4000)
        val result = bridge.updateProfile("camera1", profile)
        assertTrue(result.isSuccess)
    }

    @Test
    fun testGetNotificationsReturnsEmptyList() = runTest {
        val bridge = DesktopNotificationBridge(null, null, null)
        val notifications = bridge.getNotifications()
        assertTrue(notifications.isEmpty())
    }

    @Test
    fun testMarkNotificationAsReadSuccess() = runTest {
        val bridge = DesktopNotificationBridge(null, null, null)
        val result = bridge.markAsRead("notification1")
        assertTrue(result.isSuccess)
    }

    @Test
    fun testSendNotificationSuccess() = runTest {
        val bridge = DesktopNotificationBridge(null, null, null)
        val result = bridge.sendNotification(
            com.company.ipcamera.shared.domain.model.Notification(
                id = "1",
                title = "Test",
                message = "Test notification",
                type = com.company.ipcamera.shared.domain.model.NotificationType.INFO
            )
        )
        assertTrue(result.isSuccess)
    }

    @Test
    fun testAnalyzeVideoSuccess() = runTest {
        val bridge = DesktopAnalyticsBridge(null, null, null)
        val result = bridge.analyzeVideo("camera1", AnalysisType.MOTION)
        assertTrue(result.isSuccess)
        val analysisResult = result.getOrNull()
        assertEquals(true, analysisResult?.success)
    }

    @Test
    fun testTrackObjectsSuccess() = runTest {
        val bridge = DesktopAnalyticsBridge(null, null, null)
        val result = bridge.trackObjects("camera1", listOf(ObjectType.PERSON))
        assertTrue(result.isSuccess)
    }

    @Test
    fun testDetectObjectsSuccess() = runTest {
        val bridge = DesktopAnalyticsBridge(null, null, null)
        val result = bridge.detectObjects("camera1")
        assertTrue(result.isSuccess)
    }
}
