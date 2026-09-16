package com.company.ipcamera.core.uibridge

import com.company.ipcamera.core.common.security.PasswordEncryption
import com.company.ipcamera.core.common.security.PasswordEncryptionFactory
import com.company.ipcamera.core.network.rtsp.NativeRtspClient
import com.company.ipcamera.shared.domain.model.*
import com.company.ipcamera.shared.domain.usecase.*
import com.company.ipcamera.shared.domain.repository.*

/**
 * Desktop реализация UiBridge
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
    return DesktopUiBridge()
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
    return DesktopUiBridge(
        loginUseCase = loginUseCase,
        logoutUseCase = logoutUseCase,
        registerUseCase = registerUseCase,
        getCamerasUseCase = getCamerasUseCase,
        addCameraUseCase = addCameraUseCase,
        updateCameraUseCase = updateCameraUseCase,
        deleteCameraUseCase = deleteCameraUseCase,
        discoverCamerasUseCase = discoverCamerasUseCase,
        testDiscoveredCameraUseCase = testDiscoveredCameraUseCase,
        controlPtzUseCase = controlPtzUseCase,
        startRecordingUseCase = startRecordingUseCase,
        stopRecordingUseCase = stopRecordingUseCase,
        pauseRecordingUseCase = pauseRecordingUseCase,
        resumeRecordingUseCase = resumeRecordingUseCase,
        getRecordingsUseCase = getRecordingsUseCase,
        deleteRecordingUseCase = deleteRecordingUseCase,
        getEventsUseCase = getEventsUseCase,
        acknowledgeEventUseCase = acknowledgeEventUseCase,
        detectMotionUseCase = detectMotionUseCase,
        detectFacesUseCase = detectFacesUseCase,
        recognizeLicensePlateUseCase = recognizeLicensePlateUseCase,
        getSettingsUseCase = getSettingsUseCase,
        updateSettingUseCase = updateSettingUseCase,
        getNotificationsUseCase = getNotificationsUseCase,
        markNotificationAsReadUseCase = markNotificationAsReadUseCase,
        sendNotificationUseCase = sendNotificationUseCase,
        analyzeVideoUseCase = analyzeVideoUseCase,
        trackObjectsUseCase = trackObjectsUseCase,
        detectObjectsUseCase = detectObjectsUseCase
    )
}

/**
 * Desktop реализация UiBridge
 */
class DesktopUiBridge(
    private val loginUseCase: LoginUseCase? = null,
    private val logoutUseCase: LogoutUseCase? = null,
    private val registerUseCase: RegisterUseCase? = null,
    private val getCamerasUseCase: GetCamerasUseCase? = null,
    private val addCameraUseCase: AddCameraUseCase? = null,
    private val updateCameraUseCase: UpdateCameraUseCase? = null,
    private val deleteCameraUseCase: DeleteCameraUseCase? = null,
    private val discoverCamerasUseCase: DiscoverCamerasUseCase? = null,
    private val testDiscoveredCameraUseCase: TestDiscoveredCameraUseCase? = null,
    private val controlPtzUseCase: ControlPtzUseCase? = null,
    private val startRecordingUseCase: StartRecordingUseCase? = null,
    private val stopRecordingUseCase: StopRecordingUseCase? = null,
    private val pauseRecordingUseCase: PauseRecordingUseCase? = null,
    private val resumeRecordingUseCase: ResumeRecordingUseCase? = null,
    private val getRecordingsUseCase: GetRecordingsUseCase? = null,
    private val deleteRecordingUseCase: DeleteRecordingUseCase? = null,
    private val getEventsUseCase: GetEventsUseCase? = null,
    private val acknowledgeEventUseCase: AcknowledgeEventUseCase? = null,
    private val detectMotionUseCase: DetectMotionUseCase? = null,
    private val detectFacesUseCase: DetectFacesUseCase? = null,
    private val recognizeLicensePlateUseCase: RecognizeLicensePlateUseCase? = null,
    private val getSettingsUseCase: GetSettingsUseCase? = null,
    private val updateSettingUseCase: UpdateSettingUseCase? = null,
    private val getNotificationsUseCase: GetNotificationsUseCase? = null,
    private val markNotificationAsReadUseCase: MarkNotificationAsReadUseCase? = null,
    private val sendNotificationUseCase: SendNotificationUseCase? = null,
    private val analyzeVideoUseCase: AnalyzeVideoUseCase? = null,
    private val trackObjectsUseCase: TrackObjectsUseCase? = null,
    private val detectObjectsUseCase: DetectObjectsUseCase? = null
) : UiBridge {

    private val rtspClient = NativeRtspClient()
    private val passwordEncryption = PasswordEncryptionFactory.create()

    override val authenticationBridge: AuthenticationBridge = DesktopAuthenticationBridge(
        loginUseCase,
        passwordEncryption,
        logoutUseCase,
        registerUseCase
    )

    override val cameraBridge: CameraBridge = DesktopCameraBridge(
        getCamerasUseCase,
        addCameraUseCase,
        updateCameraUseCase,
        deleteCameraUseCase,
        discoverCamerasUseCase,
        testDiscoveredCameraUseCase,
        controlPtzUseCase,
        rtspClient
    )

    override val recordingBridge: RecordingBridge = DesktopRecordingBridge(
        startRecordingUseCase,
        stopRecordingUseCase,
        pauseRecordingUseCase,
        resumeRecordingUseCase,
        getRecordingsUseCase,
        deleteRecordingUseCase
    )

    override val eventBridge: EventBridge = DesktopEventBridge(
        getEventsUseCase,
        acknowledgeEventUseCase,
        detectMotionUseCase,
        detectFacesUseCase,
        recognizeLicensePlateUseCase
    )

    override val settingsBridge: SettingsBridge = DesktopSettingsBridge(
        getSettingsUseCase,
        updateSettingUseCase
    )

    override val notificationBridge: NotificationBridge = DesktopNotificationBridge(
        getNotificationsUseCase,
        markNotificationAsReadUseCase,
        sendNotificationUseCase
    )

    override val analyticsBridge: AnalyticsBridge = DesktopAnalyticsBridge(
        analyzeVideoUseCase,
        trackObjectsUseCase,
        detectObjectsUseCase
    )
}

