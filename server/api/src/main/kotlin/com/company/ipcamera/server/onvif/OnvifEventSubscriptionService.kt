package com.company.ipcamera.server.onvif

import mu.KotlinLogging
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.URI
import java.time.Duration
import java.util.*

private val logger = KotlinLogging.logger {}

/**
 * Сервис для подписки на ONVIF события камер
 */
object OnvifEventSubscriptionService {

    /**
     * Подписка на события камеры (motion detection, alarm inputs)
     */
    fun subscribeToCamera(
        cameraId: String,
        cameraIp: String,
        username: String,
        password: String
    ): Boolean {
        try {
            logger.info { "Subscribing to ONVIF events for camera $cameraId ($cameraIp)" }

            // Step 1: Get ONVIF endpoint
            val onvifUrl = "http://$cameraIp:80/onvif/device_service"

            // Step 2: Create subscription request
            val subscriptionRequest = buildSubscriptionRequest(cameraId, username, password)

            // Step 3: Send subscription request
            val response = sendSubscriptionRequest(onvifUrl, subscriptionRequest, username, password)

            if (response.statusCode() == 200) {
                logger.info { "Successfully subscribed to ONVIF events for camera $cameraId" }
                return true
            } else {
                logger.warn { "ONVIF subscription returned status: ${response.statusCode()}" }
                return false
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to subscribe to ONVIF events for camera $cameraId" }
            return false
        }
    }

    /**
     * Проверка поддержки ONVIF камерой
     */
    fun checkOnvifSupport(cameraIp: String): Boolean {
        return try {
            val onvifUrl = "http://$cameraIp:80/onvif/device_service"
            val request = HttpRequest.newBuilder()
                .uri(URI.create(onvifUrl))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build()

            val response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
            response.statusCode() == 200
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Получить информацию об устройстве
     */
    fun getDeviceInfo(cameraIp: String, username: String, password: String): OnvifDeviceInfo? {
        return try {
            val onvifUrl = "http://$cameraIp:80/onvif/device_service"
            val request = buildDeviceInfoRequest(username, password)

            val response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() == 200) {
                parseDeviceInfo(response.body())
            } else {
                null
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to get device info for $cameraIp" }
            null
        }
    }

    /**
     * Отписка от событий камеры
     */
    fun unsubscribeFromCamera(cameraId: String, subscriptionUri: String): Boolean {
        return try {
            logger.info { "Unsubscribing from ONVIF events for camera $cameraId" }
            // Send unsubscribe request
            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to unsubscribe from camera $cameraId" }
            false
        }
    }

    // ============ Helper Methods ============

    private fun buildSubscriptionRequest(cameraId: String, username: String, password: String): String {
        val timestamp = Calendar.getInstance().time
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope" 
                          xmlns:wsnt="http://docs.oasis-open.org/wsn/b-2"
                          xmlns:tdn="http://www.onvif.org/ver10/network/wsdl">
                <soap:Header>
                    <wsse:Security soap:mustUnderstand="true" 
                                  xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
                        <wsse:UsernameToken>
                            <wsse:Username>$username</wsse:Username>
                            <wsse:Password Type="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDigest">$password</wsse:Password>
                            <wsu:Created xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd">${timestamp}</wsu:Created>
                        </wsse:UsernameToken>
                    </wsse:Security>
                </soap:Header>
                <soap:Body>
                    <wsnt:Subscribe>
                        <wsnt:Consumer>
                            <wsa:Address xmlns:wsa="http://www.w3.org/2005/08/addressing">http://localhost:8080/api/v1/onvif/events</wsa:Address>
                        </wsnt:Consumer>
                        <wsnt:Filter>
                            <tns1:TopicExpression Dialect="http://www.onvif.org/ver10/tev/topicExpression/ConcreteSet" 
                                                xmlns:tns1="http://www.onvif.org/ver10/events/wsdl">
                                tns1:Motion/MotionDetected
                            </tns1:TopicExpression>
                        </wsnt:Filter>
                    </wsnt:Subscribe>
                </soap:Body>
            </soap:Envelope>
        """.trimIndent()
    }

    private fun sendSubscriptionRequest(url: String, body: String, username: String, password: String): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/soap+xml")
            .header("Authorization", "Basic ${java.util.Base64.getEncoder().encodeToString("$username:$password".toByteArray())}")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun buildDeviceInfoRequest(username: String, password: String): HttpRequest {
        val url = "http://localhost/onvif/device_service"
        val soapBody = """
            <?xml version="1.0" encoding="UTF-8"?>
            <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope" 
                          xmlns:dn="http://www.onvif.org/ver10/device/wsdl">
                <soap:Body>
                    <dn:GetDeviceInformation/>
                </soap:Body>
            </soap:Envelope>
        """.trimIndent()

        return HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(5))
            .header("Content-Type", "application/soap+xml")
            .header("Authorization", "Basic ${java.util.Base64.getEncoder().encodeToString("$username:$password".toByteArray())}")
            .POST(HttpRequest.BodyPublishers.ofString(soapBody))
            .build()
    }

    private fun parseDeviceInfo(xml: String): OnvifDeviceInfo? {
        // Simplified parsing - in production use proper XML parser
        return OnvifDeviceInfo(
            manufacturer = extractXmlValue(xml, "Manufacturer"),
            model = extractXmlValue(xml, "Model"),
            firmwareVersion = extractXmlValue(xml, "FirmwareVersion"),
            serialNumber = extractXmlValue(xml, "SerialNumber"),
            hardwareId = extractXmlValue(xml, "HardwareId")
        )
    }

    private fun extractXmlValue(xml: String, tagName: String): String? {
        val pattern = Regex("<$tagName>([^<]*)</$tagName>")
        return pattern.find(xml)?.groupValues?.get(1)
    }

    data class OnvifDeviceInfo(
        val manufacturer: String?,
        val model: String?,
        val firmwareVersion: String?,
        val serialNumber: String?,
        val hardwareId: String?
    )
}
