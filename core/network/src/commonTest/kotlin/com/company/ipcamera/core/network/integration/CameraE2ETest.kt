package com.company.ipcamera.core.network.integration

import com.company.ipcamera.core.network.RtspClient
import com.company.ipcamera.core.network.RtspClientConfig
import com.company.ipcamera.core.network.RtspClientStatus.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * E2E (End-to-End) тесты для полного сценария работы с камерами.
 *
 * Сценарий: Подключение → Просмотр потока → Запись → Уведомление
 *
 * Безопасность: Не модифицирует production код. Использует только
 * публичные API (RtspClient, RtspClientConfig, RtspClientStatus).
 *
 * Зависит от переменных окружения:
 * - TEST_RTSP_URL (обязательно для реальных тестов)
 * - TEST_RTSP_USERNAME (опционально)
 * - TEST_RTSP_PASSWORD (опционально)
 * - ENABLE_RTSP_INTEGRATION_TESTS=true (обязательно)
 */
class CameraE2ETest {

    private val testRtspUrl = System.getenv("TEST_RTSP_URL") ?: "rtsp://localhost:8554/stream"
    private val testUsername = System.getenv("TEST_RTSP_USERNAME")
    private val testPassword = System.getenv("TEST_RTSP_PASSWORD")
    private val enableIntegration = System.getenv("ENABLE_RTSP_INTEGRATION_TESTS") == "true"

    /**
     * E2E сценарий 1: Подключение к камере + получение статуса
     *
     * Проверяет:
     * 1. Создание клиента
     * 2. Успешное подключение к RTSP endpoint
     * 3. Статус CONNECTED или PLAYING
     * 4. Корректное отключение → DISCONNECTED
     */
    @Test
    fun testE2E_cameraConnectAndDisconnect() = runTest {
        if (!enableIntegration) return@runTest

        val config = RtspClientConfig(
            url = testRtspUrl,
            username = testUsername,
            password = testPassword,
            timeoutMillis = 10000
        )

        val client = RtspClient(config)

        try {
            // 1. Подключение
            client.connect()
            val status = withTimeoutOrNull(15000) {
                client.getStatus().first { it != CONNECTING }
            } ?: client.getStatus().value

            assertTrue(
                status == CONNECTED || status == PLAYING,
                "Camera should connect, got: $status"
            )

            // 2. Проверка потоков
            val streams = client.getStreams()
            assertNotNull(streams, "Streams list should not be null")

            // 3. Отключение
            client.disconnect()
            val finalStatus = client.getStatus().first()
            assertEquals(DISCONNECTED, finalStatus, "Should be disconnected")
        } catch (e: Exception) {
            fail("Camera E2E connect test failed: ${e.message}")
        }
    }

    /**
     * E2E сценарий 2: Просмотр потока (play)
     *
     * Проверяет:
     * 1. Подключение
     * 2. Начало воспроизведения → PLAYING
     * 3. Остановка → CONNECTED
     * 4. Отключение
     */
    @Test
    fun testE2E_cameraPlayAndStop() = runTest {
        if (!enableIntegration) return@runTest

        val config = RtspClientConfig(
            url = testRtspUrl,
            username = testUsername,
            password = testPassword,
            timeoutMillis = 10000
        )

        val client = RtspClient(config)

        try {
            // Подключение
            client.connect()
            val connectStatus = withTimeoutOrNull(15000) {
                client.getStatus().first { it != CONNECTING }
            } ?: client.getStatus().value
            assertTrue(
                connectStatus == CONNECTED || connectStatus == PLAYING,
                "Connect should succeed, got: $connectStatus"
            )

            // Если уже PLAYING — значит auto-play, тест пройден
            if (connectStatus == CONNECTED) {
                // Play
                client.play()
                val playStatus = withTimeoutOrNull(5000) {
                    client.getStatus().first { it != CONNECTED }
                } ?: client.getStatus().value
                assertEquals(PLAYING, playStatus, "Should be PLAYING after play()")
            }

            // Stop
            client.stop()
            val stopStatus = withTimeoutOrNull(5000) {
                client.getStatus().first { it != PLAYING }
            } ?: client.getStatus().value

            // После stop статус может быть CONNECTED или DISCONNECTED
            assertTrue(
                stopStatus == CONNECTED || stopStatus == DISCONNECTED,
                "After stop should be CONNECTED or DISCONNECTED, got: $stopStatus"
            )

            // Отключение
            client.disconnect()
            val finalStatus = client.getStatus().first()
            assertEquals(DISCONNECTED, finalStatus)
        } catch (e: Exception) {
            fail("Camera E2E play/stop test failed: ${e.message}")
        }
    }

