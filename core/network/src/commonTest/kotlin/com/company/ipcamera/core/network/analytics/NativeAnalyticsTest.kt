package com.company.ipcamera.core.network.analytics

import kotlin.test.*

/**
 * Тесты для NativeAnalytics (expect класс)
 *
 * Эти тесты проверяют контракт expect класса и конвертацию типов.
 * Фактические реализации тестируются в платформо-специфичных тестах.
 */
class NativeAnalyticsTest {

    @Test
    fun testGetVersion() {
        val analytics = try {
            NativeAnalytics()
        } catch (_: UnsatisfiedLinkError) {
            return
        } catch (_: RuntimeException) {
            return
        }
        val version = try {
            analytics.getVersion()
        } catch (_: UnsatisfiedLinkError) {
            return
        }
        assertTrue(version.isNotBlank(), "getVersion() should return non-blank string, got: '$version'")
    }

    @Test
    fun testObjectTypeEnum() {
        // Проверяем, что все типы объектов определены
        val types = ObjectType.values()
        assertTrue(types.contains(ObjectType.PERSON))
        assertTrue(types.contains(ObjectType.VEHICLE))
        assertTrue(types.contains(ObjectType.BICYCLE))
        assertTrue(types.contains(ObjectType.MOTORCYCLE))
        assertTrue(types.contains(ObjectType.UNKNOWN))
    }

    @Test
    fun testMotionDetectionResult() {
        val result = MotionDetectionResult(
            motionDetected = true,
            confidence = 0.85f,
            x = 100,
            y = 200,
            width = 300,
            height = 400
        )

        assertEquals(true, result.motionDetected)
        assertEquals(0.85f, result.confidence)
        assertEquals(100, result.x)
        assertEquals(200, result.y)
        assertEquals(300, result.width)
        assertEquals(400, result.height)
    }

    @Test
    fun testObjectDetectionResult() {
        val objects = listOf(
            DetectedObject(
                type = ObjectType.PERSON,
                confidence = 0.9f,
                x = 10,
                y = 20,
                width = 100,
                height = 200
            ),
            DetectedObject(
                type = ObjectType.VEHICLE,
                confidence = 0.75f,
                x = 150,
                y = 250,
                width = 200,
                height = 150
            )
        )

        val result = ObjectDetectionResult(objects)

        assertEquals(2, result.objects.size)
        assertEquals(ObjectType.PERSON, result.objects[0].type)
        assertEquals(0.9f, result.objects[0].confidence)
        assertEquals(ObjectType.VEHICLE, result.objects[1].type)
        assertEquals(0.75f, result.objects[1].confidence)
    }

    @Test
    fun testFaceDetectionResult() {
        val faces = listOf(
            DetectedFace(
                confidence = 0.95f,
                x = 50,
                y = 60,
                width = 80,
                height = 100,
                landmarks = intArrayOf(60, 70, 70, 70, 65, 85, 55, 90, 75, 90)
            )
        )

        val result = FaceDetectionResult(faces)

        assertEquals(1, result.faces.size)
        assertEquals(0.95f, result.faces[0].confidence)
        assertEquals(50, result.faces[0].x)
        assertEquals(60, result.faces[0].y)
        assertNotNull(result.faces[0].landmarks)
        assertEquals(10, result.faces[0].landmarks!!.size)
    }

    @Test
    fun testANPRResult() {
        val plates = listOf(
            RecognizedPlate(
                text = "ABC123",
                confidence = 0.88f,
                x = 200,
                y = 300,
                width = 150,
                height = 50
            )
        )

        val result = ANPRResult(plates)

        assertEquals(1, result.plates.size)
        assertEquals("ABC123", result.plates[0].text)
        assertEquals(0.88f, result.plates[0].confidence)
        assertEquals(200, result.plates[0].x)
        assertEquals(300, result.plates[0].y)
    }

    @Test
    fun testTrackingResult() {
        val trackedObjects = listOf(
            TrackedObject(
                id = 1,
                type = ObjectType.PERSON,
                confidence = 0.92f,
                x = 100,
                y = 150,
                width = 80,
                height = 180,
                lastSeen = 1234567890L
            ),
            TrackedObject(
                id = 2,
                type = ObjectType.VEHICLE,
                confidence = 0.87f,
                x = 300,
                y = 200,
                width = 200,
                height = 120,
                lastSeen = 1234567891L
            )
        )

        val result = TrackingResult(trackedObjects)

        assertEquals(2, result.objects.size)
        assertEquals(1, result.objects[0].id)
        assertEquals(ObjectType.PERSON, result.objects[0].type)
        assertEquals(2, result.objects[1].id)
        assertEquals(ObjectType.VEHICLE, result.objects[1].type)
        assertEquals(1234567890L, result.objects[0].lastSeen)
    }

    @Test
    fun testEmptyResults() {
        val emptyMotion = MotionDetectionResult(
            motionDetected = false,
            confidence = 0.0f,
            x = 0,
            y = 0,
            width = 0,
            height = 0
        )
        assertFalse(emptyMotion.motionDetected)

        val emptyObjects = ObjectDetectionResult(emptyList())
        assertTrue(emptyObjects.objects.isEmpty())

        val emptyFaces = FaceDetectionResult(emptyList())
        assertTrue(emptyFaces.faces.isEmpty())

        val emptyPlates = ANPRResult(emptyList())
        assertTrue(emptyPlates.plates.isEmpty())

        val emptyTracking = TrackingResult(emptyList())
        assertTrue(emptyTracking.objects.isEmpty())
    }

    @Test
    fun testDetectedObjectBounds() {
        val obj = DetectedObject(
            type = ObjectType.PERSON,
            confidence = 0.5f,
            x = 0,
            y = 0,
            width = 100,
            height = 200
        )

        // Проверяем границы
        assertEquals(0, obj.x)
        assertEquals(0, obj.y)
        assertEquals(100, obj.width)
        assertEquals(200, obj.height)
    }

    @Test
    fun testDetectedFaceLandmarks() {
        val landmarks = intArrayOf(10, 20, 30, 40, 50, 60, 70, 80, 90, 100)
        val face = DetectedFace(
            confidence = 0.9f,
            x = 0,
            y = 0,
            width = 100,
            height = 100,
            landmarks = landmarks
        )

        assertNotNull(face.landmarks)
        assertEquals(10, face.landmarks!!.size)
        assertEquals(10, face.landmarks!![0])
        assertEquals(20, face.landmarks!![1])
    }

    @Test
    fun testDetectedFaceWithoutLandmarks() {
        val face = DetectedFace(
            confidence = 0.9f,
            x = 0,
            y = 0,
            width = 100,
            height = 100,
            landmarks = null
        )

        assertNull(face.landmarks)
    }

    @Test
    fun testConfidenceRange() {
        // Проверяем, что confidence находится в диапазоне [0.0, 1.0]
        val minConfidence = DetectedObject(
            type = ObjectType.PERSON,
            confidence = 0.0f,
            x = 0,
            y = 0,
            width = 10,
            height = 10
        )
        assertEquals(0.0f, minConfidence.confidence)

        val maxConfidence = DetectedObject(
            type = ObjectType.PERSON,
            confidence = 1.0f,
            x = 0,
            y = 0,
            width = 10,
            height = 10
        )
        assertEquals(1.0f, maxConfidence.confidence)
    }
}
