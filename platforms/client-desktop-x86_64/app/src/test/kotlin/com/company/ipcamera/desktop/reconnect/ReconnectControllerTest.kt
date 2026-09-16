package com.company.ipcamera.desktop.reconnect

import kotlinx.coroutines.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Unit тесты для ReconnectController
 */
class ReconnectControllerTest {

    @Test
    fun `should create initial state for camera`() {
        runBlocking {
            val controller = ReconnectController(
                reconnectCallback = { true },
                coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            )
            
            controller.startReconnectMonitoring("camera1", ReconnectPolicy.BALANCED)
            
            val state = controller.getReconnectState("camera1")
            assertTrue(state != null)
            
            val currentState = state.value
            assertEquals("camera1", currentState.cameraId)
            assertFalse(currentState.isConnected)
            assertEquals(0, currentState.currentAttempt)
            
            controller.disposeAll()
        }
    }

    @Test
    fun `should not reconnect when policy is disabled`() = runBlocking {
        var reconnectCalled = false
        val controller = ReconnectController(
            reconnectCallback = {
                reconnectCalled = true
                true
            },
            coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        )
        
        controller.startReconnectMonitoring("camera1", ReconnectPolicy.NONE)
        
        controller.onStreamError("camera1", "Connection timeout")
        
        delay(200)
        
        assertFalse(reconnectCalled, "Reconnect should not be called when disabled")
        
        controller.disposeAll()
    }

    @Test
    fun `should not reconnect for non-retryable errors`() = runBlocking {
        var reconnectCalled = false
        val controller = ReconnectController(
            reconnectCallback = {
                reconnectCalled = true
                true
            },
            coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        )
        
        controller.startReconnectMonitoring("camera1", ReconnectPolicy(
            enabled = true,
            maxAttempts = 5,
            initialDelayMs = 100,
            retryOnTimeout = false,
            retryOnConnectionRefused = false,
            retryOnServerUnavailable = false,
            jitterRatio = 0.0
        ))
        
        controller.onStreamError("camera1", "Connection timed out")
        
        delay(200)
        
        assertFalse(reconnectCalled, "Reconnect should not be called for non-retryable errors")
        
        controller.disposeAll()
    }

    @Test
    fun `should stop reconnect when disposed`() = runBlocking {
        var reconnectCalled = false
        val controller = ReconnectController(
            reconnectCallback = {
                reconnectCalled = true
                true
            },
            coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        )
        
        controller.startReconnectMonitoring("camera1", ReconnectPolicy(
            enabled = true,
            maxAttempts = 5,
            initialDelayMs = 100,
            jitterRatio = 0.0
        ))
        
        controller.onStreamError("camera1", "Connection timeout")
        
        // Dispose before reconnect is attempted
        delay(50)
        controller.stopReconnect("camera1", "Test dispose")
        
        // Wait for potential reconnect
        delay(200)
        
        assertFalse(reconnectCalled, "Reconnect should not be called after dispose")
        
        controller.disposeAll()
    }

    @Test
    fun `should get reconnect stats for all cameras`() = runBlocking {
        val controller = ReconnectController(
            reconnectCallback = { true },
            coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        )
        
        controller.startReconnectMonitoring("camera1", ReconnectPolicy.BALANCED)
        controller.startReconnectMonitoring("camera2", ReconnectPolicy.AGGRESSIVE)
        
        val stats = controller.getReconnectStats()
        
        assertEquals(2, stats.size)
        assertTrue(stats.containsKey("camera1"))
        assertTrue(stats.containsKey("camera2"))
        
        controller.disposeAll()
    }
}