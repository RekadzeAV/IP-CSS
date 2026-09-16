package com.company.ipcamera.core.uibridge

import android.content.Context
import com.company.ipcamera.core.common.security.PasswordEncryption
import com.company.ipcamera.core.common.security.PasswordEncryptionFactory
import com.company.ipcamera.core.network.rtsp.NativeRtspClient
import com.company.ipcamera.shared.common.nowMillis
import com.company.ipcamera.shared.domain.model.*
import com.company.ipcamera.shared.domain.repository.DiscoveredCamera
import com.company.ipcamera.shared.domain.usecase.*

/**
 * Android реализация UiBridge
 */
actual interface UiBridge {
    actual val authenticationBridge: AuthenticationBridge
    actual val cameraBridge: CameraBridge
    actual val recordingBridge: RecordingBridge
    actual val eventBridge: EventBridge
    actual val settingsBridge: SettingsBridge
    actual val notificationBridge: NotificationBridge
    actual val analyticsBridge: AnalyticsBridge
}

/**
 * Фабрика для создания UiBridge
 */
actual fun createUiBridge(): UiBridge {
    // Android-реализации требуется Context (синглтон через companion getInstance).
    // Context недоступен без Koin/Activity — фабрика требует явной инициализации:
    // см. AndroidUiBridge.getInstance(context).
    throw IllegalStateException(
        "AndroidUiBridge requires Context: use AndroidUiBridge.getInstance(context) " +
            "after Koin initialization in MainActivity"
    )
}

actual fun createUiBridgeWithDependencies(
    loginUseCase: LoginUseCase?,
    logoutUseCase: LogoutUseCase?,
    registerUseCase: RegisterUseCase?,
    getCamerasUseCase: GetCamerasUseCase?,
    addCameraUseCase: AddCameraUseCase?,
    updateCameraUseCase: UpdateCameraUseCase?,
    deleteCameraUseCase: DeleteCameraUseCase?,
    discoverCamerasUseCase: DiscoverCamerasUseCase?,
    testDiscoveredCameraUseCase: TestDiscoveredCameraUseCase?,
    controlPtzUseCase: ControlPtzUseCase?,
    startRecordingUseCase: StartRecordingUseCase?,
    stopRecordingUseCase: StopRecordingUseCase?,
    pauseRecordingUseCase: PauseRecordingUseCase?,
    resumeRecordingUseCase: ResumeRecordingUseCase?,
    getRecordingsUseCase: GetRecordingsUseCase?,
    deleteRecordingUseCase: DeleteRecordingUseCase?,
    getEventsUseCase: GetEventsUseCase?,
    acknowledgeEventUseCase: AcknowledgeEventUseCase?,
    detectMotionUseCase: DetectMotionUseCase?,
    detectFacesUseCase: DetectFacesUseCase?,
    recognizeLicensePlateUseCase: RecognizeLicensePlateUseCase?,
    getSettingsUseCase: GetSettingsUseCase?,
    updateSettingUseCase: UpdateSettingUseCase?,
    getNotificationsUseCase: GetNotificationsUseCase?,
    markNotificationAsReadUseCase: MarkNotificationAsReadUseCase?,
    sendNotificationUseCase: SendNotificationUseCase?,
    analyzeVideoUseCase: AnalyzeVideoUseCase?,
    trackObjectsUseCase: TrackObjectsUseCase?,
    detectObjectsUseCase: DetectObjectsUseCase?
): UiBridge {
    // Android UiBridge пока работает без внедрённых UseCase (синглтон через Context).
    return createUiBridge()
}

/**
 * Android UiBridge с Context
 */
class AndroidUiBridge private constructor(
    context: Context
) : UiBridge {

    private val rtspClient = NativeRtspClient()
    private val passwordEncryption = PasswordEncryptionFactory.create()

    override val authenticationBridge: AuthenticationBridge = AndroidAuthenticationBridge(passwordEncryption)
    override val cameraBridge: CameraBridge = AndroidCameraBridge(rtspClient, passwordEncryption)
    override val recordingBridge: RecordingBridge = AndroidRecordingBridge()
    override val eventBridge: EventBridge = AndroidEventBridge()
    override val settingsBridge: SettingsBridge = AndroidSettingsBridge()
    override val notificationBridge: NotificationBridge = AndroidNotificationBridge()
    override val analyticsBridge: AnalyticsBridge = AndroidAnalyticsBridge()

    companion object {
        private var instance: AndroidUiBridge? = null

        fun getInstance(context: Context): AndroidUiBridge {
            if (instance == null) {
                instance = AndroidUiBridge(context.applicationContext)
            }
            return instance!!
        }
    }
}

/**
 * Android Authentication Bridge
 */
