package com.company.ipcamera.core.uibridge

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventBridgeTest {

    private lateinit var bridge: EventBridge

    @Test
    fun testGetEventsReturnsEmptyList() = runTest {
        bridge = DesktopEventBridge(null, null, null, null, null)

        val events = bridge.getEvents("camera1")

        assertTrue(events.isEmpty())
    }

    @Test
    fun testAcknowledgeEventSuccess() = runTest {
        bridge = DesktopEventBridge(null, null, null, null, null)

        val result = bridge.acknowledgeEvent("event1")

        assertTrue(result.isSuccess)
    }

    @Test
    fun testDetectMotionReturnsResult() = runTest {
        bridge = DesktopEventBridge(null, null, null, null, null)

        val result = bridge.detectMotion("camera1")

        assertTrue(result.isSuccess)
        val detectionResult = result.getOrNull()
        assertEquals(false, detectionResult?.detected)
    }

    @Test
    fun testDetectFacesReturnsResult() = runTest {
        bridge = DesktopEventBridge(null, null, null, null, null)

        val result = bridge.detectFaces("camera1")

        assertTrue(result.isSuccess)
    }

    @Test
    fun testRecognizeLicensePlateReturnsResult() = runTest {
        bridge = DesktopEventBridge(null, null, null, null, null)

        val result = bridge.recognizeLicensePlate("camera1")

        assertTrue(result.isSuccess)
    }

    @Test
    fun testDetectionResultDataClass() {
        val result = DetectionResult(
            detected = true,
            objects = listOf(
                DetectedObject(
                    type = ObjectType.PERSON,
                    boundingBox = Rect(100f, 100f, 200f, 300f),
                    confidence = 0.95f
                )
            ),
            confidence = 0.95f
        )

        assertEquals(true, result.detected)
        assertEquals(1, result.objects.size)
        assertEquals(ObjectType.PERSON, result.objects[0].type)
        assertEquals(0.95f, result.objects[0].confidence)
    }

    @Test
    fun testObjectTypeValues() {
        assertEquals(4, ObjectType.values().size)
        assertTrue(ObjectType.values().contains(ObjectType.PERSON))
        assertTrue(ObjectType.values().contains(ObjectType.VEHICLE))
        assertTrue(ObjectType.values().contains(ObjectType.ANIMAL))
        assertTrue(ObjectType.values().contains(ObjectType.LICENSE_PLATE))
    }

    @Test
    fun testAnalysisTypeValues() {
        assertEquals(4, AnalysisType.values().size)
        assertTrue(AnalysisType.values().contains(AnalysisType.MOTION))
        assertTrue(AnalysisType.values().contains(AnalysisType.FACE))
        assertTrue(AnalysisType.values().contains(AnalysisType.OBJECT))
        assertTrue(AnalysisType.values().contains(AnalysisType.LICENSE_PLATE))
    }

    @Test
    fun testAnalysisResultDataClass() {
        val result = AnalysisResult(
            success = true,
            analysisData = "{\"objects\": 5}",
            processingTimeMs = 1500
        )

        assertEquals(true, result.success)
        assertEquals("{\"objects\": 5}", result.analysisData)
        assertEquals(1500, result.processingTimeMs)
    }
}
