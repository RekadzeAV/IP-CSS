package com.company.ipcamera.core.network

import com.company.ipcamera.core.common.model.Resolution
import com.company.ipcamera.core.network.auth.DigestAuthHelper
import com.company.ipcamera.core.network.auth.DigestAuthParams
import com.company.ipcamera.core.network.onvif.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.encodeBase64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
private val Dispatchers.IO get() = Dispatchers.Default

/**
 * ONVIF клиент для обнаружения и управления камерами
 *
 * Поддерживает certificate pinning через переданный engine.
 * Для создания с certificate pinning используйте OnvifClientFactory.
 */
class OnvifClient(
    private val engine: HttpClientEngine
) {
    private val client: HttpClient by lazy {
        HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
    }

    // Кэш для Digest Authentication параметров по URL
    private val digestAuthCache = mutableMapOf<String, DigestAuthParams>()

    // Кэш для ONVIF Capabilities (TTL: 5 минут)
    private val capabilitiesCache = mutableMapOf<String, Pair<OnvifCapabilities, Long>>()
    private val capabilitiesCacheTTL = 5 * 60 * 1000L // 5 минут

    // Кэш для Device Information (TTL: 10 минут)
    private val deviceInfoCache = mutableMapOf<String, Pair<DeviceInformation, Long>>()
    private val deviceInfoCacheTTL = 10 * 60 * 1000L // 10 минут

    // Кэш для Profiles (TTL: 5 минут)
    private val profilesCache = mutableMapOf<String, Pair<List<OnvifProfile>, Long>>()
    private val profilesCacheTTL = 5 * 60 * 1000L // 5 минут

    // Event Service для работы с событиями
    private val eventService: OnvifEventService by lazy {
        OnvifEventServiceImpl(engine)
    }

    // Analytics Service для работы с аналитикой
    private val analyticsService: OnvifAnalyticsService by lazy {
        OnvifAnalyticsServiceImpl(engine)
    }

    // Imaging Service для работы с настройками изображения
    private val imagingService: OnvifImagingService by lazy {
        OnvifImagingServiceImpl(engine)
    }

    /**
     * Обнаружить камеры в сети через WS-Discovery и UPnP (как альтернатива)
     */
    suspend fun discoverCameras(timeoutMillis: Long = 5000, useUPnP: Boolean = true): List<DiscoveredCamera> = withContext(
        Dispatchers.IO
    ) {
        val wsCamerasDeferred = async {
            val wsDiscovery: WSDiscovery = WSDiscovery()
            try {
                logger.info { "Starting ONVIF camera discovery via WS-Discovery..." }
                val discoveredDevices = wsDiscovery.discover(timeoutMillis)
                logger.info { "WS-Discovery found ${discoveredDevices.size} devices" }

                val validDevices = discoveredDevices.filter { device ->
                    shouldProcessDevice(device)
                }
                logger.info { "Filtered ${validDevices.size} valid ONVIF devices from ${discoveredDevices.size} total devices" }

                coroutineScope {
                    validDevices.flatMap { device ->
                        device.xAddrs.map { xAddr ->
                            async { processDeviceXAddr(xAddr, device) }
                        }
                    }.awaitAll().filterNotNull()
                }
            } catch (e: Exception) {
                logger.error(e) { "Error during WS-Discovery: ${e.message}" }
                emptyList()
            } finally {
                wsDiscovery.close()
            }
        }

        val upnpCamerasDeferred = async {
            if (!useUPnP) return@async emptyList()
            val upnpDiscovery: UPnPDiscovery = UPnPDiscovery()
            try {
                logger.info { "Starting UPnP discovery in parallel..." }
                val upnpDevices = upnpDiscovery.discover(timeoutMillis)
                logger.info { "UPnP discovery found ${upnpDevices.size} devices" }

                coroutineScope {
                    upnpDevices.map { device ->
                        async { processUPnPDevice(device) }
                    }.awaitAll().filterNotNull()
                }
            } catch (e: Exception) {
                logger.warn(e) { "Error during UPnP discovery: ${e.message}" }
                emptyList()
            } finally {
                upnpDiscovery.close()
            }
        }

        val wsCameras = wsCamerasDeferred.await()
        val upnpCameras = upnpCamerasDeferred.await()
        val mergedCameras = mergeDiscoveredCameras(wsCameras + upnpCameras)

        logger.info {
            "Camera discovery completed. WS-Discovery=${wsCameras.size}, " +
                "UPnP=${upnpCameras.size}, merged=${mergedCameras.size}"
        }
        mergedCameras
    }

    /**
     * Обрабатывает UPnP устройство и пытается преобразовать его в DiscoveredCamera
     */
    private suspend fun processUPnPDevice(device: UPnPDevice): DiscoveredCamera? = withContext(Dispatchers.IO) {
        try {
            // Пытаемся получить описание устройства из location URL
            val deviceInfo = try {
                client.get(device.location).body<String>()
            } catch (e: Exception) {
                logger.debug(e) { "Failed to fetch UPnP device description from ${device.location}" }
                null
            }

            // Парсим базовую информацию из UPnP ответа
            val url = device.location
            val ipAddress = extractIpFromLocation(device.location)
            val port = extractPortFromLocation(device.location) ?: 80

            // Создаем базовую информацию о камере из UPnP устройства
            DiscoveredCamera(
                url = url,
                name = extractDeviceName(deviceInfo) ?: "UPnP Camera",
                manufacturer = extractDeviceManufacturer(deviceInfo) ?: "Unknown",
                model = extractDeviceModel(deviceInfo) ?: "Unknown"
            )
        } catch (e: Exception) {
            logger.warn(e) { "Error processing UPnP device: ${device.usn}" }
            null
        }
    }

    /**
     * Извлекает IP адрес из location URL
     */
    private fun extractIpFromLocation(location: String): String {
        return extractHostAndPort(location)?.first ?: location
    }

    /**
     * Извлекает порт из location URL
     */
    private fun extractPortFromLocation(location: String): Int? {
        return extractHostAndPort(location)?.second
    }

    /**
     * Извлекает имя устройства из UPnP XML описания
     */
    private fun extractDeviceName(xml: String?): String? {
        if (xml == null) return null
        return try {
            val namePattern = Regex("<friendlyName>(.*?)</friendlyName>", RegexOption.IGNORE_CASE)
            namePattern.find(xml)?.groupValues?.get(1)?.trim()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Извлекает модель устройства из UPnP XML описания
     */
    private fun extractDeviceModel(xml: String?): String? {
        if (xml == null) return null
        return try {
            val modelPattern = Regex("<modelName>(.*?)</modelName>", RegexOption.IGNORE_CASE)
            modelPattern.find(xml)?.groupValues?.get(1)?.trim()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Извлекает производителя устройства из UPnP XML описания
     */
    private fun extractDeviceManufacturer(xml: String?): String? {
        if (xml == null) return null
        return try {
            val manufacturerPattern = Regex("<manufacturer>(.*?)</manufacturer>", RegexOption.IGNORE_CASE)
            manufacturerPattern.find(xml)?.groupValues?.get(1)?.trim()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Получить информацию о камере через ONVIF Device Management
     * Использует кэширование для оптимизации производительности
     */
    suspend fun getDeviceInformation(
        url: String,
        username: String? = null,
        password: String? = null,
        useCache: Boolean = true
    ): DeviceInformation? = withContext(Dispatchers.IO) {
        try {
            val deviceUrl = normalizeUrl(url)
            val cacheKey = "$deviceUrl:${username ?: ""}"

            // Проверка кэша
            if (useCache) {
                val cached = deviceInfoCache[cacheKey]
                if (cached != null) {
                    val (cachedInfo, timestamp) = cached
                    if (Clock.System.now().toEpochMilliseconds() - timestamp < deviceInfoCacheTTL) {
                        logger.debug { "Returning cached device information for: $deviceUrl" }
                        return@withContext cachedInfo
                    } else {
                        // Кэш истек, удаляем
                        deviceInfoCache.remove(cacheKey)
                    }
                }
            }

            logger.info { "Getting device information from: $deviceUrl" }

            val capabilities = getCapabilities(deviceUrl, username, password, useCache)
                ?: return@withContext null

            val deviceServiceUrl = capabilities.deviceServiceUrl ?: return@withContext null

            val soapMessage = createGetDeviceInformationRequest()
            val response = sendSoapRequest(deviceServiceUrl, soapMessage, username, password)

            val deviceInfo = parseDeviceInformation(response)

            // Сохранение в кэш
            if (deviceInfo != null && useCache) {
                deviceInfoCache[cacheKey] = Pair(deviceInfo, Clock.System.now().toEpochMilliseconds())
            }

            deviceInfo
        } catch (e: Exception) {
            logger.error(e) { "Error getting device information: ${e.message}" }
            null
        }
    }

    /**
     * Получить возможности камеры (Capabilities)
     */
    suspend fun getCapabilities(
        url: String,
        username: String? = null,
        password: String? = null,
        useCache: Boolean = true
    ): OnvifCapabilities? = withContext(Dispatchers.IO) {
        try {
            val deviceUrl = normalizeUrl(url)
            val cacheKey = "$deviceUrl:${username ?: ""}"

            // Проверка кэша
            if (useCache) {
                val cached = capabilitiesCache[cacheKey]
                if (cached != null) {
                    val (cachedCapabilities, timestamp) = cached
                    if (Clock.System.now().toEpochMilliseconds() - timestamp < capabilitiesCacheTTL) {
                        logger.debug { "Returning cached capabilities for: $deviceUrl" }
                        return@withContext cachedCapabilities
                    } else {
                        // Кэш истек, удаляем
                        capabilitiesCache.remove(cacheKey)
                    }
                }
            }

            val capabilitiesUrl = "$deviceUrl/onvif/device_service"

            val soapMessage = createGetCapabilitiesRequest()
            val response = sendSoapRequest(capabilitiesUrl, soapMessage, username, password)

            val capabilities = parseCapabilities(response)

            // Сохранение в кэш
            if (capabilities != null && useCache) {
                capabilitiesCache[cacheKey] = Pair(capabilities, Clock.System.now().toEpochMilliseconds())
            }

            capabilities
        } catch (e: Exception) {
            logger.error(e) { "Error getting capabilities: ${e.message}" }
            null
        }
    }

    /**
     * Управление PTZ
     */
    suspend fun movePtz(
        url: String,
        direction: PtzDirection,
        speed: Float = 0.5f,
        username: String? = null,
        password: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val deviceUrl = normalizeUrl(url)
            val capabilities = getCapabilities(deviceUrl, username, password)
                ?: return@withContext false

            val ptzServiceUrl = capabilities.ptzServiceUrl ?: run {
                logger.warn { "PTZ service not available" }
                return@withContext false
            }

            val soapMessage = createContinuousMoveRequest(direction, speed)
            sendSoapRequest(ptzServiceUrl, soapMessage, username, password)

            true
        } catch (e: Exception) {
            logger.error(e) { "Error moving PTZ: ${e.message}" }
            false
        }
    }

    /**
     * Остановить движение PTZ
     */
    suspend fun stopPtz(
        url: String,
        profileToken: String = "Profile1",
        username: String? = null,
        password: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val deviceUrl = normalizeUrl(url)
            val capabilities = getCapabilities(deviceUrl, username, password)
                ?: return@withContext false

            val ptzServiceUrl = capabilities.ptzServiceUrl ?: return@withContext false

            val soapMessage = createStopRequest(profileToken)
            sendSoapRequest(ptzServiceUrl, soapMessage, username, password)

            true
        } catch (e: Exception) {
            logger.error(e) { "Error stopping PTZ: ${e.message}" }
            false
        }
    }

    /**
     * Приближение (Zoom In)
     */
    suspend fun zoomIn(
        url: String,
        speed: Float = 0.5f,
        profileToken: String = "Profile1",
        username: String? = null,
        password: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val deviceUrl = normalizeUrl(url)
            val capabilities = getCapabilities(deviceUrl, username, password)
                ?: return@withContext false

            val ptzServiceUrl = capabilities.ptzServiceUrl ?: run {
                logger.warn { "PTZ service not available" }
                return@withContext false
            }

            val soapMessage = createZoomRequest(profileToken, speed)
            sendSoapRequest(ptzServiceUrl, soapMessage, username, password)

            true
        } catch (e: Exception) {
            logger.error(e) { "Error zooming in: ${e.message}" }
            false
        }
    }

    /**
     * Отдаление (Zoom Out)
     */
    suspend fun zoomOut(
        url: String,
        speed: Float = 0.5f,
        profileToken: String = "Profile1",
        username: String? = null,
        password: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val deviceUrl = normalizeUrl(url)
            val capabilities = getCapabilities(deviceUrl, username, password)
                ?: return@withContext false

            val ptzServiceUrl = capabilities.ptzServiceUrl ?: run {
                logger.warn { "PTZ service not available" }
                return@withContext false
            }

            val soapMessage = createZoomRequest(profileToken, -speed)
            sendSoapRequest(ptzServiceUrl, soapMessage, username, password)

            true
        } catch (e: Exception) {
            logger.error(e) { "Error zooming out: ${e.message}" }
            false
        }
    }

    /**
     * Перейти в пресет PTZ (ONVIF GotoPreset)
     *
     * @param url URL устройства (http/https или rtsp — будет нормализован)
     * @param presetToken токен пресета (например "1"; числовые токены у большинства камер)
     * @param username имя пользователя
     * @param password пароль
     * @param profileToken профиль (по умолчанию "Profile1")
     * @return true при успехе
     */
    suspend fun gotoPreset(
        url: String,
        presetToken: String,
        username: String? = null,
        password: String? = null,
        profileToken: String = "Profile1",
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val deviceUrl = normalizeUrl(url)
            val capabilities = getCapabilities(deviceUrl, username, password)
                ?: return@withContext false

            val ptzServiceUrl = capabilities.ptzServiceUrl ?: run {
                logger.warn { "PTZ service not available" }
                return@withContext false
            }

            val soapMessage = createGotoPresetRequest(presetToken, profileToken)
            sendSoapRequest(ptzServiceUrl, soapMessage, username, password)
            true
        } catch (e: Exception) {
            logger.error(e) { "Error going to PTZ preset: ${e.message}" }
            false
        }
    }

    /**
     * Получить профили камеры
     */
    suspend fun getProfiles(
        url: String,
        username: String? = null,
        password: String? = null,
        useCache: Boolean = true
    ): List<OnvifProfile> = withContext(Dispatchers.IO) {
        try {
            val deviceUrl = normalizeUrl(url)
            val cacheKey = "$deviceUrl:${username ?: ""}"

            // Проверка кэша
            if (useCache) {
                val cached = profilesCache[cacheKey]
                if (cached != null) {
                    val (cachedProfiles, timestamp) = cached
                    if (Clock.System.now().toEpochMilliseconds() - timestamp < profilesCacheTTL) {
                        logger.debug { "Returning cached profiles for: $deviceUrl" }
                        return@withContext cachedProfiles
                    } else {
                        // Кэш истек, удаляем
                        profilesCache.remove(cacheKey)
                    }
                }
            }

            val capabilities = getCapabilities(deviceUrl, username, password, useCache)
                ?: return@withContext emptyList()

            val mediaServiceUrl = capabilities.mediaServiceUrl ?: return@withContext emptyList()

            val soapMessage = createGetProfilesRequest()
            val response = sendSoapRequest(mediaServiceUrl, soapMessage, username, password)

            val profiles = parseProfiles(response)

            // Сохранение в кэш
            if (profiles.isNotEmpty() && useCache) {
                profilesCache[cacheKey] = Pair(profiles, Clock.System.now().toEpochMilliseconds())
            }

            profiles
        } catch (e: Exception) {
            logger.error(e) { "Error getting profiles: ${e.message}" }
            emptyList()
        }
    }

    /**
     * Получить URI потока (Stream URI)
     */
    suspend fun getStreamUri(
        url: String,
        profileToken: String = "Profile1",
        username: String? = null,
        password: String? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            val deviceUrl = normalizeUrl(url)
            val capabilities = getCapabilities(deviceUrl, username, password)
                ?: return@withContext null

            val mediaServiceUrl = capabilities.mediaServiceUrl ?: return@withContext null

            val soapMessage = createGetStreamUriRequest(profileToken)
            val response = sendSoapRequest(mediaServiceUrl, soapMessage, username, password)

            parseStreamUri(response)
        } catch (e: Exception) {
            logger.error(e) { "Error getting stream URI: ${e.message}" }
            null
        }
    }

    /**
     * Проверить подключение к камере
     */
    suspend fun testConnection(
        url: String,
        username: String? = null,
        password: String? = null
    ): ConnectionTestResult = withContext(Dispatchers.IO) {
        try {
            logger.info { "Testing connection to: $url" }
            val normalizedUrl = normalizeUrl(url)

            // Попытка получить capabilities через ONVIF
            val capabilities = getCapabilities(normalizedUrl, username, password)

            if (capabilities != null) {
                logger.debug { "ONVIF capabilities retrieved successfully" }

                // Получение профилей для определения потоков
                val profiles = getProfiles(normalizedUrl, username, password)
                logger.debug { "Found ${profiles.size} profiles" }

                val streamInfo = mutableListOf<StreamInfo>()

                // Получение информации о потоках для каждого профиля
                for (profile in profiles.take(3)) { // Ограничиваем до 3 профилей
                    try {
                        val streamUri = getStreamUri(normalizedUrl, profile.token, username, password)
                        if (streamUri != null) {
                            streamInfo.add(
                                StreamInfo(
                                    type = "RTSP",
                                    resolution = profile.videoResolution?.toString() ?: "unknown",
                                    fps = profile.fps ?: 25,
                                    codec = profile.codec ?: "H.264"
                                )
                            )
                        }
                    } catch (e: Exception) {
                        logger.warn(e) { "Error getting stream URI for profile ${profile.token}" }
                    }
                }

                // Если не удалось получить потоки, создаем базовую информацию
                if (streamInfo.isEmpty() && profiles.isNotEmpty()) {
                    streamInfo.add(
                        StreamInfo(
                            type = "RTSP",
                            resolution = profiles.first().videoResolution?.toString() ?: "unknown",
                            fps = profiles.first().fps ?: 25,
                            codec = profiles.first().codec ?: "H.264"
                        )
                    )
                }

                // Определение поддержки audio из профилей
                val hasAudio = profiles.any { profile ->
                    profile.hasAudio ||
                        profile.audioCodec != null ||
                        streamInfo.any { it.type.contains("audio", ignoreCase = true) }
                }

                val result = ConnectionTestResult.Success(
                    streams = streamInfo,
                    capabilities = CameraCapabilities(
                        ptz = capabilities.ptzServiceUrl != null,
                        audio = hasAudio,
                        onvif = true,
                        analytics = capabilities.analyticsServiceUrl != null
                    )
                )

                logger.info { "Connection test successful. Found ${streamInfo.size} streams" }
                result
            } else {
                logger.warn { "Could not retrieve ONVIF capabilities. Camera may not support ONVIF or connection failed." }
                ConnectionTestResult.Failure(
                    error = "Could not connect to camera or camera does not support ONVIF",
                    code = ErrorCode.CONNECTION_FAILED
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Connection test failed: ${e.message}" }
            // Определяем тип ошибки по сообщению
            val errorCode = when {
                e.message?.contains("401", ignoreCase = true) == true ||
                    e.message?.contains("unauthorized", ignoreCase = true) == true ||
                    e.message?.contains("authentication", ignoreCase = true) == true -> {
                    ErrorCode.AUTHENTICATION_FAILED
                }
                e.message?.contains("timeout", ignoreCase = true) == true -> {
                    ErrorCode.TIMEOUT
                }
                e.message?.contains("invalid", ignoreCase = true) == true -> {
                    ErrorCode.INVALID_RESPONSE
                }
                else -> ErrorCode.CONNECTION_FAILED
            }

            ConnectionTestResult.Failure(
                error = e.message ?: "Unknown error during connection test",
                code = errorCode
            )
        }
    }

    // === ONVIF Event Service методы ===

    /**
     * Подписаться на события камеры
     *
     * @param url URL камеры
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @param filter Фильтр событий (опционально)
     * @param subscriptionTime Время подписки в секундах (по умолчанию 3600 = 1 час)
     * @return Результат с подпиской или ошибкой
     */
    suspend fun subscribeToEvents(
        url: String,
        username: String? = null,
        password: String? = null,
        filter: OnvifEventFilter? = null,
        subscriptionTime: Long = 3600
    ): Result<OnvifEventSubscription> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(url)
            eventService.subscribeToEvents(
                cameraUrl = normalizedUrl,
                username = username,
                password = password,
                filter = filter,
                subscriptionTime = subscriptionTime
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to subscribe to events for camera: $url" }
            Result.failure(e)
        }
    }

    /**
     * Отписаться от событий
     *
     * @param subscriptionId Идентификатор подписки
     * @return Результат операции
     */
    suspend fun unsubscribeFromEvents(subscriptionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            eventService.unsubscribe(subscriptionId)
        } catch (e: Exception) {
            logger.error(e) { "Failed to unsubscribe from events: $subscriptionId" }
            Result.failure(e)
        }
    }

    /**
     * Продлить подписку на события
     *
     * @param subscriptionId Идентификатор подписки
     * @param renewalTime Время продления в секундах (по умолчанию 3600 = 1 час)
     * @return Результат с обновленной подпиской или ошибкой
     */
    suspend fun renewEventSubscription(
        subscriptionId: String,
        renewalTime: Long = 3600
    ): Result<OnvifEventSubscription> = withContext(Dispatchers.IO) {
        try {
            eventService.renewSubscription(subscriptionId, renewalTime)
        } catch (e: Exception) {
            logger.error(e) { "Failed to renew event subscription: $subscriptionId" }
            Result.failure(e)
        }
    }

    /**
     * Получить свойства Event Service камеры
     *
     * @param url URL камеры
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @return Результат со свойствами или ошибкой
     */
    suspend fun getEventProperties(
        url: String,
        username: String? = null,
        password: String? = null
    ): Result<OnvifEventProperties> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(url)
            eventService.getEventProperties(normalizedUrl, username, password)
        } catch (e: Exception) {
            logger.error(e) { "Failed to get event properties for camera: $url" }
            Result.failure(e)
        }
    }

    /**
     * Получить все активные подписки для камеры
     *
     * @param url URL камеры
     * @return Список активных подписок
     */
    suspend fun getEventSubscriptions(url: String): List<OnvifEventSubscription> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(url)
            eventService.getSubscriptionsForCamera(normalizedUrl)
        } catch (e: Exception) {
            logger.error(e) { "Failed to get event subscriptions for camera: $url" }
            emptyList()
        }
    }

    /**
     * Отменить все подписки для камеры
     *
     * @param url URL камеры
     * @return Результат операции
     */
    suspend fun unsubscribeAllEvents(url: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(url)
            eventService.unsubscribeAll(normalizedUrl)
        } catch (e: Exception) {
            logger.error(e) { "Failed to unsubscribe all events for camera: $url" }
            Result.failure(e)
        }
    }

    /**
     * Создать PullPoint подписку (более надежный способ для камер без push-поддержки)
     *
     * @param url URL камеры
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @param filter Фильтр событий (опционально)
     * @param subscriptionTime Время подписки в секундах (по умолчанию 3600 = 1 час)
     * @return Результат с подпиской или ошибкой
     */
    suspend fun createPullPointSubscription(
        url: String,
        username: String? = null,
        password: String? = null,
        filter: OnvifEventFilter? = null,
        subscriptionTime: Long = 3600
    ): Result<OnvifEventSubscription> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(url)
            eventService.createPullPointSubscription(
                cameraUrl = normalizedUrl,
                username = username,
                password = password,
                filter = filter,
                subscriptionTime = subscriptionTime
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to create PullPoint subscription for camera: $url" }
            Result.failure(e)
        }
    }

    /**
     * Получить события через PullPoint (PullMessages)
     *
     * @param subscriptionId Идентификатор подписки
     * @param timeout Таймаут ожидания событий в миллисекундах (по умолчанию 1000)
     * @param maxMessages Максимальное количество сообщений для получения (по умолчанию 10)
     * @return Результат со списком событий или ошибкой
     */
    suspend fun pullEventMessages(
        subscriptionId: String,
        timeout: Long = 1000,
        maxMessages: Int = 10
    ): Result<List<OnvifEvent>> = withContext(Dispatchers.IO) {
        try {
            eventService.pullMessages(subscriptionId, timeout, maxMessages)
        } catch (e: Exception) {
            logger.error(e) { "Failed to pull event messages from subscription: $subscriptionId" }
            Result.failure(e)
        }
    }

    /**
     * Установить точку синхронизации для PullPoint (SetSynchronizationPoint)
     *
     * @param subscriptionId Идентификатор подписки
     * @return Результат операции
     */
    suspend fun setEventSynchronizationPoint(subscriptionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            eventService.setSynchronizationPoint(subscriptionId)
        } catch (e: Exception) {
            logger.error(e) { "Failed to set event synchronization point for subscription: $subscriptionId" }
            Result.failure(e)
        }
    }

    // === ONVIF Analytics Service методы ===

    /**
     * Получить список аналитических движков камеры
     *
     * @param url URL камеры
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @return Результат со списком движков или ошибкой
     */
    suspend fun getAnalyticsEngines(
        url: String,
        username: String? = null,
        password: String? = null
    ): Result<List<OnvifAnalyticsEngine>> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(url)
            analyticsService.getAnalyticsEngines(normalizedUrl, username, password)
        } catch (e: Exception) {
            logger.error(e) { "Failed to get analytics engines for camera: $url" }
            Result.failure(e)
        }
    }

    /**
     * Получить информацию о конкретном аналитическом движке
     *
     * @param url URL камеры
     * @param engineToken Токен движка
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @return Результат с информацией о движке или ошибкой
     */
    suspend fun getAnalyticsEngine(
        url: String,
        engineToken: String,
        username: String? = null,
        password: String? = null
    ): Result<OnvifAnalyticsEngine> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(url)
            analyticsService.getAnalyticsEngine(normalizedUrl, engineToken, username, password)
        } catch (e: Exception) {
            logger.error(e) { "Failed to get analytics engine: $engineToken for camera: $url" }
            Result.failure(e)
        }
    }

    /**
     * Получить список входных данных для аналитического движка
     *
     * @param url URL камеры
     * @param engineToken Токен движка
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @return Результат со списком входных данных или ошибкой
     */
    suspend fun getAnalyticsEngineInputs(
        url: String,
        engineToken: String,
        username: String? = null,
        password: String? = null
    ): Result<List<OnvifAnalyticsEngineInput>> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(url)
            analyticsService.getAnalyticsEngineInputs(normalizedUrl, engineToken, username, password)
        } catch (e: Exception) {
            logger.error(e) { "Failed to get analytics engine inputs for engine: $engineToken" }
            Result.failure(e)
        }
    }

    /**
     * Создать новый аналитический движок
     *
     * @param url URL камеры
     * @param configuration Конфигурация движка
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @return Результат с созданным движком или ошибкой
     */
    suspend fun createAnalyticsEngine(
        url: String,
        configuration: OnvifAnalyticsEngineConfiguration,
        username: String? = null,
        password: String? = null
    ): Result<OnvifAnalyticsEngine> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(url)
            analyticsService.createAnalyticsEngine(normalizedUrl, configuration, username, password)
        } catch (e: Exception) {
            logger.error(e) { "Failed to create analytics engine for camera: $url" }
            Result.failure(e)
        }
    }

    /**
     * Обновить конфигурацию аналитического движка
     *
     * @param url URL камеры
     * @param engineToken Токен движка
     * @param configuration Новая конфигурация
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @return Результат операции
     */
    suspend fun setAnalyticsEngine(
        url: String,
        engineToken: String,
        configuration: OnvifAnalyticsEngineConfiguration,
        username: String? = null,
        password: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(url)
            analyticsService.setAnalyticsEngine(normalizedUrl, engineToken, configuration, username, password)
        } catch (e: Exception) {
            logger.error(e) { "Failed to set analytics engine: $engineToken" }
            Result.failure(e)
        }
    }

    /**
     * Удалить аналитический движок
     *
     * @param url URL камеры
     * @param engineToken Токен движка
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @return Результат операции
     */
    suspend fun deleteAnalyticsEngine(
        url: String,
        engineToken: String,
        username: String? = null,
        password: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(url)
            analyticsService.deleteAnalyticsEngine(normalizedUrl, engineToken, username, password)
        } catch (e: Exception) {
            logger.error(e) { "Failed to delete analytics engine: $engineToken" }
            Result.failure(e)
        }
    }

    /**
     * Получить входные данные аналитического движка
     *
     * @param url URL камеры
     * @param engineToken Токен движка
     * @param inputToken Токен входных данных
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @return Результат с входными данными или ошибкой
     */
    suspend fun getAnalyticsEngineInput(
        url: String,
        engineToken: String,
        inputToken: String,
        username: String? = null,
        password: String? = null
    ): Result<OnvifAnalyticsEngineInput> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(url)
            analyticsService.getAnalyticsEngineInput(normalizedUrl, engineToken, inputToken, username, password)
        } catch (e: Exception) {
            logger.error(e) { "Failed to get analytics engine input: $inputToken" }
            Result.failure(e)
        }
    }

    /**
     * Установить входные данные для аналитического движка
     *
     * @param url URL камеры
     * @param engineToken Токен движка
     * @param inputToken Токен входных данных
     * @param configuration Конфигурация входных данных
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @return Результат операции
     */
    suspend fun setAnalyticsEngineInput(
        url: String,
        engineToken: String,
        inputToken: String,
        configuration: OnvifAnalyticsEngineInputConfiguration,
        username: String? = null,
        password: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(url)
            analyticsService.setAnalyticsEngineInput(
                normalizedUrl,
                engineToken,
                inputToken,
                configuration,
                username,
                password
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to set analytics engine input: $inputToken" }
            Result.failure(e)
        }
    }

    // === ONVIF Imaging Service методы ===

    /**
     * Получить настройки изображения камеры
     *
     * @param url URL камеры
     * @param videoSourceToken Токен видеоисточника
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @return Результат с настройками изображения или ошибкой
     */
    suspend fun getImagingSettings(
        url: String,
        videoSourceToken: String,
        username: String? = null,
        password: String? = null
    ): Result<OnvifImagingSettings> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(url)
            imagingService.getImagingSettings(normalizedUrl, videoSourceToken, username, password)
        } catch (e: Exception) {
            logger.error(e) { "Failed to get imaging settings for camera: $url" }
            Result.failure(e)
        }
    }

    /**
     * Установить настройки изображения камеры
     *
     * @param url URL камеры
     * @param videoSourceToken Токен видеоисточника
     * @param settings Настройки изображения
     * @param forcePersistence Принудительно сохранить настройки
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @return Результат операции
     */
    suspend fun setImagingSettings(
        url: String,
        videoSourceToken: String,
        settings: OnvifImagingSettings,
        forcePersistence: Boolean = false,
        username: String? = null,
        password: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(url)
            imagingService.setImagingSettings(
                normalizedUrl,
                videoSourceToken,
                settings,
                forcePersistence,
                username,
                password
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to set imaging settings for camera: $url" }
            Result.failure(e)
        }
    }

    /**
     * Получить доступные опции для настроек изображения
     *
     * @param url URL камеры
     * @param videoSourceToken Токен видеоисточника
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @return Результат с опциями или ошибкой
     */
    suspend fun getImagingOptions(
        url: String,
        videoSourceToken: String,
        username: String? = null,
        password: String? = null
    ): Result<OnvifImagingOptions> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(url)
            imagingService.getOptions(normalizedUrl, videoSourceToken, username, password)
        } catch (e: Exception) {
            logger.error(e) { "Failed to get imaging options for camera: $url" }
            Result.failure(e)
        }
    }

    /**
     * Получить список пресетов изображения
     *
     * @param url URL камеры
     * @param videoSourceToken Токен видеоисточника
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @return Результат со списком пресетов или ошибкой
     */
    suspend fun getImagingPresets(
        url: String,
        videoSourceToken: String,
        username: String? = null,
        password: String? = null
    ): Result<List<OnvifImagingPreset>> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(url)
            imagingService.getPresets(normalizedUrl, videoSourceToken, username, password)
        } catch (e: Exception) {
            logger.error(e) { "Failed to get imaging presets for camera: $url" }
            Result.failure(e)
        }
    }

    /**
     * Установить пресет изображения
     *
     * @param url URL камеры
     * @param videoSourceToken Токен видеоисточника
     * @param presetToken Токен пресета (опционально, для обновления существующего)
     * @param presetName Имя пресета
     * @param settings Настройки изображения для пресета
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @return Результат с токеном пресета или ошибкой
     */
    suspend fun setImagingPreset(
        url: String,
        videoSourceToken: String,
        presetToken: String? = null,
        presetName: String,
        settings: OnvifImagingSettings,
        username: String? = null,
        password: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(url)
            imagingService.setPreset(
                normalizedUrl,
                videoSourceToken,
                presetToken,
                presetName,
                settings,
                username,
                password
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to set imaging preset for camera: $url" }
            Result.failure(e)
        }
    }

    /**
     * Удалить пресет изображения
     *
     * @param url URL камеры
     * @param videoSourceToken Токен видеоисточника
     * @param presetToken Токен пресета
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @return Результат операции
     */
    suspend fun removeImagingPreset(
        url: String,
        videoSourceToken: String,
        presetToken: String,
        username: String? = null,
        password: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(url)
            imagingService.removePreset(normalizedUrl, videoSourceToken, presetToken, username, password)
        } catch (e: Exception) {
            logger.error(e) { "Failed to remove imaging preset for camera: $url" }
            Result.failure(e)
        }
    }

    // === Приватные методы для работы с SOAP ===

    /**
     * Проверка, нужно ли обрабатывать устройство
     */
    private fun shouldProcessDevice(device: DiscoveredDevice): Boolean {
        // Фильтрация по типам устройств ONVIF
        val isOnvifDevice = device.types.any { type ->
            type.contains("NetworkVideoTransmitter", ignoreCase = true) ||
                type.contains("NetworkVideoDisplay", ignoreCase = true) ||
                type.contains("Video", ignoreCase = true) ||
                type.contains("Camera", ignoreCase = true) ||
                type.contains("onvif", ignoreCase = true) ||
                type.contains("Device", ignoreCase = true) ||
                type.contains("NetworkVideoRecorder", ignoreCase = true) ||
                type.contains("NetworkVideoEncoder", ignoreCase = true)
        }

        // Проверка scopes на наличие ONVIF
        val hasOnvifScope = device.scopes.any { scope ->
            scope.contains("onvif", ignoreCase = true) ||
                scope.contains("www.onvif.org", ignoreCase = true) ||
                scope.contains("onvif.org", ignoreCase = true)
        }

        // Улучшенная логика фильтрации
        return when {
            // Если есть явные ONVIF признаки - обрабатываем
            isOnvifDevice -> true
            hasOnvifScope -> true
            // Если типы не указаны, но есть XAddrs - проверяем по XAddr (может быть ONVIF)
            device.types.isEmpty() && device.xAddrs.isNotEmpty() -> true
            // Если типы указаны, но нет ONVIF признаков - пропускаем
            device.types.isNotEmpty() && !isOnvifDevice -> false
            // По умолчанию, если есть XAddrs - пробуем обработать
            else -> device.xAddrs.isNotEmpty()
        }
    }

    /**
     * Объединение результатов discovery с дедупликацией по host:port.
     * При конфликте сохраняем более информативную запись (manufacturer/model/capabilities).
     */
    private fun mergeDiscoveredCameras(cameras: List<DiscoveredCamera>): List<DiscoveredCamera> {
        if (cameras.isEmpty()) return emptyList()

        val byEndpoint = LinkedHashMap<String, DiscoveredCamera>()
        for (camera in cameras) {
            val endpointKey = extractHostAndPort(camera.url)?.let { (host, port) ->
                if (port != null) "${host.lowercase()}:$port" else host.lowercase()
            } ?: camera.url.trim().lowercase().trimEnd('/')

            val existing = byEndpoint[endpointKey]
            if (existing == null) {
                byEndpoint[endpointKey] = camera
                continue
            }

            val existingScore =
                (if (!existing.manufacturer.isNullOrBlank()) 1 else 0) +
                    (if (!existing.model.isNullOrBlank()) 1 else 0) +
                    (if (existing.capabilities != null) 1 else 0)
            val incomingScore =
                (if (!camera.manufacturer.isNullOrBlank()) 1 else 0) +
                    (if (!camera.model.isNullOrBlank()) 1 else 0) +
                    (if (camera.capabilities != null) 1 else 0)

            if (incomingScore > existingScore) {
                byEndpoint[endpointKey] = camera
            }
        }

        return byEndpoint.values.toList()
    }

    /**
     * Обработка одного XAddr с retry логикой
     */
    private suspend fun processDeviceXAddr(
        xAddr: String,
        device: DiscoveredDevice
    ): DiscoveredCamera? {
        return try {
            // Нормализация URL
            val normalizedUrl = normalizeUrl(xAddr)

            // Пропускаем пустые URL
            if (normalizedUrl.isBlank()) {
                logger.debug { "Skipping empty XAddr" }
                return null
            }

            // Валидация URL перед обработкой
            if (!isValidUrl(normalizedUrl)) {
                logger.debug { "Invalid URL format: $normalizedUrl" }
                return null
            }

            // Retry логика для получения информации о камере
            var deviceInfo: DeviceInformation? = null
            var capabilities: OnvifCapabilities? = null

            // Попытка получить информацию о камере с retry
            deviceInfo = retryWithTimeout(
                maxRetries = 2,
                timeoutMs = 3000,
                operation = { getDeviceInformation(normalizedUrl) }
            )

            // Попытка получить capabilities с retry
            capabilities = retryWithTimeout(
                maxRetries = 2,
                timeoutMs = 2000,
                operation = { getCapabilities(normalizedUrl) }
            )

            val camera = DiscoveredCamera(
                url = normalizedUrl,
                name = deviceInfo?.model
                    ?: deviceInfo?.manufacturer?.let { "$it Camera" }
                    ?: "ONVIF Device",
                manufacturer = deviceInfo?.manufacturer,
                model = deviceInfo?.model,
                capabilities = capabilities?.let { cap ->
                    OnvifCapabilities(
                        deviceServiceUrl = cap.deviceServiceUrl,
                        mediaServiceUrl = cap.mediaServiceUrl,
                        ptzServiceUrl = cap.ptzServiceUrl,
                        eventServiceUrl = cap.eventServiceUrl,
                        analyticsServiceUrl = cap.analyticsServiceUrl,
                        imagingServiceUrl = cap.imagingServiceUrl
                    )
                }
            )

            logger.info {
                "Discovered camera: ${camera.name} at ${camera.url} " +
                    "(types: ${device.types.take(2).joinToString()}, " +
                    "scopes: ${device.scopes.take(1).joinToString()})"
            }

            camera
        } catch (e: Exception) {
            logger.debug(e) { "Error processing device at $xAddr: ${e.message}" }
            // Добавляем камеру даже без полной информации, если URL валидный
            val normalizedUrl = try {
                normalizeUrl(xAddr)
            } catch (e2: Exception) {
                null
            }
            if (normalizedUrl != null && normalizedUrl.isNotBlank() && isValidUrl(normalizedUrl)) {
                DiscoveredCamera(
                    url = normalizedUrl,
                    name = "ONVIF Device",
                    capabilities = null
                )
            } else {
                null
            }
        }
    }

    /**
     * Retry логика с таймаутом
     */
    private suspend fun <T> retryWithTimeout(
        maxRetries: Int,
        timeoutMs: Long,
        operation: suspend () -> T?
    ): T? {
        var lastException: Exception? = null
        for (attempt in 0..maxRetries) {
            try {
                return withTimeout(timeoutMs) {
                    operation()
                }
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries) {
                    delay(200 * (attempt + 1).toLong()) // Exponential backoff
                }
            }
        }
        logger.debug(lastException) { "Failed after $maxRetries retries" }
        return null
    }

    /**
     * Валидация URL
     */
    private fun isValidUrl(url: String): Boolean {
        val schemeEnd = url.indexOf("://")
        if (schemeEnd <= 0) return false
        val scheme = url.substring(0, schemeEnd).lowercase()
        if (scheme != "http" && scheme != "https") return false
        val hostStart = schemeEnd + 3
        val pathStart = url.indexOf('/', hostStart).takeIf { it >= 0 } ?: url.length
        val authority = url.substring(hostStart, pathStart)
        val host = authority.substringAfter('@', authority).substringBefore(':')
        return host.isNotBlank()
    }

    private fun normalizeUrl(url: String): String {
        return if (url.startsWith("rtsp://")) {
            url.replace("rtsp://", "http://").substringBefore("/")
        } else if (!url.startsWith("http")) {
            "http://$url"
        } else {
            url
        }
    }

    private fun createGetCapabilitiesRequest(): String {
        return """<?xml version="1.0" encoding="UTF-8"?>
<s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope">
    <s:Body>
        <tds:GetCapabilities xmlns:tds="http://www.onvif.org/ver10/device/wsdl">
            <tds:Category>All</tds:Category>
        </tds:GetCapabilities>
    </s:Body>
</s:Envelope>"""
    }

    private fun createGetDeviceInformationRequest(): String {
        return """<?xml version="1.0" encoding="UTF-8"?>
<s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope">
    <s:Body>
        <tds:GetDeviceInformation xmlns:tds="http://www.onvif.org/ver10/device/wsdl"/>
    </s:Body>
</s:Envelope>"""
    }

    private fun createGetProfilesRequest(): String {
        return """<?xml version="1.0" encoding="UTF-8"?>
<s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope">
    <s:Body>
        <trt:GetProfiles xmlns:trt="http://www.onvif.org/ver10/media/wsdl"/>
    </s:Body>
</s:Envelope>"""
    }

    private fun createGetStreamUriRequest(profileToken: String): String {
        return """<?xml version="1.0" encoding="UTF-8"?>
<s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope">
    <s:Body>
        <trt:GetStreamUri xmlns:trt="http://www.onvif.org/ver10/media/wsdl">
            <trt:ProfileToken>$profileToken</trt:ProfileToken>
            <tt:StreamSetup xmlns:tt="http://www.onvif.org/ver10/schema">
                <tt:Stream>RTP-Unicast</tt:Stream>
                <tt:Transport>
                    <tt:Protocol>RTSP</tt:Protocol>
                </tt:Transport>
            </tt:StreamSetup>
        </trt:GetStreamUri>
    </s:Body>
</s:Envelope>"""
    }

    private fun createContinuousMoveRequest(direction: PtzDirection, speed: Float): String {
        val pan = direction.getPan(speed)
        val tilt = direction.getTilt(speed)
        val zoom = direction.getZoom(speed)

        val panTiltElement = if (pan != 0f || tilt != 0f) {
            """<tt:PanTilt x="$pan" y="$tilt" space="http://www.onvif.org/ver10/tptz/PanTiltSpaces/PositionGenericSpace"/>"""
        } else {
            ""
        }

        val zoomElement = if (zoom != 0f) {
            """<tt:Zoom x="$zoom" space="http://www.onvif.org/ver10/tptz/ZoomSpaces/PositionGenericSpace"/>"""
        } else {
            ""
        }

        val velocityContent = buildString {
            if (panTiltElement.isNotEmpty()) {
                append(panTiltElement)
            }
            if (zoomElement.isNotEmpty()) {
                append(zoomElement)
            }
        }

        return """<?xml version="1.0" encoding="UTF-8"?>
<s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope">
    <s:Body>
        <tptz:ContinuousMove xmlns:tptz="http://www.onvif.org/ver20/ptz/wsdl">
            <tptz:ProfileToken>Profile1</tptz:ProfileToken>
            <tt:Velocity xmlns:tt="http://www.onvif.org/ver10/schema">
                $velocityContent
            </tt:Velocity>
        </tptz:ContinuousMove>
    </s:Body>
</s:Envelope>"""
    }

    private fun createZoomRequest(profileToken: String, zoomSpeed: Float): String {
        return """<?xml version="1.0" encoding="UTF-8"?>
<s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope">
    <s:Body>
        <tptz:ContinuousMove xmlns:tptz="http://www.onvif.org/ver20/ptz/wsdl">
            <tptz:ProfileToken>$profileToken</tptz:ProfileToken>
            <tt:Velocity xmlns:tt="http://www.onvif.org/ver10/schema">
                <tt:Zoom x="$zoomSpeed" space="http://www.onvif.org/ver10/tptz/ZoomSpaces/PositionGenericSpace"/>
            </tt:Velocity>
        </tptz:ContinuousMove>
    </s:Body>
</s:Envelope>"""
    }

    private fun createStopRequest(profileToken: String): String {
        return """<?xml version="1.0" encoding="UTF-8"?>
<s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope">
    <s:Body>
        <tptz:Stop xmlns:tptz="http://www.onvif.org/ver20/ptz/wsdl">
            <tptz:ProfileToken>$profileToken</tptz:ProfileToken>
            <tptz:PanTilt>true</tptz:PanTilt>
            <tptz:Zoom>true</tptz:Zoom>
        </tptz:Stop>
    </s:Body>
</s:Envelope>"""
    }

    private fun createGotoPresetRequest(presetToken: String, profileToken: String): String {
        return """<?xml version="1.0" encoding="UTF-8"?>
<s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope">
    <s:Body>
        <tptz:GotoPreset xmlns:tptz="http://www.onvif.org/ver20/ptz/wsdl">
            <tptz:ProfileToken>$profileToken</tptz:ProfileToken>
            <tptz:PresetToken>$presetToken</tptz:PresetToken>
        </tptz:GotoPreset>
    </s:Body>
</s:Envelope>"""
    }

    private suspend fun sendSoapRequest(
        url: String,
        soapBody: String,
        username: String? = null,
        password: String? = null,
        retries: Int = 2
    ): String {
        var lastException: Exception? = null
        var useDigestAuth = false
        var digestParams: DigestAuthParams? = null
        var nc = 1 // Счетчик запросов для Digest Authentication
        var attempt = 0

        // Проверяем кэш для Digest параметров перед первым запросом
        digestParams = digestAuthCache[url]
        if (digestParams != null) {
            useDigestAuth = true
            logger.debug { "Using cached Digest Authentication parameters for $url" }
        }

        while (attempt <= retries) {
            try {
                val response = client.post(url) {
                    contentType(ContentType.Text.Xml)
                    header("SOAPAction", "")

                    // Аутентификация: Basic или Digest
                    if (username != null && password != null) {
                        if (useDigestAuth && digestParams != null) {
                            // Используем Digest Authentication
                            // Извлекаем путь из URL для Digest (только путь, без query параметров)
                            val uriPath = try {
                                extractPathFromUrl(url)
                            } catch (e: Exception) {
                                // Fallback: извлекаем путь вручную
                                val pathStart = url.indexOf("/", url.indexOf("://") + 3)
                                if (pathStart >= 0) {
                                    val pathEnd = url.indexOf("?", pathStart).takeIf { it >= 0 } ?: url.length
                                    url.substring(pathStart, pathEnd).ifEmpty { "/" }
                                } else {
                                    "/"
                                }
                            }

                            val digestHeader = DigestAuthHelper.generateDigestAuthHeader(
                                username = username,
                                password = password,
                                method = "POST",
                                uri = uriPath,
                                params = digestParams,
                                nc = nc.toString(16).padStart(8, '0'),
                                entityBody = soapBody // Для qop=auth-int
                            )
                            header("Authorization", digestHeader)
                            nc++ // Увеличиваем счетчик для следующего запроса
                            logger.debug { "Using Digest Authentication for $url (nc=$nc)" }
                        } else {
                            // Используем Basic Authentication
                            val credentials = "$username:$password"
                            val encoded = credentials.encodeToByteArray().encodeBase64()
                            header("Authorization", "Basic $encoded")
                            logger.debug { "Using Basic Authentication for $url" }
                        }
                    }

                    setBody(soapBody)
                }

                // Проверка статуса ответа
                if (response.status.value in 200..299) {
                    val responseBody = response.body<String>()

                    // Проверка на SOAP Fault в успешном ответе
                    if (OnvifFaultParser.isSoapFault(responseBody)) {
                        val faultException = OnvifFaultParser.parseSoapFault(responseBody)
                        logger.error { "SOAP Fault in response: ${faultException.faultReason}" }
                        throw faultException
                    }

                    logger.debug { "SOAP request successful to $url (attempt ${attempt + 1})" }
                    return responseBody
                } else if (response.status.value == HttpStatusCode.Unauthorized.value) {
                    // 401 Unauthorized - попытка использовать Digest Authentication
                    logger.debug { "Received 401 Unauthorized, attempting Digest Authentication" }

                    // Извлекаем WWW-Authenticate заголовок (улучшенная обработка)
                    val wwwAuthenticate = response.headers["WWW-Authenticate"]
                        ?: response.headers["www-authenticate"]
                        ?: response.headers.getAll("WWW-Authenticate")?.firstOrNull()
                        ?: response.headers.getAll("www-authenticate")?.firstOrNull()
                        ?: run {
                            // Попытка найти заголовок без учета регистра
                            response.headers.entries().firstOrNull {
                                it.key.equals("WWW-Authenticate", ignoreCase = true)
                            }?.value?.firstOrNull()
                        }

                    if (wwwAuthenticate != null && wwwAuthenticate.startsWith("Digest", ignoreCase = true)) {
                        // Парсим Digest параметры
                        val parsedParams = DigestAuthHelper.parseWWWAuthenticate(wwwAuthenticate)

                        if (parsedParams != null) {
                            // Проверка на stale nonce - если stale=true, очищаем кэш и запрашиваем новый nonce
                            if (parsedParams.stale) {
                                logger.warn { "Received stale nonce from $url, clearing cache and requesting new nonce" }
                                digestAuthCache.remove(url)
                                digestParams = null
                                useDigestAuth = false
                                nc = 1
                                // Продолжаем попытку с новым nonce
                                if (attempt < retries) {
                                    delay(100) // Небольшая задержка перед retry
                                    attempt++
                                    continue
                                }
                            } else {
                                // Кэшируем параметры для этого URL
                                digestAuthCache[url] = parsedParams
                                digestParams = parsedParams
                                useDigestAuth = true
                                nc = 1 // Сбрасываем счетчик для нового nonce
                                logger.info { "Switching to Digest Authentication for $url (algorithm: ${parsedParams.algorithm}, qop: ${parsedParams.qop})" }

                                // Повторяем запрос с Digest Authentication
                                if (attempt < retries) {
                                    delay(100) // Небольшая задержка перед retry
                                    attempt++
                                    continue
                                }
                            }
                        } else {
                            logger.warn { "Failed to parse WWW-Authenticate header: $wwwAuthenticate" }
                            // Если не удалось распарсить, пробуем использовать Basic Auth еще раз
                            if (attempt < retries) {
                                delay(100)
                                attempt++
                                continue
                            }
                        }
                    } else if (wwwAuthenticate != null && wwwAuthenticate.startsWith("Basic", ignoreCase = true)) {
                        // Если сервер требует Basic Auth, продолжаем с Basic
                        logger.debug { "Server requires Basic Authentication for $url" }
                        if (attempt < retries) {
                            delay(100)
                            attempt++
                            continue
                        }
                    } else if (wwwAuthenticate == null && username != null && password != null) {
                        // Если заголовок отсутствует, но есть учетные данные, пробуем еще раз с Basic
                        logger.debug { "No WWW-Authenticate header found, retrying with Basic Auth" }
                        if (attempt < retries) {
                            delay(100)
                            attempt++
                            continue
                        }
                    }

                    // Если не удалось использовать Digest, выбрасываем ошибку
                    val errorMsg = "HTTP ${response.status.value}: ${response.status.description}"
                    logger.warn { "SOAP request failed: $errorMsg (attempt ${attempt + 1})" }
                    if (attempt < retries) {
                        kotlinx.coroutines.delay(500 * (attempt + 1).toLong()) // Exponential backoff
                        attempt++
                    } else {
                        throw OnvifAuthenticationException(
                            message = errorMsg,
                            httpStatusCode = response.status.value
                        )
                    }
                } else if (response.status.value == HttpStatusCode.Forbidden.value) {
                    // 403 Forbidden
                    val errorMsg = "HTTP ${response.status.value}: ${response.status.description}"
                    logger.warn { "SOAP request forbidden: $errorMsg" }
                    throw OnvifAuthenticationException(
                        message = "Access forbidden: $errorMsg",
                        httpStatusCode = response.status.value
                    )
                } else if (response.status.value == HttpStatusCode.NotFound.value) {
                    // 404 Not Found - метод не поддерживается
                    val errorMsg = "HTTP ${response.status.value}: ${response.status.description}"
                    logger.warn { "SOAP request not found: $errorMsg" }
                    throw OnvifNotSupportedException("Method not found at $url")
                } else {
                    // Другие HTTP ошибки
                    val responseBody = try {
                        response.body<String>()
                    } catch (e: Exception) {
                        null
                    }

                    // Проверка на SOAP Fault
                    if (responseBody != null && OnvifFaultParser.isSoapFault(responseBody)) {
                        throw OnvifFaultParser.parseSoapFault(responseBody)
                    }

                    val errorMsg = "HTTP ${response.status.value}: ${response.status.description}"
                    logger.warn { "SOAP request failed: $errorMsg (attempt ${attempt + 1})" }
                    if (attempt < retries) {
                        kotlinx.coroutines.delay(500 * (attempt + 1).toLong()) // Exponential backoff
                        attempt++
                    } else {
                        throw OnvifException("HTTP error: $errorMsg")
                    }
                }
            } catch (e: OnvifException) {
                // Пробрасываем ONVIF исключения без изменений (не retry для постоянных ошибок)
                throw e
            } catch (e: Exception) {
                lastException = OnvifNetworkException(
                    message = "Network error: ${e.message}",
                    cause = e
                )
                logger.warn(e) { "SOAP request error (attempt ${attempt + 1}): ${e.message}" }
                if (attempt < retries) {
                    kotlinx.coroutines.delay(500 * (attempt + 1).toLong())
                    attempt++
                } else {
                    break
                }
            }
        }

        throw lastException ?: OnvifException("SOAP request failed after $retries retries")
    }

    private fun parseCapabilities(xml: String): OnvifCapabilities? {
        return try {
            parseCapabilitiesFallback(xml)
        } catch (e: Exception) {
            logger.error(e) { "Error parsing capabilities XML" }
            null
        }
    }

    private fun parseCapabilitiesFallback(xml: String): OnvifCapabilities? {
        val deviceUrl = extractXmlChildValue(xml, "Device", "XAddr")
        val mediaUrl = extractXmlChildValue(xml, "Media", "XAddr")
        val ptzUrl = extractXmlChildValue(xml, "PTZ", "XAddr")
        val eventUrl = extractXmlChildValue(xml, "Events", "XAddr")
            ?: extractXmlChildValue(xml, "Event", "XAddr")
        val analyticsUrl = extractXmlChildValue(xml, "Analytics", "XAddr")
        val imagingUrl = extractXmlChildValue(xml, "Imaging", "XAddr")

        // Неверный/пустой XML: если ни один сервис не найден, считаем парсинг неуспешным
        if (deviceUrl == null && mediaUrl == null && ptzUrl == null && eventUrl == null &&
            analyticsUrl == null && imagingUrl == null
        ) {
            logger.debug { "No capability service URLs found in XML" }
            return null
        }

        return OnvifCapabilities(
            deviceServiceUrl = deviceUrl,
            mediaServiceUrl = mediaUrl,
            ptzServiceUrl = ptzUrl,
            eventServiceUrl = eventUrl,
            analyticsServiceUrl = analyticsUrl,
            imagingServiceUrl = imagingUrl
        )
    }

    private fun parseDeviceInformation(xml: String): DeviceInformation? {
        return try {
            parseDeviceInformationFallback(xml)
        } catch (e: Exception) {
            logger.error(e) { "Error parsing device information" }
            null
        }
    }

    private fun parseDeviceInformationFallback(xml: String): DeviceInformation? {
        return DeviceInformation(
            manufacturer = extractXmlValue(xml, "Manufacturer") ?: "Unknown",
            model = extractXmlValue(xml, "Model") ?: "Unknown",
            firmwareVersion = extractXmlValue(xml, "FirmwareVersion") ?: "Unknown",
            serialNumber = extractXmlValue(xml, "SerialNumber") ?: "Unknown",
            hardwareId = extractXmlValue(xml, "HardwareId") ?: "Unknown"
        )
    }

    private fun parseProfiles(xml: String): List<OnvifProfile> {
        return try {
            parseProfilesFallback(xml)
        } catch (e: Exception) {
            logger.error(e) { "Error parsing profiles" }
            emptyList()
        }
    }

    private fun parseProfilesFallback(xml: String): List<OnvifProfile> {
        val result = mutableListOf<OnvifProfile>()

        // Поиск всех Profile элементов (включая <trt:Profiles ...>)
        val profileRegex = Regex("<trt:Profiles?[^>]*token=\"([^\"]+)\"[^>]*>([\\s\\S]*?)</trt:Profiles?>")
        val matches = profileRegex.findAll(xml)

        for (match in matches) {
            val token = match.groupValues[1]
            val block = match.groupValues[2]

            val name = extractXmlValue(block, "Name") ?: token

            // Попытка извлечь информацию о видео
            val width = extractXmlValue(block, "Width")?.toIntOrNull() ?: 1920
            val height = extractXmlValue(block, "Height")?.toIntOrNull() ?: 1080
            val codec = extractXmlValue(block, "Encoding") ?: "H.264"
            val fps = extractXmlValue(block, "FrameRateLimit")?.toIntOrNull() ?: 25

            // Попытка извлечь информацию об аудио
            val hasAudio = block.contains("AudioEncoderConfiguration", ignoreCase = true) ||
                block.contains("AudioSourceConfiguration", ignoreCase = true)
            val audioCodec = extractXmlChildValue(block, "AudioEncoderConfiguration", "Encoding")
                ?: extractXmlChildValue(block, "AudioSourceConfiguration", "Encoding")
                ?: extractXmlValue(block, "AudioEncoding")
                ?: extractXmlValue(block, "AudioCodec")

            result.add(
                OnvifProfile(
                    token = token,
                    name = name,
                    videoResolution = Resolution(width, height),
                    fps = fps,
                    codec = codec,
                    hasAudio = hasAudio,
                    audioCodec = audioCodec
                )
            )
        }

        if (result.isEmpty()) {
            // Fallback: создаем один профиль по умолчанию
            result.add(
                OnvifProfile(
                    token = "Profile1",
                    name = "Profile1",
                    videoResolution = Resolution(1920, 1080),
                    fps = 25,
                    codec = "H.264",
                    hasAudio = false,
                    audioCodec = null
                )
            )
        }

        return result
    }

    private fun parseStreamUri(xml: String): String? {
        return try {
            parseStreamUriFallback(xml)
        } catch (e: Exception) {
            logger.error(e) { "Error parsing stream URI" }
            null
        }
    }

    private fun parseStreamUriFallback(xml: String): String? {
        return extractXmlValue(xml, "Uri")
    }

    /**
     * Извлечь значение дочернего тега внутри родительского блока.
     * Например: parent=Device, child=XAddr → ищет <Device>...<XAddr>value</XAddr>...</Device>.
     * Учитывает namespace-префиксы (tt:, tds: и т.п.).
     */
    private fun extractXmlChildValue(xml: String, parentTag: String, childTag: String): String? {
        return try {
            // Находим блок родителя (не жадный, но с учётом вложенности не требуется для XAddr)
            val parentPatterns = listOf(
                Regex("<[^>]*:?$parentTag[^>]*>([\\s\\S]*?)</[^>]*:?$parentTag>"),
                Regex("<$parentTag[^>]*>([\\s\\S]*?)</$parentTag>")
            )
            val parentBlock = parentPatterns.firstNotNullOfOrNull { it.find(xml)?.groupValues?.get(1) }
                ?: return null
            extractXmlValue(parentBlock, childTag)
        } catch (e: Exception) {
            logger.debug(e) { "Error extracting child XML value: $parentTag/$childTag" }
            null
        }
    }

    // Упрощенная функция для извлечения значений из XML
    // Используется как fallback когда XML парсинг не удается
    private fun extractXmlValue(xml: String, tagName: String, attribute: String? = null): String? {
        return try {
            if (attribute != null) {
                // Поиск атрибута в теге (например, <tds:Device XAddr="http://...")
                val patterns = listOf(
                    Regex("<[^:]*:$tagName[^>]*$attribute=\"([^\"]+)\""),
                    Regex("<$tagName[^>]*$attribute=\"([^\"]+)\""),
                    Regex("<[^>]*:$tagName[^>]*$attribute=\"([^\"]+)\"")
                )
                patterns.firstNotNullOfOrNull { it.find(xml)?.groupValues?.get(1) }
            } else {
                // Поиск значения между тегами (например, <tds:Manufacturer>Hikvision</tds:Manufacturer>)
                val patterns = listOf(
                    Regex("<[^>]*:$tagName[^>]*>([^<]+)</[^>]*:$tagName>"),
                    Regex("<$tagName[^>]*>([^<]+)</$tagName>"),
                    Regex("<[^>]*:$tagName>([^<]+)</[^>]*:$tagName>")
                )
                patterns.firstNotNullOfOrNull { it.find(xml)?.groupValues?.get(1) }?.trim()
            }
        } catch (e: Exception) {
            logger.debug(e) { "Error extracting XML value for tag: $tagName, attribute: $attribute" }
            null
        }
    }

    /**
     * Очистить кэш capabilities для конкретной камеры
     */
    fun clearCapabilitiesCache(url: String, username: String? = null) {
        val deviceUrl = normalizeUrl(url)
        val cacheKey = "$deviceUrl:${username ?: ""}"
        capabilitiesCache.remove(cacheKey)
        logger.debug { "Cleared capabilities cache for: $deviceUrl" }
    }

    /**
     * Очистить кэш device information для конкретной камеры
     */
    fun clearDeviceInfoCache(url: String, username: String? = null) {
        val deviceUrl = normalizeUrl(url)
        val cacheKey = "$deviceUrl:${username ?: ""}"
        deviceInfoCache.remove(cacheKey)
        logger.debug { "Cleared device info cache for: $deviceUrl" }
    }

    /**
     * Очистить все кэши
     */
    fun clearAllCaches() {
        capabilitiesCache.clear()
        deviceInfoCache.clear()
        profilesCache.clear()
        digestAuthCache.clear()
        logger.debug { "Cleared all caches" }
    }

    /**
     * Инвалидировать истекшие записи в кэшах
     */
    fun invalidateExpiredCacheEntries() {
        val currentTime = Clock.System.now().toEpochMilliseconds()

        // Очистка истекших capabilities
        capabilitiesCache.entries.removeAll { (_, value) ->
            currentTime - value.second >= capabilitiesCacheTTL
        }

        // Очистка истекших device info
        deviceInfoCache.entries.removeAll { (_, value) ->
            currentTime - value.second >= deviceInfoCacheTTL
        }

        logger.debug { "Invalidated expired cache entries" }
    }

    /**
     * Инвалидировать кэш для конкретного URL
     *
     * @param url URL камеры
     */
    fun invalidateCache(url: String) {
        val normalizedUrl = normalizeUrl(url)
        val cacheKey = "$normalizedUrl:"

        // Удаляем все записи, начинающиеся с этого URL
        capabilitiesCache.keys.removeAll { it.startsWith(normalizedUrl) }
        deviceInfoCache.keys.removeAll { it.startsWith(normalizedUrl) }
        profilesCache.keys.removeAll { it.startsWith(normalizedUrl) }
        digestAuthCache.remove(normalizedUrl)

        logger.debug { "Cache invalidated for: $normalizedUrl" }
    }

    fun close() {
        client.close()
        clearAllCaches()
    }

    private fun extractPathFromUrl(url: String): String {
        val schemeIndex = url.indexOf("://")
        val hostStart = if (schemeIndex >= 0) schemeIndex + 3 else 0
        val pathStart = url.indexOf('/', hostStart)
        if (pathStart < 0) return "/"
        val pathEnd = url.indexOf('?', pathStart).takeIf { it >= 0 } ?: url.length
        return url.substring(pathStart, pathEnd).ifEmpty { "/" }
    }

    private fun extractHostAndPort(url: String): Pair<String, Int?>? {
        val schemeIndex = url.indexOf("://")
        if (schemeIndex <= 0) return null
        val hostStart = schemeIndex + 3
        val hostEnd = url.indexOf('/', hostStart).takeIf { it >= 0 } ?: url.length
        val authority = url.substring(hostStart, hostEnd).substringAfter('@', "")
        if (authority.isBlank()) return null
        val host = authority.substringBefore(':').ifBlank { return null }
        val port = authority.substringAfter(':', "").toIntOrNull()
        return host to port
    }
}

