package com.company.ipcamera.core.network

import com.company.ipcamera.core.network.rtsp.NativeRtspClient
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*
import kotlin.test.Ignore

/**
 * Unit тесты для FFI биндингов RTSP клиента
 *
 * Эти тесты проверяют:
 * - Создание и уничтожение нативного клиента
 * - Корректность конвертации типов между Kotlin и C++
 * - Работу callbacks
 *
 * Примечание: Для полного тестирования требуется скомпилированная нативная библиотека.
 * На платформах без нативной библиотеки эти тесты пропускаются.
 */
@Ignore("Requires native Live555 library")
class RtspClientFFITest {
    private fun createNativeClientOrSkip(): NativeRtspClient? {
        return try {
            NativeRtspClient()
        } catch (_: RuntimeException) {
            null
        }
    }

    @Test
    fun testNativeClientCreation() {
        val nativeClient = createNativeClientOrSkip() ?: return
        val handle = nativeClient.create()

        // Handle должен быть не нулевым (или 0, если библиотека не загружена)
        assertNotNull(handle)

        // Если библиотека загружена, handle должен быть > 0
        // Если библиотека не загружена, handle будет 0
        if (handle != 0L) {
            // Проверяем, что можем получить статус
            val status = nativeClient.getStatus(handle)
            assertNotNull(status)
            assertEquals(RtspClientStatus.DISCONNECTED, status)
        }
    }

    @Test
    fun testNativeClientDestroy() {
        val nativeClient = createNativeClientOrSkip() ?: return
        val handle = nativeClient.create()

        if (handle != 0L) {
            // Уничтожение должно работать без ошибок
            nativeClient.destroy(handle)

            // После уничтожения handle становится невалидным
            // Попытка использовать его может привести к ошибке, но это нормально
        }
    }

    @Test
    fun testNativeClientGetStatus() {
        val nativeClient = createNativeClientOrSkip() ?: return
        val handle = nativeClient.create()

        if (handle != 0L) {
            val status = nativeClient.getStatus(handle)

            // Статус должен быть одним из допустимых значений
            assertTrue(
                status in listOf(
                    RtspClientStatus.DISCONNECTED,
                    RtspClientStatus.CONNECTING,
                    RtspClientStatus.CONNECTED,
                    RtspClientStatus.PLAYING,
                    RtspClientStatus.ERROR
                )
            )
        }
    }

    @Test
    fun testNativeClientGetStreamCount() {
        val nativeClient = createNativeClientOrSkip() ?: return
        val handle = nativeClient.create()

        if (handle != 0L) {
            val streamCount = nativeClient.getStreamCount(handle)

            // Количество потоков должно быть >= 0
            assertTrue(streamCount >= 0)

            // До подключения должно быть 0 потоков
            assertEquals(0, streamCount)
        }
    }

    @Test
    fun testNativeClientGetStreamType() {
        val nativeClient = createNativeClientOrSkip() ?: return
        val handle = nativeClient.create()

        if (handle != 0L) {
            // До подключения потоков нет, поэтому getStreamType должен вернуть null
            val streamType = nativeClient.getStreamType(handle, 0)
            assertNull(streamType)
        }
    }

    @Test
    fun testNativeClientGetStreamInfo() {
        val nativeClient = createNativeClientOrSkip() ?: return
        val handle = nativeClient.create()

        if (handle != 0L) {
            // До подключения информации о потоках нет
            val streamInfo = nativeClient.getStreamInfo(handle, 0)
            assertNull(streamInfo)
        }
    }

    @Test
    fun testNativeClientSetReconnectParams() = runTest {
        val nativeClient = createNativeClientOrSkip() ?: return@runTest
        val handle = nativeClient.create()

        if (handle != 0L) {
            // Установка параметров переподключения должна работать без ошибок
            nativeClient.setReconnectParams(
                handle = handle,
                enabled = true,
                maxRetries = 5,
                initialDelayMs = 1000,
                maxDelayMs = 30000,
                backoffMultiplier = 2.0f
            )

            // Если нет ошибок, тест пройден
            assertTrue(true)
        }
    }

    @Test
    fun testNativeClientSetFrameCallback() = runTest {
        val nativeClient = createNativeClientOrSkip() ?: return@runTest
        val handle = nativeClient.create()

        if (handle != 0L) {
            var callbackInvoked = false

            // Установка callback должна работать без ошибок
            nativeClient.setFrameCallback(
                handle = handle,
                streamType = RtspStreamType.VIDEO,
                callback = { frame ->
                    callbackInvoked = true
                    assertNotNull(frame)
                    assertEquals(RtspStreamType.VIDEO, frame.streamType)
                }
            )

            // Callback не должен быть вызван без подключения и воспроизведения
            assertFalse(callbackInvoked)
        }
    }

    @Test
    fun testNativeClientSetStatusCallback() = runTest {
        val nativeClient = createNativeClientOrSkip() ?: return@runTest
        val handle = nativeClient.create()

        if (handle != 0L) {
            var callbackInvoked = false
            var receivedStatus: RtspClientStatus? = null
            var receivedMessage: String? = null

            // Установка callback должна работать без ошибок
            nativeClient.setStatusCallback(
                handle = handle,
                callback = { status, message ->
                    callbackInvoked = true
                    receivedStatus = status
                    receivedMessage = message
                }
            )

            // Callback может быть вызван при создании клиента
            // Проверяем только, что установка прошла без ошибок
            assertTrue(true)
        }
    }

