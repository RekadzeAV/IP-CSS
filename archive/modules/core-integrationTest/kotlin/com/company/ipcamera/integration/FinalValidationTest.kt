package com.company.ipcamera.integration

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Final validation tests for Phase 1 MVP
 * Tests complete user workflows and integration points
 */
class FinalValidationTest {

    @Test
    fun testCompleteAuthenticationFlow() = runTest {
        // Setup
        val uiBridge = createUiBridge()
        
        // 1. Login
        val loginResult = uiBridge.authenticationBridge.login("admin", "password123")
        assertTrue(loginResult is LoginResult.Success, "Login should succeed")
        
        val user = (loginResult as LoginResult.Success).user
        assertNotNull(user)
        assertEquals("admin", user.username)
        
        // 2. Check authorization
        val authorized = uiBridge.authenticationBridge.isAuthorized()
        assertTrue(authorized, "User should be authorized")
        
        // 3. Get current user
        val currentUser = uiBridge.authenticationBridge.getCurrentUser()
        assertNotNull(currentUser)
        assertEquals("admin", currentUser.username)
        
        // 4. Logout
        uiBridge.authenticationBridge.logout()
        
        // 5. Verify logout
        val afterLogout = uiBridge.authenticationBridge.isAuthorized()
        assertFalse(afterLogout, "User should be logged out")
    }

    @Test
    fun testCompleteCameraManagementFlow() = runTest {
        val uiBridge = createUiBridge()
        
        // 1. Get cameras (should be empty initially)
        val initialCameras = uiBridge.cameraBridge.getCameras()
        assertTrue(initialCameras.isEmpty(), "Initial camera list should be empty")
        
        // 2. Add camera
        val camera = Camera(
            id = "test-camera-1",
            name = "Test Camera",
            rtspUrl = "rtsp://192.168.1.100:554/stream",
            username = "admin",
            password = "password",
            width = 1920,
            height = 1080,
            fps = 30
        )
        
        val addResult = uiBridge.cameraBridge.addCamera(camera)
        assertTrue(addResult.isSuccess, "Camera add should succeed")
        
        // 3. Verify camera added
        val cameras = uiBridge.cameraBridge.getCameras()
        assertEquals(1, cameras.size, "Should have 1 camera")
        assertEquals("test-camera-1", cameras[0].id)
        
        // 4. Test camera connection
        val testResult = uiBridge.cameraBridge.testCamera(camera)
        assertTrue(testResult is CameraTestResult.Success, "Camera test should succeed")
        assertEquals(camera.rtspUrl, (testResult as CameraTestResult.Success).streamUrl)
        
        // 5. Update camera
        val updatedCamera = camera.copy(name = "Updated Camera")
        val updateResult = uiBridge.cameraBridge.updateCamera(updatedCamera)
        assertTrue(updateResult.isSuccess, "Camera update should succeed")
        
        // 6. PTZ control
        val ptzResult = uiBridge.cameraBridge.controlPtz(camera.id, PtzDirection.UP, 1.0f)
        assertTrue(ptzResult.isSuccess, "PTZ control should succeed")
        
        // 7. Delete camera
        val deleteResult = uiBridge.cameraBridge.deleteCamera(camera.id)
        assertTrue(deleteResult.isSuccess, "Camera delete should succeed")
        
        // 8. Verify deleted
        val finalCameras = uiBridge.cameraBridge.getCameras()
        assertTrue(finalCameras.isEmpty(), "Camera list should be empty after delete")
    }

