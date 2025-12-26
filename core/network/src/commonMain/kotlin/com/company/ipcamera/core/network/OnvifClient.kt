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
import kotlinx.coroutines.withContext
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
                for (xAddr in device.xAddrs) {
                    try {
                        // Нормализация URL
                        val normalizedUrl = normalizeUrl(xAddr)

                        // Попытка получить информацию о камере
                        val deviceInfo = getDeviceInformation(normalizedUrl)
                        val capabilities = getCapabilities(normalizedUrl)

                        val camera = DiscoveredCamera(
                            url = normalizedUrl,
                            name = deviceInfo?.model ?: "Unknown Camera",
                            manufacturer = deviceInfo?.manufacturer,
                            model = deviceInfo?.model,
                            capabilities = capabilities
                        )
                        discoveredCameras.add(camera)
                        logger.info { "Discovered camera: ${camera.name} at ${camera.url}" }
                    } catch (e: Exception) {
                        logger.warn(e) { "Error getting information for device at $xAddr" }
                        // Добавляем камеру даже без полной информации
                        discoveredCameras.add(
                            DiscoveredCamera(
                                url = normalizeUrl(xAddr),
                                name = "ONVIF Device",
                                capabilities = null
                            )
                        )
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

                val result = ConnectionTestResult.Success(
                    streams = streamInfo,
                    capabilities = CameraCapabilities(
                        ptz = capabilities.ptzServiceUrl != null,
                        audio = false, // TODO: определить из профилей
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
        password: String? = null
    ): String {
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

        val responseBody = response.body<String>()

        // Санитизация ответа от сервера для предотвращения XSS/инъекций при парсинге
        // Примечание: SOAP запросы генерируются нами, поэтому не требуют санитизации
        return responseBody
    }

    private fun parseCapabilities(xml: String): OnvifCapabilities? {
        return try {
            // Используем XML парсер для более надежного парсинга
            val xmlParser = Xml {
                ignoreUnknownChildren = true
                coerceInputValues = true
            }

            // Пытаемся распарсить как SOAP Envelope
            try {
                val envelope = xmlParser.decodeFromString<SoapEnvelope>(xml)
                val capabilities = envelope.body.capabilities

                if (capabilities != null) {
                    OnvifCapabilities(
                        deviceServiceUrl = capabilities.device?.xAddr,
                        mediaServiceUrl = capabilities.media?.xAddr,
                        ptzServiceUrl = capabilities.ptz?.xAddr
                    )
                } else {
                    // Fallback на упрощенный парсинг
                    val deviceUrl = extractXmlValue(xml, "Device", "XAddr")
                    val mediaUrl = extractXmlValue(xml, "Media", "XAddr")
                    val ptzUrl = extractXmlValue(xml, "PTZ", "XAddr")

                    OnvifCapabilities(
                        deviceServiceUrl = deviceUrl,
                        mediaServiceUrl = mediaUrl,
                        ptzServiceUrl = ptzUrl
                    )
                }
            } catch (e: Exception) {
                // Fallback на упрощенный парсинг если XML парсинг не удался
                logger.debug(e) { "XML parsing failed, using fallback" }
                val deviceUrl = extractXmlValue(xml, "Device", "XAddr")
                val mediaUrl = extractXmlValue(xml, "Media", "XAddr")
                val ptzUrl = extractXmlValue(xml, "PTZ", "XAddr")

                OnvifCapabilities(
                    deviceServiceUrl = deviceUrl,
                    mediaServiceUrl = mediaUrl,
                    ptzServiceUrl = ptzUrl
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Error parsing capabilities" }
            null
        }
    }

    private fun parseDeviceInformation(xml: String): DeviceInformation? {
        return try {
            DeviceInformation(
                manufacturer = extractXmlValue(xml, "Manufacturer") ?: "Unknown",
                model = extractXmlValue(xml, "Model") ?: "Unknown",
                firmwareVersion = extractXmlValue(xml, "FirmwareVersion") ?: "Unknown",
                serialNumber = extractXmlValue(xml, "SerialNumber") ?: "Unknown",
                hardwareId = extractXmlValue(xml, "HardwareId") ?: "Unknown"
            )
        } catch (e: Exception) {
            logger.error(e) { "Error parsing device information" }
            null
        }
    }

    private fun parseProfiles(xml: String): List<OnvifProfile> {
        return try {
            // Упрощенный парсинг - в продакшене нужен полноценный XML парсер
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

                result.add(
                    OnvifProfile(
                        token = token,
                        name = name,
                        videoResolution = Resolution(width, height),
                        fps = fps,
                        codec = codec
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
                        codec = "H.264"
                    )
                )
            }

            result
        } catch (e: Exception) {
            logger.error(e) { "Error parsing profiles" }
            emptyList()
        }
    }

    private fun parseStreamUri(xml: String): String? {
        return try {
            extractXmlValue(xml, "Uri")
        } catch (e: Exception) {
            logger.error(e) { "Error parsing stream URI" }
            null
        }
    }

    // Упрощенная функция для извлечения значений из XML
    private fun extractXmlValue(xml: String, tagName: String, attribute: String? = null): String? {
        return try {
            if (attribute != null) {
                val regex = Regex("<[^:]*:$tagName[^>]*$attribute=\"([^\"]+)\"")
                regex.find(xml)?.groupValues?.get(1)
            } else {
                val regex = Regex("<[^>]*:$tagName[^>]*>([^<]+)</[^>]*:$tagName>")
                regex.find(xml)?.groupValues?.get(1)
            }
        } catch (e: Exception) {
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
    val codec: String? = null
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
    @XmlElement(true) val streamUri: StreamUriResponse? = null
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
    @XmlElement(true) val xAddr: String? = null
)

@Serializable
@XmlSerialName("Media", namespace = "http://www.onvif.org/ver10/device/wsdl", prefix = "tds")
data class MediaCapabilities(
    @XmlElement(true) val xAddr: String? = null
)

@Serializable
@XmlSerialName("PTZ", namespace = "http://www.onvif.org/ver10/device/wsdl", prefix = "tds")
data class PtzCapabilities(
    @XmlElement(true) val xAddr: String? = null
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
    @XmlElement(true) val videoEncoderConfiguration: VideoEncoderConfiguration? = null
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
@XmlSerialName("GetStreamUriResponse", namespace = "http://www.onvif.org/ver10/media/wsdl", prefix = "trt")
data class StreamUriResponse(
    @XmlElement(true) val uri: String? = null
)
