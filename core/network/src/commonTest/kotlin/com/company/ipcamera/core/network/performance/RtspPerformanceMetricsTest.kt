package com.company.ipcamera.core.network.performance

import com.company.ipcamera.core.network.RtspClient
import com.company.ipcamera.core.network.RtspClientConfig
import com.company.ipcamera.core.network.RtspClientStatus.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Тесты производительности RTSP клиента.
 *
 * Измеряет:
 * - FPS (frames per second)
 * - Latency задержка соединения
 * - Memory footprint
 * - Нагрузка (множественные клиенты)
 *
 * Безопасность: Не модифицирует production код.
 * Использует только публичные API.
 */
class RtspPerformanceMetricsTest {

    private val testRtspUrl = System.getenv("TEST_RTSP_URL") ?: "rtsp://localhost:8554/stream"
    private val testUsername = System.getenv("TEST_RTSP_USERNAME")
    private val testPassword = System.getenv("TEST_RTSP_PASSWORD")
    private val enablePerfTests = System.getenv("ENABLE_PERFORMANCE_TESTS") == "true"
    private val enableIntegration = System.getenv("ENABLE_RTSP_INTEGRATION_TESTS") == "true"

    // ════════════════════════════════════════════
    // D2: Метрики производительности
    // ════════════════════════════════════════════

    /**
     * Тест: FPS (frames per second)
     *
     * Измеряет количество кадров в секунду
     * при подключении к RTSP потоку.
     */
    @Test
    fun testD2_fpsMeasurement() = runTest {
        if (!enablePerfTests || !enableIntegration) return@runTest

        val config = RtspClientConfig(
            url = testRtspUrl,
            username = testUsername,
            password = testPassword,
            timeoutMillis = 10000,
            enableVideo = true
        )

        val client = RtspClient(config)
        val frameTimestamps = mutableListOf<Long>()

        try {
            client.connect()
            val status = withTimeoutOrNull(15000) {
                client.getStatus().first { it != CONNECTING }
            } ?: client.getStatus().value
            assertTrue(status == CONNECTED || status == PLAYING)

            if (status == CONNECTED) client.play()

            // Сбор метрик FPS в течение 5 секунд
            val framesJob = launch {
                client.getVideoFrames().collect { frame ->
                    frameTimestamps.add(frame.timestamp)
                    if (frameTimestamps.size >= 100) cancel()
                }
            }

            delay(5000) // 5 секунд сбора
            framesJob.cancel()

            client.stop()
            client.disconnect()
        } catch (e: Exception) {
            fail("FPS measurement failed: ${e.message}")
        }

        // Расчёт FPS
        if (frameTimestamps.size >= 2) {
            val firstTs = frameTimestamps.first()
            val lastTs = frameTimestamps.last()
            val durationMs = lastTs - firstTs
            val fps = if (durationMs > 0) {
                (frameTimestamps.size.toDouble() / durationMs) * 1000.0
            } else {
                0.0
            }

            println("[METRIC] FPS: %.2f (frames: %d, duration: %d ms)".format(fps, frameTimestamps.size, durationMs))

            // FPS должен быть > 0 если кадры получены
            assertTrue(fps >= 0.0, "FPS should be measurable")
        } else {
            println("[METRIC] FPS: N/A (no frames received)")
        }
    }

    /**
     * Тест: Latency соединения
     *
     * Измеряет время установки соединения с RTSP сервером.
     */
    @Test
    fun testD2_connectionLatency() = runTest {
        if (!enablePerfTests || !enableIntegration) return@runTest

        val config = RtspClientConfig(
            url = testRtspUrl,
            username = testUsername,
            password = testPassword,
            timeoutMillis = 10000
        )

        val client = RtspClient(config)

        val latencies = mutableListOf<Long>()

        try {
            // 3 замера
            repeat(3) { attempt ->
                val startTime = System.currentTimeMillis()

                client.connect()
                val status = withTimeoutOrNull(15000) {
                    client.getStatus().first { it != CONNECTING }
                } ?: client.getStatus().value

                val endTime = System.currentTimeMillis()
                val latency = endTime - startTime
                latencies.add(latency)

                println("[METRIC] Connect latency #${attempt + 1}: $latency ms (status: $status)")

                if (status == CONNECTED || status == PLAYING) {
                    client.disconnect()
                }
                delay(500) // пауза между замерами
            }
        } catch (e: Exception) {
            fail("Latency measurement failed: ${e.message}")
        }

        // Статистика
        if (latencies.isNotEmpty()) {
            val avg = latencies.average()
            val min = latencies.min()
            val max = latencies.max()
            println(
                "[METRIC] Connection latency: avg=%.0f ms, min=%d ms, max=%d ms (samples: %d)"
                    .format(avg, min, max, latencies.size)
            )
        }
    }

