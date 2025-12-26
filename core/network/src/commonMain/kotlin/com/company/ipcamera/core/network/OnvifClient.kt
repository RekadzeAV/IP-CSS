package com.company.ipcamera.core.network

import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.model.CameraStatus
import com.company.ipcamera.shared.domain.model.Resolution
import com.company.ipcamera.shared.domain.repository.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
// ONVIF клиент использует HTTP и XML парсинг
// Для полноценной реализации требуется библиотека для работы с SOAP
// В текущей версии используется упрощенный подход через HTTP запросы

private val logger = KotlinLogging.logger {}

/**
 * ONVIF клиент для обнаружения и управления камерами
 */
class OnvifClient(
    private val engine: HttpClientEngine
) {
    private val client: HttpClient by lazy {
        HttpClient(engine) {
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
        try {
            logger.info { "Starting ONVIF camera discovery..." }
            
            // WS-Discovery multicast запрос
            val discovered = mutableListOf<DiscoveredCamera>()
            
            // Простой подход: попробуем подключиться к известным IP адресам
            // В реальной реализации здесь должен быть WS-Discovery протокол через UDP multicast
            // Для упрощения, мы пытаемся подключиться к камерам через ONVIF Device Management
            
            // В продакшене здесь должен быть:
            // 1. UDP multicast на 239.255.255.250:3702
            // 2. SOAP запрос Probe с типами устройств
            // 3. Обработка ProbeMatches ответов
            
            logger.warn { "WS-Discovery not fully implemented, using fallback discovery" }
            
            // Fallback: попробуем получить информацию о камере по URL
            // Это заглушка для демонстрации структуры
            emptyList()
            
        } catch (e: Exception) {
            logger.error(e) { "Error during camera discovery" }
            emptyList()
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
            val capabilities = getCapabilities(url, username, password)
            
            if (capabilities != null) {
                val profiles = getProfiles(url, username, password)
                val streamInfo = if (profiles.isNotEmpty()) {
                    val streamUri = getStreamUri(url, profiles.first().token, username, password)
                    listOf(
                        StreamInfo(
                            type = "RTSP",
                            resolution = profiles.first().videoResolution?.toString() ?: "unknown",
                            fps = profiles.first().fps ?: 25,
                            codec = profiles.first().codec ?: "H.264"
                        )
                    )
                } else {
                    emptyList()
                }
                
                ConnectionTestResult.Success(
                    streams = streamInfo,
                    capabilities = CameraCapabilities(
                        ptz = capabilities.ptzServiceUrl != null,
                        audio = false, // TODO: определить из профилей
                        onvif = true,
                        analytics = false
                    )
                )
            } else {
                ConnectionTestResult.Failure(
                    error = "Could not connect to camera",
                    code = ErrorCode.CONNECTION_FAILED
                )
            }
            
        } catch (e: Exception) {
            logger.error(e) { "Connection test failed: ${e.message}" }
            ConnectionTestResult.Failure(
                error = e.message ?: "Unknown error",
                code = ErrorCode.CONNECTION_FAILED
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
        
        return response.body<String>()
    }
    
    private fun parseCapabilities(xml: String): OnvifCapabilities? {
        return try {
            // Упрощенный XML парсинг через регулярные выражения или простой поиск
            // В продакшене следует использовать полноценный XML парсер
            val deviceUrl = extractXmlValue(xml, "Device", "XAddr")
            val mediaUrl = extractXmlValue(xml, "Media", "XAddr")
            val ptzUrl = extractXmlValue(xml, "PTZ", "XAddr")
            
            OnvifCapabilities(
                deviceServiceUrl = deviceUrl,
                mediaServiceUrl = mediaUrl,
                ptzServiceUrl = ptzUrl
            )
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
