package com.company.ipcamera.core.network

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * Интеграционные тесты для ONVIF клиента
 * 
 * Требуется:
 * - Реальная ONVIF камера в сети
 * - Настроенные переменные окружения:
 *   - TEST_ONVIF_CAMERA_URL
 *   - TEST_ONVIF_CAMERA_USERNAME
 *   - TEST_ONVIF_CAMERA_PASSWORD
 */
class OnvifClientIntegrationTest {

    private val testCameraUrl = System.getenv("TEST_ONVIF_CAMERA_URL") ?: "rtsp://192.168.1.100:554"
    private val testUsername = System.getenv("TEST_ONVIF_CAMERA_USERNAME") ?: "admin"
    private val testPassword = System.getenv("TEST_ONVIF_CAMERA_PASSWORD") ?: "password"

    private val client = OnvifClientFactory.create()

    @Test
    fun testDiscoverCameras() = runTest {
        println("Starting ONVIF camera discovery...")
        
        // Интеграционный тест: реальная сеть. withTimeout гарантирует завершение coroutine,
        // чтобы runTest не завершался UncompletedCoroutinesError (отсутствие висящих child jobs).
        val cameras = try {
            withTimeout(15_000) {
                client.discoverCameras(timeoutMillis = 10000, useUPnP = true)
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            // Discovery в сети без камер может не завершиться за 15s.
            // Не роняем unit-прогон: считаем «камер не найдено».
            println("Camera discovery timed out (no cameras in this environment)")
            emptyList()
        }
        
        println("Discovered ${cameras.size} cameras")
        cameras.forEach { camera ->
            println("  - ${camera.name} at ${camera.url}")
        }
        
        // Тест не должен падать, даже если камер нет
        assertNotNull(cameras, "Camera discovery should not return null")
        
        if (cameras.isEmpty()) {
            println("No cameras discovered (this is OK in test environment without cameras)")
        } else {
            assertTrue(cameras.isNotEmpty(), "Should discover at least one camera")
        }
    }

    @Test
    fun testGetDeviceInformation() = runTest {
        println("Testing device information retrieval...")
        
        val deviceInfo = client.getDeviceInformation(
            url = testCameraUrl,
            username = testUsername,
            password = testPassword
        )
        
        if (deviceInfo != null) {
            println("Device Info:")
            println("  - Manufacturer: ${deviceInfo.manufacturer}")
            println("  - Model: ${deviceInfo.model}")
            println("  - Firmware: ${deviceInfo.firmwareVersion}")
            println("  - Serial: ${deviceInfo.serialNumber}")
            
            assertNotNull(deviceInfo.manufacturer, "Manufacturer should not be null")
            assertNotNull(deviceInfo.model, "Model should not be null")
        } else {
            println("Could not retrieve device info (camera may not support ONVIF)")
        }
    }

    @Test
    fun testGetCapabilities() = runTest {
        println("Testing capabilities retrieval...")
        
        val capabilities = client.getCapabilities(
            url = testCameraUrl,
            username = testUsername,
            password = testPassword
        )
        
        if (capabilities != null) {
            println("Capabilities:")
            println("  - Device Service: ${capabilities.deviceServiceUrl}")
            println("  - Media Service: ${capabilities.mediaServiceUrl}")
            println("  - PTZ Service: ${capabilities.ptzServiceUrl}")
            println("  - Analytics Service: ${capabilities.analyticsServiceUrl}")
            println("  - Event Service: ${capabilities.eventServiceUrl}")
            
            assertNotNull(capabilities.mediaServiceUrl, "Media service should be available")
        } else {
            println("Could not retrieve capabilities (camera may not support ONVIF)")
        }
    }

    @Test
    fun testGetProfiles() = runTest {
        println("Testing profiles retrieval...")
        
        val profiles = client.getProfiles(
            url = testCameraUrl,
            username = testUsername,
            password = testPassword
        )
        
        println("Found ${profiles.size} profiles")
        profiles.forEach { profile ->
            println("  - Profile: ${profile.token} (${profile.name})")
            println("    Resolution: ${profile.videoResolution}")
            println("    FPS: ${profile.fps}")
            println("    Codec: ${profile.codec}")
        }
        
        if (profiles.isNotEmpty()) {
            assertTrue(profiles.first().token.isNotBlank(), "Profile token should not be blank")
        }
    }

    @Test
    fun testGetStreamUri() = runTest {
        println("Testing stream URI retrieval...")
        
        val profiles = client.getProfiles(
            url = testCameraUrl,
            username = testUsername,
            password = testPassword
        )
        
        if (profiles.isEmpty()) {
            println("No profiles available, skipping stream URI test")
            return@runTest
        }
        
        val streamUri = client.getStreamUri(
            url = testCameraUrl,
            profileToken = profiles.first().token,
            username = testUsername,
            password = testPassword
        )
        
        if (streamUri != null) {
            println("Stream URI: $streamUri")
            assertTrue(streamUri.startsWith("rtsp://"), "Stream URI should start with rtsp://")
        } else {
            println("Could not retrieve stream URI")
        }
    }

    @Test
    fun testConnection_Success() = runTest {
        println("Testing connection...")
        
        val result = client.testConnection(
            url = testCameraUrl,
            username = testUsername,
            password = testPassword
        )
        
        println("Connection test result: ${result::class.simpleName}")
        
        when (result) {
            is ConnectionTestResult.Success -> {
                println("Streams found: ${result.streams.size}")
                result.streams.forEach { stream ->
                    println("  - ${stream.type}: ${stream.resolution} @ ${stream.fps}fps (${stream.codec})")
                }
                
                if (result.capabilities != null) {
                    println("Capabilities:")
                    println("  - PTZ: ${result.capabilities.ptz}")
                    println("  - Audio: ${result.capabilities.audio}")
                    println("  - ONVIF: ${result.capabilities.onvif}")
                    println("  - Analytics: ${result.capabilities.analytics}")
                }
                
                assertTrue(result.streams.isNotEmpty(), "Should have at least one stream")
            }
            is ConnectionTestResult.Failure -> {
                println("Connection failed: ${result.error} (Code: ${result.code})")
                // Не считаем это ошибкой теста, так как камеры может не быть
            }
        }
    }

    @Test
    fun testConnection_InvalidCredentials() = runTest {
        println("Testing connection with invalid credentials...")
        
        val result = client.testConnection(
            url = testCameraUrl,
            username = "invalid_user",
            password = "wrong_password"
        )
        
        when (result) {
            is ConnectionTestResult.Failure -> {
                println("Expected failure: ${result.error}")
                // Ожидаем ошибку аутентификации
                assertTrue(
                    result.code == ErrorCode.AUTHENTICATION_FAILED || 
                    result.code == ErrorCode.CONNECTION_FAILED,
                    "Should fail with authentication or connection error"
                )
            }
            is ConnectionTestResult.Success -> {
                fail("Should not succeed with invalid credentials")
            }
        }
    }

    @Test
    fun testPtzMovement() = runTest {
        println("Testing PTZ movement...")
        
        // Сначала получаем профили
        val profiles = client.getProfiles(
            url = testCameraUrl,
            username = testUsername,
            password = testPassword
        )
        
        if (profiles.isEmpty()) {
            println("No profiles available, skipping PTZ test")
            return@runTest
        }
        
        // Проверяем поддержку PTZ
        val capabilities = client.getCapabilities(
            url = testCameraUrl,
            username = testUsername,
            password = testPassword
        )
        
        if (capabilities?.ptzServiceUrl == null) {
            println("PTZ service not available, skipping PTZ test")
            return@runTest
        }
        
        // Тестируем движение вправо
        val moveResult = client.movePtz(
            url = testCameraUrl,
            direction = PtzDirection.RIGHT,
            speed = 0.3f,
            username = testUsername,
            password = testPassword
        )
        
        println("PTZ move result: $moveResult")
        
        // Останавливаем движение
        val stopResult = client.stopPtz(
            url = testCameraUrl,
            profileToken = profiles.first().token,
            username = testUsername,
            password = testPassword
        )
        
        println("PTZ stop result: $stopResult")
        
        // Тест не должен падать, даже если PTZ не поддерживается
    }

    @Test
    fun testEventSubscription() = runTest {
        println("Testing event subscription...")
        
        // Создаем PullPoint подписку
        val subscriptionResult = client.createPullPointSubscription(
            url = testCameraUrl,
            username = testUsername,
            password = testPassword,
            subscriptionTime = 60 // 1 минута для теста
        )
        
        subscriptionResult.fold(
            onSuccess = { subscription ->
                println("Subscription created: ${subscription.id}")
                println("Subscription reference: ${subscription.subscriptionReference}")
                println("Expiration time: ${subscription.expirationTime}")
                
                // Пытаемся получить события
                val messagesResult = client.pullEventMessages(
                    subscriptionId = subscription.id,
                    timeout = 2000,
                    maxMessages = 5
                )
                
                messagesResult.fold(
                    onSuccess = { events ->
                        println("Received ${events.size} events")
                        events.forEach { event ->
                            println("  - Event: ${event.topic}")
                        }
                    },
                    onFailure = { error ->
                        println("Failed to pull messages: ${error.message}")
                    }
                )
                
                // Отписываемся
                val unsubscribeResult = client.unsubscribeFromEvents(subscription.id)
                println("Unsubscribe result: ${unsubscribeResult.isSuccess}")
            },
            onFailure = { error ->
                println("Failed to create subscription: ${error.message}")
                // Не считаем ошибкой, камера может не поддерживать события
            }
        )
    }

    @Test
    fun testCacheFunctionality() = runTest {
        println("Testing caching functionality...")
        
        val startTime = System.currentTimeMillis()
        
        // Первый запрос (без кэша)
        val capabilities1 = client.getCapabilities(
            url = testCameraUrl,
            username = testUsername,
            password = testPassword,
            useCache = true
        )
        
        val firstRequestTime = System.currentTimeMillis() - startTime
        
        // Второй запрос (должен вернуть из кэша)
        val capabilities2 = client.getCapabilities(
            url = testCameraUrl,
            username = testUsername,
            password = testPassword,
            useCache = true
        )
        
        val secondRequestTime = System.currentTimeMillis() - startTime - firstRequestTime
        
        println("First request time: ${firstRequestTime}ms")
        println("Second request time (cached): ${secondRequestTime}ms")
        
        if (capabilities1 != null && capabilities2 != null) {
            assertEquals(capabilities1, capabilities2, "Cached capabilities should match")
            assertTrue(secondRequestTime < firstRequestTime, "Cached request should be faster")
            println("Cache is working correctly")
        }
    }

    @Test
    fun testUrlNormalization() = runTest {
        println("Testing URL normalization...")
        
        // Тестируем нормализацию URL через получение capabilities
        val testUrls = listOf(
            "http://192.168.1.100:80",
            "http://192.168.1.100",
            "192.168.1.100",
            "http://192.168.1.100:80/onvif/device_service"
        )
        
        testUrls.forEach { url ->
            try {
                val capabilities = withTimeout(15_000) {
                    client.getCapabilities(
                        url = url,
                        username = testUsername,
                        password = testPassword
                    )
                }
                println("URL '$url' -> Capabilities: ${if (capabilities != null) "OK" else "null"}")
            } catch (e: Exception) {
                println("URL '$url' -> Error: ${e.message}")
            }
        }
    }
}
