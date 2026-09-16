package com.company.ipcamera.server.repository

import com.company.ipcamera.core.common.model.CameraStatus
import com.company.ipcamera.core.common.model.Resolution
import com.company.ipcamera.core.common.security.PasswordEncryption
import com.company.ipcamera.core.common.security.PasswordEncryptionFactory
import com.company.ipcamera.core.network.NetworkScanner
import com.company.ipcamera.shared.database.CameraDatabase
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.model.CameraSettings
import com.company.ipcamera.shared.domain.model.CameraStatistics
import com.company.ipcamera.shared.domain.model.PTZConfig
import com.company.ipcamera.shared.domain.model.StreamConfig
import com.company.ipcamera.shared.domain.repository.CameraRepository
import com.company.ipcamera.shared.domain.repository.CameraCapabilities
import com.company.ipcamera.shared.domain.repository.ConnectionTestResult
import com.company.ipcamera.shared.domain.repository.DiscoveredCamera
import com.company.ipcamera.shared.domain.repository.DiscoveryConfig
import com.company.ipcamera.shared.domain.repository.DiscoveryMethod
import com.company.ipcamera.shared.domain.repository.DiscoveryProgress
import com.company.ipcamera.shared.domain.repository.ErrorCode
import com.company.ipcamera.shared.domain.repository.StreamInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import mu.KotlinLogging
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI

private val logger = KotlinLogging.logger {}

/**
 * Серверная реализация [CameraRepository] поверх общей SQLDelight [CameraDatabase]
 * (таблица camera) — тот же пул HikariCP/PostgreSQL, что и остальные серверные репозитории.
 *
 * Пароли шифруются через [PasswordEncryption] перед записью и расшифровываются при чтении
 * (контракт аналогичен клиентскому мапперу CameraEntityMapper).
 *
 * Discovery делегируется в [NetworkScanner] (jvmMain core:network) — реальное сканирование
 * подсети на RTSP/HTTP-порты. WS-Discovery/UPnP-методы не используются на сервере.
 * Результат сканера (тип core.network.DiscoveredCamera) маппится в shared-модель.
 */
