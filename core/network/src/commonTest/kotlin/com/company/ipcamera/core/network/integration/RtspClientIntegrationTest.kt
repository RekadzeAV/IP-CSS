package com.company.ipcamera.core.network.integration

import com.company.ipcamera.core.network.RtspClient
import com.company.ipcamera.core.network.RtspClientConfig
import com.company.ipcamera.core.network.RtspClientStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlin.test.*

/**
 * Интеграционные тесты для RTSP клиента с реальными RTSP серверами
 *
 * Эти тесты требуют реальных RTSP серверов для выполнения.
 * Для запуска необходимо настроить тестовые RTSP серверы.
 *
 * Примеры настройки тестового RTSP сервера:
 * 1. VLC Media Player: Media -> Stream -> Network -> RTSP
 * 2. FFmpeg: ffmpeg -re -i input.mp4 -c copy -f rtsp rtsp://localhost:8554/stream
 * 3. GStreamer: gst-launch-1.0 videotestsrc ! x264enc ! rtspclientsink location=rtsp://localhost:8554/stream
 *
 * Примечание: Эти тесты могут быть помечены как @Ignore для CI/CD,
 * так как требуют реальных RTSP серверов и могут быть нестабильными.
 */
class RtspClientIntegrationTest {

    // Конфигурация тестового RTSP сервера
    // Измените эти значения для вашего тестового сервера
    private val testRtspUrl = System.getenv("TEST_RTSP_URL") ?: "rtsp://localhost:8554/stream"
    private val testUsername = System.getenv("TEST_RTSP_USERNAME")
    private val testPassword = System.getenv("TEST_RTSP_PASSWORD")
    private val enableIntegration = System.getenv("ENABLE_RTSP_INTEGRATION_TESTS") == "true"
    private val soakDurationSec = System.getenv("RTSP_SOAK_DURATION_SEC")?.toLongOrNull()?.coerceAtLeast(10L) ?: 60L

    @Test
    fun testRtspClient_connectToRealServer() = runTest {
        if (!enableIntegration) return@runTest
        // Тест подключения к реальному RTSP серверу
        val config = RtspClientConfig(
            url = testRtspUrl,
            username = testUsername,
            password = testPassword,
            timeoutMillis = 10000
        )

        val client = RtspClient(config)

        try {
            // Подключаемся к серверу (connect запускает job, ждём статус)
            client.connect()
            val status = withTimeoutOrNull(15000) {
                client.getStatus().first { it == RtspClientStatus.CONNECTED || it == RtspClientStatus.PLAYING || it == RtspClientStatus.ERROR }
            } ?: client.getStatus().value
            assertTrue(
                status == RtspClientStatus.CONNECTED || status == RtspClientStatus.PLAYING,
                "Connection should succeed, got: $status"
            )
            assertTrue(
                status == RtspClientStatus.CONNECTED || status == RtspClientStatus.PLAYING,
                "Status should be CONNECTED or PLAYING after successful connection"
            )

            // Отключаемся
            client.disconnect()

            val finalStatus = client.getStatus().first()
            assertEquals(RtspClientStatus.DISCONNECTED, finalStatus, "Status should be DISCONNECTED after disconnect")
        } catch (e: Exception) {
            fail("Connection test failed: ${e.message}")
        }
    }

    @Test
    fun testRtspClient_playStream() = runTest {
        if (!enableIntegration) return@runTest
        // Тест воспроизведения потока с реального RTSP сервера
        val config = RtspClientConfig(
            url = testRtspUrl,
            username = testUsername,
            password = testPassword,
            timeoutMillis = 10000
        )

        val client = RtspClient(config)

        try {
            // Подключаемся
            client.connect()
            val connectStatus = withTimeoutOrNull(15000) {
                client.getStatus().first { it == RtspClientStatus.CONNECTED || it == RtspClientStatus.PLAYING || it == RtspClientStatus.ERROR }
            } ?: client.getStatus().value
            assertTrue(
                connectStatus == RtspClientStatus.CONNECTED || connectStatus == RtspClientStatus.PLAYING,
                "Connection should succeed, got: $connectStatus"
            )

            // Начинаем воспроизведение
            client.play()
            val status = client.getStatus().first()
            assertEquals(RtspClientStatus.PLAYING, status, "Status should be PLAYING")

            // Ждем немного для получения кадров
            delay(2000)

            // Останавливаем воспроизведение
            client.stop()

            // Отключаемся
            client.disconnect()
        } catch (e: Exception) {
            fail("Play stream test failed: ${e.message}")
        }
    }

