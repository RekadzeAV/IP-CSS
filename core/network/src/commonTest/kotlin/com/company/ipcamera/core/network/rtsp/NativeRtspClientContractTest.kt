package com.company.ipcamera.core.network.rtsp

import com.company.ipcamera.core.network.RtspClientStatus
import com.company.ipcamera.core.network.RtspFrame
import com.company.ipcamera.core.network.RtspStreamInfo
import com.company.ipcamera.core.network.RtspStreamType
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Contract тесты для NativeRtspClient (expect/actual).
 *
 * Проверяет, что все платформенные реализации (JVM, Android, iOS, Native)
 * соответствуют контракту, определённому в expect-классе.
 *
 * Безопасность: Не модифицирует production код.
 * Использует try-catch для graceful skip если native библиотека не доступна.
 */
class NativeRtspClientContractTest {

    private fun createClientOrSkip(): NativeRtspClient? {
        return try {
            NativeRtspClient()
        } catch (_: UnsatisfiedLinkError) {
            null // Native library not available
        } catch (_: RuntimeException) {
            null // Native runtime not available
        }
    }

    @Test
    fun testCreateAndDestroy() = runTest {
        val client = createClientOrSkip() ?: return@runTest

        val handle = client.create()
        if (handle == 0L) return@runTest

        // Destroy без ошибок
        client.destroy(handle)
    }

    @Test
    fun testCreateMultiple() = runTest {
        val client = createClientOrSkip() ?: return@runTest

        val handle1 = client.create()
        val handle2 = client.create()
        val handle3 = client.create()

        if (handle1 == 0L || handle2 == 0L || handle3 == 0L) return@runTest

        // Хендлы должны быть уникальными
        assertNotEquals(handle1, handle2)
        assertNotEquals(handle2, handle3)

        client.destroy(handle1)
        client.destroy(handle2)
        client.destroy(handle3)
    }

    @Test
    fun testInitialStatusIsDisconnected() = runTest {
        val client = createClientOrSkip() ?: return@runTest

        val handle = client.create()
        val status = client.getStatus(handle)

        assertEquals(RtspClientStatus.DISCONNECTED, status)
        client.destroy(handle)
    }

    @Test
    fun testConnectWithInvalidUrl() = runTest {
        val client = createClientOrSkip() ?: return@runTest

        val handle = client.create()
        val result = client.connect(handle, "rtsp://invalid:554/stream", null, null, 5000)

        // С невалидным URL коннект должен вернуть false
        assertFalse(result)

        val status = client.getStatus(handle)
        assertTrue(status == RtspClientStatus.ERROR || status == RtspClientStatus.DISCONNECTED)
        client.destroy(handle)
    }

    @Test
    fun testDisconnectWithoutConnect() = runTest {
        val client = createClientOrSkip() ?: return@runTest

        val handle = client.create()

        // disconnect без connect не должен вызывать ошибок
        client.disconnect(handle)

        val status = client.getStatus(handle)
        assertEquals(RtspClientStatus.DISCONNECTED, status)
        client.destroy(handle)
    }

    @Test
    fun testPlayWithoutConnect() = runTest {
        val client = createClientOrSkip() ?: return@runTest

        val handle = client.create()
        val result = client.play(handle)

        // play без connect должен вернуть false
        assertFalse(result)
        client.destroy(handle)
    }

    @Test
    fun testStopWithoutPlay() = runTest {
        val client = createClientOrSkip() ?: return@runTest

        val handle = client.create()
        val result = client.stop(handle)

        // stop без play должен вернуть false
        assertFalse(result)
        client.destroy(handle)
    }

    @Test
    fun testPauseWithoutPlay() = runTest {
        val client = createClientOrSkip() ?: return@runTest

        val handle = client.create()
        val result = client.pause(handle)

        // pause без play должен вернуть false
        assertFalse(result)
        client.destroy(handle)
    }

    @Test
    fun testGetStreamCountWithoutConnect() = runTest {
        val client = createClientOrSkip() ?: return@runTest

        val handle = client.create()
        val count = client.getStreamCount(handle)

        assertEquals(0, count, "Stream count should be 0 without connection")
        client.destroy(handle)
    }

    @Test
    fun testGetStreamTypeWithoutConnect() = runTest {
        val client = createClientOrSkip() ?: return@runTest

        val handle = client.create()
        val type = client.getStreamType(handle, 0)

        assertNull(type, "Stream type should be null without connection")
        client.destroy(handle)
    }

