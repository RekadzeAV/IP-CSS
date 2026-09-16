package com.company.ipcamera.core.uibridge

import com.company.ipcamera.shared.domain.usecase.*

/**
 * Фабрика для создания UiBridge
 * Возвращает платформо-специфичную реализацию (без внедрённых Use Cases).
 */
expect fun createUiBridge(): UiBridge

/**
 * Платформо-специфичная фабрика с внедрением Use Cases.
 * Используется фабричной функцией [createUiBridge].
 */
expect fun createUiBridgeWithDependencies(
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
): UiBridge

/**
 * Создает UiBridge с кастомными Use Cases.
 */
fun createUiBridge(
    loginUseCase: LoginUseCase? = null,
    getCamerasUseCase: GetCamerasUseCase? = null,
    addCameraUseCase: AddCameraUseCase? = null,
    updateCameraUseCase: UpdateCameraUseCase? = null,
    deleteCameraUseCase: DeleteCameraUseCase? = null,
    discoverCamerasUseCase: DiscoverCamerasUseCase? = null,
    testDiscoveredCameraUseCase: TestDiscoveredCameraUseCase? = null,
    controlPtzUseCase: ControlPtzUseCase? = null,
    startRecordingUseCase: StartRecordingUseCase? = null,
    stopRecordingUseCase: StopRecordingUseCase? = null,
    pauseRecordingUseCase: PauseRecordingUseCase? = null,
    resumeRecordingUseCase: ResumeRecordingUseCase? = null,
    getRecordingsUseCase: GetRecordingsUseCase? = null,
    deleteRecordingUseCase: DeleteRecordingUseCase? = null,
    getEventsUseCase: GetEventsUseCase? = null,
    acknowledgeEventUseCase: AcknowledgeEventUseCase? = null,
    detectMotionUseCase: DetectMotionUseCase? = null,
    detectFacesUseCase: DetectFacesUseCase? = null,
    recognizeLicensePlateUseCase: RecognizeLicensePlateUseCase? = null,
    getSettingsUseCase: GetSettingsUseCase? = null,
    updateSettingUseCase: UpdateSettingUseCase? = null,
    getNotificationsUseCase: GetNotificationsUseCase? = null,
    markNotificationAsReadUseCase: MarkNotificationAsReadUseCase? = null,
    sendNotificationUseCase: SendNotificationUseCase? = null,
    analyzeVideoUseCase: AnalyzeVideoUseCase? = null,
    trackObjectsUseCase: TrackObjectsUseCase? = null,
    detectObjectsUseCase: DetectObjectsUseCase? = null
): UiBridge {
    return createUiBridgeWithDependencies(
        loginUseCase = loginUseCase,
        logoutUseCase = null,
        registerUseCase = null,
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
