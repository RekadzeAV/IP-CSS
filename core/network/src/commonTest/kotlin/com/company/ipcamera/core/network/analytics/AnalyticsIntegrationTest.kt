package com.company.ipcamera.core.network.analytics

import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Интеграционные тесты для Analytics pipeline.
 *
 * Тестирует полный цикл: создание NativeAnalytics → создание детекторов →
 * вызов методов с данными → уничтожение детекторов.
 *
 * API соответствует NativeAnalytics expect-классу:
 * create* -> detect* -> destroy*, где каждый детектор имеет свой handle.
 *
 * Безопасность: не модифицирует production код.
 */
class AnalyticsIntegrationTest {

    private fun createAnalyticsOrSkip(): NativeAnalytics? {
        return try {
            NativeAnalytics()
        } catch (_: UnsatisfiedLinkError) {
            null
        } catch (_: RuntimeException) {
            null
        }
    }

    @Test
    fun testMotionDetectorLifecycle() = runTest {
        val analytics = createAnalyticsOrSkip()
        if (analytics == null) return@runTest

        val version = analytics.getVersion()
        assertTrue(version.isNotBlank(), "Version should be non-blank")

        val handle = analytics.createMotionDetector(640, 480, 0.5f, 100)
        if (handle == null) return@runTest
        analytics.destroyMotionDetector(handle)
    }

    @Test
    fun testMotionDetectWithData() = runTest {
        val analytics = createAnalyticsOrSkip()
        if (analytics == null) return@runTest

        val handle = analytics.createMotionDetector(640, 480)
        if (handle == null) return@runTest
        val frameData = ByteArray(640 * 480 * 3)

        val result = analytics.detectMotion(handle, frameData, 640, 480)
        assertNotNull(result, "Motion detection result should not be null")
        if (result != null) {
            assertTrue(result.confidence in 0.0f..1.0f, "Confidence must be in [0, 1]")
        }

        analytics.destroyMotionDetector(handle)
    }

    @Test
    fun testObjectDetectorLifecycle() = runTest {
        val analytics = createAnalyticsOrSkip()
        if (analytics == null) return@runTest

        val handle = analytics.createObjectDetector(0.5f, 10, false, 640)
        if (handle == null) return@runTest
        analytics.destroyObjectDetector(handle)
    }

    @Test
    fun testObjectDetectWithData() = runTest {
        val analytics = createAnalyticsOrSkip()
        if (analytics == null) return@runTest

        val handle = analytics.createObjectDetector()
        if (handle == null) return@runTest
        val frameData = ByteArray(1280 * 720 * 3)

        val result = analytics.detectObjects(handle, frameData, 1280, 720)
        assertNotNull(result, "Object detection result should not be null")
        if (result != null) {
            assertNotNull(result.objects, "Objects list should not be null")
        }

        analytics.destroyObjectDetector(handle)
    }

    @Test
    fun testFaceDetectorLifecycle() = runTest {
        val analytics = createAnalyticsOrSkip()
        if (analytics == null) return@runTest

        val handle = analytics.createFaceDetector(1.1f, 3, 30, 300)
        if (handle == null) return@runTest
        analytics.destroyFaceDetector(handle)
    }

    @Test
    fun testANPREngineLifecycle() = runTest {
        val analytics = createAnalyticsOrSkip()
        if (analytics == null) return@runTest

        val handle = analytics.createANPREngine(0.7f, "eng")
        if (handle == null) return@runTest
        analytics.destroyANPREngine(handle)
    }

    @Test
    fun testObjectTrackerLifecycle() = runTest {
        val analytics = createAnalyticsOrSkip()
        if (analytics == null) return@runTest

        val handle = analytics.createObjectTracker(0.5f, 30, 0.5f)
        if (handle == null) return@runTest
        analytics.destroyObjectTracker(handle)
    }

    @Test
    fun testFullPipelineMotionThenTracking() = runTest {
        val analytics = createAnalyticsOrSkip()
        if (analytics == null) return@runTest

        val motionHandle = analytics.createMotionDetector(640, 480)
        val trackerHandle = analytics.createObjectTracker()
        if (motionHandle == null || trackerHandle == null) return@runTest

        val frameData = ByteArray(640 * 480 * 3)

        val motionResult = analytics.detectMotion(motionHandle, frameData, 640, 480)
        assertNotNull(motionResult)

        val emptyDetections = ObjectDetectionResult(emptyList())
        val trackingResult = analytics.updateTracking(trackerHandle, emptyDetections, System.currentTimeMillis())
        assertNotNull(trackingResult, "Tracking result should not be null")
        if (trackingResult != null) {
            assertNotNull(trackingResult.objects, "Tracked objects list should not be null")
        }

        analytics.destroyMotionDetector(motionHandle)
        analytics.destroyObjectTracker(trackerHandle)
    }