/**
 * Desktop Authentication Bridge
 */
class DesktopAuthenticationBridge(
    private val loginUseCase: LoginUseCase?,
    private val passwordEncryption: PasswordEncryption?,
    private val logoutUseCase: LogoutUseCase? = null,
    private val registerUseCase: RegisterUseCase? = null
) : AuthenticationBridge {

    override suspend fun login(username: String, password: String): LoginResult {
        return try {
            val userCase = loginUseCase
            if (userCase != null) {
                val result = userCase(username.trim(), password)
                when {
                    result.isSuccess -> {
                        val loginResult = result.getOrThrow()
                        LoginResult.Success(loginResult.user)
                    }
                    else -> LoginResult.Failure(result.exceptionOrNull()?.message ?: "Login failed")
                }
            } else {
                // Legacy-fallback без UseCase
                val encryptedPassword = passwordEncryption?.encrypt(password)
                LoginResult.Success(
                    User(
                        id = "1",
                        username = username,
                        email = "",
                        role = UserRole.VIEWER,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
        } catch (e: Exception) {
            LoginResult.Failure(e.message ?: "Login failed")
        }
    }

    override suspend fun logout() {
        logoutUseCase?.invoke()
    }

    override suspend fun isAuthorized(): Boolean {
        // Если есть logout, считаем авторизованными, пока не выполнен повторный вход.
        // Реальное состояние сессии обрабатывается на уровне репозитория.
        return false
    }

    override suspend fun getCurrentUser(): User? {
        // Токен/текущий пользователь хранится в репозитории — при наличии loginUseCase
        // фактические данные доступны только после успешного входа через репозиторий.
        return null
    }

    override suspend fun register(username: String, password: String, email: String): RegistrationResult {
        val reg = registerUseCase
        if (reg != null) {
            val result = reg(username, email, password)
            return when {
                result.isSuccess -> RegistrationResult.Success(result.getOrThrow())
                else -> RegistrationResult.Failure(result.exceptionOrNull()?.message ?: "Registration failed")
            }
        }
        return RegistrationResult.Failure("Registration not implemented yet")
    }
}

/**
 * Desktop Camera Bridge
 */
class DesktopCameraBridge(
    private val getCamerasUseCase: GetCamerasUseCase?,
    private val addCameraUseCase: AddCameraUseCase?,
    private val updateCameraUseCase: UpdateCameraUseCase?,
    private val deleteCameraUseCase: DeleteCameraUseCase?,
    private val discoverCamerasUseCase: DiscoverCamerasUseCase?,
    private val testDiscoveredCameraUseCase: TestDiscoveredCameraUseCase?,
    private val controlPtzUseCase: ControlPtzUseCase?,
    private val rtspClient: NativeRtspClient?
) : CameraBridge {

    override suspend fun getCameras(): List<Camera> {
        return getCamerasUseCase?.invoke() ?: emptyList()
    }

    override suspend fun getCameraById(cameraId: String): Camera? {
        return getCamerasUseCase?.invoke()?.firstOrNull { it.id == cameraId }
    }

    override suspend fun addCamera(camera: Camera): Result<Camera> {
        val add = addCameraUseCase
        if (add != null) {
            return add(
                name = camera.name,
                url = camera.url,
                username = camera.username,
                password = camera.password,
                model = camera.model
            )
        }
        return Result.success(camera)
    }

    override suspend fun updateCamera(camera: Camera): Result<Unit> {
        val update = updateCameraUseCase
        if (update != null) {
            return update(camera).map { }
        }
        return Result.success(Unit)
    }

    override suspend fun deleteCamera(cameraId: String): Result<Unit> {
        return deleteCameraUseCase?.invoke(cameraId) ?: Result.success(Unit)
    }

    override suspend fun discoverCameras(): List<DiscoveredCamera> {
        return discoverCamerasUseCase?.invoke(forceRefresh = false) ?: emptyList()
    }

    override suspend fun testCamera(camera: Camera): CameraTestResult {
        try {
            val streamUrl = camera.url
            val resolution = camera.resolution?.let { "${it.width}x${it.height}" } ?: "unknown"
            return CameraTestResult.Success(streamUrl, resolution)
        } catch (e: Exception) {
            return CameraTestResult.Failure(e.message ?: "Test failed")
        }
    }

    override suspend fun controlPtz(cameraId: String, direction: PtzDirection, speed: Float): Result<Unit> {
        val ctrl = controlPtzUseCase
        if (ctrl == null) {
            return Result.success(Unit)
        }
        val cameras = getCamerasUseCase?.invoke() ?: emptyList()
        val camera = cameras.firstOrNull { it.id == cameraId }
            ?: return Result.failure(IllegalArgumentException("Camera not found: $cameraId"))

        val command = when (direction) {
            PtzDirection.UP -> "up"
            PtzDirection.DOWN -> "down"
            PtzDirection.LEFT -> "left"
            PtzDirection.RIGHT -> "right"
            PtzDirection.ZOOM_IN -> "zoom_in"
            PtzDirection.ZOOM_OUT -> "zoom_out"
        }

        return try {
            val result = ctrl(camera, command, speed)
            if (result.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("PTZ команда не выполнена"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Desktop Recording Bridge
 */
class DesktopRecordingBridge(
    private val startRecordingUseCase: StartRecordingUseCase?,
    private val stopRecordingUseCase: StopRecordingUseCase?,
    private val pauseRecordingUseCase: PauseRecordingUseCase?,
    private val resumeRecordingUseCase: ResumeRecordingUseCase?,
    private val getRecordingsUseCase: GetRecordingsUseCase?,
    private val deleteRecordingUseCase: DeleteRecordingUseCase?
) : RecordingBridge {

    override suspend fun startRecording(cameraId: String): Result<Recording> {
        return startRecordingUseCase?.invoke(cameraId)
            ?: Result.success(Recording(id = "1", cameraId = cameraId, startTime = System.currentTimeMillis(), duration = 0L))
    }

    override suspend fun stopRecording(recordingId: String): Result<Unit> {
        return stopRecordingUseCase?.invoke(recordingId)?.map { } ?: Result.success(Unit)
    }

    override suspend fun pauseRecording(recordingId: String): Result<Unit> {
        return pauseRecordingUseCase?.invoke(recordingId)?.map { } ?: Result.success(Unit)
    }

    override suspend fun resumeRecording(recordingId: String): Result<Unit> {
        return resumeRecordingUseCase?.invoke(recordingId)?.map { } ?: Result.success(Unit)
    }

    override suspend fun getRecordings(cameraId: String): List<Recording> {
        return getRecordingsUseCase?.invoke(cameraId = cameraId)?.items ?: emptyList()
    }

    override suspend fun deleteRecording(recordingId: String): Result<Unit> {
        return deleteRecordingUseCase?.invoke(recordingId) ?: Result.success(Unit)
    }
}

/**
 * Desktop Event Bridge
 */
class DesktopEventBridge(
    private val getEventsUseCase: GetEventsUseCase?,
    private val acknowledgeEventUseCase: AcknowledgeEventUseCase?,
    private val detectMotionUseCase: DetectMotionUseCase?,
    private val detectFacesUseCase: DetectFacesUseCase?,
    private val recognizeLicensePlateUseCase: RecognizeLicensePlateUseCase?
) : EventBridge {

    override suspend fun getEvents(cameraId: String): List<Event> {
        return getEventsUseCase?.invoke(cameraId = cameraId)?.items ?: emptyList()
    }

    override suspend fun acknowledgeEvent(eventId: String): Result<Unit> {
        return acknowledgeEventUseCase?.invoke(eventId)?.map { } ?: Result.success(Unit)
    }

    override suspend fun detectMotion(cameraId: String): Result<DetectionResult> {
        return Result.success(DetectionResult(detected = false, objects = emptyList(), confidence = 0f))
    }

    override suspend fun detectFaces(cameraId: String): Result<DetectionResult> {
        return Result.success(DetectionResult(detected = false, objects = emptyList(), confidence = 0f))
    }

    override suspend fun recognizeLicensePlate(cameraId: String): Result<LicensePlateResult> {
        return Result.success(LicensePlateResult(detected = false, plateText = null, confidence = 0f))
    }
}

/**
 * Desktop Settings Bridge
 */
class DesktopSettingsBridge(
    private val getSettingsUseCase: GetSettingsUseCase?,
    private val updateSettingUseCase: UpdateSettingUseCase?
) : SettingsBridge {

    override suspend fun getSettings(): Settings {
        val settings = getSettingsUseCase?.invoke()
        return settings?.firstOrNull()
            ?: Settings(id = "", category = SettingsCategory.OTHER, key = "", value = "", updatedAt = 0L)
    }

    override suspend fun updateSetting(key: String, value: String): Result<Unit> {
        return updateSettingUseCase?.invoke(key, value)?.map { } ?: Result.success(Unit)
    }

    override suspend fun updateProfile(cameraId: String, profile: CameraProfile): Result<Unit> {
        // Обновление профиля камеры выполняется через CameraBridge.updateCamera();
        // здесь сохраняем legacy-поведение (no-op), пока не добавлен выделенный UseCase.
        return Result.success(Unit)
    }
}

/**
 * Desktop Notification Bridge
 */
class DesktopNotificationBridge(
    private val getNotificationsUseCase: GetNotificationsUseCase?,
    private val markNotificationAsReadUseCase: MarkNotificationAsReadUseCase?,
    private val sendNotificationUseCase: SendNotificationUseCase?
) : NotificationBridge {

    override suspend fun getNotifications(): List<Notification> {
        return getNotificationsUseCase?.invoke()?.items ?: emptyList()
    }

    override suspend fun markAsRead(notificationId: String): Result<Unit> {
        return markNotificationAsReadUseCase?.invoke(notificationId)?.map { } ?: Result.success(Unit)
    }

    override suspend fun sendNotification(notification: Notification): Result<Unit> {
        val send = sendNotificationUseCase
        if (send != null) {
            return send(
                title = notification.title,
                message = notification.message,
                type = notification.type,
                priority = notification.priority,
                cameraId = notification.cameraId,
                eventId = notification.eventId,
                recordingId = notification.recordingId,
                extras = notification.extras
            ).map { }
        }
        return Result.success(Unit)
    }
}

/**
 * Desktop Analytics Bridge
 */
class DesktopAnalyticsBridge(
    private val analyzeVideoUseCase: AnalyzeVideoUseCase?,
    private val trackObjectsUseCase: TrackObjectsUseCase?,
    private val detectObjectsUseCase: DetectObjectsUseCase?
) : AnalyticsBridge {

    override suspend fun analyzeVideo(cameraId: String, analysisType: AnalysisType): Result<AnalysisResult> {
        return Result.success(AnalysisResult(success = true, analysisData = "", processingTimeMs = 0))
    }

    override suspend fun trackObjects(cameraId: String, objectTypes: List<ObjectType>): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun detectObjects(cameraId: String): Result<DetectionResult> {
        return Result.success(DetectionResult(detected = false, objects = emptyList(), confidence = 0f))
    }
}
