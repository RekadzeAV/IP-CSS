package com.company.ipcamera.e2e

import com.company.ipcamera.e2e.fixture.*
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertTrue

/**
 * E2E тесты для критических сценариев IP-CSS приложения.
 * 
 * Сценарии:
 * 1. Login authentication flow
 * 2. Camera CRUD operations
 * 3. Recording lifecycle
 * 4. User management
 * 5. Settings management
 */
class CriticalScenariosE2ETest {

    @Test
    fun `E2E - Login authentication flow`() = runTest {
        val fixture = E2ETestFixture()
        
        try {
            // Start application
            fixture.startApplication()
            
            // Navigate to login page
            fixture.navigateTo("/login")
            
            // Enter credentials
            fixture.fillLoginForm("admin", "admin123")
            
            // Submit login
            fixture.clickLoginButton()
            
            // Verify redirect to dashboard
            fixture.waitForUrl("/dashboard")
            
            // Verify user info displayed
            assertTrue(fixture.isUserLoggedIn())
            
        } finally {
            fixture.close()
        }
    }

    @Test
    fun `E2E - Camera CRUD operations`() = runTest {
        val fixture = E2ETestFixture()
        
        try {
            // Login first
            fixture.loginAsAdmin()
            
            // Navigate to cameras page
            fixture.navigateTo("/cameras")
            
            // Add new camera
            val cameraName = "E2E Test Camera ${System.currentTimeMillis()}"
            fixture.addCamera(
                name = cameraName,
                url = "rtsp://127.0.0.1:8554/test",
                username = "admin",
                password = "password"
            )
            
            // Verify camera added
            assertTrue(fixture.cameraExists(cameraName))
            
            // Edit camera
            fixture.editCamera(cameraName, newName = "E2E Updated Camera")
            
            // Verify camera updated
            assertTrue(fixture.cameraExists("E2E Updated Camera"))
            
            // Delete camera
            fixture.deleteCamera("E2E Updated Camera")
            
            // Verify camera deleted
            assertTrue(!fixture.cameraExists("E2E Updated Camera"))
            
        } finally {
            fixture.close()
        }
    }

    @Test
    fun `E2E - Recording lifecycle`() = runTest {
        val fixture = E2ETestFixture()
        
        try {
            // Login first
            fixture.loginAsAdmin()
            
            // Navigate to cameras page
            fixture.navigateTo("/cameras")
            
            // Start recording
            fixture.startRecording("test-camera")
            
            // Verify recording started
            assertTrue(fixture.isRecording("test-camera"))
            
            // Wait for recording to accumulate
            kotlinx.coroutines.delay(5000)
            
            // Pause recording
            fixture.pauseRecording("test-camera")
            
            // Verify recording paused
            assertTrue(fixture.isPaused("test-camera"))
            
            // Resume recording
            fixture.resumeRecording("test-camera")
            
            // Verify recording resumed
            assertTrue(fixture.isRecording("test-camera"))
            
            // Stop recording
            fixture.stopRecording("test-camera")
            
            // Verify recording stopped
            assertTrue(!fixture.isRecording("test-camera"))
            
            // Verify recording saved
            assertTrue(fixture.recordingExists("test-camera"))
            
        } finally {
            fixture.close()
        }
    }

    @Test
    fun `E2E - User creation and management`() = runTest {
        val fixture = E2ETestFixture()
        
        try {
            // Login as admin
            fixture.loginAsAdmin()
            
            // Navigate to users page
            fixture.navigateTo("/users")
            
            // Create new user
            val username = "e2e_test_user_${System.currentTimeMillis()}"
            fixture.createUser(
                username = username,
                password = "testpass123",
                role = "OPERATOR"
            )
            
            // Verify user created
            assertTrue(fixture.userExists(username))
            
            // Edit user
            fixture.editUser(username, newPassword = "newpass123")
            
            // Verify user updated
            assertTrue(fixture.userExists(username))
            
            // Delete user
            fixture.deleteUser(username)
            
            // Verify user deleted
            assertTrue(!fixture.userExists(username))
            
        } finally {
            fixture.close()
        }
    }

    @Test
    fun `E2E - Settings management`() = runTest {
        val fixture = E2ETestFixture()
        
        try {
            // Login as admin
            fixture.loginAsAdmin()
            
            // Navigate to settings page
            fixture.navigateTo("/settings")
            
            // Update video settings
            fixture.updateVideoSettings(
                resolution = "1920x1080",
                fps = 30,
                bitrate = "4000kbps"
            )
            
            // Save settings
            fixture.saveSettings()
            
            // Verify settings saved
            assertTrue(fixture.getVideoSettings().resolution == "1920x1080")
            
        } finally {
            fixture.close()
        }
    }
}
