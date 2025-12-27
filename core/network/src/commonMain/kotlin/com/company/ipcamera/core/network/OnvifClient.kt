package com.company.ipcamera.core.network

import com.company.ipcamera.core.common.model.CameraStatus
import com.company.ipcamera.core.common.model.Resolution
import com.company.ipcamera.core.common.security.InputValidator
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.xml.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.xml.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * ONVIF клиент для обнаружения и управления камерами
 */
class OnvifClient(
    private val engine: HttpClientEngine
) {
    private val client: HttpClient by lazy {
        HttpClient(engine) {
            install(ContentNegotiation) {
                xml()
            }
            engine {
                config {
                    requestTimeoutMillis = 5000
                    connectTimeoutMillis = 3000
                }
            }
        }
    }

    /**
     * Обнаружить камеры в сети через WS-Discovery
     */
    suspend fun discoverCameras(timeoutMillis: Long = 5000): List<DiscoveredCamera> = withContext(Dispatchers.IO) {
        val wsDiscovery = WSDiscovery()
        try {
            logger.info { "Starting ONVIF camera discovery via WS-Discovery..." }

            // Использование WS-Discovery для обнаружения устройств
            val discoveredDevices = wsDiscovery.discover(timeoutMillis)
            logger.info { "WS-Discovery found ${discoveredDevices.size} devices" }

            val discoveredCameras = mutableListOf<DiscoveredCamera>()

            // Преобразование обнаруженных устройств в камеры
            for (device in discoveredDevices) {
                // Фильтрация по типам устройств ONVIF
                val isOnvifDevice = device.types.any { type ->
                    type.contains("NetworkVideoTransmitter", ignoreCase = true) ||
                    type.contains("NetworkVideoDisplay", ignoreCase = true) ||
                    type.contains("Video", ignoreCase = true) ||
                    type.contains("Camera", ignoreCase = true) ||
                    type.contains("onvif", ignoreCase = true) ||
                    type.contains("Device", ignoreCase = true)
                }

                // Проверка scopes на наличие ONVIF
                val hasOnvifScope = device.scopes.any { scope ->
                    scope.contains("onvif", ignoreCase = true) ||
                    scope.contains("www.onvif.org", ignoreCase = true)
                }

                // Если типы не указаны, но есть ONVIF scope, считаем валидным
                // Если типы указаны и нет ONVIF признаков, пропускаем
                val shouldProcess = when {
                    device.types.isEmpty() -> hasOnvifScope || true // Если типов нет, проверяем по XAddr
                    isOnvifDevice -> true
                    hasOnvifScope -> true
                    else -> false
                }

                if (shouldProcess) {
                    for (xAddr in device.xAddrs) {
                        try {
                            // Нормализация URL
                            val normalizedUrl = normalizeUrl(xAddr)

                            // Пропускаем пустые URL
                            if (normalizedUrl.isBlank()) {
                                logger.debug { "Skipping empty XAddr" }
                                continue
                            }

                            // Попытка получить информацию о камере с таймаутом
                            var deviceInfo: DeviceInformation? = null
                            var capabilities: CameraCapabilities? = null

                            try {
                                withTimeout(3000) { // 3 секунды таймаут для получения информации
                                    deviceInfo = getDeviceInformation(normalizedUrl)
                                }
                            } catch (e: Exception) {
                                logger.debug(e) { "Timeout or error getting device info for $normalizedUrl" }
                            }

                            try {
                                withTimeout(2000) { // 2 секунды для capabilities
                                    capabilities = getCapabilities(normalizedUrl)
                                }
                            } catch (e: Exception) {
                                logger.debug(e) { "Timeout or error getting capabilities for $normalizedUrl" }
                            }

                            val camera = DiscoveredCamera(
                                url = normalizedUrl,
                                name = deviceInfo?.model
                                    ?: deviceInfo?.manufacturer?.let { "$it Camera" }
                                    ?: "ONVIF Device",
                                manufacturer = deviceInfo?.manufacturer,
                                model = deviceInfo?.model,
                                capabilities = capabilities
                            )
                            discoveredCameras.add(camera)
                            logger.info {
                                "Discovered camera: ${camera.name} at ${camera.url} " +
                                "(types: ${device.types.take(2).joinToString()}, " +
                                "scopes: ${device.scopes.take(1).joinToString()})"
                            }
                        } catch (e: Exception) {
                            logger.debug(e) { "Error processing device at $xAddr: ${e.message}" }
                            // Добавляем камеру даже без полной информации, если URL валидный
                            val normalizedUrl = try {
                                normalizeUrl(xAddr)
                            } catch (e2: Exception) {
                                null
                            }
                            if (normalizedUrl != null && normalizedUrl.isNotBlank()) {
                                discoveredCameras.add(
                                    DiscoveredCamera(
                                        url = normalizedUrl,
                                        name = "ONVIF Device",
                                        capabilities = null
                                    )
                                )
                            }
                        }
                    }
                } else {
                    logger.debug {
                        "Skipping device with types: ${device.types.take(2).joinToString()} " +
                        "(not an ONVIF video device)"
                    }
                }
            }

            logger.info { "Camera discovery completed. Found ${discoveredCameras.size} cameras" }
            discoveredCameras

        } catch (e: Exception) {
            logger.error(e) { "Error during camera discovery" }
            emptyList()
        } finally {
            wsDiscovery.close()
        }
    }

    /**
     * Получить информацию о камере через ONVIF Device Management
     */
    suspend fun getDeviceInformation(
        url: String,
        username: String? = null,
        password: String? = null
    ): DeviceInformation? = withContext(Dispatchers.IO) {
        try {
            logger.info { "Getting device information from: $url" }

            val deviceUrl = normalizeUrl(url)
            val capabilities = getCapabilities(deviceUrl, username, password)
                ?: return@withContext null

            val deviceServiceUrl = capabilities.deviceServiceUrl ?: return@withContext null

            val soapMessage = createGetDeviceInformationRequest()
            val response = sendSoapRequest(deviceServiceUrl, soapMessage, username, password)

            parseDeviceInformation(response)

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
        password: String? = null
    ): OnvifCapabilities? = withContext(Dispatchers.IO) {
        try {
            val deviceUrl = normalizeUrl(url)
            val capabilitiesUrl = "$deviceUrl/onvif/device_service"

            val soapMessage = createGetCapabilitiesRequest()
            val response = sendSoapRequest(capabilitiesUrl, soapMessage, username, password)

            parseCapabilities(response)

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
     * Получить профили камеры
     */
    suspend fun getProfiles(
        url: String,
        username: String? = null,
        password: String? = null
    ): List<OnvifProfile> = withContext(Dispatchers.IO) {
        try {
            val deviceUrl = normalizeUrl(url)
            val capabilities = getCapabilities(deviceUrl, username, password)
                ?: return@withContext emptyList()

            val mediaServiceUrl = capabilities.mediaServiceUrl ?: return@withContext emptyList()

            val soapMessage = createGetProfilesRequest()
            val response = sendSoapRequest(mediaServiceUrl, soapMessage, username, password)

            parseProfiles(response)

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
                        analytics = false
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

        } catch (e: java.net.SocketTimeoutException) {
            logger.error(e) { "Connection test timeout: ${e.message}" }
            ConnectionTestResult.Failure(
                error = "Connection timeout: ${e.message}",
                code = ErrorCode.TIMEOUT
            )
        } catch (e: java.net.UnknownHostException) {
            logger.error(e) { "Unknown host: ${e.message}" }
            ConnectionTestResult.Failure(
                error = "Unknown host: ${e.message}",
                code = ErrorCode.CONNECTION_FAILED
            )
        } catch (e: java.net.ConnectException) {
            logger.error(e) { "Connection refused: ${e.message}" }
            ConnectionTestResult.Failure(
                error = "Connection refused: ${e.message}",
                code = ErrorCode.CONNECTION_FAILED
            )
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

    // === Приватные методы для работы с SOAP ===

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
        return """<?xml version="1.0" encoding="UTF-8"?>
<s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope">
    <s:Body>
        <tptz:ContinuousMove xmlns:tptz="http://www.onvif.org/ver20/ptz/wsdl">
            <tptz:ProfileToken>Profile1</tptz:ProfileToken>
            <tt:Velocity xmlns:tt="http://www.onvif.org/ver10/schema">
                <tt:PanTilt x="$pan" y="$tilt" space="http://www.onvif.org/ver10/tptz/PanTiltSpaces/PositionGenericSpace"/>
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
        </tptz:Stop>
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

        repeat(retries + 1) { attempt ->
            try {
                val response = client.post(url) {
                    contentType(ContentType.Text.Xml.withCharset(Charsets.UTF_8))
                    header("SOAPAction", "")

                    // Basic Auth если предоставлены учетные данные
                    if (username != null && password != null) {
                        val credentials = "$username:$password"
                        val encoded = java.util.Base64.getEncoder().encodeToString(credentials.toByteArray())
                        header("Authorization", "Basic $encoded")
                    }

                    setBody(soapBody)
                }

                // Проверка статуса ответа
                if (response.status.value in 200..299) {
                    val responseBody = response.body<String>()
                    logger.debug { "SOAP request successful to $url (attempt ${attempt + 1})" }
                    return responseBody
                } else {
                    val errorMsg = "HTTP ${response.status.value}: ${response.status.description}"
                    logger.warn { "SOAP request failed: $errorMsg (attempt ${attempt + 1})" }
                    if (attempt < retries) {
                        kotlinx.coroutines.delay(500 * (attempt + 1).toLong()) // Exponential backoff
                    } else {
                        throw Exception(errorMsg)
                    }
                }
            } catch (e: java.net.SocketTimeoutException) {
                lastException = e
                logger.warn(e) { "SOAP request timeout (attempt ${attempt + 1})" }
                if (attempt < retries) {
                    kotlinx.coroutines.delay(500 * (attempt + 1).toLong())
                }
            } catch (e: Exception) {
                lastException = e
                logger.warn(e) { "SOAP request error (attempt ${attempt + 1}): ${e.message}" }
                if (attempt < retries) {
                    kotlinx.coroutines.delay(500 * (attempt + 1).toLong())
                } else {
                    throw e
                }
            }
        }

        throw lastException ?: Exception("SOAP request failed after $retries retries")
    }

    private fun parseCapabilities(xml: String): OnvifCapabilities? {
        return try {
            val xmlParser = createXmlParser()

            // Пытаемся распарсить как SOAP Envelope
            try {
                val envelope = xmlParser.decodeFromString<SoapEnvelope>(xml)

                // Проверка на SOAP Fault
                if (envelope.body.fault != null) {
                    logger.warn { "SOAP Fault received: ${envelope.body.fault.reason?.text ?: "Unknown error"}" }
                    return null
                }

                val capabilities = envelope.body.capabilities?.capabilities

                if (capabilities != null) {
                    OnvifCapabilities(
                        deviceServiceUrl = capabilities.device?.xAddr,
                        mediaServiceUrl = capabilities.media?.xAddr,
                        ptzServiceUrl = capabilities.ptz?.xAddr
                    )
                } else {
                    // Fallback на упрощенный парсинг
                    logger.debug { "Capabilities not found in SOAP body, using fallback parsing" }
                    parseCapabilitiesFallback(xml)
                }
            } catch (e: kotlinx.serialization.SerializationException) {
                // Fallback на упрощенный парсинг если XML парсинг не удался
                logger.debug(e) { "XML serialization failed, using fallback regex parsing" }
                parseCapabilitiesFallback(xml)
            } catch (e: Exception) {
                // Fallback на упрощенный парсинг если XML парсинг не удался
                logger.debug(e) { "XML parsing failed with unexpected error, using fallback" }
                parseCapabilitiesFallback(xml)
            }
        } catch (e: Exception) {
            logger.error(e) { "Error parsing capabilities XML" }
            null
        }
    }

    private fun parseCapabilitiesFallback(xml: String): OnvifCapabilities? {
        val deviceUrl = extractXmlValue(xml, "Device", "XAddr")
        val mediaUrl = extractXmlValue(xml, "Media", "XAddr")
        val ptzUrl = extractXmlValue(xml, "PTZ", "XAddr")

        return OnvifCapabilities(
            deviceServiceUrl = deviceUrl,
            mediaServiceUrl = mediaUrl,
            ptzServiceUrl = ptzUrl
        )
    }

    private fun parseDeviceInformation(xml: String): DeviceInformation? {
        return try {
            val xmlParser = createXmlParser()

            // Пытаемся распарсить как SOAP Envelope
            try {
                val envelope = xmlParser.decodeFromString<SoapEnvelope>(xml)

                // Проверка на SOAP Fault
                if (envelope.body.fault != null) {
                    logger.warn { "SOAP Fault received: ${envelope.body.fault.reason?.text ?: "Unknown error"}" }
                    return null
                }

                val deviceInfo = envelope.body.deviceInformation

                if (deviceInfo != null) {
                    DeviceInformation(
                        manufacturer = deviceInfo.manufacturer ?: "Unknown",
                        model = deviceInfo.model ?: "Unknown",
                        firmwareVersion = deviceInfo.firmwareVersion ?: "Unknown",
                        serialNumber = deviceInfo.serialNumber ?: "Unknown",
                        hardwareId = deviceInfo.hardwareId ?: "Unknown"
                    )
                } else {
                    // Fallback на упрощенный парсинг
                    logger.debug { "DeviceInformation not found in SOAP body, using fallback parsing" }
                    parseDeviceInformationFallback(xml)
                }
            } catch (e: kotlinx.serialization.SerializationException) {
                // Fallback на упрощенный парсинг если XML парсинг не удался
                logger.debug(e) { "XML serialization failed, using fallback regex parsing" }
                parseDeviceInformationFallback(xml)
            } catch (e: Exception) {
                // Fallback на упрощенный парсинг если XML парсинг не удался
                logger.debug(e) { "XML parsing failed with unexpected error, using fallback" }
                parseDeviceInformationFallback(xml)
            }
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
            val xmlParser = createXmlParser()

            // Пытаемся распарсить как SOAP Envelope
            try {
                val envelope = xmlParser.decodeFromString<SoapEnvelope>(xml)

                // Проверка на SOAP Fault
                if (envelope.body.fault != null) {
                    logger.warn { "SOAP Fault received: ${envelope.body.fault.reason?.text ?: "Unknown error"}" }
                    return emptyList()
                }

                val profilesResponse = envelope.body.profiles
                val profiles = profilesResponse?.profiles

                if (profiles != null && profiles.isNotEmpty()) {
                    profiles.mapNotNull { profileXml ->
                        try {
                            val encoderConfig = profileXml.videoEncoderConfiguration
                            val resolution = encoderConfig?.resolution
                            val width = resolution?.width ?: 1920
                            val height = resolution?.height ?: 1080
                            val codec = encoderConfig?.encoding ?: "H.264"
                            val fps = encoderConfig?.rateControl?.frameRateLimit ?: 25

                            // Проверка наличия аудио конфигурации
                            val hasAudio = profileXml.audioEncoderConfiguration != null ||
                                         profileXml.audioSourceConfiguration != null
                            val audioCodec = profileXml.audioEncoderConfiguration?.encoding

                            OnvifProfile(
                                token = profileXml.token,
                                name = profileXml.name ?: profileXml.token,
                                videoResolution = Resolution(width, height),
                                fps = fps,
                                codec = codec,
                                hasAudio = hasAudio,
                                audioCodec = audioCodec
                            )
                        } catch (e: Exception) {
                            logger.warn(e) { "Error parsing profile ${profileXml.token}" }
                            null
                        }
                    }
                } else {
                    // Fallback на упрощенный парсинг
                    logger.debug { "Profiles not found in SOAP body, using fallback parsing" }
                    parseProfilesFallback(xml)
                }
            } catch (e: kotlinx.serialization.SerializationException) {
                // Fallback на упрощенный парсинг если XML парсинг не удался
                logger.debug(e) { "XML serialization failed, using fallback regex parsing" }
                parseProfilesFallback(xml)
            } catch (e: Exception) {
                // Fallback на упрощенный парсинг если XML парсинг не удался
                logger.debug(e) { "XML parsing failed with unexpected error, using fallback" }
                parseProfilesFallback(xml)
            }
        } catch (e: Exception) {
            logger.error(e) { "Error parsing profiles" }
            emptyList()
        }
    }

    private fun parseProfilesFallback(xml: String): List<OnvifProfile> {
        val result = mutableListOf<OnvifProfile>()

        // Поиск всех Profile элементов
        val profileRegex = Regex("<trt:Profile[^>]*token=\"([^\"]+)\"[^>]*>")
        val matches = profileRegex.findAll(xml)

        for (match in matches) {
            val token = match.groupValues[1]
            val name = extractXmlValue(xml, "Name") ?: token

            // Попытка извлечь информацию о видео
            val width = extractXmlValue(xml, "Width")?.toIntOrNull() ?: 1920
            val height = extractXmlValue(xml, "Height")?.toIntOrNull() ?: 1080
            val codec = extractXmlValue(xml, "Encoding") ?: "H.264"
            val fps = extractXmlValue(xml, "FrameRateLimit")?.toIntOrNull() ?: 25

            // Попытка извлечь информацию об аудио
            val hasAudio = extractXmlValue(xml, "AudioEncoderConfiguration") != null ||
                          extractXmlValue(xml, "AudioSourceConfiguration") != null
            val audioCodec = extractXmlValue(xml, "AudioEncoding") ?:
                           extractXmlValue(xml, "AudioCodec")

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
            val xmlParser = createXmlParser()

            // Пытаемся распарсить как SOAP Envelope
            try {
                val envelope = xmlParser.decodeFromString<SoapEnvelope>(xml)

                // Проверка на SOAP Fault
                if (envelope.body.fault != null) {
                    logger.warn { "SOAP Fault received: ${envelope.body.fault.reason?.text ?: "Unknown error"}" }
                    return null
                }

                val streamUriResponse = envelope.body.streamUri
                streamUriResponse?.uri ?: parseStreamUriFallback(xml)
            } catch (e: kotlinx.serialization.SerializationException) {
                // Fallback на упрощенный парсинг если XML парсинг не удался
                logger.debug(e) { "XML serialization failed, using fallback regex parsing" }
                parseStreamUriFallback(xml)
            } catch (e: Exception) {
                // Fallback на упрощенный парсинг если XML парсинг не удался
                logger.debug(e) { "XML parsing failed with unexpected error, using fallback" }
                parseStreamUriFallback(xml)
            }
        } catch (e: Exception) {
            logger.error(e) { "Error parsing stream URI" }
            null
        }
    }

    private fun parseStreamUriFallback(xml: String): String? {
        return extractXmlValue(xml, "Uri")
    }

    /**
     * Создать настроенный XML парсер для SOAP
     */
    private fun createXmlParser(): Xml {
        return Xml {
            ignoreUnknownChildren = true
            coerceInputValues = true
            isCoerceInputValues = true
            // Поддержка различных префиксов для namespaces
            autoPolymorphic = false
            // Игнорировать неизвестные элементы для лучшей совместимости
            ignoreUnknownNamespaces = true
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

    fun close() {
        client.close()
    }
}

/**
 * Данные о возможностях ONVIF камеры
 */
data class OnvifCapabilities(
    val deviceServiceUrl: String?,
    val mediaServiceUrl: String?,
    val ptzServiceUrl: String?
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

// === SOAP XML Serialization Classes ===

@Serializable
@XmlSerialName("Envelope", namespace = "http://www.w3.org/2003/05/soap-envelope", prefix = "s")
data class SoapEnvelope(
    @XmlElement(true) val body: SoapBody
)

@Serializable
@XmlSerialName("Body", namespace = "http://www.w3.org/2003/05/soap-envelope", prefix = "s")
data class SoapBody(
    @XmlElement(true) val capabilities: CapabilitiesResponse? = null,
    @XmlElement(true) val deviceInformation: DeviceInformationResponse? = null,
    @XmlElement(true) val profiles: ProfilesResponse? = null,
    @XmlElement(true) val streamUri: StreamUriResponse? = null,
    @XmlElement(true) val fault: SoapFault? = null
)

@Serializable
@XmlSerialName("Fault", namespace = "http://www.w3.org/2003/05/soap-envelope", prefix = "s")
data class SoapFault(
    @XmlElement(true) val code: FaultCode? = null,
    @XmlElement(true) val reason: FaultReason? = null,
    @XmlElement(true) val detail: String? = null
)

@Serializable
@XmlSerialName("Code", namespace = "http://www.w3.org/2003/05/soap-envelope", prefix = "s")
data class FaultCode(
    @XmlElement(true) val value: String? = null,
    @XmlElement(true) val subcode: FaultSubcode? = null
)

@Serializable
@XmlSerialName("Subcode", namespace = "http://www.w3.org/2003/05/soap-envelope", prefix = "s")
data class FaultSubcode(
    @XmlElement(true) val value: String? = null
)

@Serializable
@XmlSerialName("Reason", namespace = "http://www.w3.org/2003/05/soap-envelope", prefix = "s")
data class FaultReason(
    @XmlElement(true) val text: String? = null
)

@Serializable
@XmlSerialName("GetCapabilitiesResponse", namespace = "http://www.onvif.org/ver10/device/wsdl", prefix = "tds")
data class CapabilitiesResponse(
    @XmlElement(true) val capabilities: Capabilities? = null
)

@Serializable
@XmlSerialName("Capabilities", namespace = "http://www.onvif.org/ver10/device/wsdl", prefix = "tds")
data class Capabilities(
    @XmlElement(true) val device: DeviceCapabilities? = null,
    @XmlElement(true) val media: MediaCapabilities? = null,
    @XmlElement(true) val ptz: PtzCapabilities? = null
)

@Serializable
@XmlSerialName("Device", namespace = "http://www.onvif.org/ver10/device/wsdl", prefix = "tds")
data class DeviceCapabilities(
    @XmlAttribute(true) val xAddr: String? = null
)

@Serializable
@XmlSerialName("Media", namespace = "http://www.onvif.org/ver10/device/wsdl", prefix = "tds")
data class MediaCapabilities(
    @XmlAttribute(true) val xAddr: String? = null
)

@Serializable
@XmlSerialName("PTZ", namespace = "http://www.onvif.org/ver10/device/wsdl", prefix = "tds")
data class PtzCapabilities(
    @XmlAttribute(true) val xAddr: String? = null
)

@Serializable
@XmlSerialName("GetDeviceInformationResponse", namespace = "http://www.onvif.org/ver10/device/wsdl", prefix = "tds")
data class DeviceInformationResponse(
    @XmlElement(true) val manufacturer: String? = null,
    @XmlElement(true) val model: String? = null,
    @XmlElement(true) val firmwareVersion: String? = null,
    @XmlElement(true) val serialNumber: String? = null,
    @XmlElement(true) val hardwareId: String? = null
)

@Serializable
@XmlSerialName("GetProfilesResponse", namespace = "http://www.onvif.org/ver10/media/wsdl", prefix = "trt")
data class ProfilesResponse(
    @XmlElement(true) val profiles: List<OnvifProfileXml>? = null
)

@Serializable
@XmlSerialName("Profile", namespace = "http://www.onvif.org/ver10/media/wsdl", prefix = "trt")
data class OnvifProfileXml(
    @XmlAttribute(true) val token: String,
    @XmlElement(true) val name: String? = null,
    @XmlElement(true) val videoSourceConfiguration: VideoSourceConfiguration? = null,
    @XmlElement(true) val videoEncoderConfiguration: VideoEncoderConfiguration? = null,
    @XmlElement(true) val audioEncoderConfiguration: AudioEncoderConfiguration? = null,
    @XmlElement(true) val audioSourceConfiguration: AudioSourceConfiguration? = null
)

@Serializable
@XmlSerialName("VideoSourceConfiguration", namespace = "http://www.onvif.org/ver10/schema", prefix = "tt")
data class VideoSourceConfiguration(
    @XmlAttribute(true) val token: String? = null
)

@Serializable
@XmlSerialName("VideoEncoderConfiguration", namespace = "http://www.onvif.org/ver10/schema", prefix = "tt")
data class VideoEncoderConfiguration(
    @XmlElement(true) val resolution: ResolutionXml? = null,
    @XmlElement(true) val rateControl: RateControl? = null,
    @XmlElement(true) val encoding: String? = null
)

@Serializable
@XmlSerialName("Resolution", namespace = "http://www.onvif.org/ver10/schema", prefix = "tt")
data class ResolutionXml(
    @XmlElement(true) val width: Int? = null,
    @XmlElement(true) val height: Int? = null
)

@Serializable
@XmlSerialName("RateControl", namespace = "http://www.onvif.org/ver10/schema", prefix = "tt")
data class RateControl(
    @XmlElement(true) val frameRateLimit: Int? = null
)

@Serializable
@XmlSerialName("AudioEncoderConfiguration", namespace = "http://www.onvif.org/ver10/schema", prefix = "tt")
data class AudioEncoderConfiguration(
    @XmlAttribute(true) val token: String? = null,
    @XmlElement(true) val encoding: String? = null,
    @XmlElement(true) val bitrate: Int? = null,
    @XmlElement(true) val sampleRate: Int? = null
)

@Serializable
@XmlSerialName("AudioSourceConfiguration", namespace = "http://www.onvif.org/ver10/schema", prefix = "tt")
data class AudioSourceConfiguration(
    @XmlAttribute(true) val token: String? = null,
    @XmlAttribute(true) val sourceToken: String? = null
)

@Serializable
@XmlSerialName("GetStreamUriResponse", namespace = "http://www.onvif.org/ver10/media/wsdl", prefix = "trt")
data class StreamUriResponse(
    @XmlElement(true) val uri: String? = null
)