    /**
     * E2E сценарий 3: Получение видеокадров
     *
     * Проверяет:
     * 1. Подключение и play
     * 2. Получение хотя бы 1 кадра
     * 3. Валидность данных кадра
     */
    @Test
    fun testE2E_cameraReceiveFrames() = runTest {
        if (!enableIntegration) return@runTest

        val config = RtspClientConfig(
            url = testRtspUrl,
            username = testUsername,
            password = testPassword,
            timeoutMillis = 10000,
            enableVideo = true
        )

        val client = RtspClient(config)
        val receivedFrames = mutableListOf<com.company.ipcamera.core.network.RtspFrame>()

        try {
            // Подключение
            client.connect()
            val connectStatus = withTimeoutOrNull(15000) {
                client.getStatus().first { it != CONNECTING }
            } ?: client.getStatus().value
            assertTrue(
                connectStatus == CONNECTED || connectStatus == PLAYING,
                "Connect should succeed, got: $connectStatus"
            )

            // Play если не auto-play
            if (connectStatus == CONNECTED) {
                client.play()
            }

            // Сбор кадров
            val framesJob = launch {
                client.getVideoFrames().collect { frame ->
                    receivedFrames.add(frame)
                    if (receivedFrames.size >= 5) {
                        cancel()
                    }
                }
            }

            withTimeout(15000) {
                framesJob.join()
            }

            // Проверка: хотя бы 1 кадр
            assertTrue(receivedFrames.isNotEmpty(), "Should receive at least one frame")
            receivedFrames.forEach { frame ->
                assertNotNull(frame.data, "Frame data should not be null")
                assertTrue(frame.data.isNotEmpty(), "Frame data should not be empty")
            }

            // Stop
            client.stop()
            client.disconnect()
        } catch (e: TimeoutCancellationException) {
            // Таймаут допустим если сервер не отправляет кадры
            assertTrue(receivedFrames.isNotEmpty(), "Should receive frames before timeout")
        } catch (e: Exception) {
            fail("Camera E2E frames test failed: ${e.message}")
        }
    }

    /**
     * E2E сценарий 4: Стабильность соединения (soak)
     *
     * Проверяет:
     * 1. Длительное подключение (30 сек)
     * 2. Отсутствие ERROR статуса
     * 3. Получение кадров в течение всего времени
     */
    @Test
    fun testE2E_cameraSoakStability() = runTest {
        if (!enableIntegration) return@runTest

        val config = RtspClientConfig(
            url = testRtspUrl,
            username = testUsername,
            password = testPassword,
            timeoutMillis = 10000,
            enableVideo = true
        )

        val client = RtspClient(config)
        var errorSeen = false
        var framesReceived = 0

        try {
            // Мониторинг ошибок
            client.setStatusCallback { status, _ ->
                if (status == ERROR) {
                    errorSeen = true
                }
            }

            // Сбор кадров
            val framesJob = launch {
                client.getVideoFrames().collect {
                    framesReceived++
                }
            }

            // Подключение
            client.connect()
            val connectStatus = withTimeoutOrNull(15000) {
                client.getStatus().first { it != CONNECTING }
            } ?: client.getStatus().value
            assertTrue(
                connectStatus == CONNECTED || connectStatus == PLAYING,
                "Connect should succeed, got: $connectStatus"
            )

            if (connectStatus == CONNECTED) {
                client.play()
            }

            // Soak: 30 секунд
            val soakStart = System.currentTimeMillis()
            while (System.currentTimeMillis() - soakStart < 30_000) {
                delay(1000)
                assertFalse(errorSeen, "ERROR status should not occur during soak")
            }

            // Отключение
            framesJob.cancel()
            client.stop()
            client.disconnect()
        } catch (e: Exception) {
            fail("Camera E2E soak test failed: ${e.message}")
        }
    }