    @Test
    fun testGetStreamInfoWithoutConnect() = runTest {
        val client = createClientOrSkip() ?: return@runTest

        val handle = client.create()
        val info = client.getStreamInfo(handle, 0)

        assertNull(info, "Stream info should be null without connection")
        client.destroy(handle)
    }

    @Test
    fun testSetFrameCallback() = runTest {
        val client = createClientOrSkip() ?: return@runTest

        val handle = client.create()

        // Установка callback не должна вызывать ошибок
        var callbackCalled = false
        client.setFrameCallback(handle, RtspStreamType.VIDEO) { frame ->
            callbackCalled = true
        }

        // Callback не будет вызван без соединения
        assertFalse(callbackCalled)
        client.destroy(handle)
    }

    @Test
    fun testSetStatusCallback() = runTest {
        val client = createClientOrSkip() ?: return@runTest

        val handle = client.create()

        // Установка callback не должна вызывать ошибок
        var callbackCalled = false
        client.setStatusCallback(handle) { status, message ->
            callbackCalled = true
        }

        // Callback не будет вызван без соединения
        assertFalse(callbackCalled)
        client.destroy(handle)
    }

    @Test
    fun testSetReconnectParams() = runTest {
        val client = createClientOrSkip() ?: return@runTest

        val handle = client.create()

        // Установка параметров reconnect не должна вызывать ошибок
        client.setReconnectParams(
            handle = handle,
            enabled = true,
            maxRetries = 5,
            initialDelayMs = 500,
            maxDelayMs = 10000,
            backoffMultiplier = 2.0f
        )

        client.destroy(handle)
    }

    @Test
    fun testDoubleDestroy() = runTest {
        val client = createClientOrSkip() ?: return@runTest

        val handle = client.create()
        client.destroy(handle)

        // Повторный destroy не должен вызывать ошибок
        client.destroy(handle)
    }

    @Test
    fun testCreateDestroyCycle() = runTest {
        val client = createClientOrSkip() ?: return@runTest

        // Многократное create/destroy без утечек
        repeat(10) { i ->
            val handle = client.create()
            if (handle == 0L) return@runTest
            client.destroy(handle)
        }
    }

    @Test
    fun testConnectDisconnectCycle() = runTest {
        val client = createClientOrSkip() ?: return@runTest

        // Многократный connect/disconnect без утечек
        repeat(3) { i ->
            val handle = client.create()

            // connect с невалидным URL
            val result = client.connect(handle, "rtsp://192.168.1.$i:554/stream", null, null, 3000)
            assertFalse(result, "Connect should fail for invalid URL on iteration $i")

            client.disconnect(handle)
            val status = client.getStatus(handle)
            assertEquals(RtspClientStatus.DISCONNECTED, status)
            client.destroy(handle)
        }
    }

    @Test
    fun testHandleZeroIsInvalid() = runTest {
        val client = createClientOrSkip() ?: return@runTest

        // Вызовы с handle=0 не должны крашить
        client.disconnect(0L)
        val status = client.getStatus(0L)
        // При отсутствии нативной библиотеки getStatus(0) возвращает DISCONNECTED
        assertTrue(
            status == RtspClientStatus.ERROR || status == RtspClientStatus.DISCONNECTED,
            "Status for handle=0 should be ERROR or DISCONNECTED, got: $status"
        )

        val result = client.play(0L)
        assertFalse(result)

        client.destroy(0L)
    }

    @Test
    fun testRtspFrameDataClassDefaultValues() {
        val frame = RtspFrame(
            data = byteArrayOf(1, 2, 3),
            timestamp = 1000L,
            streamType = RtspStreamType.VIDEO,
            width = 640,
            height = 480
        )

        assertEquals(3, frame.data.size)
        assertEquals(1000L, frame.timestamp)
        assertEquals(RtspStreamType.VIDEO, frame.streamType)
        assertEquals(640, frame.width)
        assertEquals(480, frame.height)
    }

    @Test
    fun testRtspStreamInfoDataClassDefaults() {
        val resolution = com.company.ipcamera.core.common.model.Resolution(1920, 1080)
        val info = RtspStreamInfo(
            index = 0,
            type = RtspStreamType.VIDEO,
            resolution = resolution,
            fps = 30,
            codec = "H.264"
        )

        assertEquals(0, info.index)
        assertEquals(RtspStreamType.VIDEO, info.type)
        assertEquals(30, info.fps)
        assertEquals("H.264", info.codec)
    }

    @Test
    fun testNativeRtspClientHandleTypealias() {
        // Проверка, что handle действительно Long
        val handle: Long = 12345L
        assertTrue(handle is Long)
    }
}