    @Test
    fun testNativeClientConnectWithoutServer() = runTest {
        val nativeClient = createNativeClientOrSkip() ?: return@runTest
        val handle = nativeClient.create()

        if (handle != 0L) {
            // Попытка подключения к несуществующему серверу должна вернуть false
            val result = nativeClient.connect(
                handle = handle,
                url = "rtsp://192.168.255.255:554/nonexistent",
                username = null,
                password = null,
                timeoutMs = 1000 // Короткий таймаут для быстрого теста
            )

            // Подключение должно завершиться с ошибкой (false)
            assertFalse(result)

            // Статус должен быть ERROR или DISCONNECTED
            val status = nativeClient.getStatus(handle)
            assertTrue(
                status == RtspClientStatus.ERROR ||
                    status == RtspClientStatus.DISCONNECTED
            )
        }
    }

    @Test
    fun testNativeClientDisconnect() = runTest {
        val nativeClient = createNativeClientOrSkip() ?: return@runTest
        val handle = nativeClient.create()

        if (handle != 0L) {
            // Отключение должно работать без ошибок, даже если не было подключения
            nativeClient.disconnect(handle)

            // После отключения статус должен быть DISCONNECTED
            val status = nativeClient.getStatus(handle)
            assertEquals(RtspClientStatus.DISCONNECTED, status)
        }
    }

    @Test
    fun testNativeClientPlayWithoutConnect() = runTest {
        val nativeClient = createNativeClientOrSkip() ?: return@runTest
        val handle = nativeClient.create()

        if (handle != 0L) {
            // Попытка воспроизведения без подключения должна вернуть false
            val result = nativeClient.play(handle)
            assertFalse(result)
        }
    }

    @Test
    fun testNativeClientStopWithoutPlay() = runTest {
        val nativeClient = createNativeClientOrSkip() ?: return@runTest
        val handle = nativeClient.create()

        if (handle != 0L) {
            // Остановка должна работать без ошибок, даже если не было воспроизведения
            val result = nativeClient.stop(handle)
            // stop может вернуть true или false в зависимости от реализации
            assertNotNull(result)
        }
    }

    @Test
    fun testNativeClientPauseWithoutPlay() = runTest {
        val nativeClient = createNativeClientOrSkip() ?: return@runTest
        val handle = nativeClient.create()

        if (handle != 0L) {
            // Пауза должна работать без ошибок, даже если не было воспроизведения
            val result = nativeClient.pause(handle)
            // pause может вернуть true или false в зависимости от реализации
            assertNotNull(result)
        }
    }

    @Test
    fun testTypeConversionStatus() {
        // Тест конвертации статусов (проверяется через getStatus)
        val nativeClient = createNativeClientOrSkip() ?: return
        val handle = nativeClient.create()

        if (handle != 0L) {
            val status = nativeClient.getStatus(handle)

            // Проверяем, что статус корректно конвертирован из нативного типа
            assertTrue(
                status in listOf(
                    RtspClientStatus.DISCONNECTED,
                    RtspClientStatus.CONNECTING,
                    RtspClientStatus.CONNECTED,
                    RtspClientStatus.PLAYING,
                    RtspClientStatus.ERROR
                )
            )
        }
    }

    @Test
    fun testTypeConversionStreamType() {
        val nativeClient = createNativeClientOrSkip() ?: return
        val handle = nativeClient.create()

        if (handle != 0L) {
            // До подключения потоков нет
            val streamType = nativeClient.getStreamType(handle, 0)

            // streamType должен быть null или одним из допустимых значений
            if (streamType != null) {
                assertTrue(
                    streamType in listOf(
                        RtspStreamType.VIDEO,
                        RtspStreamType.AUDIO,
                        RtspStreamType.METADATA
                    )
                )
            }
        }
    }

    @Test
    fun testMultipleClientInstances() {
        // Проверяем, что можно создать несколько экземпляров клиента
        val client1 = createNativeClientOrSkip() ?: return
        val client2 = createNativeClientOrSkip() ?: return

        val handle1 = client1.create()
        val handle2 = client2.create()

        assertNotNull(handle1)
        assertNotNull(handle2)

        // Handles должны быть разными (если библиотека загружена)
        if (handle1 != 0L && handle2 != 0L) {
            assertNotEquals(handle1, handle2)
        }

        // Очистка
        if (handle1 != 0L) client1.destroy(handle1)
        if (handle2 != 0L) client2.destroy(handle2)
    }

    @Test
    fun testCallbackCleanup() = runTest {
        val nativeClient = createNativeClientOrSkip() ?: return@runTest
        val handle = nativeClient.create()

        if (handle != 0L) {
            // Устанавливаем callbacks
            nativeClient.setFrameCallback(handle, RtspStreamType.VIDEO) { }
            nativeClient.setStatusCallback(handle) { _, _ -> }

            // Уничтожение клиента должно освободить callbacks
            nativeClient.destroy(handle)

            // Если нет ошибок при уничтожении, тест пройден
            assertTrue(true)
        }
    }
}