/**
 * Данные о возможностях ONVIF камеры
 */
data class OnvifCapabilities(
    val deviceServiceUrl: String?,
    val mediaServiceUrl: String?,
    val ptzServiceUrl: String?,
    val eventServiceUrl: String? = null,
    val analyticsServiceUrl: String? = null,
    val imagingServiceUrl: String? = null
)

/**
 * Информация об устройстве
 */
data class DeviceInformation(
    val manufacturer: String,
    val model: String,
    val firmwareVersion: String,
    val serialNumber: String,
    val hardwareId: String
)

/**
 * ONVIF профиль камеры
 */
data class OnvifProfile(
    val token: String,
    val name: String,
    val videoResolution: Resolution? = null,
    val fps: Int? = null,
    val codec: String? = null,
    val hasAudio: Boolean = false,
    val audioCodec: String? = null
)

/**
 * Направление движения PTZ
 */
enum class PtzDirection {
    UP, DOWN, LEFT, RIGHT, UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT, ZOOM_IN, ZOOM_OUT, STOP;

    fun getPan(speed: Float): Float {
        return when (this) {
            LEFT, UP_LEFT, DOWN_LEFT -> -speed
            RIGHT, UP_RIGHT, DOWN_RIGHT -> speed
            else -> 0f
        }
    }

    fun getTilt(speed: Float): Float {
        return when (this) {
            UP, UP_LEFT, UP_RIGHT -> speed
            DOWN, DOWN_LEFT, DOWN_RIGHT -> -speed
            else -> 0f
        }
    }

    fun getZoom(speed: Float): Float {
        return when (this) {
            ZOOM_IN -> speed
            ZOOM_OUT -> -speed
            else -> 0f
        }
    }
}
