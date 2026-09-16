package com.company.ipcamera.core.network.onvif

import com.company.ipcamera.core.network.auth.DigestAuthHelper
import com.company.ipcamera.core.network.auth.DigestAuthParams
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.encodeBase64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
private val Dispatchers.IO get() = Dispatchers.Default

/**
 * Реализация OnvifImagingService
 */
class OnvifImagingServiceImpl(
    private val engine: HttpClientEngine
) : OnvifImagingService {

    private val client: HttpClient by lazy {
        HttpClient(engine) {
            install(ContentNegotiation)
        }
    }

    // Кэш для Digest Authentication
    private val digestAuthCache = mutableMapOf<String, DigestAuthParams>()

    override suspend fun getImagingSettings(
        cameraUrl: String,
        videoSourceToken: String,
        username: String?,
        password: String?
    ): Result<OnvifImagingSettings> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(cameraUrl)
            val imagingServiceUrl = getImagingServiceUrl(normalizedUrl, username, password)
                ?: return@withContext Result.failure(
                    IllegalStateException("Imaging Service URL not found in capabilities")
                )

            val request = createGetImagingSettingsRequest(videoSourceToken)
            val response = sendSoapRequest(
                url = imagingServiceUrl,
                soapMessage = request,
                username = username,
                password = password
            )

            val settings = OnvifImagingParser.parseGetImagingSettingsResponse(response)
                .getOrElse { error ->
                    return@withContext Result.failure(error)
                }

            Result.success(settings)
        } catch (e: Exception) {
            logger.error(e) { "Failed to get imaging settings for camera: $cameraUrl" }
            Result.failure(e)
        }
    }

    override suspend fun setImagingSettings(
        cameraUrl: String,
        videoSourceToken: String,
        settings: OnvifImagingSettings,
        forcePersistence: Boolean,
        username: String?,
        password: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(cameraUrl)
            val imagingServiceUrl = getImagingServiceUrl(normalizedUrl, username, password)
                ?: return@withContext Result.failure(
                    IllegalStateException("Imaging Service URL not found")
                )

            val request = createSetImagingSettingsRequest(videoSourceToken, settings, forcePersistence)
            sendSoapRequest(
                url = imagingServiceUrl,
                soapMessage = request,
                username = username,
                password = password
            )

            logger.info { "Updated imaging settings for video source: $videoSourceToken" }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Failed to set imaging settings for camera: $cameraUrl" }
            Result.failure(e)
        }
    }

    override suspend fun getOptions(
        cameraUrl: String,
        videoSourceToken: String,
        username: String?,
        password: String?
    ): Result<OnvifImagingOptions> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(cameraUrl)
            val imagingServiceUrl = getImagingServiceUrl(normalizedUrl, username, password)
                ?: return@withContext Result.failure(
                    IllegalStateException("Imaging Service URL not found")
                )

            val request = createGetOptionsRequest(videoSourceToken)
            val response = sendSoapRequest(
                url = imagingServiceUrl,
                soapMessage = request,
                username = username,
                password = password
            )

            val options = OnvifImagingParser.parseGetOptionsResponse(response)
                .getOrElse { error ->
                    return@withContext Result.failure(error)
                }

            Result.success(options)
        } catch (e: Exception) {
            logger.error(e) { "Failed to get imaging options for camera: $cameraUrl" }
            Result.failure(e)
        }
    }

    override suspend fun getMoveOptions(
        cameraUrl: String,
        videoSourceToken: String,
        username: String?,
        password: String?
    ): Result<OnvifMoveOptions> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(cameraUrl)
            val imagingServiceUrl = getImagingServiceUrl(normalizedUrl, username, password)
                ?: return@withContext Result.failure(
                    IllegalStateException("Imaging Service URL not found")
                )

            val request = createGetMoveOptionsRequest(videoSourceToken)
            val response = sendSoapRequest(
                url = imagingServiceUrl,
                soapMessage = request,
                username = username,
                password = password
            )

            val options = OnvifImagingParser.parseGetMoveOptionsResponse(response)
                .getOrElse { error ->
                    return@withContext Result.failure(error)
                }

            Result.success(options)
        } catch (e: Exception) {
            logger.error(e) { "Failed to get move options for camera: $cameraUrl" }
            Result.failure(e)
        }
    }

    override suspend fun move(
        cameraUrl: String,
        videoSourceToken: String,
        focus: OnvifFocusSettings,
        username: String?,
        password: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(cameraUrl)
            val imagingServiceUrl = getImagingServiceUrl(normalizedUrl, username, password)
                ?: return@withContext Result.failure(
                    IllegalStateException("Imaging Service URL not found")
                )

            val request = createMoveRequest(videoSourceToken, focus)
            sendSoapRequest(
                url = imagingServiceUrl,
                soapMessage = request,
                username = username,
                password = password
            )

            logger.info { "Moved focus for video source: $videoSourceToken" }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Failed to move focus for camera: $cameraUrl" }
            Result.failure(e)
        }
    }

    override suspend fun stop(
        cameraUrl: String,
        videoSourceToken: String,
        username: String?,
        password: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(cameraUrl)
            val imagingServiceUrl = getImagingServiceUrl(normalizedUrl, username, password)
                ?: return@withContext Result.failure(
                    IllegalStateException("Imaging Service URL not found")
                )

            val request = createStopRequest(videoSourceToken)
            sendSoapRequest(
                url = imagingServiceUrl,
                soapMessage = request,
                username = username,
                password = password
            )

            logger.info { "Stopped imaging operation for video source: $videoSourceToken" }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Failed to stop imaging operation for camera: $cameraUrl" }
            Result.failure(e)
        }
    }

    override suspend fun getStatus(
        cameraUrl: String,
        videoSourceToken: String,
        username: String?,
        password: String?
    ): Result<OnvifImagingStatus> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(cameraUrl)
            val imagingServiceUrl = getImagingServiceUrl(normalizedUrl, username, password)
                ?: return@withContext Result.failure(
                    IllegalStateException("Imaging Service URL not found")
                )

            val request = createGetStatusRequest(videoSourceToken)
            val response = sendSoapRequest(
                url = imagingServiceUrl,
                soapMessage = request,
                username = username,
                password = password
            )

            val status = OnvifImagingParser.parseGetStatusResponse(response)
                .getOrElse { error ->
                    return@withContext Result.failure(error)
                }

            Result.success(status)
        } catch (e: Exception) {
            logger.error(e) { "Failed to get imaging status for camera: $cameraUrl" }
            Result.failure(e)
        }
    }

    override suspend fun getPresets(
        cameraUrl: String,
        videoSourceToken: String,
        username: String?,
        password: String?
    ): Result<List<OnvifImagingPreset>> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(cameraUrl)
            val imagingServiceUrl = getImagingServiceUrl(normalizedUrl, username, password)
                ?: return@withContext Result.failure(
                    IllegalStateException("Imaging Service URL not found")
                )

            val request = createGetPresetsRequest(videoSourceToken)
            val response = sendSoapRequest(
                url = imagingServiceUrl,
                soapMessage = request,
                username = username,
                password = password
            )

            val presets = OnvifImagingParser.parseGetPresetsResponse(response)
                .getOrElse { error ->
                    return@withContext Result.failure(error)
                }

            Result.success(presets)
        } catch (e: Exception) {
            logger.error(e) { "Failed to get imaging presets for camera: $cameraUrl" }
            Result.failure(e)
        }
    }

    override suspend fun setPreset(
        cameraUrl: String,
        videoSourceToken: String,
        presetToken: String?,
        presetName: String,
        settings: OnvifImagingSettings,
        username: String?,
        password: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(cameraUrl)
            val imagingServiceUrl = getImagingServiceUrl(normalizedUrl, username, password)
                ?: return@withContext Result.failure(
                    IllegalStateException("Imaging Service URL not found")
                )

            val request = createSetPresetRequest(videoSourceToken, presetToken, presetName, settings)
            val response = sendSoapRequest(
                url = imagingServiceUrl,
                soapMessage = request,
                username = username,
                password = password
            )

            val token = OnvifImagingParser.parseSetPresetResponse(response)
                .getOrElse { error ->
                    return@withContext Result.failure(error)
                }

            logger.info { "Set imaging preset: $presetName (token: $token) for video source: $videoSourceToken" }
            Result.success(token)
        } catch (e: Exception) {
            logger.error(e) { "Failed to set imaging preset for camera: $cameraUrl" }
            Result.failure(e)
        }
    }

    override suspend fun removePreset(
        cameraUrl: String,
        videoSourceToken: String,
        presetToken: String,
        username: String?,
        password: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(cameraUrl)
            val imagingServiceUrl = getImagingServiceUrl(normalizedUrl, username, password)
                ?: return@withContext Result.failure(
                    IllegalStateException("Imaging Service URL not found")
                )

            val request = createRemovePresetRequest(videoSourceToken, presetToken)
            sendSoapRequest(
                url = imagingServiceUrl,
                soapMessage = request,
                username = username,
                password = password
            )

            logger.info { "Removed imaging preset: $presetToken for video source: $videoSourceToken" }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Failed to remove imaging preset for camera: $cameraUrl" }
            Result.failure(e)
        }
    }

    // Вспомогательные методы

    private suspend fun getImagingServiceUrl(
        cameraUrl: String,
        username: String?,
        password: String?
    ): String? {
        val normalizedUrl = normalizeUrl(cameraUrl)

        // Попытка получить Imaging Service URL через GetCapabilities
        try {
            val capabilitiesRequest = createGetCapabilitiesRequest()
            val response = sendSoapRequest(
                url = normalizedUrl,
                soapMessage = capabilitiesRequest,
                username = username,
                password = password
            )

            val imagingServiceUrl = extractImagingServiceUrl(response)
            if (imagingServiceUrl != null) {
                logger.debug { "Found Imaging Service URL in capabilities: $imagingServiceUrl" }
                return imagingServiceUrl
            }
        } catch (e: Exception) {
            logger.debug(e) { "Failed to get Imaging Service URL from capabilities, trying fallback" }
        }

        // Fallback: Стандартные пути для Imaging Service
        val possiblePaths = listOf(
            "$normalizedUrl/onvif/imaging_service",
            "$normalizedUrl/onvif/imaging",
            "$normalizedUrl/onvif/ImagingService",
            "$normalizedUrl/onvif/image"
        )

        return possiblePaths.firstOrNull()
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

    private fun extractImagingServiceUrl(xml: String): String? {
        val patterns = listOf(
            Regex("""<tds:Imaging[^>]*XAddr\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""<Imaging[^>]*XAddr\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""(?s)<tds:Imaging[^>]*>.*?<tds:XAddr[^>]*>([^<]+)</tds:XAddr>"""),
            Regex("""Imaging.*?XAddr[^>]*>([^<]+)<""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(xml)
            if (match != null) {
                val url = match.groupValues.getOrNull(1)?.trim()
                if (!url.isNullOrBlank()) {
                    return url
                }
            }
        }

        return null
    }

    private suspend fun sendSoapRequest(
        url: String,
        soapMessage: String,
        username: String?,
        password: String?
    ): String {
        var digestParams = digestAuthCache[url]
        var useDigestAuth = digestParams != null
        var nc = 1
        var attempt = 0
        val maxRetries = 2

        while (attempt <= maxRetries) {
            try {
                val uriPath = try {
                    extractPathFromUrl(url)
                } catch (e: Exception) {
                    "/"
                }

                val response = client.post(url) {
                    contentType(ContentType.Text.Xml)
                    header("SOAPAction", "")

                    if (username != null && password != null) {
                        if (useDigestAuth && digestParams != null) {
                            val digestHeader = DigestAuthHelper.generateDigestAuthHeader(
                                username = username,
                                password = password,
                                method = "POST",
                                uri = uriPath,
                                params = digestParams,
                                nc = nc.toString(16).padStart(8, '0'),
                                entityBody = soapMessage
                            )
                            header(HttpHeaders.Authorization, digestHeader)
                            nc++
                        } else {
                            val credentials = "$username:$password"
                            val encoded = credentials.encodeToByteArray().encodeBase64()
                            header(HttpHeaders.Authorization, "Basic $encoded")
                        }
                    }

                    setBody(soapMessage)
                }

                if (response.status.value in 200..299) {
                    return response.body<String>()
                } else if (response.status.value == HttpStatusCode.Unauthorized.value) {
                    val wwwAuthenticate = response.headers[HttpHeaders.WWWAuthenticate]
                        ?: response.headers.getAll("WWW-Authenticate")?.firstOrNull()

                    if (wwwAuthenticate != null && wwwAuthenticate.startsWith("Digest", ignoreCase = true)) {
                        val parsedParams = DigestAuthHelper.parseWWWAuthenticate(wwwAuthenticate)
                        if (parsedParams != null) {
                            if (parsedParams.stale) {
                                digestAuthCache.remove(url)
                                digestParams = null
                                useDigestAuth = false
                            } else {
                                digestParams = parsedParams
                                digestAuthCache[url] = digestParams
                                useDigestAuth = true
                            }
                            attempt++
                            continue
                        }
                    }
                }

                throw IllegalStateException("SOAP request failed with status: ${response.status}")
            } catch (e: Exception) {
                if (attempt >= maxRetries) {
                    logger.error(e) { "Failed to send SOAP request after $maxRetries retries" }
                    throw e
                }
                attempt++
            }
        }

        throw IllegalStateException("Failed to send SOAP request")
    }

    // SOAP Request Builders

    private fun createGetImagingSettingsRequest(videoSourceToken: String): String {
        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope"
                       xmlns:timg="http://www.onvif.org/ver20/imaging/wsdl">
            <soap:Body>
                <timg:GetImagingSettings>
                    <timg:VideoSourceToken>$videoSourceToken</timg:VideoSourceToken>
                </timg:GetImagingSettings>
            </soap:Body>
        </soap:Envelope>
        """.trimIndent()
    }

    private fun createSetImagingSettingsRequest(
        videoSourceToken: String,
        settings: OnvifImagingSettings,
        forcePersistence: Boolean
    ): String {
        val brightnessXml = settings.brightness?.let { "<timg:Brightness>$it</timg:Brightness>" } ?: ""
        val contrastXml = settings.contrast?.let { "<timg:Contrast>$it</timg:Contrast>" } ?: ""
        val colorSaturationXml = settings.colorSaturation?.let { "<timg:ColorSaturation>$it</timg:ColorSaturation>" } ?: ""
        val sharpnessXml = settings.sharpness?.let { "<timg:Sharpness>$it</timg:Sharpness>" } ?: ""

        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope"
                       xmlns:timg="http://www.onvif.org/ver20/imaging/wsdl">
            <soap:Body>
                <timg:SetImagingSettings>
                    <timg:VideoSourceToken>$videoSourceToken</timg:VideoSourceToken>
                    <timg:ImagingSettings>
                        $brightnessXml
                        $contrastXml
                        $colorSaturationXml
                        $sharpnessXml
                    </timg:ImagingSettings>
                    <timg:ForcePersistence>$forcePersistence</timg:ForcePersistence>
                </timg:SetImagingSettings>
            </soap:Body>
        </soap:Envelope>
        """.trimIndent()
    }

    private fun createGetOptionsRequest(videoSourceToken: String): String {
        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope"
                       xmlns:timg="http://www.onvif.org/ver20/imaging/wsdl">
            <soap:Body>
                <timg:GetOptions>
                    <timg:VideoSourceToken>$videoSourceToken</timg:VideoSourceToken>
                </timg:GetOptions>
            </soap:Body>
        </soap:Envelope>
        """.trimIndent()
    }

    private fun createGetMoveOptionsRequest(videoSourceToken: String): String {
        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope"
                       xmlns:timg="http://www.onvif.org/ver20/imaging/wsdl">
            <soap:Body>
                <timg:GetMoveOptions>
                    <timg:VideoSourceToken>$videoSourceToken</timg:VideoSourceToken>
                </timg:GetMoveOptions>
            </soap:Body>
        </soap:Envelope>
        """.trimIndent()
    }

    private fun createMoveRequest(videoSourceToken: String, focus: OnvifFocusSettings): String {
        val focusXml = if (focus.mode == FocusMode.MANUAL && focus.position != null) {
            "<timg:Focus><timg:AbsoluteFocus><timg:Position>${focus.position}</timg:Position></timg:AbsoluteFocus></timg:Focus>"
        } else {
            "<timg:Focus><timg:AutoFocusMode>${focus.mode.name}</timg:AutoFocusMode></timg:Focus>"
        }

        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope"
                       xmlns:timg="http://www.onvif.org/ver20/imaging/wsdl">
            <soap:Body>
                <timg:Move>
                    <timg:VideoSourceToken>$videoSourceToken</timg:VideoSourceToken>
                    $focusXml
                </timg:Move>
            </soap:Body>
        </soap:Envelope>
        """.trimIndent()
    }

    private fun createStopRequest(videoSourceToken: String): String {
        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope"
                       xmlns:timg="http://www.onvif.org/ver20/imaging/wsdl">
            <soap:Body>
                <timg:Stop>
                    <timg:VideoSourceToken>$videoSourceToken</timg:VideoSourceToken>
                </timg:Stop>
            </soap:Body>
        </soap:Envelope>
        """.trimIndent()
    }

    private fun createGetStatusRequest(videoSourceToken: String): String {
        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope"
                       xmlns:timg="http://www.onvif.org/ver20/imaging/wsdl">
            <soap:Body>
                <timg:GetStatus>
                    <timg:VideoSourceToken>$videoSourceToken</timg:VideoSourceToken>
                </timg:GetStatus>
            </soap:Body>
        </soap:Envelope>
        """.trimIndent()
    }

    private fun createGetPresetsRequest(videoSourceToken: String): String {
        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope"
                       xmlns:timg="http://www.onvif.org/ver20/imaging/wsdl">
            <soap:Body>
                <timg:GetPresets>
                    <timg:VideoSourceToken>$videoSourceToken</timg:VideoSourceToken>
                </timg:GetPresets>
            </soap:Body>
        </soap:Envelope>
        """.trimIndent()
    }

    private fun createSetPresetRequest(
        videoSourceToken: String,
        presetToken: String?,
        presetName: String,
        settings: OnvifImagingSettings
    ): String {
        val tokenXml = presetToken?.let { "<timg:PresetToken>$it</timg:PresetToken>" } ?: ""
        val brightnessXml = settings.brightness?.let { "<timg:Brightness>$it</timg:Brightness>" } ?: ""

        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope"
                       xmlns:timg="http://www.onvif.org/ver20/imaging/wsdl">
            <soap:Body>
                <timg:SetPreset>
                    <timg:VideoSourceToken>$videoSourceToken</timg:VideoSourceToken>
                    $tokenXml
                    <timg:Preset>
                        <timg:Name>$presetName</timg:Name>
                        <timg:ImagingSettings>
                            $brightnessXml
                        </timg:ImagingSettings>
                    </timg:Preset>
                </timg:SetPreset>
            </soap:Body>
        </soap:Envelope>
        """.trimIndent()
    }

    private fun createRemovePresetRequest(videoSourceToken: String, presetToken: String): String {
        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope"
                       xmlns:timg="http://www.onvif.org/ver20/imaging/wsdl">
            <soap:Body>
                <timg:RemovePreset>
                    <timg:VideoSourceToken>$videoSourceToken</timg:VideoSourceToken>
                    <timg:PresetToken>$presetToken</timg:PresetToken>
                </timg:RemovePreset>
            </soap:Body>
        </soap:Envelope>
        """.trimIndent()
    }

    private fun normalizeUrl(url: String): String {
        return url.trimEnd('/')
    }

    private fun extractPathFromUrl(url: String): String {
        val schemeIndex = url.indexOf("://")
        val hostStart = if (schemeIndex >= 0) schemeIndex + 3 else 0
        val pathStart = url.indexOf('/', hostStart)
        if (pathStart < 0) return "/"
        val pathEnd = url.indexOf('?', pathStart).takeIf { it >= 0 } ?: url.length
        return url.substring(pathStart, pathEnd).ifEmpty { "/" }
    }
}
