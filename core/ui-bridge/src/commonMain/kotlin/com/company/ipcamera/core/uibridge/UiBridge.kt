package com.company.ipcamera.core.uibridge

import com.company.ipcamera.core.common.security.PasswordEncryption
import com.company.ipcamera.shared.domain.model.*
import com.company.ipcamera.shared.domain.repository.DiscoveredCamera
import com.company.ipcamera.shared.domain.usecase.*

/**
 * Основной интерфейс UI Bridge
 * Служит мостом между UI и бизнес-логикой (Use Cases)
 */
expect interface UiBridge {
    val authenticationBridge: AuthenticationBridge
    val cameraBridge: CameraBridge
    val recordingBridge: RecordingBridge
    val eventBridge: EventBridge
    val settingsBridge: SettingsBridge
    val notificationBridge: NotificationBridge
    val analyticsBridge: AnalyticsBridge
}

/**
 * Authentication Bridge - управление аутентификацией
 */
interface AuthenticationBridge {
    /**
     * Логин пользователя
     */
    suspend fun login(username: String, password: String): LoginResult

    /**
     * Выход из системы
     */
    suspend fun logout()

    /**
     * Проверка авторизации
     */
    suspend fun isAuthorized(): Boolean

    /**
     * Получение текущего пользователя
     */
    suspend fun getCurrentUser(): User?

    /**
     * Регистрация нового пользователя
     */
    suspend fun register(username: String, password: String, email: String): RegistrationResult
}

/**
 * Camera Bridge - управление камерами
 */
interface CameraBridge {
    /**
     * Получение списка камер
     */
    suspend fun getCameras(): List<Camera>

    /**
     * Получение камеры по ID
     */
    suspend fun getCameraById(cameraId: String): Camera?

    /**
     * Добавление новой камеры
     */
    suspend fun addCamera(camera: Camera): Result<Camera>

    /**
     * Обновление камеры
     */
    suspend fun updateCamera(camera: Camera): Result<Unit>

    /**
     * Удаление камеры
     */
    suspend fun deleteCamera(cameraId: String): Result<Unit>

    /**
     * Поиск камер в сети
     */
    suspend fun discoverCameras(): List<DiscoveredCamera>

    /**
     * Протестировать камеру
     */
    suspend fun testCamera(camera: Camera): CameraTestResult

    /**
     * Управление PTZ
     */
    suspend fun controlPtz(cameraId: String, direction: PtzDirection, speed: Float): Result<Unit>
}

/**
 * Recording Bridge - управление записью
 */
interface RecordingBridge {
    /**
     * Начать запись
     */
    suspend fun startRecording(cameraId: String): Result<Recording>

    /**
     * Остановить запись
     */
    suspend fun stopRecording(recordingId: String): Result<Unit>

    /**
     * Пауза записи
     */
    suspend fun pauseRecording(recordingId: String): Result<Unit>

    /**
     * Возобновить запись
     */
    suspend fun resumeRecording(recordingId: String): Result<Unit>

    /**
     * Получение списка записей
     */
    suspend fun getRecordings(cameraId: String): List<Recording>

    /**
     * Удаление записи
     */
    suspend fun deleteRecording(recordingId: String): Result<Unit>
}

/**
 * Event Bridge - управление событиями
 */
interface EventBridge {
    /**
     * Получение списка событий
     */
    suspend fun getEvents(cameraId: String): List<Event>

    /**
     * Подтверждение события
     */
    suspend fun acknowledgeEvent(eventId: String): Result<Unit>

    /**
     * Обнаружение движения
     */
    suspend fun detectMotion(cameraId: String): Result<DetectionResult>

    /**
     * Обнаружение лиц
     */
    suspend fun detectFaces(cameraId: String): Result<DetectionResult>

    /**
     * Распознавание номеров
     */
    suspend fun recognizeLicensePlate(cameraId: String): Result<LicensePlateResult>
}

/**
 * Settings Bridge - управление настройками
 */
interface SettingsBridge {
    /**
     * Получение настроек
     */
    suspend fun getSettings(): Settings

    /**
     * Обновление настройки
     */
    suspend fun updateSetting(key: String, value: String): Result<Unit>

    /**
     * Обновление профиля камеры
     */
    suspend fun updateProfile(cameraId: String, profile: CameraProfile): Result<Unit>
}

/**
 * Notification Bridge - управление уведомлениями
 */
interface NotificationBridge {
    /**
     * Получение уведомлений
     */
    suspend fun getNotifications(): List<Notification>

    /**
     * Отметить уведомление как прочитанное
     */
    suspend fun markAsRead(notificationId: String): Result<Unit>

    /**
     * Отправить уведомление
     */
    suspend fun sendNotification(notification: Notification): Result<Unit>
}

/**
 * Analytics Bridge - управление аналитикой
 */
interface AnalyticsBridge {
    /**
     * Анализ видео
     */
    suspend fun analyzeVideo(cameraId: String, analysisType: AnalysisType): Result<AnalysisResult>

    /**
     * Отслеживание объектов
     */
    suspend fun trackObjects(cameraId: String, objectTypes: List<ObjectType>): Result<Unit>

    /**
     * Обнаружение объектов
     */
    suspend fun detectObjects(cameraId: String): Result<DetectionResult>
}

/**
 * Результат логина
 */
sealed class LoginResult {
    data class Success(val user: User) : LoginResult()
    data class Failure(val error: String) : LoginResult()
}

/**
 * Результат регистрации
 */
sealed class RegistrationResult {
    data class Success(val user: User) : RegistrationResult()
    data class Failure(val error: String) : RegistrationResult()
}

/**
 * Результат теста камеры
 */
sealed class CameraTestResult {
    data class Success(val streamUrl: String, val resolution: String) : CameraTestResult()
    data class Failure(val error: String) : CameraTestResult()
}

/**
 * Результат распознавания лицензионной пластины
 */
data class LicensePlateResult(
    val detected: Boolean,
    val plateText: String?,
    val confidence: Float
)

/**
 * Результат обнаружения
 */
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

enum class ObjectType {
    PERSON,
    VEHICLE,
    ANIMAL,
    LICENSE_PLATE
}

data class Rect(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

/**
 * Результат анализа
 */
data class AnalysisResult(
    val success: Boolean,
    val analysisData: String,
    val processingTimeMs: Long
)

enum class AnalysisType {
    MOTION,
    FACE,
    OBJECT,
    LICENSE_PLATE
}

/**
 * Направление PTZ
 */
enum class PtzDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT,
    ZOOM_IN,
    ZOOM_OUT
}

/**
 * Профиль камеры
 */
data class CameraProfile(
    val name: String,
    val resolution: String,
    val fps: Int,
    val bitrate: Int
)
