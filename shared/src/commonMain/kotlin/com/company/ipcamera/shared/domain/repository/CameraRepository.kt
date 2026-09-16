package com.company.ipcamera.shared.domain.repository

import com.company.ipcamera.core.common.model.CameraStatus
import com.company.ipcamera.shared.domain.model.Camera

/**
 * Репозиторий для работы с камерами
 */
interface CameraRepository {
    /**
     * Получить все камеры
     */
    suspend fun getCameras(): List<Camera>

    /**
     * Получить камеру по ID
     */
    suspend fun getCameraById(id: String): Camera?

    /**
     * Добавить новую камеру
     */
    suspend fun addCamera(camera: Camera): Result<Camera>

    /**
     * Обновить камеру
     */
    suspend fun updateCamera(camera: Camera): Result<Camera>

    /**
     * Удалить камеру
     */
    suspend fun removeCamera(id: String): Result<Unit>

/**
     * Обнаружить камеры в сети.
     * @param forceRefresh если true, игнорировать кэш и выполнить новое обнаружение (для явного действия «обновить список»).
     */
    suspend fun discoverCameras(forceRefresh: Boolean = false): List<DiscoveredCamera>

/**
     * Обнаружить камеры с прогресс-индикатором
     * @param forceRefresh если true, игнорировать кэш
     * @param config конфигурация обнаружения
     */
    suspend fun discoverCamerasWithProgress(
        forceRefresh: Boolean = false,
        config: DiscoveryConfig = DiscoveryConfig(),
    ): kotlinx.coroutines.flow.Flow<DiscoveryProgress>

    /**
     * Проверить подключение к камере
     */
    suspend fun testConnection(camera: Camera): ConnectionTestResult

    /**
     * Получить статус камеры
     */
    suspend fun getCameraStatus(id: String): CameraStatus
}

/**
 * Обнаруженная камера в сети
 */
data class DiscoveredCamera(
    val name: String,
    val url: String,
    val model: String? = null,
    val manufacturer: String? = null,
    val ipAddress: String,
    val port: Int = 554,
    val username: String? = null,
    val password: String? = null,
)

/**
 * Результат проверки подключения
 */
sealed class ConnectionTestResult {
    data class Success(
        val streams: List<StreamInfo>,
        val capabilities: CameraCapabilities,
        // Расширенные поля для детальной диагностики
        val onvifVersion: String? = null,
        val rtspVersion: String? = null,
        val latencyMs: Long? = null,
        val supportedCodecs: List<String> = emptyList(),
        val authenticationMethod: String? = null,
        val connectionQuality: Float? = null,
    ) : ConnectionTestResult()

    data class Failure(
        val error: String,
        val code: ErrorCode,
        // Расширенная диагностика ошибок
        val diagnostics: ConnectionDiagnostics? = null,
    ) : ConnectionTestResult()
}

data class StreamInfo(
    val type: String,
    val resolution: String,
    val fps: Int,
    val codec: String,
    // Дополнительные параметры потока
    val bitrate: Int? = null,
    val audioCodec: String? = null,
)

data class CameraCapabilities(
    val ptz: Boolean = false,
    val audio: Boolean = false,
    val onvif: Boolean = false,
    val analytics: Boolean = false,
    // Расширенные возможности
    val motionDetection: Boolean = false,
    val objectDetection: Boolean = false,
    val maxVideoStreams: Int = 0,
    val maxAudioStreams: Int = 0,
    val supportedResolutions: List<String> = emptyList(),
    val supportedCodecs: List<String> = emptyList(),
)

/**
 * Диагностика подключения
 */
data class ConnectionDiagnostics(
    val networkReachable: Boolean = false,
    val portOpen: Boolean = false,
    val authenticationSupported: Boolean = false,
    val onvifServiceAvailable: Boolean = false,
    val rtspServiceAvailable: Boolean = false,
    val sslCertificateValid: Boolean? = null,
    val bandwidthMbs: Float? = null,
    val recommendedSettings: ConnectionRecommendances? = null,
)

/**
 * Рекомендации по подключению
 */
data class ConnectionRecommendances(
    val suggestedTransportProtocol: String = "TCP",
    val suggestedResolution: String = "1080p",
    val suggestedCodec: String? = null,
    val bandwidthLimitation: Boolean = false,
)

enum class ErrorCode {
    CONNECTION_FAILED,
    AUTHENTICATION_FAILED,
    UNSUPPORTED_FORMAT,
    TIMEOUT,
    UNKNOWN,
}

/**
 * Методы обнаружения камер
 */
enum class DiscoveryMethod {
    WS_DISCOVERY, // ONVIF WS-Discovery (multicast)
    UPNP_DISCOVERY, // UPnP Device Discovery
    SUBNET_SCAN, // Сканирование подсети
    MANUAL_INPUT, // Ручной ввод адреса
    DNS_SD, // DNS Service Discovery (Bonjour/Avahi)
}

/**
 * Конфигурация обнаружения камер
 */
data class DiscoveryConfig(
    val methods: List<DiscoveryMethod> =
        listOf(
            DiscoveryMethod.WS_DISCOVERY,
            DiscoveryMethod.UPNP_DISCOVERY,
        ),
    val subnetRange: String? = null, // "192.168.1.0/24"
    val ports: List<Int> = listOf(554, 80, 8080),
    val timeoutPerMethod: Long = 5000,
    val parallelDiscovery: Boolean = true,
    val cacheEnabled: Boolean = true,
    val cacheTtlMs: Long = 7 * 60 * 1000,
)

/**
 * Прогресс обнаружения камер
 */
data class DiscoveryProgress(
    val currentMethod: DiscoveryMethod,
    val methodProgress: Float, // 0.0-1.0 для текущего метода
    val overallProgress: Float, // 0.0-1.0 общее
    val devicesFoundSoFar: Int,
    val devicesTotalEstimated: Int?, // Приблизительное общее количество
    val elapsedTimeMs: Long,
    val remainingTimeMs: Long?, // Оценочное оставшееся время
    val currentActivity: String, // "Scanning subnet...", "Waiting for responses..."
    val discoveredCamerasSnapshot: List<DiscoveredCamera>,
)