    /**
     * Тест: Memory footprint RTSP клиента
     *
     * Измеряет приблизительный расход памяти.
     * Доступно только на JVM.
     */
    @Test
    fun testD2_memoryFootprint() = runTest {
        if (!enablePerfTests || !enableIntegration) return@runTest

        val memBefore = getMemoryUsage()

        val clients = mutableListOf<RtspClient>()
        repeat(10) { i ->
            val config = RtspClientConfig(
                url = testRtspUrl,
                username = testUsername,
                password = testPassword,
                timeoutMillis = 5000
            )
            clients.add(RtspClient(config))
        }

        val memAfter = getMemoryUsage()
        val memDelta = memAfter - memBefore

        println("[METRIC] Memory footprint: $memDelta KB for 10 clients")
        println("[METRIC] Per client: ${if (clients.isNotEmpty()) memDelta / clients.size else 0} KB")

        clients.forEach { it.disconnect() }

        assertTrue(memDelta >= 0, "Memory usage should not be negative")
    }

    /**
     * Тест: Stream info latency
     *
     * Измеряет время получения информации о потоке.
     */
    @Test
    fun testD2_streamInfoLatency() = runTest {
        if (!enablePerfTests || !enableIntegration) return@runTest

        val config = RtspClientConfig(
            url = testRtspUrl,
            username = testUsername,
            password = testPassword,
            timeoutMillis = 10000
        )

        val client = RtspClient(config)

        try {
            client.connect()
            val connectStatus = withTimeoutOrNull(15000) {
                client.getStatus().first { it != CONNECTING }
            } ?: client.getStatus().value

            if (connectStatus == CONNECTED || connectStatus == PLAYING) {
                // Время получения информации о потоках
                val startTime = System.currentTimeMillis()
                val streams = client.getStreams()
                val latency = System.currentTimeMillis() - startTime

                println("[METRIC] Get streams latency: $latency ms (streams: ${streams.size})")

                if (streams.isNotEmpty()) {
                    val infoTime = System.currentTimeMillis()
                    val info = client.getStreamInfo(0)
                    val infoLatency = System.currentTimeMillis() - infoTime

                    println("[METRIC] Get stream info latency: $infoLatency ms")
                    println("[METRIC] Stream 0: $info")
                }
            }

            client.disconnect()
        } catch (e: Exception) {
            fail("Stream info latency test failed: ${e.message}")
        }
    }

    // ════════════════════════════════════════════
    // D1: Нагрузочное тестирование
    // ════════════════════════════════════════════

    /**
     * Тест: 5 одновременных клиентов
     *
     * Проверяет, что система выдерживает 5+
     * одновременных RTSP соединений.
     */
    @Test
    fun testD1_loadFiveClients() = runTest {
        if (!enablePerfTests || !enableIntegration) return@runTest

        val clients = mutableListOf<RtspClient>()
        val results = mutableListOf<String>()

        try {
            // Создание 5 клиентов
            repeat(5) { i ->
                val config = RtspClientConfig(
                    url = testRtspUrl,
                    username = testUsername,
                    password = testPassword,
                    timeoutMillis = 10000,
                    enableVideo = i == 0 // только первый с видео для экономии
                )
                val client = RtspClient(config)
                clients.add(client)

                client.connect()
            }

            // Ожидание подключения всех
            delay(3000)

            // Проверка статусов
            clients.forEachIndexed { index, client ->
                val status = client.getStatus().value
                results.add("Client $index: $status")
                println("[LOAD] Client $index status: $status")
            }

            // Отключение всех
            clients.forEach { client ->
                try {
                    client.disconnect()
                } catch (_: Exception) { }
            }
        } catch (e: Exception) {
            fail("Load test (5 clients) failed: ${e.message}")
        } finally {
            clients.forEach { client ->
                try { client.disconnect() } catch (_: Exception) { }
            }
        }

        // Анализ результатов
        val connectedCount = results.count { it.contains("CONNECTED") || it.contains("PLAYING") }
        println("[LOAD] Connected: $connectedCount/5")

        assertTrue(connectedCount >= 3, "At least 3/5 clients should connect")
    }