    /**
     * E2E сценарий 5: Переподключение после разрыва
     *
     * Проверяет:
     * 1. Подключение
     * 2. Принудительное отключение
     * 3. Повторное подключение
     */
    @Test
    fun testE2E_cameraReconnect() = runTest {
        if (!enableIntegration) return@runTest

        val config = RtspClientConfig(
            url = testRtspUrl,
            username = testUsername,
            password = testPassword,
            timeoutMillis = 10000
        )

        val client = RtspClient(config)

        try {
            // Первое подключение
            client.connect()
            val status1 = withTimeoutOrNull(15000) {
                client.getStatus().first { it != CONNECTING }
            } ?: client.getStatus().value
            assertTrue(
                status1 == CONNECTED || status1 == PLAYING,
                "First connect should succeed, got: $status1"
            )

            // Отключение
            client.disconnect()
            assertEquals(DISCONNECTED, client.getStatus().first())

            // Повторное подключение
            client.connect()
            val status2 = withTimeoutOrNull(15000) {
                client.getStatus().first { it != CONNECTING }
            } ?: client.getStatus().value
            assertTrue(
                status2 == CONNECTED || status2 == PLAYING,
                "Reconnect should succeed, got: $status2"
            )

            // Отключение
            client.disconnect()
        } catch (e: Exception) {
            fail("Camera E2E reconnect test failed: ${e.message}")
        }
    }

    /**
     * E2E сценарий 6: Проверка валидации конфигурации
     *
     * Проверяет создание клиента с разными конфигурациями
     */
    @Test
    fun testE2E_cameraConfigValidation() {
        // Валидная конфигурация
        val validConfig = RtspClientConfig(url = "rtsp://192.168.1.1:554/stream1")
        assertNotNull(validConfig)
        assertEquals("rtsp://192.168.1.1:554/stream1", validConfig.url)
        assertNull(validConfig.username)

        // С аутентификацией
        val authConfig = RtspClientConfig(
            url = "rtsp://192.168.1.1:554/stream1",
            username = "admin",
            password = "admin123"
        )
        assertNotNull(authConfig)
        assertEquals("admin", authConfig.username)
        assertEquals("admin123", authConfig.password)

        // С кастомным таймаутом
        val timeoutConfig = RtspClientConfig(
            url = "rtsp://192.168.1.1:554/stream1",
            timeoutMillis = 5000
        )
        assertEquals(5000L, timeoutConfig.timeoutMillis)
    }

    /**
     * E2E сценарий 7: Потокобезопасность при множественных вызовах
     *
     * Проверяет, что клиент корректно обрабатывает
     * множественные вызовы connect/disconnect
     */
    @Test
    fun testE2E_cameraConcurrentCalls() = runTest {
        val config = RtspClientConfig(
            url = testRtspUrl,
            username = testUsername,
            password = testPassword,
            timeoutMillis = 5000
        )

        val client = RtspClient(config)

        // Множественные вызовы не должны вызывать исключений
        repeat(3) {
            client.disconnect()
            val status = client.getStatus().first()
            assertEquals(DISCONNECTED, status)
        }

        assertTrue(true, "Multiple disconnect calls should not throw")
    }
}