class ServerCameraRepository(
    private val database: CameraDatabase,
    private val passwordEncryption: PasswordEncryption = PasswordEncryptionFactory.create(),
    private val scanner: NetworkScanner? = null,
) : CameraRepository {

    private val cameraQueries get() = database.cameraDatabaseQueries

    // =========================================================================
    // CRUD
    // =========================================================================

    override suspend fun getCameras(): List<Camera> =
        cameraQueries.selectAll().executeAsList().map { toDomain(it) }

    override suspend fun getCameraById(id: String): Camera? =
        cameraQueries.selectById(id).executeAsOneOrNull()?.let { toDomain(it) }

    override suspend fun addCamera(camera: Camera): Result<Camera> = runCatching {
        insert(camera)
        camera
    }.onFailure { logger.error(it) { "addCamera failed for ${camera.id}" } }

    override suspend fun updateCamera(camera: Camera): Result<Camera> = runCatching {
        if (getCameraById(camera.id) == null) {
            throw NoSuchElementException("Camera not found: ${camera.id}")
        }
        update(camera)
        camera
    }.onFailure { logger.error(it) { "updateCamera failed for ${camera.id}" } }

    override suspend fun removeCamera(id: String): Result<Unit> = runCatching {
        cameraQueries.deleteCamera(id)
    }.onFailure { logger.error(it) { "removeCamera failed for $id" } }

    // =========================================================================
    // Discovery
    // =========================================================================

    override suspend fun discoverCameras(forceRefresh: Boolean): List<DiscoveredCamera> {
        val sc = scanner ?: run {
            logger.warn { "NetworkScanner not configured — discovery unavailable" }
            return emptyList()
        }
        return try {
            sc.scanSubnet(defaultSubnet(), ports = defaultPorts())
                .mapNotNull { toSharedDiscovered(it) }
        } catch (e: Exception) {
            logger.error(e) { "Subnet discovery failed: ${e.message}" }
            emptyList()
        }
    }

    override suspend fun discoverCamerasWithProgress(
        forceRefresh: Boolean,
        config: DiscoveryConfig,
    ): Flow<DiscoveryProgress> = flow {
        val sc = scanner
        if (sc == null) {
            logger.warn { "NetworkScanner not configured — discovery unavailable" }
            return@flow
        }
        val startedAt = System.currentTimeMillis()
        emit(progressSnapshot(DiscoveryMethod.SUBNET_SCAN, 0f, 0, null, startedAt, "Starting subnet scan...", emptyList()))
        val found = try {
            sc.scanSubnet(config.subnetRange ?: defaultSubnet(), ports = config.ports)
                .mapNotNull { toSharedDiscovered(it) }
        } catch (e: Exception) {
            logger.error(e) { "Subnet discovery failed: ${e.message}" }
            emptyList()
        }
        emit(progressSnapshot(DiscoveryMethod.SUBNET_SCAN, 1f, found.size, found.size, startedAt, "Scan completed", found))
    }

    // =========================================================================
    // Connection / status
    // =========================================================================

    /**
     * Проверка доступности камеры: TCP-подключение к RTSP-порту (из URL).
     * ONVIF-профилирование (стримы/кодеки) — отдельная задача; здесь честная
     * сетевая проверка достижимости вместо стаба.
     */
    override suspend fun testConnection(camera: Camera): ConnectionTestResult {
        return try {
            val host = camera.url.substringAfter("://").substringBefore(':').substringBefore('/')
            val port = camera.url.substringAfter("://").substringBefore('/').substringAfter(':').toIntOrNull() ?: 554
            val reachable = withContext(Dispatchers.IO) {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), 5000)
                    true
                }
            }
            if (reachable) {
                ConnectionTestResult.Success(
                    streams = listOf(
                        StreamInfo(type = "MAIN", resolution = camera.resolution?.let { "${it.width}x${it.height}" } ?: "unknown", fps = camera.fps, codec = camera.codec)
                    ),
                    capabilities = CameraCapabilities(
                        ptz = camera.ptz?.enabled ?: false,
                        audio = camera.audio,
                        onvif = false,
                        analytics = false,
                    ),
                    latencyMs = null,
                )
            } else {
                ConnectionTestResult.Failure(error = "Port $port not reachable on $host", code = ErrorCode.CONNECTION_FAILED)
            }
        } catch (e: Exception) {
            ConnectionTestResult.Failure(error = e.message ?: "Connection failed", code = ErrorCode.CONNECTION_FAILED)
        }
    }

    override suspend fun getCameraStatus(id: String): CameraStatus =
        getCameraById(id)?.status ?: CameraStatus.UNKNOWN

    /** Обновление статуса камеры (используется CameraService при мониторинге). */
    suspend fun updateStatus(id: String, status: CameraStatus, lastSeen: Long?): Result<Unit> = runCatching {
        cameraQueries.updateCameraStatus(
            status = status.name,
            last_seen = lastSeen,
            updated_at = System.currentTimeMillis(),
            id = id,
        )
    }

    // =========================================================================
    // Mapping (шифрование пароля при записи / расшифровка при чтении)
    // =========================================================================

    private fun toDomain(db: com.company.ipcamera.shared.database.Camera): Camera {
        val decryptedPassword = db.password?.let { stored ->
            try {
                passwordEncryption.decrypt(stored)
            } catch (e: Exception) {
                logger.error(e) { "Failed to decrypt password for camera ${db.id}" }
                null
            }
        }
        return Camera(
            id = db.id,
            name = db.name,
            url = db.url,
            username = db.username,
            password = decryptedPassword,
            model = db.model,
            status = CameraStatus.valueOf(db.status),
            resolution = db.resolution_width?.let { w -> db.resolution_height?.let { h -> Resolution(w.toInt(), h.toInt()) } },
            fps = db.fps.toInt(),
            bitrate = db.bitrate.toInt(),
            codec = db.codec,
            audio = db.audio == 1L,
            ptz = db.ptz_config?.let { json.decodeFromString<PTZConfig>(it) },
            streams = db.streams?.let { json.decodeFromString<List<StreamConfig>>(it) } ?: emptyList(),
            settings = db.settings?.let { json.decodeFromString<CameraSettings>(it) } ?: CameraSettings(),
            statistics = db.statistics?.let { json.decodeFromString<CameraStatistics>(it) },
            createdAt = db.created_at,
            updatedAt = db.updated_at,
            lastSeen = db.last_seen,
        )
    }

    private suspend fun insert(camera: Camera) {
        cameraQueries.insertCamera(
            id = camera.id,
            name = camera.name,
            url = camera.url,
            username = camera.username,
            password = camera.password?.let { encryptIfNeeded(it, camera.id) },
            model = camera.model,
            status = camera.status.name,
            resolution_width = camera.resolution?.width?.toLong(),
            resolution_height = camera.resolution?.height?.toLong(),
            fps = camera.fps.toLong(),
            bitrate = camera.bitrate.toLong(),
            codec = camera.codec,
            audio = if (camera.audio) 1L else 0L,
            ptz_config = camera.ptz?.let { json.encodeToString(it) },
            streams = camera.streams.takeIf { it.isNotEmpty() }?.let { json.encodeToString(it) },
            settings = json.encodeToString(camera.settings),
            statistics = camera.statistics?.let { json.encodeToString(it) },
            created_at = camera.createdAt,
            updated_at = camera.updatedAt,
            last_seen = camera.lastSeen,
        )
    }

    private suspend fun update(camera: Camera) {
        cameraQueries.updateCamera(
            name = camera.name,
            url = camera.url,
            username = camera.username,
            password = camera.password?.let { encryptIfNeeded(it, camera.id) },
            model = camera.model,
            status = camera.status.name,
            resolution_width = camera.resolution?.width?.toLong(),
            resolution_height = camera.resolution?.height?.toLong(),
            fps = camera.fps.toLong(),
            bitrate = camera.bitrate.toLong(),
            codec = camera.codec,
            audio = if (camera.audio) 1L else 0L,
            ptz_config = camera.ptz?.let { json.encodeToString(it) },
            streams = camera.streams.takeIf { it.isNotEmpty() }?.let { json.encodeToString(it) },
            settings = json.encodeToString(camera.settings),
            statistics = camera.statistics?.let { json.encodeToString(it) },
            updated_at = camera.updatedAt,
            last_seen = camera.lastSeen,
            id = camera.id,
        )
    }

    /** Шифрует пароль, если он ещё не зашифрован (повторное шифрование не выполняется). */
    private fun encryptIfNeeded(password: String, cameraId: String): String =
        try {
            if (passwordEncryption.isEncrypted(password)) password else passwordEncryption.encrypt(password)
        } catch (e: Exception) {
            logger.error(e) { "Failed to encrypt password for camera $cameraId" }
            throw e
        }

    private fun defaultSubnet(): String {
        val local = InetAddress.getLocalHost().hostAddress ?: return "192.168.1.0/24"
        val prefix = local.substringBeforeLast('.')
        return "$prefix.0/24"
    }

    private fun defaultPorts(): List<Int> = listOf(554, 80, 8080)

    /** Маппинг сканера (core.network) → shared-модель DiscoveredCamera. */
    private fun toSharedDiscovered(d: com.company.ipcamera.core.network.DiscoveredCamera): DiscoveredCamera? {
        val url = d.url
        val host = url.substringAfter("://").substringBefore(':').substringBefore('/')
        val port = url.substringAfter("://").substringBefore('/').substringAfter(':').toIntOrNull() ?: 554
        return DiscoveredCamera(
            name = d.name ?: host,
            url = url,
            model = d.model,
            manufacturer = d.manufacturer,
            ipAddress = host,
            port = port,
            username = null,
            password = null,
        )
    }

    private fun progressSnapshot(
        method: DiscoveryMethod,
        methodProgress: Float,
        found: Int,
        totalEstimate: Int?,
        startedAt: Long,
        activity: String,
        cameras: List<DiscoveredCamera>,
    ): DiscoveryProgress = DiscoveryProgress(
        currentMethod = method,
        methodProgress = methodProgress,
        overallProgress = methodProgress,
        devicesFoundSoFar = found,
        devicesTotalEstimated = totalEstimate,
        elapsedTimeMs = System.currentTimeMillis() - startedAt,
        remainingTimeMs = null,
        currentActivity = activity,
        discoveredCamerasSnapshot = cameras,
    )

    private companion object {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }
}
