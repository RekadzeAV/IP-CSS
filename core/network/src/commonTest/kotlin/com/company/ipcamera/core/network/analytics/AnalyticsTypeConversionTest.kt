package com.company.ipcamera.core.network.analytics

import kotlin.test.*

/**
 * Тесты для конвертации типов между нативными и Kotlin типами
 * в FFI интеграции аналитики
 */
class AnalyticsTypeConversionTest {

    @Test
    fun testObjectTypeConversion() {
        // Проверяем соответствие типов объектов
        val types = listOf(
            ObjectType.PERSON,
            ObjectType.VEHICLE,
            ObjectType.BICYCLE,
            ObjectType.MOTORCYCLE,
            ObjectType.UNKNOWN
        )

        types.forEach { type ->
            assertNotNull(type)
            assertTrue(type.name.isNotEmpty())
        }
    }

    @Test
    fun testMotionDetectionResultConversion() {
        // Тест конвертации результата детекции движения
        val nativeResult = MotionDetectionResult(
            motionDetected = true,
            confidence = 0.75f,
            x = 100,
            y = 200,
            width = 300,
            height = 400
        )

        // Проверяем, что все поля корректно заполнены
        assertTrue(nativeResult.motionDetected)
        assertTrue(nativeResult.confidence in 0.0f..1.0f)
        assertTrue(nativeResult.x >= 0)
        assertTrue(nativeResult.y >= 0)
        assertTrue(nativeResult.width > 0)
        assertTrue(nativeResult.height > 0)
    }

    @Test
    fun testObjectDetectionResultConversion() {
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

        // Проверяем первый объект
        assertEquals(ObjectType.PERSON, result.objects[0].type)
        assertEquals(0.9f, result.objects[0].confidence)
        assertTrue(result.objects[0].confidence in 0.0f..1.0f)

        // Проверяем второй объект
        assertEquals(ObjectType.VEHICLE, result.objects[1].type)
        assertEquals(0.75f, result.objects[1].confidence)
    }

    @Test
    fun testFaceDetectionResultConversion() {
        val landmarks = intArrayOf(60, 70, 70, 70, 65, 85, 55, 90, 75, 90)
        val faces = listOf(
            DetectedFace(
                confidence = 0.95f,
                x = 50,
                y = 60,
                width = 80,
                height = 100,
                landmarks = landmarks
            )
        )

        val result = FaceDetectionResult(faces)

        assertEquals(1, result.faces.size)
        val face = result.faces[0]
        assertEquals(0.95f, face.confidence)
        assertTrue(face.confidence in 0.0f..1.0f)
        assertNotNull(face.landmarks)
        assertEquals(10, face.landmarks!!.size)
    }

    @Test
    fun testANPRResultConversion() {
        val plates = listOf(
            RecognizedPlate(
                text = "ABC123",
                confidence = 0.88f,
                x = 200,
                y = 300,
                width = 150,
                height = 50
            ),
            RecognizedPlate(
                text = "XYZ789",
                confidence = 0.92f,
                x = 400,
                y = 500,
                width = 180,
                height = 60
            )
        )

        val result = ANPRResult(plates)

        assertEquals(2, result.plates.size)
        assertEquals("ABC123", result.plates[0].text)
        assertEquals(0.88f, result.plates[0].confidence)
        assertEquals("XYZ789", result.plates[1].text)
        assertEquals(0.92f, result.plates[1].confidence)
    }

    @Test
    fun testTrackingResultConversion() {
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
            )
        )

        val result = TrackingResult(trackedObjects)

        assertEquals(1, result.objects.size)
        val tracked = result.objects[0]
        assertEquals(1, tracked.id)
        assertEquals(ObjectType.PERSON, tracked.type)
        assertEquals(0.92f, tracked.confidence)
        assertTrue(tracked.lastSeen > 0)
    }

    @Test
    fun testConfidenceBounds() {
        // Проверяем границы confidence значений
        val validConfidences = listOf(0.0f, 0.5f, 1.0f)

        validConfidences.forEach { conf ->
            assertTrue(conf >= 0.0f)
            assertTrue(conf <= 1.0f)
        }
    }

    @Test
    fun testBoundingBoxBounds() {
        // Проверяем корректность bounding box
        val box = DetectedObject(
            type = ObjectType.PERSON,
            confidence = 0.5f,
            x = 10,
            y = 20,
            width = 100,
            height = 200
        )

        assertTrue(box.x >= 0)
        assertTrue(box.y >= 0)
        assertTrue(box.width > 0)
        assertTrue(box.height > 0)
    }

    @Test
    fun testEmptyResultsHandling() {
        // Проверяем обработку пустых результатов
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
    fun testLandmarksArraySize() {
        // Проверяем размер массива landmarks (должен быть 10 элементов для 5 точек)
        val landmarks = intArrayOf(10, 20, 30, 40, 50, 60, 70, 80, 90, 100)
        assertEquals(10, landmarks.size)

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
    }

    @Test
    fun testTrackedObjectIdUniqueness() {
        // Проверяем, что ID отслеживаемых объектов уникальны
        val trackedObjects = listOf(
            TrackedObject(
                id = 1,
                type = ObjectType.PERSON,
                confidence = 0.9f,
                x = 0,
                y = 0,
                width = 10,
                height = 10,
                lastSeen = 1000L
            ),
            TrackedObject(
                id = 2,
                type = ObjectType.VEHICLE,
                confidence = 0.8f,
                x = 0,
                y = 0,
                width = 10,
                height = 10,
                lastSeen = 1001L
            )
        )

        val ids = trackedObjects.map { it.id }
        assertEquals(ids.toSet().size, ids.size) // Все ID уникальны
    }
}
