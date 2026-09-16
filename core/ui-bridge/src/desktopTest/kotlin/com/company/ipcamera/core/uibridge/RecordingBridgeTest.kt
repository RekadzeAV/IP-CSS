package com.company.ipcamera.core.uibridge

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecordingBridgeTest {

    private lateinit var bridge: RecordingBridge

    @Test
    fun testStartRecordingSuccess() = runTest {
        bridge = DesktopRecordingBridge(null, null, null, null, null, null)

        val result = bridge.startRecording("camera1")

        assertTrue(result.isSuccess)
        assertEquals("camera1", result.getOrNull()?.cameraId)
    }

    @Test
    fun testStopRecordingSuccess() = runTest {
        bridge = DesktopRecordingBridge(null, null, null, null, null, null)

        val result = bridge.stopRecording("recording1")

        assertTrue(result.isSuccess)
    }

    @Test
    fun testPauseRecordingSuccess() = runTest {
        bridge = DesktopRecordingBridge(null, null, null, null, null, null)

        val result = bridge.pauseRecording("recording1")

        assertTrue(result.isSuccess)
    }

    @Test
    fun testResumeRecordingSuccess() = runTest {
        bridge = DesktopRecordingBridge(null, null, null, null, null, null)

        val result = bridge.resumeRecording("recording1")

        assertTrue(result.isSuccess)
    }

    @Test
    fun testGetRecordingsReturnsEmptyList() = runTest {
        bridge = DesktopRecordingBridge(null, null, null, null, null, null)

        val recordings = bridge.getRecordings("camera1")

        assertTrue(recordings.isEmpty())
    }

    @Test
    fun testDeleteRecordingSuccess() = runTest {
        bridge = DesktopRecordingBridge(null, null, null, null, null, null)

        val result = bridge.deleteRecording("recording1")

        assertTrue(result.isSuccess)
    }
}
