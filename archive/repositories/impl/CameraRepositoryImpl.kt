package com.company.ipcamera.shared.data.repository

import com.company.ipcamera.core.common.model.CameraStatus
import com.company.ipcamera.core.common.security.InputValidator
import com.company.ipcamera.core.network.NetworkScanner
import com.company.ipcamera.core.network.OnvifClient
import com.company.ipcamera.core.network.RtspClient
import com.company.ipcamera.core.network.RtspClientConfig
import com.company.ipcamera.core.network.RtspClientStatus
import com.company.ipcamera.shared.common.createHttpClientEngine
import com.company.ipcamera.shared.data.local.CameraEntityMapper
import com.company.ipcamera.shared.data.local.DatabaseFactory
import com.company.ipcamera.shared.data.local.createDatabaseSync
import com.company.ipcamera.shared.data.local.isPostgresFlywayParityDriver
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.repository.*
import io.ktor.client.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import mu.KotlinLogging
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

/**
 * Результат тестирования RTSP подключения
 */
private sealed class RtspConnectionResult {
    abstract val connected: Boolean
    abstract val latencyMs: Long
    abstract val firstFrameReceived: Boolean

    data class Success(
        override val connected: Boolean = true,
        override val latencyMs: Long = 0L,
        override val firstFrameReceived: Boolean = false,
    ) : RtspConnectionResult()

    data class Failure(
        val error: String,
        val code: ErrorCode,
        override val connected: Boolean = false,
        override val latencyMs: Long = 0L,
        override val firstFrameReceived: Boolean = false,
    ) : RtspConnectionResult()
}

/**
 * Реализация CameraRepository с использованием SQLDelight
 * Включает in-memory кэширование для оптимизации производительности
 */