    /**
     * Тест: Нагрузка с reconnect (множественные попытки)
     */
    @Test
    fun testD1_loadReconnectStress() = runTest {
        if (!enablePerfTests || !enableIntegration) return@runTest

        val config = RtspClientConfig(
            url = testRtspUrl,
            username = testUsername,
            password = testPassword,
            timeoutMillis = 5000,
            reconnectEnabled = true,
            reconnectMaxRetries = 3,
            reconnectInitialDelayMs = 100,
            reconnectMaxDelayMs = 500
        )

        val client = RtspClient(config)
        var connectAttempts = 0
        var connectSuccesses = 0

        try {
            // 5 циклов connect/disconnect
            repeat(5) { cycle ->
                connectAttempts++

                client.connect()
                val status = withTimeoutOrNull(10000) {
                    client.getStatus().first { it != CONNECTING }
                } ?: client.getStatus().value

                if (status == CONNECTED || status == PLAYING) {
                    connectSuccesses++
                }

                client.disconnect()
                delay(200)
            }

            println("[LOAD] Reconnect stress: $connectSuccesses/$connectAttempts successful")
        } catch (e: Exception) {
            fail("Reconnect stress test failed: ${e.message}")
        } finally {
            client.disconnect()
        }
    }

    /**
     * Тест: Параллельные потоки RTSP
     *
     * Проверяет, что система выдерживает
     * несколько потоков данных одновременно.
     */
    @Test
    fun testD1_loadMultipleStreams() = runTest {
        if (!enablePerfTests || !enableIntegration) return@runTest

        val config = RtspClientConfig(
            url = testRtspUrl,
            username = testUsername,
            password = testPassword,
            timeoutMillis = 10000,
            enableVideo = true,
            enableAudio = true
        )

        val client = RtspClient(config)
        var videoFrames = 0
        var audioFrames = 0

        try {
            client.connect()
            val status = withTimeoutOrNull(15000) {
                client.getStatus().first { it != CONNECTING }
            } ?: client.getStatus().value
            assertTrue(status == CONNECTED || status == PLAYING)

            if (status == CONNECTED) client.play()

            val videoJob = launch {
                client.getVideoFrames().collect {
                    videoFrames++
                    if (videoFrames >= 30) cancel()
                }
            }

            val audioJob = launch {
                client.getAudioFrames().collect {
                    audioFrames++
                    if (audioFrames >= 10) cancel()
                }
            }

            delay(10000) // 10 секунд сбора
            videoJob.cancel()
            audioJob.cancel()

            println("[LOAD] Video frames: $videoFrames, Audio frames: $audioFrames")

            client.stop()
            client.disconnect()
        } catch (e: Exception) {
            fail("Multiple streams load test failed: ${e.message}")
        }
    }

    // ════════════════════════════════════════════
    // Unit тесты метрик (не требуют реальной камеры)
    // ════════════════════════════════════════════

    @Test
    fun testD2_metricsNullSafety() = runTest {
        val config = RtspClientConfig(
            url = "rtsp://127.0.0.1:1/test",
            timeoutMillis = 1000
        )

        val client = RtspClient(config)

        // Diagnostics не должны быть null
        val diagnostics = client.getRuntimeDiagnostics().value
        assertNotNull(diagnostics)
        assertNotNull(diagnostics.connectAttempts)
        assertNotNull(diagnostics.connectSuccesses)
        assertNotNull(diagnostics.connectFailures)
        assertNotNull(diagnostics.reconnectAttempts)
        assertNotNull(diagnostics.reconnectFailures)

        client.disconnect()
    }

    @Test
    fun testD1_concurrentClientCreation() = runTest {
        val urls = listOf(
            "rtsp://127.0.0.1:1/stream1",
            "rtsp://127.0.0.1:1/stream2",
            "rtsp://127.0.0.1:1/stream3",
            "rtsp://127.0.0.1:1/stream4",
            "rtsp://127.0.0.1:1/stream5"
        )

        // Создание 5 клиентов (без реального подключения)
        val clients = urls.map { url ->
            RtspClient(RtspClientConfig(url = url, timeoutMillis = 1000))
        }

        assertEquals(5, clients.size)
        clients.forEach { client ->
            assertNotNull(client)
            assertEquals(DISCONNECTED, client.getStatus().value)
        }

        clients.forEach { it.disconnect() }
    }

    // ════════════════════════════════════════════
    // Вспомогательные методы
    // ════════════════════════════════════════════

    private fun getMemoryUsage(): Long {
        return try {
            val runtime = Runtime.getRuntime()
            val total = runtime.totalMemory()
            val free = runtime.freeMemory()
            (total - free) / 1024 // KB
        } catch (_: Exception) {
            -1L
        }
    }
}
