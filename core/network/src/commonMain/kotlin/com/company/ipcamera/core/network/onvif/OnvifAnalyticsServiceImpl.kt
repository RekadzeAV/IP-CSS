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
 * Реализация OnvifAnalyticsService
 */
class OnvifAnalyticsServiceImpl(
    private val engine: HttpClientEngine
) : OnvifAnalyticsService {

    private val client: HttpClient by lazy {
        HttpClient(engine) {
            install(ContentNegotiation)
        }
    }

    // Кэш для Digest Authentication
    private val digestAuthCache = mutableMapOf<String, DigestAuthParams>()

    override suspend fun getAnalyticsEngines(
        cameraUrl: String,
        username: String?,
        password: String?
    ): Result<List<OnvifAnalyticsEngine>> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(cameraUrl)
            val analyticsServiceUrl = getAnalyticsServiceUrl(normalizedUrl, username, password)
                ?: return@withContext Result.failure(
                    IllegalStateException("Analytics Service URL not found in capabilities")
                )

            val request = createGetAnalyticsEnginesRequest()
            val response = sendSoapRequest(
                url = analyticsServiceUrl,
                soapMessage = request,
                username = username,
                password = password
            )

            val engines = OnvifAnalyticsParser.parseGetAnalyticsEnginesResponse(response)
                .getOrElse { error ->
                    return@withContext Result.failure(error)
                }

            logger.info { "Retrieved ${engines.size} analytics engines from camera: $normalizedUrl" }
            Result.success(engines)
        } catch (e: Exception) {
            logger.error(e) { "Failed to get analytics engines for camera: $cameraUrl" }
            Result.failure(e)
        }
    }

    override suspend fun getAnalyticsEngine(
        cameraUrl: String,
        engineToken: String,
        username: String?,
        password: String?
    ): Result<OnvifAnalyticsEngine> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(cameraUrl)
            val analyticsServiceUrl = getAnalyticsServiceUrl(normalizedUrl, username, password)
                ?: return@withContext Result.failure(
                    IllegalStateException("Analytics Service URL not found")
                )

            val request = createGetAnalyticsEngineRequest(engineToken)
            val response = sendSoapRequest(
                url = analyticsServiceUrl,
                soapMessage = request,
                username = username,
                password = password
            )

            val engine = OnvifAnalyticsParser.parseGetAnalyticsEngineResponse(response)
                .getOrElse { error ->
                    return@withContext Result.failure(error)
                }

            Result.success(engine)
        } catch (e: Exception) {
            logger.error(e) { "Failed to get analytics engine: $engineToken for camera: $cameraUrl" }
            Result.failure(e)
        }
    }

    override suspend fun getAnalyticsEngineInputs(
        cameraUrl: String,
        engineToken: String,
        username: String?,
        password: String?
    ): Result<List<OnvifAnalyticsEngineInput>> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(cameraUrl)
            val analyticsServiceUrl = getAnalyticsServiceUrl(normalizedUrl, username, password)
                ?: return@withContext Result.failure(
                    IllegalStateException("Analytics Service URL not found")
                )

            val request = createGetAnalyticsEngineInputsRequest(engineToken)
            val response = sendSoapRequest(
                url = analyticsServiceUrl,
                soapMessage = request,
                username = username,
                password = password
            )

            val inputs = OnvifAnalyticsParser.parseGetAnalyticsEngineInputsResponse(response)
                .getOrElse { error ->
                    return@withContext Result.failure(error)
                }

            Result.success(inputs)
        } catch (e: Exception) {
            logger.error(e) { "Failed to get analytics engine inputs for engine: $engineToken" }
            Result.failure(e)
        }
    }

    override suspend fun createAnalyticsEngine(
        cameraUrl: String,
        configuration: OnvifAnalyticsEngineConfiguration,
        username: String?,
        password: String?
    ): Result<OnvifAnalyticsEngine> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(cameraUrl)
            val analyticsServiceUrl = getAnalyticsServiceUrl(normalizedUrl, username, password)
                ?: return@withContext Result.failure(
                    IllegalStateException("Analytics Service URL not found")
                )

            val request = createCreateAnalyticsEngineRequest(configuration)
            val response = sendSoapRequest(
                url = analyticsServiceUrl,
                soapMessage = request,
                username = username,
                password = password
            )

            val engine = OnvifAnalyticsParser.parseCreateAnalyticsEngineResponse(response)
                .getOrElse { error ->
                    return@withContext Result.failure(error)
                }

            logger.info { "Created analytics engine: ${engine.token} for camera: $normalizedUrl" }
            Result.success(engine)
        } catch (e: Exception) {
            logger.error(e) { "Failed to create analytics engine for camera: $cameraUrl" }
            Result.failure(e)
        }
    }

    override suspend fun setAnalyticsEngine(
        cameraUrl: String,
        engineToken: String,
        configuration: OnvifAnalyticsEngineConfiguration,
        username: String?,
        password: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(cameraUrl)
            val analyticsServiceUrl = getAnalyticsServiceUrl(normalizedUrl, username, password)
                ?: return@withContext Result.failure(
                    IllegalStateException("Analytics Service URL not found")
                )

            val request = createSetAnalyticsEngineRequest(engineToken, configuration)
            sendSoapRequest(
                url = analyticsServiceUrl,
                soapMessage = request,
                username = username,
                password = password
            )

            logger.info { "Updated analytics engine: $engineToken for camera: $normalizedUrl" }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Failed to set analytics engine: $engineToken" }
            Result.failure(e)
        }
    }

    override suspend fun deleteAnalyticsEngine(
        cameraUrl: String,
        engineToken: String,
        username: String?,
        password: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(cameraUrl)
            val analyticsServiceUrl = getAnalyticsServiceUrl(normalizedUrl, username, password)
                ?: return@withContext Result.failure(
                    IllegalStateException("Analytics Service URL not found")
                )

            val request = createDeleteAnalyticsEngineRequest(engineToken)
            sendSoapRequest(
                url = analyticsServiceUrl,
                soapMessage = request,
                username = username,
                password = password
            )

            logger.info { "Deleted analytics engine: $engineToken for camera: $normalizedUrl" }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Failed to delete analytics engine: $engineToken" }
            Result.failure(e)
        }
    }

    override suspend fun getAnalyticsEngineInput(
        cameraUrl: String,
        engineToken: String,
        inputToken: String,
        username: String?,
        password: String?
    ): Result<OnvifAnalyticsEngineInput> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(cameraUrl)
            val analyticsServiceUrl = getAnalyticsServiceUrl(normalizedUrl, username, password)
                ?: return@withContext Result.failure(
                    IllegalStateException("Analytics Service URL not found")
                )

            val request = createGetAnalyticsEngineInputRequest(engineToken, inputToken)
            val response = sendSoapRequest(
                url = analyticsServiceUrl,
                soapMessage = request,
                username = username,
                password = password
            )

            val input = OnvifAnalyticsParser.parseGetAnalyticsEngineInputResponse(response)
                .getOrElse { error ->
                    return@withContext Result.failure(error)
                }

            Result.success(input)
        } catch (e: Exception) {
            logger.error(e) { "Failed to get analytics engine input: $inputToken" }
            Result.failure(e)
        }
    }

    override suspend fun setAnalyticsEngineInput(
        cameraUrl: String,
        engineToken: String,
        inputToken: String,
        configuration: OnvifAnalyticsEngineInputConfiguration,
        username: String?,
        password: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(cameraUrl)
            val analyticsServiceUrl = getAnalyticsServiceUrl(normalizedUrl, username, password)
                ?: return@withContext Result.failure(
                    IllegalStateException("Analytics Service URL not found")
                )

            val request = createSetAnalyticsEngineInputRequest(engineToken, inputToken, configuration)
            sendSoapRequest(
                url = analyticsServiceUrl,
                soapMessage = request,
                username = username,
                password = password
            )

            logger.info { "Updated analytics engine input: $inputToken for engine: $engineToken" }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Failed to set analytics engine input: $inputToken" }
            Result.failure(e)
        }
    }

    // Вспомогательные методы

    private suspend fun getAnalyticsServiceUrl(
        cameraUrl: String,
        username: String?,
        password: String?
    ): String? {
        val normalizedUrl = normalizeUrl(cameraUrl)

        // Попытка получить Analytics Service URL через GetCapabilities
        try {
            val capabilitiesRequest = createGetCapabilitiesRequest()
            val response = sendSoapRequest(
                url = normalizedUrl,
                soapMessage = capabilitiesRequest,
                username = username,
                password = password
            )

            val analyticsServiceUrl = extractAnalyticsServiceUrl(response)
            if (analyticsServiceUrl != null) {
                logger.debug { "Found Analytics Service URL in capabilities: $analyticsServiceUrl" }
                return analyticsServiceUrl
            }
        } catch (e: Exception) {
            logger.debug(e) { "Failed to get Analytics Service URL from capabilities, trying fallback" }
        }

        // Fallback: Стандартные пути для Analytics Service
        val possiblePaths = listOf(
            "$normalizedUrl/onvif/analytics_service",
            "$normalizedUrl/onvif/analytics",
            "$normalizedUrl/onvif/AnalyticsService",
            "$normalizedUrl/onvif/analytics/engine"
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

    private fun extractAnalyticsServiceUrl(xml: String): String? {
        val patterns = listOf(
            Regex("""<tds:Analytics[^>]*XAddr\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""<Analytics[^>]*XAddr\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""(?s)<tds:Analytics[^>]*>.*?<tds:XAddr[^>]*>([^<]+)</tds:XAddr>"""),
            Regex("""Analytics.*?XAddr[^>]*>([^<]+)<""", RegexOption.IGNORE_CASE)
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

    private fun createGetAnalyticsEnginesRequest(): String {
        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope"
                       xmlns:tana="http://www.onvif.org/ver20/analytics/wsdl">
            <soap:Body>
                <tana:GetAnalyticsEngines/>
            </soap:Body>
        </soap:Envelope>
        """.trimIndent()
    }

    private fun createGetAnalyticsEngineRequest(engineToken: String): String {
        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope"
                       xmlns:tana="http://www.onvif.org/ver20/analytics/wsdl">
            <soap:Body>
                <tana:GetAnalyticsEngine>
                    <tana:EngineToken>$engineToken</tana:EngineToken>
                </tana:GetAnalyticsEngine>
            </soap:Body>
        </soap:Envelope>
        """.trimIndent()
    }

    private fun createGetAnalyticsEngineInputsRequest(engineToken: String): String {
        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope"
                       xmlns:tana="http://www.onvif.org/ver20/analytics/wsdl">
            <soap:Body>
                <tana:GetAnalyticsEngineInputs>
                    <tana:EngineToken>$engineToken</tana:EngineToken>
                </tana:GetAnalyticsEngineInputs>
            </soap:Body>
        </soap:Envelope>
        """.trimIndent()
    }

    private fun createCreateAnalyticsEngineRequest(configuration: OnvifAnalyticsEngineConfiguration): String {
        // Упрощенная версия - в реальности нужно больше параметров
        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope"
                       xmlns:tana="http://www.onvif.org/ver20/analytics/wsdl">
            <soap:Body>
                <tana:CreateAnalyticsEngine>
                    <tana:Configuration>
                        ${configuration.parameters.entries.joinToString("\n") { "<tana:${it.key}>${it.value}</tana:${it.key}>" }}
                    </tana:Configuration>
                </tana:CreateAnalyticsEngine>
            </soap:Body>
        </soap:Envelope>
        """.trimIndent()
    }

    private fun createSetAnalyticsEngineRequest(
        engineToken: String,
        configuration: OnvifAnalyticsEngineConfiguration
    ): String {
        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope"
                       xmlns:tana="http://www.onvif.org/ver20/analytics/wsdl">
            <soap:Body>
                <tana:SetAnalyticsEngine>
                    <tana:EngineToken>$engineToken</tana:EngineToken>
                    <tana:Configuration>
                        ${configuration.parameters.entries.joinToString("\n") { "<tana:${it.key}>${it.value}</tana:${it.key}>" }}
                    </tana:Configuration>
                </tana:SetAnalyticsEngine>
            </soap:Body>
        </soap:Envelope>
        """.trimIndent()
    }

    private fun createDeleteAnalyticsEngineRequest(engineToken: String): String {
        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope"
                       xmlns:tana="http://www.onvif.org/ver20/analytics/wsdl">
            <soap:Body>
                <tana:DeleteAnalyticsEngine>
                    <tana:EngineToken>$engineToken</tana:EngineToken>
                </tana:DeleteAnalyticsEngine>
            </soap:Body>
        </soap:Envelope>
        """.trimIndent()
    }

    private fun createGetAnalyticsEngineInputRequest(engineToken: String, inputToken: String): String {
        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope"
                       xmlns:tana="http://www.onvif.org/ver20/analytics/wsdl">
            <soap:Body>
                <tana:GetAnalyticsEngineInput>
                    <tana:EngineToken>$engineToken</tana:EngineToken>
                    <tana:InputToken>$inputToken</tana:InputToken>
                </tana:GetAnalyticsEngineInput>
            </soap:Body>
        </soap:Envelope>
        """.trimIndent()
    }

    private fun createSetAnalyticsEngineInputRequest(
        engineToken: String,
        inputToken: String,
        configuration: OnvifAnalyticsEngineInputConfiguration
    ): String {
        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope"
                       xmlns:tana="http://www.onvif.org/ver20/analytics/wsdl">
            <soap:Body>
                <tana:SetAnalyticsEngineInput>
                    <tana:EngineToken>$engineToken</tana:EngineToken>
                    <tana:InputToken>$inputToken</tana:InputToken>
                    <tana:Configuration>
                        ${configuration.parameters.entries.joinToString("\n") { "<tana:${it.key}>${it.value}</tana:${it.key}>" }}
                    </tana:Configuration>
                </tana:SetAnalyticsEngineInput>
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