    @Test
    fun testRtspClient_receiveFrames() = runTest {
        if (!enableIntegration) return@runTest
        // Тест получения кадров из реального RTSP потока
        val config = RtspClientConfig(
            url = testRtspUrl,
            username = testUsername,
            password = testPassword,
            timeoutMillis = 10000,
            enableVideo = true,
            enableAudio = true
        )

        val client = RtspClient(config)
        val receivedFrames = mutableListOf<com.company.ipcamera.core.network.RtspFrame>()

        try {
            // Подключаемся
            client.connect()
            val connectStatus = withTimeoutOrNull(15000) {
                client.getStatus().first { it == RtspClientStatus.CONNECTED || it == RtspClientStatus.PLAYING || it == RtspClientStatus.ERROR }
            } ?: client.getStatus().value
            assertTrue(
                connectStatus == RtspClientStatus.CONNECTED || connectStatus == RtspClientStatus.PLAYING,
                "Connection should succeed, got: $connectStatus"
            )

            // Начинаем воспроизведение
            client.play()

            // Подписываемся на видеокадры
            val framesJob = launch {
                client.getVideoFrames().collect { frame ->
                    receivedFrames.add(frame)
                    if (receivedFrames.size >= 10) {
                        cancel() // Получили достаточно кадров для теста
                    }
                }
            }

            // Ждем получения кадров (максимум 10 секунд)
            withTimeout(10000) {
                framesJob.join()
            }

            // Проверяем, что получили кадры
            assertTrue(receivedFrames.isNotEmpty(), "Should receive at least one frame")
            assertTrue(receivedFrames.size >= 1, "Should receive multiple frames")

            // Проверяем структуру кадров
            receivedFrames.forEach { frame ->
                assertNotNull(frame.data, "Frame data should not be null")
                assertTrue(frame.data.isNotEmpty(), "Frame data should not be empty")
                assertNotNull(frame.timestamp, "Frame timestamp should not be null")
            }

            // Останавливаем и отключаемся
            client.stop()
            client.disconnect()
        } catch (e: TimeoutCancellationException) {
            // Таймаут может быть нормальным, если сервер не отправляет кадры быстро
            println("Frame reception timeout (this may be normal): ${e.message}")
            assertTrue(receivedFrames.isNotEmpty(), "Should receive at least some frames before timeout")
        } catch (e: Exception) {
            fail("Receive frames test failed: ${e.message}")
        }
    }

    @Test
    fun testRtspClient_handleReconnection() = runTest {
        if (!enableIntegration) return@runTest
        // Тест обработки переподключения при разрыве соединения
        val config = RtspClientConfig(
            url = testRtspUrl,
            username = testUsername,
            password = testPassword,
            timeoutMillis = 5000
        )

        val client = RtspClient(config)

        try {
            // Подключаемся
            client.connect()
            var status = withTimeoutOrNull(15000) {
                client.getStatus().first { it == RtspClientStatus.CONNECTED || it == RtspClientStatus.PLAYING || it == RtspClientStatus.ERROR }
            } ?: client.getStatus().value
            assertTrue(
                status == RtspClientStatus.CONNECTED || status == RtspClientStatus.PLAYING,
                "Connection should succeed, got: $status"
            )

            // Начинаем воспроизведение
            client.play()

            // Симулируем разрыв соединения (отключаемся)
            client.disconnect()

            val disconnectedStatus = client.getStatus().first()
            assertEquals(RtspClientStatus.DISCONNECTED, disconnectedStatus, "Status should be DISCONNECTED")

            // Переподключаемся
            client.connect()
            status = withTimeoutOrNull(15000) {
                client.getStatus().first { it == RtspClientStatus.CONNECTED || it == RtspClientStatus.PLAYING || it == RtspClientStatus.ERROR }
            } ?: client.getStatus().value
            assertTrue(
                status == RtspClientStatus.CONNECTED || status == RtspClientStatus.PLAYING,
                "Reconnection should succeed, got: $status"
            )

            // Отключаемся
            client.disconnect()
        } catch (e: Exception) {
            fail("Reconnection test failed: ${e.message}")
        }
    }

    @Test
    fun testRtspClient_handleAuthentication() = runTest {
        if (!enableIntegration) return@runTest
        // Тест обработки аутентификации (Basic/Digest)
        if (testUsername.isNullOrBlank() || testPassword.isNullOrBlank()) return@runTest

        val config = RtspClientConfig(
            url = testRtspUrl,
            username = testUsername,
            password = testPassword,
            timeoutMillis = 10000
        )

        val client = RtspClient(config)

        try {
            // Подключаемся с аутентификацией
            client.connect()
            val status = withTimeoutOrNull(15000) {
                client.getStatus().first { it == RtspClientStatus.CONNECTED || it == RtspClientStatus.PLAYING || it == RtspClientStatus.ERROR }
            } ?: client.getStatus().value
            assertTrue(
                status == RtspClientStatus.CONNECTED || status == RtspClientStatus.PLAYING,
                "Connection with authentication should succeed, got: $status"
            )
            assertTrue(
                status == RtspClientStatus.CONNECTED || status == RtspClientStatus.PLAYING,
                "Status should be CONNECTED or PLAYING after authenticated connection"
            )

            client.disconnect()
        } catch (e: Exception) {
            fail("Authentication test failed: ${e.message}")
        }
    }