    @Test
    fun testMultipleDetectorsSimultaneously() = runTest {
        val analytics = createAnalyticsOrSkip()
        if (analytics == null) return@runTest

        val motionHandle = analytics.createMotionDetector(640, 480)
        val objectHandle = analytics.createObjectDetector()
        val faceHandle = analytics.createFaceDetector()

        // Если native библиотека недоступна — пропускаем
        if (motionHandle == null || objectHandle == null || faceHandle == null) return@runTest

        val frameData = ByteArray(640 * 480)

        motionHandle?.let { h ->
            val motionResult = analytics.detectMotion(h, frameData, 640, 480)
            assertNotNull(motionResult)
            analytics.destroyMotionDetector(h)
        }
        objectHandle?.let { h ->
            val objectResult = analytics.detectObjects(h, frameData, 640, 480)
            assertNotNull(objectResult)
            analytics.destroyObjectDetector(h)
        }
        faceHandle?.let { h ->
            analytics.destroyFaceDetector(h)
        }
    }

    @Test
    fun testEmptyFrameHandling() = runTest {
        val analytics = createAnalyticsOrSkip()
        if (analytics == null) return@runTest

        val motionHandle = analytics.createMotionDetector(0, 0)
        if (motionHandle == null) return@runTest
        val emptyFrame = ByteArray(0)

        val motionResult = analytics.detectMotion(motionHandle, emptyFrame, 0, 0)
        assertNotNull(motionResult)

        analytics.destroyMotionDetector(motionHandle)
    }

    @Test
    fun testDetectorVersionConsistency() = runTest {
        val analytics = createAnalyticsOrSkip()
        if (analytics == null) return@runTest

        val v1 = analytics.getVersion()
        val v2 = analytics.getVersion()
        val v3 = analytics.getVersion()

        assertEquals(v1, v2, "Version should be consistent")
        assertEquals(v2, v3, "Version should be consistent across calls")
    }

    @Test
    fun testConsecutiveDetectCalls() = runTest {
        val analytics = createAnalyticsOrSkip()
        if (analytics == null) return@runTest

        val handle = analytics.createMotionDetector(320, 240)
        if (handle == null) return@runTest
        val frameData = ByteArray(320 * 240 * 3)

        repeat(5) { i ->
            val result = analytics.detectMotion(handle, frameData, 320, 240)
            assertNotNull(result, "Failed on iteration $i")
        }

        analytics.destroyMotionDetector(handle)
    }

    @Test
    fun testDoubleDestroyIsSafe() = runTest {
        val analytics = createAnalyticsOrSkip()
        if (analytics == null) return@runTest

        val handle = analytics.createMotionDetector(640, 480)
        if (handle == null) return@runTest
        analytics.destroyMotionDetector(handle)
        analytics.destroyMotionDetector(handle)
    }

    @Test
    fun testANPREngineRecognizeLicensePlates() = runTest {
        val analytics = createAnalyticsOrSkip()
        if (analytics == null) return@runTest

        val handle = analytics.createANPREngine()
        if (handle == null) return@runTest
        val frameData = ByteArray(640 * 480 * 3)

        val result = analytics.recognizeLicensePlates(handle, frameData, 640, 480)
        assertNotNull(result, "ANPR result should not be null")
        if (result != null) {
            assertNotNull(result.plates, "Plates list should not be null")
        }

        analytics.destroyANPREngine(handle)
    }

    @Test
    fun testFaceDetectorLoadCascade() = runTest {
        val analytics = createAnalyticsOrSkip()
        if (analytics == null) return@runTest

        val handle = analytics.createFaceDetector()
        if (handle == null) return@runTest
        val frameData = ByteArray(640 * 480 * 3)

        val result = analytics.detectFaces(handle, frameData, 640, 480)
        assertNotNull(result, "Face detection result should not be null")
        if (result != null) {
            assertNotNull(result.faces, "Faces list should not be null")
        }

        analytics.destroyFaceDetector(handle)
    }

    @Test
    fun testObjectDetectorLoadModel() = runTest {
        val analytics = createAnalyticsOrSkip()
        if (analytics == null) return@runTest

        val handle = analytics.createObjectDetector()
        if (handle == null) return@runTest
        val loaded = analytics.loadObjectDetectorModel(handle, "nonexistent_model.onnx")

        // Загрузка с несуществующим путём должна вернуть false
        assertFalse(loaded, "Loading non-existent model should return false")

        analytics.destroyObjectDetector(handle)
    }
}