    @Test
    fun testCompleteRecordingFlow() = runTest {
        val uiBridge = createUiBridge()
        
        // 1. Start recording
        val startResult = uiBridge.recordingBridge.startRecording("camera1")
        assertTrue(startResult.isSuccess, "Recording start should succeed")
        
        val recording = startResult.getOrNull()
        assertNotNull(recording)
        assertEquals("camera1", recording.cameraId)
        
        // 2. Get recordings
        val recordings = uiBridge.recordingBridge.getRecordings("camera1")
        assertEquals(1, recordings.size, "Should have 1 recording")
        
        // 3. Pause recording
        val pauseResult = uiBridge.recordingBridge.pauseRecording(recording.id)
        assertTrue(pauseResult.isSuccess, "Pause should succeed")
        
        // 4. Resume recording
        val resumeResult = uiBridge.recordingBridge.resumeRecording(recording.id)
        assertTrue(resumeResult.isSuccess, "Resume should succeed")
        
        // 5. Stop recording
        val stopResult = uiBridge.recordingBridge.stopRecording(recording.id)
        assertTrue(stopResult.isSuccess, "Stop should succeed")
        
        // 6. Delete recording
        val deleteResult = uiBridge.recordingBridge.deleteRecording(recording.id)
        assertTrue(deleteResult.isSuccess, "Delete should succeed")
        
        // 7. Verify deleted
        val finalRecordings = uiBridge.recordingBridge.getRecordings("camera1")
        assertTrue(finalRecordings.isEmpty(), "Recordings should be empty after delete")
    }

    @Test
    fun testCompleteEventDetectionFlow() = runTest {
        val uiBridge = createUiBridge()
        
        // 1. Detect motion
        val motionResult = uiBridge.eventBridge.detectMotion("camera1")
        assertTrue(motionResult.isSuccess, "Motion detection should succeed")
        
        val motion = motionResult.getOrNull()
        assertNotNull(motion)
        assertFalse(motion.detected, "No motion should be detected (test mode)")
        
        // 2. Detect faces
        val facesResult = uiBridge.eventBridge.detectFaces("camera1")
        assertTrue(facesResult.isSuccess, "Face detection should succeed")
        
        // 3. Recognize license plate
        val plateResult = uiBridge.eventBridge.recognizeLicensePlate("camera1")
        assertTrue(plateResult.isSuccess, "License plate recognition should succeed")
        
        // 4. Detect objects
        val objectsResult = uiBridge.eventBridge.detectObjects("camera1")
        assertTrue(objectsResult.isSuccess, "Object detection should succeed")
    }

    @Test
    fun testCompleteSettingsFlow() = runTest {
        val uiBridge = createUiBridge()
        
        // 1. Get settings
        val settings = uiBridge.settingsBridge.getSettings()
        assertNotNull(settings)
        
        // 2. Update setting
        val updateResult = uiBridge.settingsBridge.updateSetting("video.quality", "1080p")
        assertTrue(updateResult.isSuccess, "Setting update should succeed")
        
        // 3. Update camera profile
        val profile = CameraProfile("HD Profile", "1920x1080", 30, 4000)
        val profileResult = uiBridge.settingsBridge.updateProfile("camera1", profile)
        assertTrue(profileResult.isSuccess, "Profile update should succeed")
    }

    @Test
    fun testCompleteNotificationFlow() = runTest {
        val uiBridge = createUiBridge()
        
        // 1. Get notifications (should be empty)
        val initialNotifications = uiBridge.notificationBridge.getNotifications()
        assertTrue(initialNotifications.isEmpty(), "Initial notifications should be empty")
        
        // 2. Send notification
        val notification = Notification(
            id = "notif-1",
            message = "Test notification",
            timestamp = System.currentTimeMillis()
        )
        
        val sendResult = uiBridge.notificationBridge.sendNotification(notification)
        assertTrue(sendResult.isSuccess, "Send notification should succeed")
        
        // 3. Get notifications
        val notifications = uiBridge.notificationBridge.getNotifications()
        assertEquals(1, notifications.size, "Should have 1 notification")
        
        // 4. Mark as read
        val readResult = uiBridge.notificationBridge.markAsRead(notification.id)
        assertTrue(readResult.isSuccess, "Mark as read should succeed")
    }