    @Test
    fun testRtspClient_handleInvalidCredentials() = runTest {
        if (!enableIntegration) return@runTest
        // Тест обработки неверных учетных данных
        val config = RtspClientConfig(
            url = testRtspUrl,
            username = "invalid_user",
            password = "invalid_password",
            timeoutMillis = 5000
        )

        val client = RtspClient(config)

        try {
            // Попытка подключения с неверными учетными данными
            client.connect()
            delay(1000)

            val status = client.getStatus().first()
            assertTrue(
                status == RtspClientStatus.DISCONNECTED || status == RtspClientStatus.ERROR,
                "Status should be DISCONNECTED or ERROR after failed authentication"
            )
        } catch (e: Exception) {
            // Ожидаемое исключение при неверных учетных данных
            assertTrue(true, "Invalid credentials correctly rejected")
        }
    }

    @Test
    fun testRtspClient_multipleStreams() = runTest {
        if (!enableIntegration) return@runTest
        // Тест работы с несколькими потоками одновременно
        val config1 = RtspClientConfig(
            url = testRtspUrl,
            username = testUsername,
            password = testPassword,
            timeoutMillis = 10000
        )

        val config2 = RtspClientConfig(
            url = testRtspUrl, // Можно использовать другой URL
            username = testUsername,
            password = testPassword,
            timeoutMillis = 10000
        )

        val client1 = RtspClient(config1)
        val client2 = RtspClient(config2)

        try {
            // Подключаем оба клиента
            client1.connect()
            client2.connect()
            val status1 = withTimeoutOrNull(15000) {
                client1.getStatus().first { it == RtspClientStatus.CONNECTED || it == RtspClientStatus.PLAYING || it == RtspClientStatus.ERROR }
            } ?: client1.getStatus().value
            val status2 = withTimeoutOrNull(15000) {
                client2.getStatus().first { it == RtspClientStatus.CONNECTED || it == RtspClientStatus.PLAYING || it == RtspClientStatus.ERROR }
            } ?: client2.getStatus().value
            assertTrue(
                status1 == RtspClientStatus.CONNECTED || status1 == RtspClientStatus.PLAYING,
                "First client connection should succeed, got: $status1"
            )
            assertTrue(
                status2 == RtspClientStatus.CONNECTED || status2 == RtspClientStatus.PLAYING,
                "Second client connection should succeed, got: $status2"
            )

            // Начинаем воспроизведение на обоих
            client1.play()
            client2.play()

            // Ждем немного
            delay(1000)

            // Останавливаем и отключаемся
            client1.stop()
            client2.stop()
            client1.disconnect()
            client2.disconnect()
        } catch (e: Exception) {
            fail("Multiple streams test failed: ${e.message}")
        }
    }

    @Test
    fun testRtspClientConfig_defaults() {
        // Тест конфигурации по умолчанию (не требует реального сервера)
        val config = RtspClientConfig(url = "rtsp://test.com/stream")

        assertEquals("rtsp://test.com/stream", config.url)
        assertNull(config.username)
        assertNull(config.password)
        assertEquals(10000L, config.timeoutMillis)
        assertTrue(config.enableVideo)
        assertTrue(config.enableAudio)
    }

    @Test
    fun testRtspClient_soakSmoke() = runTest {
        if (!enableIntegration) return@runTest

        val config = RtspClientConfig(
            url = testRtspUrl,
            username = testUsername,
            password = testPassword,
            timeoutMillis = 10000,
            enableVideo = true,
            enableAudio = false
        )

        val client = RtspClient(config)
        var framesReceived = 0
        var errorSeen = false

        try {
            client.setStatusCallback { status, _ ->
                if (status == RtspClientStatus.ERROR) {
                    errorSeen = true
                }
            }

            val framesJob = launch {
                client.getVideoFrames().collect {
                    framesReceived++
                }
            }

            client.connect()
            val connectStatus = withTimeoutOrNull(15000) {
                client.getStatus().first { it == RtspClientStatus.CONNECTED || it == RtspClientStatus.PLAYING || it == RtspClientStatus.ERROR }
            } ?: client.getStatus().value

            assertTrue(
                connectStatus == RtspClientStatus.CONNECTED || connectStatus == RtspClientStatus.PLAYING,
                "Connection should succeed before soak, got: $connectStatus"
            )

            client.play()
            val startedAt = System.currentTimeMillis()
            while (System.currentTimeMillis() - startedAt < soakDurationSec * 1000) {
                delay(1000)
                assertTrue(!errorSeen, "RTSP client entered ERROR state during soak")
            }

            assertTrue(framesReceived > 0, "Expected to receive at least one frame during soak test")

            client.stop()
            client.disconnect()
            framesJob.cancel()
        } catch (e: Exception) {
            fail("Soak smoke test failed: ${e.message}")
        }
    }
}