class CameraRepositoryImpl(
    private val databaseFactory: DatabaseFactory,
) : CameraRepository {
    private val driver = databaseFactory.createDriver()
    private val database = createDatabaseSync(driver)
    private val mapper = CameraEntityMapper.forProduction()

    // HTTP клиент для NetworkScanner и других операций
    private val httpClient by lazy { HttpClient(createHttpClientEngine()) }
    private val networkScanner by lazy { NetworkScanner(httpClient, discoveryTimeout = 5000) }

    // Кэш для списка всех камер
    private val allCamerasCacheKey = "all_cameras"

    // Кэш для отдельных камер по ID
    private val cameraCache = CameraCache(maxSize = 1000, expirationTime = 5.minutes)

    // Кэш для результатов обнаружения камер (TTL: 7 минут)
    private val discoveryCache = CameraCache(maxSize = 10, expirationTime = 7.minutes)
    private val discoveryCacheKey = "discovered_cameras"

    // Кэш для статусов камер (TTL: 20 секунд для быстрого обновления)
    private val statusCache = CameraCache(maxSize = 1000, expirationTime = 20.seconds)

    override suspend fun getCameras(): List<Camera> =
        withContext(Dispatchers.Default) {
            try {
                // Проверяем кэш
                val cached = cameraCache.get<List<Camera>>(allCamerasCacheKey)
                if (cached != null) {
                    logger.debug { "Cache hit for all cameras" }
                    return@withContext cached
                }

                // Если нет в кэше, загружаем из БД
                val cameras = database.cameraDatabaseQueries.selectAll().executeAsList().map { mapper.toDomain(it) }

                // Сохраняем в кэш
                cameraCache.put(allCamerasCacheKey, cameras)
                logger.debug { "Loaded ${cameras.size} cameras from database and cached" }

                cameras
            } catch (e: Exception) {
                logger.error(e) { "Error getting cameras" }
                emptyList()
            }
        }

    override suspend fun getCameraById(id: String): Camera? =
        withContext(Dispatchers.Default) {
            try {
                // Проверяем кэш
                val cached = cameraCache.get<Camera>(id)
                if (cached != null) {
                    logger.debug { "Cache hit for camera: $id" }
                    return@withContext cached
                }

                // Если нет в кэше, загружаем из БД
                val camera =
                    database.cameraDatabaseQueries.selectById(
                        id,
                    ).executeAsOneOrNull()?.let { mapper.toDomain(it) }

                // Сохраняем в кэш, если камера найдена
                camera?.let { cameraCache.put(id, it) }

                camera
            } catch (e: Exception) {
                logger.error(e) { "Error getting camera by id: $id" }
                null
            }
        }

    override suspend fun addCamera(camera: Camera): Result<Camera> =
        withContext(Dispatchers.Default) {
            try {
                // Валидация входных данных
                val urlValidation = InputValidator.validateCameraUrl(camera.url)
                if (urlValidation is com.company.ipcamera.core.common.security.ValidationResult.Error) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Некорректный URL: ${urlValidation.message}"),
                    )
                }

                val nameValidation = InputValidator.validateCameraName(camera.name)
                if (nameValidation is com.company.ipcamera.core.common.security.ValidationResult.Error) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Некорректное имя: ${nameValidation.message}"),
                    )
                }

                val usernameValidation = InputValidator.validateUsername(camera.username)
                if (usernameValidation is com.company.ipcamera.core.common.security.ValidationResult.Error) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Некорректное имя пользователя: ${usernameValidation.message}"),
                    )
                }

                val passwordValidation = InputValidator.validatePassword(camera.password)
                if (passwordValidation is com.company.ipcamera.core.common.security.ValidationResult.Error) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Некорректный пароль: ${passwordValidation.message}"),
                    )
                }

                val dbCamera = mapper.toDatabase(camera)
                insertOrUpsertCamera(dbCamera)

                // Инвалидируем кэш списка всех камер и добавляем новую камеру в кэш
                cameraCache.remove(allCamerasCacheKey)
                cameraCache.put(camera.id, camera)
                // Инвалидируем кэш статусов для этой камеры
                statusCache.remove("status_${camera.id}")
                logger.debug { "Cache invalidated and camera ${camera.id} cached after add" }

                Result.success(camera)
            } catch (e: Exception) {
                logger.error(e) { "Error adding camera: ${camera.id}" }
                Result.failure(e)
            }
        }

    override suspend fun updateCamera(camera: Camera): Result<Camera> =
        withContext(Dispatchers.Default) {
            try {
                // Валидация входных данных
                val urlValidation = InputValidator.validateCameraUrl(camera.url)
                if (urlValidation is com.company.ipcamera.core.common.security.ValidationResult.Error) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Некорректный URL: ${urlValidation.message}"),
                    )
                }

                val nameValidation = InputValidator.validateCameraName(camera.name)
                if (nameValidation is com.company.ipcamera.core.common.security.ValidationResult.Error) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Некорректное имя: ${nameValidation.message}"),
                    )
                }

                val usernameValidation = InputValidator.validateUsername(camera.username)
                if (usernameValidation is com.company.ipcamera.core.common.security.ValidationResult.Error) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Некорректное имя пользователя: ${usernameValidation.message}"),
                    )
                }

                val passwordValidation = InputValidator.validatePassword(camera.password)
                if (passwordValidation is com.company.ipcamera.core.common.security.ValidationResult.Error) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Некорректный пароль: ${passwordValidation.message}"),
                    )
                }

                val updatedCamera = camera.copy(updatedAt = Clock.System.now().toEpochMilliseconds())
                val dbCamera = mapper.toDatabase(updatedCamera)
                // Используем правильный UPDATE запрос вместо INSERT OR REPLACE
                // Это сохраняет created_at и правильно обновляет только измененные поля
                database.cameraDatabaseQueries.updateCamera(
                    name = dbCamera.name,
                    url = dbCamera.url,
                    username = dbCamera.username,
                    password = dbCamera.password,
                    model = dbCamera.model,
                    status = dbCamera.status,
                    resolution_width = dbCamera.resolution_width,
                    resolution_height = dbCamera.resolution_height,
                    fps = dbCamera.fps,
                    bitrate = dbCamera.bitrate,
                    codec = dbCamera.codec,
                    audio = dbCamera.audio,
                    ptz_config = dbCamera.ptz_config,
                    streams = dbCamera.streams,
                    settings = dbCamera.settings,
                    statistics = dbCamera.statistics,
                    updated_at = dbCamera.updated_at,
                    last_seen = dbCamera.last_seen,
                    id = dbCamera.id,
                )

                // Инвалидируем кэш списка всех камер и обновляем кэш конкретной камеры
                cameraCache.remove(allCamerasCacheKey)
                cameraCache.put(camera.id, updatedCamera)
                // Инвалидируем кэш статусов для этой камеры
                statusCache.remove("status_${camera.id}")
                logger.debug { "Cache invalidated and camera ${camera.id} cached after update" }

                Result.success(updatedCamera)
            } catch (e: Exception) {
                logger.error(e) { "Error updating camera: ${camera.id}" }
                Result.failure(e)
            }
        }

    override suspend fun removeCamera(id: String): Result<Unit> =
        withContext(Dispatchers.Default) {
            try {
                database.cameraDatabaseQueries.deleteCamera(id)

                // Инвалидируем кэш списка всех камер и удаляем камеру из кэша
                cameraCache.remove(allCamerasCacheKey)
                cameraCache.remove(id)
                // Удаляем статус из кэша
                statusCache.remove("status_$id")
                logger.debug { "Cache invalidated and camera $id removed from cache after delete" }

                Result.success(Unit)
            } catch (e: Exception) {
                logger.error(e) { "Error removing camera: $id" }
                Result.failure(e)
            }
        }

    override suspend fun discoverCameras(forceRefresh: Boolean): List<DiscoveredCamera> =
        withContext(Dispatchers.IO) {
            try {
                if (forceRefresh) {
                    discoveryCache.remove(discoveryCacheKey)
                }
                // Проверяем кэш результатов обнаружения
                val cached = discoveryCache.get<List<DiscoveredCamera>>(discoveryCacheKey)
                if (cached != null) {
                    logger.debug { "Cache hit for discovered cameras (${cached.size} cameras)" }
                    return@withContext cached
                }

                logger.info { "Starting camera discovery via ONVIF..." }
                val engine = createHttpClientEngine()
                val onvifClient = OnvifClient(engine)

                try {
                    // OnvifClient.discoverCameras() использует WS-Discovery для обнаружения камер
                    // WS-Discovery полностью реализован для всех платформ (JVM, Android, iOS)
                    // Метод отправляет Probe запрос на multicast адрес 239.255.255.250:3702
                    // и собирает ответы от ONVIF устройств в сети
                    val discovered = onvifClient.discoverCameras(timeoutMillis = 5000)

                    logger.info { "Discovered ${discovered.size} cameras via ONVIF" }

                    // Преобразуем из core.network.DiscoveredCamera в repository.DiscoveredCamera
                    val result =
                        discovered.map { onvifCamera ->
                            val ipAddress = extractIpFromUrl(onvifCamera.url)
                            val port = extractPortFromUrl(onvifCamera.url) ?: 554

                            DiscoveredCamera(
                                name = onvifCamera.name ?: "Unknown Camera",
                                url = onvifCamera.url,
                                model = onvifCamera.model,
                                manufacturer = onvifCamera.manufacturer,
                                ipAddress = ipAddress,
                                port = port,
                            )
                        }

                    // Сохраняем в кэш
                    discoveryCache.put(discoveryCacheKey, result)
                    logger.debug { "Cached ${result.size} discovered cameras" }

                    result
                } finally {
                    onvifClient.close()
                    engine.close()
                }
            } catch (e: Exception) {
                logger.error(e) { "Error during camera discovery: ${e.message}" }
                emptyList()
            }
        }

    override suspend fun discoverCamerasWithProgress(
        forceRefresh: Boolean,
        config: DiscoveryConfig,
    ): Flow<DiscoveryProgress> =
        flow {
            val startTime = Clock.System.now().toEpochMilliseconds()
            val allDiscoveredCameras = mutableMapOf<String, DiscoveredCamera>() // Для дедупликации

            // Определяем общее количество методов
            val totalMethods = config.methods.size
            var completedMethods = 0

            for (method in config.methods) {
                val methodStartTime = Clock.System.now().toEpochMilliseconds()

                when (method) {
                    DiscoveryMethod.WS_DISCOVERY -> {
                        emit(
                            DiscoveryProgress(
                                currentMethod = DiscoveryMethod.WS_DISCOVERY,
                                methodProgress = 0f,
                                overallProgress = completedMethods.toFloat() / totalMethods,
                                devicesFoundSoFar = allDiscoveredCameras.size,
                                devicesTotalEstimated = null,
                                elapsedTimeMs = Clock.System.now().toEpochMilliseconds() - startTime,
                                remainingTimeMs = null,
                                currentActivity = "Running WS-Discovery...",
                                discoveredCamerasSnapshot = allDiscoveredCameras.values.toList(),
                            ),
                        )

                        try {
                            val engine = createHttpClientEngine()
                            val onvifClient = OnvifClient(engine)

                            val discovered = onvifClient.discoverCameras(timeoutMillis = config.timeoutPerMethod)

                            discovered.forEach { device ->
                                val ipAddress = extractIpFromUrl(device.url)
                                val port = extractPortFromUrl(device.url) ?: 554

                                val camera =
                                    DiscoveredCamera(
                                        name = device.name ?: "Unknown Camera",
                                        url = device.url,
                                        model = device.model,
                                        manufacturer = device.manufacturer,
                                        ipAddress = ipAddress,
                                        port = port,
                                    )

                                if (!allDiscoveredCameras.contains(ipAddress)) {
                                    allDiscoveredCameras[ipAddress] = camera
                                }
                            }

                            onvifClient.close()
                            engine.close()

                            emit(
                                DiscoveryProgress(
                                    currentMethod = DiscoveryMethod.WS_DISCOVERY,
                                    methodProgress = 1f,
                                    overallProgress = (completedMethods + 1).toFloat() / totalMethods,
                                    devicesFoundSoFar = allDiscoveredCameras.size,
                                    devicesTotalEstimated = null,
                                    elapsedTimeMs = Clock.System.now().toEpochMilliseconds() - startTime,
                                    remainingTimeMs =
                                        estimateRemainingTime(
                                            completedMethods + 1,
                                            totalMethods,
                                            startTime,
                                        ),
                                    currentActivity = "WS-Discovery completed",
                                    discoveredCamerasSnapshot = allDiscoveredCameras.values.toList(),
                                ),
                            )
                        } catch (e: Exception) {
                            logger.error(e) { "WS-Discovery failed: ${e.message}" }
                        }
                    }

                    DiscoveryMethod.SUBNET_SCAN -> {
                        if (config.subnetRange == null) {
                            logger.warn { "Subnet scan requested but no subnetRange configured" }
                            completedMethods++
                            continue
                        }

                        emit(
                            DiscoveryProgress(
                                currentMethod = DiscoveryMethod.SUBNET_SCAN,
                                methodProgress = 0f,
                                overallProgress = completedMethods.toFloat() / totalMethods,
                                devicesFoundSoFar = allDiscoveredCameras.size,
                                devicesTotalEstimated = null,
                                elapsedTimeMs = Clock.System.now().toEpochMilliseconds() - startTime,
                                remainingTimeMs = null,
                                currentActivity = "Scanning subnet ${config.subnetRange}...",
                                discoveredCamerasSnapshot = allDiscoveredCameras.values.toList(),
                            ),
                        )

                        try {
                            networkScanner.scanSubnetWithProgress(
                                subnetRange = config.subnetRange,
                                ports = config.ports,
                                timeoutPerHost = config.timeoutPerMethod / 10, // Делим на 10 для каждого хоста
                                maxConcurrent = if (config.parallelDiscovery) 50 else 10,
                            ).collect { scanProgress ->
                                // Объединяем результаты
                                scanProgress.discoveredCameras.forEach { device ->
                                    val ipAddress = extractIpFromUrl(device.url)
                                    if (!allDiscoveredCameras.contains(ipAddress)) {
                                        val camera =
                                            DiscoveredCamera(
                                                name = device.name ?: "Unknown Camera",
                                                url = device.url,
                                                model = device.model,
                                                manufacturer = device.manufacturer,
                                                ipAddress = ipAddress,
                                                port = extractPortFromUrl(device.url) ?: 554,
                                            )
                                        allDiscoveredCameras[ipAddress] = camera
                                    }
                                }

                                emit(
                                    DiscoveryProgress(
                                        currentMethod = DiscoveryMethod.SUBNET_SCAN,
                                        methodProgress = scanProgress.progress,
                                        overallProgress = (completedMethods + scanProgress.progress) / totalMethods,
                                        devicesFoundSoFar = allDiscoveredCameras.size,
                                        devicesTotalEstimated = null,
                                        elapsedTimeMs = Clock.System.now().toEpochMilliseconds() - startTime,
                                        remainingTimeMs =
                                            estimateRemainingTime(
                                                completedMethods,
                                                totalMethods,
                                                startTime,
                                            ),
                                        currentActivity = scanProgress.currentActivity,
                                        discoveredCamerasSnapshot = allDiscoveredCameras.values.toList(),
                                    ),
                                )
                            }

                            completedMethods++
                        } catch (e: Exception) {
                            logger.error(e) { "Subnet scan failed: ${e.message}" }
                        }
                    }

                    DiscoveryMethod.UPNP_DISCOVERY -> {
                        emit(
                            DiscoveryProgress(
                                currentMethod = DiscoveryMethod.UPNP_DISCOVERY,
                                methodProgress = 0f,
                                overallProgress = completedMethods.toFloat() / totalMethods,
                                devicesFoundSoFar = allDiscoveredCameras.size,
                                devicesTotalEstimated = null,
                                elapsedTimeMs = Clock.System.now().toEpochMilliseconds() - startTime,
                                remainingTimeMs = null,
                                currentActivity = "Running UPnP discovery...",
                                discoveredCamerasSnapshot = allDiscoveredCameras.values.toList(),
                            ),
                        )

                        try {
                            val engine = createHttpClientEngine()
                            val onvifClient = OnvifClient(engine)

                            // UPnP discovery как альтернатива
                            val discovered =
                                onvifClient.discoverCameras(
                                    timeoutMillis = config.timeoutPerMethod,
                                    useUPnP = true,
                                )

                            discovered.forEach { device ->
                                val ipAddress = extractIpFromUrl(device.url)
                                val port = extractPortFromUrl(device.url) ?: 554

                                val camera =
                                    DiscoveredCamera(
                                        name = device.name ?: "UPnP Camera",
                                        url = device.url,
                                        model = device.model,
                                        manufacturer = device.manufacturer,
                                        ipAddress = ipAddress,
                                        port = port,
                                    )

                                if (!allDiscoveredCameras.contains(ipAddress)) {
                                    allDiscoveredCameras[ipAddress] = camera
                                }
                            }

                            onvifClient.close()
                            engine.close()

                            emit(
                                DiscoveryProgress(
                                    currentMethod = DiscoveryMethod.UPNP_DISCOVERY,
                                    methodProgress = 1f,
                                    overallProgress = (completedMethods + 1).toFloat() / totalMethods,
                                    devicesFoundSoFar = allDiscoveredCameras.size,
                                    devicesTotalEstimated = null,
                                    elapsedTimeMs = Clock.System.now().toEpochMilliseconds() - startTime,
                                    remainingTimeMs =
                                        estimateRemainingTime(
                                            completedMethods + 1,
                                            totalMethods,
                                            startTime,
                                        ),
                                    currentActivity = "UPnP discovery completed",
                                    discoveredCamerasSnapshot = allDiscoveredCameras.values.toList(),
                                ),
                            )
                        } catch (e: Exception) {
                            logger.error(e) { "UPnP discovery failed: ${e.message}" }
                        }
                    }

                    DiscoveryMethod.MANUAL_INPUT, DiscoveryMethod.DNS_SD -> {
                        // Не поддерживается в текущей реализации
                        logger.debug { "Method $method not yet implemented" }
                    }
                }

                completedMethods++
            }

            // Финальный прогресс
            emit(
                DiscoveryProgress(
                    currentMethod = DiscoveryMethod.WS_DISCOVERY,
                    methodProgress = 1f,
                    overallProgress = 1f,
                    devicesFoundSoFar = allDiscoveredCameras.size,
                    devicesTotalEstimated = null,
                    elapsedTimeMs = Clock.System.now().toEpochMilliseconds() - startTime,
                    remainingTimeMs = 0L,
                    currentActivity = "Discovery completed",
                    discoveredCamerasSnapshot = allDiscoveredCameras.values.toList(),
                ),
            )

            // Кэшируем результат если включено
            if (config.cacheEnabled && allDiscoveredCameras.isNotEmpty()) {
                discoveryCache.put(discoveryCacheKey, allDiscoveredCameras.values.toList())
            }
        }.flowOn(Dispatchers.IO)

    override suspend fun testConnection(camera: Camera): ConnectionTestResult =
        withContext(Dispatchers.IO) {
            try {
                logger.info { "Testing connection to camera: ${camera.name} (${camera.url})" }
                val engine = createHttpClientEngine()
                val onvifClient = OnvifClient(engine)

                val testStart = Clock.System.now().toEpochMilliseconds()

                try {
                    // 1. ONVIF проверка
                    val onvifResult =
                        onvifClient.testConnection(
                            url = camera.url,
                            username = camera.username,
                            password = camera.password,
                        )

                    val onvifLatency = Clock.System.now().toEpochMilliseconds() - testStart

                    // 2. RTSP проверка
                    val rtspResult = testRtspConnection(camera)

                    // 3. Комбинированный результат
                    val combinedResult = combineConnectionResults(onvifResult, rtspResult, onvifLatency)

                    when (combinedResult) {
                        is ConnectionTestResult.Success -> {
                            logger.info { "Camera connection test successful: ${camera.name}" }
                        }
                        is ConnectionTestResult.Failure -> {
                            logger.warn { "Camera connection test failed: ${camera.name} - ${combinedResult.error}" }
                        }
                    }

                    combinedResult
                } finally {
                    onvifClient.close()
                    engine.close()
                }
            } catch (e: Exception) {
                logger.error(e) { "Error testing camera connection: ${e.message}" }
                ConnectionTestResult.Failure(
                    error = e.message ?: "Unknown error during connection test",
                    code = ErrorCode.CONNECTION_FAILED,
                    diagnostics =
                        ConnectionDiagnostics(
                            networkReachable = false,
                            portOpen = false,
                            authenticationSupported = false,
                            onvifServiceAvailable = false,
                            rtspServiceAvailable = false,
                        ),
                )
            }
        }

    /**
     * Тестирование RTSP подключения
     */
    private suspend fun testRtspConnection(camera: Camera): RtspConnectionResult =
        withContext(Dispatchers.IO) {
            try {
                val rtspUrl = extractRtspUrl(camera.url)
                logger.debug { "Testing RTSP connection to: $rtspUrl" }

                val config =
                    RtspClientConfig(
                        url = rtspUrl,
                        username = camera.username,
                        password = camera.password,
                        timeoutMillis = 5000,
                        enableVideo = true,
                        enableAudio = true,
                        allowSimulatedFallback = false,
                    )

                val rtspClient = RtspClient(config)
                val connectStart = Clock.System.now().toEpochMilliseconds()

                rtspClient.connect()

                // Ждем статуса CONNECTED или PLAYING
                val connectedStatus =
                    withTimeoutOrNull(5000) {
                        while (true) {
                            when (rtspClient.getStatus().value) {
                                RtspClientStatus.CONNECTED, RtspClientStatus.PLAYING -> return@withTimeoutOrNull true
                                RtspClientStatus.ERROR -> return@withTimeoutOrNull false
                                else -> delay(100)
                            }
                        }
                    }

                val rtspLatency = Clock.System.now().toEpochMilliseconds() - connectStart

                rtspClient.disconnect()

                return@withContext RtspConnectionResult.Success(
                    connected = connectedStatus == true,
                    latencyMs = rtspLatency,
                    firstFrameReceived = connectedStatus == true,
                )
            } catch (e: Exception) {
                logger.warn(e) { "RTSP connection test failed: ${e.message}" }
                return@withContext RtspConnectionResult.Failure(
                    error = e.message ?: "RTSP connection failed",
                    code = ErrorCode.CONNECTION_FAILED,
                )
            }
        }

    /**
     * Извлечение RTSP URL из основного URL камеры
     */
    private fun extractRtspUrl(url: String): String {
        // Если URL уже RTSP, возвращаем как есть
        if (url.startsWith("rtsp://", ignoreCase = true)) {
            return url
        }

        // Пытаемся преобразовать HTTP/HTTPS URL в RTSP
        return when {
            url.startsWith("http://", ignoreCase = true) -> {
                "rtsp://${url.removePrefix("http://")}"
            }
            url.startsWith("https://", ignoreCase = true) -> {
                "rtsp://${url.removePrefix("https://")}"
            }
            else -> {
                // Предполагаем, что это уже RTSP URL без протокола
                "rtsp://$url"
            }
        }
    }

    /**
     * Комбинирование результатов ONVIF и RTSP тестов
     */
    private fun combineConnectionResults(
        onvifResult: com.company.ipcamera.core.network.ConnectionTestResult,
        rtspResult: RtspConnectionResult,
        onvifLatency: Long,
    ): ConnectionTestResult {
        val onvifSuccess = onvifResult is com.company.ipcamera.core.network.ConnectionTestResult.Success
        val rtspSuccess = rtspResult is RtspConnectionResult.Success && rtspResult.connected

        return when {
            // Оба успешны - максимальная информация
            onvifSuccess && rtspSuccess -> {
                val onvifStreams = (onvifResult as com.company.ipcamera.core.network.ConnectionTestResult.Success).streams
                val onvifCaps = onvifResult.capabilities

                ConnectionTestResult.Success(
                    streams =
                        onvifStreams.map { stream ->
                            StreamInfo(
                                type = stream.type,
                                resolution = stream.resolution,
                                fps = stream.fps,
                                codec = stream.codec,
                                bitrate = null,
                                audioCodec = stream.codec.takeIf { it.contains("audio", ignoreCase = true) },
                            )
                        },
                    capabilities =
                        CameraCapabilities(
                            ptz = onvifCaps.ptz,
                            audio = onvifCaps.audio,
                            onvif = onvifCaps.onvif,
                            analytics = onvifCaps.analytics,
                            supportedResolutions = emptyList(),
                            supportedCodecs = emptyList(),
                        ),
                    onvifVersion = "2.0", // По умолчанию
                    rtspVersion = "1.0",
                    latencyMs = onvifLatency + (rtspResult as RtspConnectionResult.Success).latencyMs,
                    supportedCodecs = listOf("H.264", "AAC"),
                    authenticationMethod = "Digest",
                    connectionQuality = 1.0f,
                )
            }

            // Только ONVIF успешен
            onvifSuccess -> {
                val onvifStreams = (onvifResult as com.company.ipcamera.core.network.ConnectionTestResult.Success).streams
                val onvifCaps = onvifResult.capabilities

                ConnectionTestResult.Success(
                    streams =
                        onvifStreams.map { stream ->
                            StreamInfo(
                                type = stream.type,
                                resolution = stream.resolution,
                                fps = stream.fps,
                                codec = stream.codec,
                            )
                        },
                    capabilities =
                        CameraCapabilities(
                            ptz = onvifCaps.ptz,
                            audio = onvifCaps.audio,
                            onvif = onvifCaps.onvif,
                            analytics = onvifCaps.analytics,
                        ),
                    onvifVersion = "2.0",
                    rtspVersion = null,
                    latencyMs = onvifLatency,
                    supportedCodecs = emptyList(),
                    authenticationMethod = "Digest",
                    connectionQuality = 0.7f,
                )
            }

            // Только RTSP успешен
            rtspSuccess -> {
                ConnectionTestResult.Success(
                    streams =
                        listOf(
                            StreamInfo(
                                type = "RTSP",
                                resolution = "unknown",
                                fps = 25,
                                codec = "H.264",
                            ),
                        ),
                    capabilities =
                        CameraCapabilities(
                            ptz = false,
                            audio = false,
                            onvif = false,
                            analytics = false,
                        ),
                    onvifVersion = null,
                    rtspVersion = "1.0",
                    latencyMs = (rtspResult as RtspConnectionResult.Success).latencyMs,
                    supportedCodecs = listOf("H.264"),
                    authenticationMethod = null,
                    connectionQuality = 0.5f,
                )
            }

            // Оба не успешны - возвращаем ошибку с диагностикой
            else -> {
                val onvifFailure = onvifResult as? com.company.ipcamera.core.network.ConnectionTestResult.Failure
                val rtspFailure = rtspResult as? RtspConnectionResult.Failure

                ConnectionTestResult.Failure(
                    error =
                        buildString {
                            if (onvifFailure != null) {
                                append("ONVIF: ${onvifFailure.error}. ")
                            }
                            if (rtspFailure != null) {
                                append("RTSP: ${rtspFailure.error}")
                            }
                        },
                    code = ErrorCode.CONNECTION_FAILED,
                    diagnostics =
                        ConnectionDiagnostics(
                            networkReachable = true, // Если дошли до проверки, сеть доступна
                            portOpen = true,
                            authenticationSupported = true,
                            onvifServiceAvailable = false,
                            rtspServiceAvailable = false,
                        ),
                )
            }
        }
    }

    /**
     * Оценка оставшегося времени для discovery
     */
    private fun estimateRemainingTime(
        completedMethods: Int,
        totalMethods: Int,
        startTime: Long,
    ): Long? {
        if (completedMethods == 0 || totalMethods == 0) return null

        val elapsed = Clock.System.now().toEpochMilliseconds() - startTime
        val avgTimePerMethod = elapsed / completedMethods
        val remainingMethods = totalMethods - completedMethods

        return avgTimePerMethod * remainingMethods
    }

    override suspend fun getCameraStatus(id: String): CameraStatus =
        withContext(Dispatchers.Default) {
            try {
                // Проверяем кэш статусов (быстрый доступ с коротким TTL)
                val statusCacheKey = "status_$id"
                val cachedStatus = statusCache.get<CameraStatus>(statusCacheKey)
                if (cachedStatus != null) {
                    logger.debug { "Cache hit for camera status: $id = $cachedStatus" }
                    return@withContext cachedStatus
                }

                // Если нет в кэше, загружаем из БД (используем кэш камер)
                val camera = getCameraById(id)
                val status = camera?.status ?: CameraStatus.UNKNOWN

                // Сохраняем в кэш статусов
                statusCache.put(statusCacheKey, status)
                logger.debug { "Cached camera status: $id = $status" }

                status
            } catch (e: Exception) {
                logger.error(e) { "Error getting camera status: $id" }
                CameraStatus.ERROR
            }
        }

    private fun extractIpFromUrl(url: String): String =
        try {
            url
                .removePrefix("rtsp://")
                .removePrefix("http://")
                .removePrefix("https://")
                .substringBefore("/")
                .substringBefore(":")
        } catch (e: Exception) {
            logger.warn(e) { "Failed to extract IP from URL: $url" }
            url
        }

    private fun extractPortFromUrl(url: String): Int? =
        try {
            val cleanUrl =
                url
                    .removePrefix("rtsp://")
                    .removePrefix("http://")
                    .removePrefix("https://")
                    .substringBefore("/")
            if (cleanUrl.contains(":")) cleanUrl.substringAfter(":").toIntOrNull() else null
        } catch (e: Exception) {
            logger.warn(e) { "Failed to extract port from URL: $url" }
            null
        }

    private fun mapErrorCode(code: com.company.ipcamera.core.network.ErrorCode): ErrorCode =
        when (code) {
            com.company.ipcamera.core.network.ErrorCode.CONNECTION_FAILED -> ErrorCode.CONNECTION_FAILED
            com.company.ipcamera.core.network.ErrorCode.AUTHENTICATION_FAILED -> ErrorCode.AUTHENTICATION_FAILED
            com.company.ipcamera.core.network.ErrorCode.TIMEOUT -> ErrorCode.TIMEOUT
            com.company.ipcamera.core.network.ErrorCode.INVALID_RESPONSE,
            com.company.ipcamera.core.network.ErrorCode.UNKNOWN_ERROR,
            -> ErrorCode.UNKNOWN
        }

    private suspend fun insertOrUpsertCamera(dbCamera: com.company.ipcamera.shared.database.Camera) {
        if (!isPostgresFlywayParityDriver(driver)) {
            database.cameraDatabaseQueries.insertCamera(
                id = dbCamera.id,
                name = dbCamera.name,
                url = dbCamera.url,
                username = dbCamera.username,
                password = dbCamera.password,
                model = dbCamera.model,
                status = dbCamera.status,
                resolution_width = dbCamera.resolution_width,
                resolution_height = dbCamera.resolution_height,
                fps = dbCamera.fps,
                bitrate = dbCamera.bitrate,
                codec = dbCamera.codec,
                audio = dbCamera.audio,
                ptz_config = dbCamera.ptz_config,
                streams = dbCamera.streams,
                settings = dbCamera.settings,
                statistics = dbCamera.statistics,
                created_at = dbCamera.created_at,
                updated_at = dbCamera.updated_at,
                last_seen = dbCamera.last_seen,
            )
            return
        }

        // PostgreSQL path: SQLite-specific `INSERT OR REPLACE` from SQLDelight queries is invalid here.
        // Use native UPSERT while keeping the same table contract and values.
        driver.execute(
            identifier = null,
            sql =
                """
                INSERT INTO camera(
                    id, name, url, username, password, model, status,
                    resolution_width, resolution_height, fps, bitrate, codec, audio,
                    ptz_config, streams, settings, statistics,
                    created_at, updated_at, last_seen
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    name = EXCLUDED.name,
                    url = EXCLUDED.url,
                    username = EXCLUDED.username,
                    password = EXCLUDED.password,
                    model = EXCLUDED.model,
                    status = EXCLUDED.status,
                    resolution_width = EXCLUDED.resolution_width,
                    resolution_height = EXCLUDED.resolution_height,
                    fps = EXCLUDED.fps,
                    bitrate = EXCLUDED.bitrate,
                    codec = EXCLUDED.codec,
                    audio = EXCLUDED.audio,
                    ptz_config = EXCLUDED.ptz_config,
                    streams = EXCLUDED.streams,
                    settings = EXCLUDED.settings,
                    statistics = EXCLUDED.statistics,
                    created_at = EXCLUDED.created_at,
                    updated_at = EXCLUDED.updated_at,
                    last_seen = EXCLUDED.last_seen
                """.trimIndent(),
            parameters = 20,
        ) {
            val createdAt = normalizeEpochForPostgresIntColumn(dbCamera.created_at)
            val updatedAt = normalizeEpochForPostgresIntColumn(dbCamera.updated_at)
            val lastSeen = dbCamera.last_seen?.let { normalizeEpochForPostgresIntColumn(it) }
            bindString(0, dbCamera.id)
            bindString(1, dbCamera.name)
            bindString(2, dbCamera.url)
            bindString(3, dbCamera.username)
            bindString(4, dbCamera.password)
            bindString(5, dbCamera.model)
            bindString(6, dbCamera.status)
            bindLong(7, dbCamera.resolution_width)
            bindLong(8, dbCamera.resolution_height)
            bindLong(9, dbCamera.fps)
            bindLong(10, dbCamera.bitrate)
            bindString(11, dbCamera.codec)
            bindLong(12, dbCamera.audio)
            bindString(13, dbCamera.ptz_config)
            bindString(14, dbCamera.streams)
            bindString(15, dbCamera.settings)
            bindString(16, dbCamera.statistics)
            bindLong(17, createdAt)
            bindLong(18, updatedAt)
            bindLong(19, lastSeen)
        }
    }

    private fun normalizeEpochForPostgresIntColumn(value: Long): Long {
        return if (value > Int.MAX_VALUE) value / 1000L else value
    }
}