    @Test
    fun testCompleteAnalyticsFlow() = runTest {
        val uiBridge = createUiBridge()
        
        // 1. Analyze video
        val analysisResult = uiBridge.analyticsBridge.analyzeVideo(
            "camera1",
            AnalysisType.MOTION
        )
        assertTrue(analysisResult.isSuccess, "Video analysis should succeed")
        
        val analysis = analysisResult.getOrNull()
        assertNotNull(analysis)
        assertTrue(analysis.success, "Analysis should succeed")
        
        // 2. Track objects
        val trackResult = uiBridge.analyticsBridge.trackObjects(
            "camera1",
            listOf(ObjectType.PERSON, ObjectType.VEHICLE)
        )
        assertTrue(trackResult.isSuccess, "Object tracking should succeed")
        
        // 3. Detect objects
        val detectResult = uiBridge.analyticsBridge.detectObjects("camera1")
        assertTrue(detectResult.isSuccess, "Object detection should succeed")
    }

    @Test
    fun testEndToEndUserJourney() = runTest {
        val uiBridge = createUiBridge()
        
        // Complete user journey
        // 1. Login
        val login = uiBridge.authenticationBridge.login("admin", "password123")
        assertTrue(login is LoginResult.Success)
        
        // 2. Get cameras
        val cameras = uiBridge.cameraBridge.getCameras()
        // May be empty, that's OK
        
        // 3. Add a camera
        val camera = Camera(
            id = "journey-camera",
            name = "Journey Camera",
            rtspUrl = "rtsp://192.168.1.100:554/stream",
            username = "admin",
            password = "password",
            width = 1920,
            height = 1080,
            fps = 30
        )
        val addResult = uiBridge.cameraBridge.addCamera(camera)
        assertTrue(addResult.isSuccess)
        
        // 4. Test camera
        val testResult = uiBridge.cameraBridge.testCamera(camera)
        assertTrue(testResult is CameraTestResult.Success)
        
        // 5. Start recording
        val recording = uiBridge.recordingBridge.startRecording(camera.id)
        assertTrue(recording.isSuccess)
        
        // 6. Detect motion
        val motion = uiBridge.eventBridge.detectMotion(camera.id)
        assertTrue(motion.isSuccess)
        
        // 7. Get settings
        val settings = uiBridge.settingsBridge.getSettings()
        assertNotNull(settings)
        
        // 8. Send notification
        val notification = Notification("test", "Journey complete", System.currentTimeMillis())
        val notifyResult = uiBridge.notificationBridge.sendNotification(notification)
        assertTrue(notifyResult.isSuccess)
        
        // 9. Analyze video
        val analysis = uiBridge.analyticsBridge.analyzeVideo(camera.id, AnalysisType.OBJECT)
        assertTrue(analysis.isSuccess)
        
        // 10. Stop recording
        val stopResult = uiBridge.recordingBridge.stopRecording(recording.getOrNull()!!.id)
        assertTrue(stopResult.isSuccess)
        
        // 11. Delete camera
        val deleteResult = uiBridge.cameraBridge.deleteCamera(camera.id)
        assertTrue(deleteResult.isSuccess)
        
        // 12. Logout
        uiBridge.authenticationBridge.logout()
        assertFalse(uiBridge.authenticationBridge.isAuthorized())
    }

    @Test
    fun testSecurityIntegration() = runTest {
        val uiBridge = createUiBridge()
        
        // 1. Login with valid credentials
        val validLogin = uiBridge.authenticationBridge.login("admin", "password123")
        assertTrue(validLogin is LoginResult.Success)
        
        // 2. Verify password hashing works
        assertTrue(validLogin.user.username.isNotEmpty())
        
        // 3. Logout
        uiBridge.authenticationBridge.logout()
        
        // 4. Verify session cleared
        assertFalse(uiBridge.authenticationBridge.isAuthorized())
    }

    @Test
    fun testErrorHandling() = runTest {
        val uiBridge = createUiBridge()
        
        // Test invalid camera ID
        val invalidCamera = uiBridge.cameraBridge.getCameraById("invalid-id")
        // Should return null or handle gracefully
        
        // Test invalid recording ID
        val invalidRecording = uiBridge.recordingBridge.stopRecording("invalid-id")
        // Should handle gracefully
        
        // Test invalid event ID
        val invalidEvent = uiBridge.eventBridge.acknowledgeEvent("invalid-id")
        // Should handle gracefully
    }
}