class AndroidAuthenticationBridge(
    private val passwordEncryption: PasswordEncryption
) : AuthenticationBridge {

    override suspend fun login(username: String, password: String): LoginResult {
        return try {
            LoginResult.Success(
                User(
                    id = "1",
                    username = username,
                    email = null,
                    role = UserRole.VIEWER,
                    createdAt = nowMillis()
                )
            )
        } catch (e: Exception) {
            LoginResult.Failure(e.message ?: "Login failed")
        }
    }

    override suspend fun logout() {
        // Android-specific logout logic
    }

    override suspend fun isAuthorized(): Boolean {
        return false
    }

    override suspend fun getCurrentUser(): User? {
        return null
    }

    override suspend fun register(username: String, password: String, email: String): RegistrationResult {
        return RegistrationResult.Failure("Registration not implemented yet")
    }
}

/**
 * Android Camera Bridge
 */
class AndroidCameraBridge(
    private val rtspClient: NativeRtspClient,
    private val passwordEncryption: PasswordEncryption
) : CameraBridge {

    override suspend fun getCameras(): List<Camera> {
        return emptyList()
    }

    override suspend fun getCameraById(cameraId: String): Camera? {
        return null
    }

    override suspend fun addCamera(camera: Camera): Result<Camera> {
        return Result.success(camera)
    }

    override suspend fun updateCamera(camera: Camera): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun deleteCamera(cameraId: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun discoverCameras(): List<DiscoveredCamera> {
        return emptyList()
    }

    override suspend fun testCamera(camera: Camera): CameraTestResult {
        return try {
            val streamUrl = camera.url
            val resolution = camera.resolution?.let { "${it.width}x${it.height}" } ?: "unknown"
            CameraTestResult.Success(streamUrl, resolution)
        } catch (e: Exception) {
            CameraTestResult.Failure(e.message ?: "Test failed")
        }
    }

    override suspend fun controlPtz(cameraId: String, direction: PtzDirection, speed: Float): Result<Unit> {
        return Result.success(Unit)
    }
}

// Stub classes для других Bridges
class AndroidRecordingBridge : RecordingBridge {
    override suspend fun startRecording(cameraId: String): Result<Recording> =
        Result.success(Recording(id = "1", cameraId = cameraId, startTime = nowMillis(), duration = 0L))
    override suspend fun stopRecording(recordingId: String): Result<Unit> = Result.success(Unit)
    override suspend fun pauseRecording(recordingId: String): Result<Unit> = Result.success(Unit)
    override suspend fun resumeRecording(recordingId: String): Result<Unit> = Result.success(Unit)
    override suspend fun getRecordings(cameraId: String): List<Recording> = emptyList()
    override suspend fun deleteRecording(recordingId: String): Result<Unit> = Result.success(Unit)
}

class AndroidEventBridge : EventBridge {
    override suspend fun getEvents(cameraId: String): List<Event> = emptyList()
    override suspend fun acknowledgeEvent(eventId: String): Result<Unit> = Result.success(Unit)
    override suspend fun detectMotion(cameraId: String): Result<DetectionResult> = Result.success(DetectionResult(false, emptyList(), 0f))
    override suspend fun detectFaces(cameraId: String): Result<DetectionResult> = Result.success(DetectionResult(false, emptyList(), 0f))
    override suspend fun recognizeLicensePlate(cameraId: String): Result<LicensePlateResult> =
        Result.success(LicensePlateResult(detected = false, plateText = null, confidence = 0f))
}

class AndroidSettingsBridge : SettingsBridge {
    override suspend fun getSettings(): Settings = Settings(
        id = "local",
        category = SettingsCategory.SYSTEM,
        key = "local",
        value = "{}",
        updatedAt = nowMillis()
    )
    override suspend fun updateSetting(key: String, value: String): Result<Unit> = Result.success(Unit)
    override suspend fun updateProfile(cameraId: String, profile: CameraProfile): Result<Unit> = Result.success(Unit)
}

class AndroidNotificationBridge : NotificationBridge {
    override suspend fun getNotifications(): List<Notification> = emptyList()
    override suspend fun markAsRead(notificationId: String): Result<Unit> = Result.success(Unit)
    override suspend fun sendNotification(notification: Notification): Result<Unit> = Result.success(Unit)
}

class AndroidAnalyticsBridge : AnalyticsBridge {
    override suspend fun analyzeVideo(cameraId: String, analysisType: AnalysisType): Result<AnalysisResult> = 
        Result.success(AnalysisResult(true, "", 0))
    override suspend fun trackObjects(cameraId: String, objectTypes: List<ObjectType>): Result<Unit> = Result.success(Unit)
    override suspend fun detectObjects(cameraId: String): Result<DetectionResult> = Result.success(DetectionResult(false, emptyList(), 0f))
}
