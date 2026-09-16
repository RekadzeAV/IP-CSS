# UI Bridge API

## Overview

The UI Bridge Module (`core:ui-bridge`) provides a cross-platform abstraction layer between UI and business logic. It serves as a facade pattern, exposing simple APIs for UI components to interact with use cases and repositories.

**Module Path:** `core:ui-bridge`  
**Platforms:** Desktop (JVM), Android, iOS, macOS, Linux, Windows  
**Kotlin Multiplatform:** ✅ Yes

---

## Table of Contents

1. [Architecture](#architecture)
2. [Main Interface](#main-interface)
3. [Authentication Bridge](#authentication-bridge)
4. [Camera Bridge](#camera-bridge)
5. [Recording Bridge](#recording-bridge)
6. [Event Bridge](#event-bridge)
7. [Settings Bridge](#settings-bridge)
8. [Notification Bridge](#notification-bridge)
9. [Analytics Bridge](#analytics-bridge)
10. [Data Models](#data-models)
11. [Examples](#examples)

---

## Architecture

### Layer Structure

```
┌─────────────────────────────────────┐
│          UI Layer                   │
│  (Compose / Jetpack / SwiftUI)      │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│         UI Bridge Layer             │
│  (Platform-specific implementations)│
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│      Use Cases Layer                │
│     (Business Logic)                │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│     Repository Layer                │
│     (Data Sources)                  │
└─────────────────────────────────────┘
```

### Design Principles

- **Facade Pattern** — Simplifies complex subsystem interactions
- **Dependency Injection** — Use Cases injected via constructor
- **Coroutine-based** — All operations are suspend functions
- **Result-based** — Success/Failure patterns for error handling

---

## Main Interface

### `UiBridge`

The root interface providing access to all bridges.

```kotlin
interface UiBridge {
    val authenticationBridge: AuthenticationBridge
    val cameraBridge: CameraBridge
    val recordingBridge: RecordingBridge
    val eventBridge: EventBridge
    val settingsBridge: SettingsBridge
    val notificationBridge: NotificationBridge
    val analyticsBridge: AnalyticsBridge
}
```

### Factory

```kotlin
expect fun createUiBridge(): UiBridge
```

### Usage

```kotlin
// Create bridge instance
val uiBridge = createUiBridge()

// Access bridges
val authBridge = uiBridge.authenticationBridge
val cameraBridge = uiBridge.cameraBridge
// ...
```

---

## Authentication Bridge

### Interface: `AuthenticationBridge`

```kotlin
interface AuthenticationBridge {
    suspend fun login(username: String, password: String): LoginResult
    suspend fun logout()
    suspend fun isAuthorized(): Boolean
    suspend fun getCurrentUser(): User?
    suspend fun register(username: String, password: String, email: String): RegistrationResult
}
```

### Result Types

```kotlin
sealed class LoginResult {
    data class Success(val user: User) : LoginResult()
    data class Failure(val error: String) : LoginResult()
}

sealed class RegistrationResult {
    data class Success(val user: User) : RegistrationResult()
    data class Failure(val error: String) : RegistrationResult()
}
```

### Usage

```kotlin
// Login
val result = uiBridge.authenticationBridge.login("admin", "password123")
when (result) {
    is LoginResult.Success -> {
        val user = result.user
        // Navigate to main screen
    }
    is LoginResult.Failure -> {
        // Show error: result.error
    }
}

// Check authorization
val authorized = uiBridge.authenticationBridge.isAuthorized()

// Get current user
val user = uiBridge.authenticationBridge.getCurrentUser()

// Logout
uiBridge.authenticationBridge.logout()

// Register
val regResult = uiBridge.authenticationBridge.register("newuser", "password", "user@email.com")
```

---

## Camera Bridge

### Interface: `CameraBridge`

```kotlin
interface CameraBridge {
    suspend fun getCameras(): List<Camera>
    suspend fun getCameraById(cameraId: String): Camera?
    suspend fun addCamera(camera: Camera): Result<Camera>
    suspend fun updateCamera(camera: Camera): Result<Unit>
    suspend fun deleteCamera(cameraId: String): Result<Unit>
    suspend fun discoverCameras(): List<DiscoveredCamera>
    suspend fun testCamera(camera: Camera): CameraTestResult
    suspend fun controlPtz(cameraId: String, direction: PtzDirection, speed: Float): Result<Unit>
}
```

### Usage

```kotlin
// Get all cameras
val cameras = uiBridge.cameraBridge.getCameras()
cameras.forEach { camera ->
    println("${camera.name}: ${camera.rtspUrl}")
}

// Get specific camera
val camera = uiBridge.cameraBridge.getCameraById("camera-123")

// Add new camera
val newCamera = Camera(
    id = "new-id",
    name = "Front Door",
    rtspUrl = "rtsp://192.168.1.100:554/stream",
    username = "admin",
    password = "password",
    width = 1920,
    height = 1080,
    fps = 30
)
val addResult = uiBridge.cameraBridge.addCamera(newCamera)

// Test camera connection
val testResult = uiBridge.cameraBridge.testCamera(newCamera)
when (testResult) {
    is CameraTestResult.Success -> {
        println("Stream URL: ${testResult.streamUrl}")
        println("Resolution: ${testResult.resolution}")
    }
    is CameraTestResult.Failure -> {
        println("Test failed: ${testResult.error}")
    }
}

// PTZ Control
uiBridge.cameraBridge.controlPtz(
    cameraId = "camera-123",
    direction = PtzDirection.UP,
    speed = 1.0f
)

// Discover cameras
val discovered = uiBridge.cameraBridge.discoverCameras()
```

### PTZ Directions

```kotlin
enum class PtzDirection {
    UP, DOWN, LEFT, RIGHT, ZOOM_IN, ZOOM_OUT
}
```

---

## Recording Bridge

### Interface: `RecordingBridge`

```kotlin
interface RecordingBridge {
    suspend fun startRecording(cameraId: String): Result<Recording>
    suspend fun stopRecording(recordingId: String): Result<Unit>
    suspend fun pauseRecording(recordingId: String): Result<Unit>
    suspend fun resumeRecording(recordingId: String): Result<Unit>
    suspend fun getRecordings(cameraId: String): List<Recording>
    suspend fun deleteRecording(recordingId: String): Result<Unit>
}
```

### Usage

```kotlin
// Start recording
val recordingResult = uiBridge.recordingBridge.startRecording("camera-123")
val recording = recordingResult.getOrNull()

// Stop recording
uiBridge.recordingBridge.stopRecording(recording!!.id)

// Pause/Resume
uiBridge.recordingBridge.pauseRecording(recording.id)
uiBridge.recordingBridge.resumeRecording(recording.id)

// Get recordings
val recordings = uiBridge.recordingBridge.getRecordings("camera-123")

// Delete recording
uiBridge.recordingBridge.deleteRecording(recording.id)
```

---

## Event Bridge

### Interface: `EventBridge`

```kotlin
interface EventBridge {
    suspend fun getEvents(cameraId: String): List<Event>
    suspend fun acknowledgeEvent(eventId: String): Result<Unit>
    suspend fun detectMotion(cameraId: String): Result<DetectionResult>
    suspend fun detectFaces(cameraId: String): Result<DetectionResult>
    suspend fun recognizeLicensePlate(cameraId: String): Result<LicensePlateResult>
}
```

### Usage

```kotlin
// Get events
val events = uiBridge.eventBridge.getEvents("camera-123")

// Acknowledge event
uiBridge.eventBridge.acknowledgeEvent("event-456")

// Detect motion
val motionResult = uiBridge.eventBridge.detectMotion("camera-123")
val motionDetected = motionResult.getOrNull()?.detected

// Detect faces
val facesResult = uiBridge.eventBridge.detectFaces("camera-123")
val faces = facesResult.getOrNull()?.objects

// License plate recognition
val plateResult = uiBridge.eventBridge.recognizeLicensePlate("camera-123")
val plateNumber = plateResult.getOrNull()?.plateNumber
```

---

## Settings Bridge

### Interface: `SettingsBridge`

```kotlin
interface SettingsBridge {
    suspend fun getSettings(): Settings
    suspend fun updateSetting(key: String, value: String): Result<Unit>
    suspend fun updateProfile(cameraId: String, profile: CameraProfile): Result<Unit>
}
```

### Usage

```kotlin
// Get settings
val settings = uiBridge.settingsBridge.getSettings()

// Update setting
uiBridge.settingsBridge.updateSetting("video.quality", "1080p")

// Update camera profile
val profile = CameraProfile(
    name = "HD Profile",
    resolution = "1920x1080",
    fps = 30,
    bitrate = 4000
)
uiBridge.settingsBridge.updateProfile("camera-123", profile)
```

---

## Notification Bridge

### Interface: `NotificationBridge`

```kotlin
interface NotificationBridge {
    suspend fun getNotifications(): List<Notification>
    suspend fun markAsRead(notificationId: String): Result<Unit>
    suspend fun sendNotification(notification: Notification): Result<Unit>
}
```

### Usage

```kotlin
// Get notifications
val notifications = uiBridge.notificationBridge.getNotifications()

// Mark as read
uiBridge.notificationBridge.markAsRead("notif-789")

// Send notification
val notification = Notification(
    id = "notif-new",
    message = "Motion detected",
    timestamp = System.currentTimeMillis()
)
uiBridge.notificationBridge.sendNotification(notification)
```

---

## Analytics Bridge

### Interface: `AnalyticsBridge`

```kotlin
interface AnalyticsBridge {
    suspend fun analyzeVideo(cameraId: String, analysisType: AnalysisType): Result<AnalysisResult>
    suspend fun trackObjects(cameraId: String, objectTypes: List<ObjectType>): Result<Unit>
    suspend fun detectObjects(cameraId: String): Result<DetectionResult>
}
```

### Analysis Types

```kotlin
enum class AnalysisType {
    MOTION, FACE, OBJECT, LICENSE_PLATE
}

enum class ObjectType {
    PERSON, VEHICLE, ANIMAL, LICENSE_PLATE
}
```

### Usage

```kotlin
// Analyze video
val analysisResult = uiBridge.analyticsBridge.analyzeVideo(
    cameraId = "camera-123",
    analysisType = AnalysisType.OBJECT
)
val analysis = analysisResult.getOrNull()

// Track objects
uiBridge.analyticsBridge.trackObjects(
    cameraId = "camera-123",
    objectTypes = listOf(ObjectType.PERSON, ObjectType.VEHICLE)
)

// Detect objects
val detection = uiBridge.analyticsBridge.detectObjects("camera-123")
```

---

## Data Models

### Camera

```kotlin
data class Camera(
    val id: String,
    val name: String,
    val rtspUrl: String,
    val username: String,
    val password: String,
    val width: Int,
    val height: Int,
    val fps: Int
)
```

### Recording

```kotlin
data class Recording(
    val id: String,
    val cameraId: String,
    val startTime: Long,
    val endTime: Long? = null
)
```

### Event

```kotlin
data class Event(
    val id: String,
    val cameraId: String,
    val type: EventType,
    val timestamp: Long,
    val acknowledged: Boolean
)

enum class EventType {
    MOTION, FACE, OBJECT, LICENSE_PLATE
}
```

### DetectionResult

```kotlin
data class DetectionResult(
    val detected: Boolean,
    val objects: List<DetectedObject>,
    val confidence: Float
)

data class DetectedObject(
    val type: ObjectType,
    val boundingBox: Rect,
    val confidence: Float
)

data class Rect(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)
```

### CameraProfile

```kotlin
data class CameraProfile(
    val name: String,
    val resolution: String,
    val fps: Int,
    val bitrate: Int
)
```

### AnalysisResult

```kotlin
data class AnalysisResult(
    val success: Boolean,
    val analysisData: String,
    val processingTimeMs: Long
)
```

---

## Examples

### Desktop Compose Integration

```kotlin
class CameraViewModel : ViewModel() {
    private val uiBridge = createUiBridge()
    
    private val _cameras = MutableStateFlow<List<Camera>>(emptyList())
    val cameras: StateFlow<List<Camera>> = _cameras.asStateFlow()
    
    fun loadCameras() {
        viewModelScope.launch {
            _cameras.value = uiBridge.cameraBridge.getCameras()
        }
    }
    
    fun addCamera(camera: Camera) {
        viewModelScope.launch {
            uiBridge.cameraBridge.addCamera(camera)
            loadCameras()
        }
    }
    
    fun testCamera(camera: Camera) {
        viewModelScope.launch {
            val result = uiBridge.cameraBridge.testCamera(camera)
            // Handle result
        }
    }
}
```

### Android ViewModel Integration

```kotlin
class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val uiBridge = AndroidUiBridge.getInstance(application)
    
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()
    
    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            val result = uiBridge.authenticationBridge.login(username, password)
            _loginState.value = when (result) {
                is LoginResult.Success -> LoginState.Success(result.user)
                is LoginResult.Failure -> LoginState.Error(result.error)
            }
        }
    }
}
```

### Complete User Flow

```kotlin
class UserFlowExample {
    private val uiBridge = createUiBridge()
    
    suspend fun completeFlow() {
        // 1. Login
        val loginResult = uiBridge.authenticationBridge.login("admin", "password")
        require(loginResult is LoginResult.Success)
        
        // 2. Get cameras
        val cameras = uiBridge.cameraBridge.getCameras()
        require(cameras.isNotEmpty())
        
        // 3. Test first camera
        val testResult = uiBridge.cameraBridge.testCamera(cameras[0])
        require(testResult is CameraTestResult.Success)
        
        // 4. Start recording
        val recording = uiBridge.recordingBridge.startRecording(cameras[0].id)
        require(recording.isSuccess)
        
        // 5. Detect motion
        val motion = uiBridge.eventBridge.detectMotion(cameras[0].id)
        
        // 6. Stop recording
        uiBridge.recordingBridge.stopRecording(recording.getOrNull()!!.id)
        
        // 7. Logout
        uiBridge.authenticationBridge.logout()
    }
}
```

---

## Testing

### Unit Test Example

```kotlin
class CameraBridgeTest {
    private lateinit var bridge: CameraBridge
    
    @BeforeTest
    fun setup() {
        bridge = DesktopCameraBridge(null, null, null, null, null, null, null, null)
    }
    
    @Test
    fun testAddCamera() = runTest {
        val camera = Camera("1", "Test", "rtsp://...", "admin", "pass", 1920, 1080, 30)
        val result = bridge.addCamera(camera)
        
        assertTrue(result.isSuccess)
    }
}
```

---

## API Reference

### Packages

- `com.company.ipcamera.core.uibridge` — Main bridge interfaces
- `com.company.ipcamera.core.uibridge.model` — Data models
- `com.company.ipcamera.core.uibridge.desktop` — Desktop implementations
- `com.company.ipcamera.core.uibridge.android` — Android implementations

### Dependencies

- `core:common` — Common utilities
- `core:network` — RTSP client
- `core:security` — Security module
- `shared` — Use cases & repositories
- `kotlinx-coroutines-core` — Async operations

---

**API Version:** 1.0  
**Last Updated:** 2026-05-17  
**Maintainer:** NLP-Core-Team
