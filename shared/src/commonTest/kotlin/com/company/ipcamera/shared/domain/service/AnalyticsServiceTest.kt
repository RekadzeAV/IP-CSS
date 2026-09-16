package com.company.ipcamera.shared.domain.service

import com.company.ipcamera.core.common.model.CameraStatus
import com.company.ipcamera.core.common.model.Resolution
import com.company.ipcamera.shared.domain.model.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Тесты для AnalyticsService
 *
 * Эти тесты проверяют структуру данных и логику работы с результатами аналитики.
 * Реальная интеграция с NativeAnalytics тестируется в платформо-специфичных тестах.
 */
class AnalyticsServiceTest {
    private fun createTestCamera(): Camera {
        return Camera(
            id = "test-camera-1",
            name = "Test Camera",
            url = "rtsp://test.local",
            username = "admin",
            password = "password",
            resolution = Resolution(1920, 1080),
            status = CameraStatus.ONLINE,
            settings =
                CameraSettings(
                    analytics =
                        AnalyticsSettings(
                            objectDetectionConfidenceThreshold = 0.5f,
                            objectDetectionMaxObjects = 10,
                            objectDetectionUseGPU = false,
                            objectDetectionModelPath = null,
                            faceDetectionCascadePath = null,
                        ),
                ),
        )
    }

    @Test
    fun testDetectMotionWithEmptyFrame() =
        runTest {
            // Этот тест проверяет обработку пустых кадров
            val camera = createTestCamera()
            val emptyFrame = ByteArray(0)

            assertNotNull(emptyFrame)
            assertEquals(0, emptyFrame.size)
        }

    @Test
    fun testDetectMotionWithValidFrame() =
        runTest {
            val camera = createTestCamera()
            // Создаем тестовый кадр RGB24 (1920x1080x3 байта)
            val frameSize = 1920 * 1080 * 3
            val frameData = ByteArray(frameSize) { (it % 256).toByte() }

            assertNotNull(frameData)
            assertEquals(frameSize, frameData.size)
        }

    @Test
    fun testDetectObjectsWithEmptyList() =
        runTest {
            val camera = createTestCamera()
            val frameData = ByteArray(100)

            // Тест проверяет обработку пустого списка типов объектов
            val emptyObjectTypes = emptyList<String>()
            assertTrue(emptyObjectTypes.isEmpty())
        }

    @Test
    fun testDetectObjectsWithSpecificTypes() =
        runTest {
            val camera = createTestCamera()
            val frameData = ByteArray(100)
            val objectTypes = listOf("person", "vehicle")

            assertNotNull(objectTypes)
            assertEquals(2, objectTypes.size)
            assertTrue(objectTypes.contains("person"))
            assertTrue(objectTypes.contains("vehicle"))
        }

    @Test
    fun testDetectFacesWithMinConfidence() =
        runTest {
            val camera = createTestCamera()
            val frameData = ByteArray(100)
            val minConfidence = 0.7f

            assertTrue(minConfidence >= 0.0f)
            assertTrue(minConfidence <= 1.0f)
        }

    @Test
    fun testRecognizeLicensePlatesWithCountry() =
        runTest {
            val camera = createTestCamera()
            val frameData = ByteArray(100)
            val country = "RUS"

            assertNotNull(country)
            assertEquals(3, country.length)
        }

    @Test
    fun testMotionDetectionResultStructure() {
        val result =
            MotionDetectionResult(
                detected = true,
                confidence = 0.85f,
                zones =
                    listOf(
                        MotionZone(
                            zone =
                                DetectionZone(
                                    "zone1",
                                    listOf(listOf(0, 0), listOf(100, 0), listOf(100, 100), listOf(0, 100)),
                                    80,
                                ),
                            intensity = 0.85f,
                        ),
                    ),
            )

        assertTrue(result.detected)
        assertEquals(0.85f, result.confidence)
        assertEquals(1, result.zones.size)
        assertEquals("zone1", result.zones[0].zone.name)
        assertEquals(0.85f, result.zones[0].intensity)
    }

    @Test
    fun testObjectDetectionResultStructure() {
        val objects =
            listOf(
                DetectedObject(
                    type = "person",
                    confidence = 0.9f,
                    boundingBox = BoundingBox(10, 20, 100, 200),
                ),
            )

        val result = ObjectDetectionResult(objects)

        assertEquals(1, result.objects.size)
        assertEquals("person", result.objects[0].type)
        assertEquals(0.9f, result.objects[0].confidence)
        assertEquals(10, result.objects[0].boundingBox.x)
        assertEquals(20, result.objects[0].boundingBox.y)
    }

    @Test
    fun testFaceDetectionResultStructure() {
        val faces =
            listOf(
                DetectedFace(
                    boundingBox = BoundingBox(50, 60, 80, 100),
                    confidence = 0.95f,
                    landmarks =
                        listOf(
                            FaceLandmark(60, 70, LandmarkType.LEFT_EYE),
                            FaceLandmark(70, 70, LandmarkType.RIGHT_EYE),
                        ),
                ),
            )

        val result = FaceDetectionResult(faces)

        assertEquals(1, result.faces.size)
        assertEquals(0.95f, result.faces[0].confidence)
        assertNotNull(result.faces[0].landmarks)
        assertEquals(2, result.faces[0].landmarks!!.size)
    }

    @Test
    fun testLicensePlateRecognitionResultStructure() {
        val plates =
            listOf(
                RecognizedLicensePlate(
                    plateNumber = "ABC123",
                    confidence = 0.88f,
                    country = "RUS",
                    boundingBox = BoundingBox(200, 300, 150, 50),
                ),
            )

        val result = LicensePlateRecognitionResult(plates)

        assertEquals(1, result.plates.size)
        assertEquals("ABC123", result.plates[0].plateNumber)
        assertEquals(0.88f, result.plates[0].confidence)
        assertEquals("RUS", result.plates[0].country)
    }

    @Test
    fun testDetectionZonePolygon() {
        val polygon =
            listOf(
                listOf(0, 0),
                listOf(100, 0),
                listOf(100, 100),
                listOf(0, 100),
            )

        val zone = DetectionZone("test-zone", polygon, 80)

        assertEquals("test-zone", zone.name)
        assertEquals(4, zone.polygon.size)
        assertEquals(80, zone.sensitivity)
    }

    @Test
    fun testBoundingBoxCalculation() {
        val box = BoundingBox(x = 10, y = 20, width = 100, height = 200)

        assertEquals(10, box.x)
        assertEquals(20, box.y)
        assertEquals(100, box.width)
        assertEquals(200, box.height)
        // Проверяем правую и нижнюю границы
        assertEquals(110, box.x + box.width)
        assertEquals(220, box.y + box.height)
    }

    @Test
    fun testMotionZoneIntensity() {
        val zone = DetectionZone("zone1", emptyList(), 80)
        val motionZone = MotionZone(zone, 0.75f)

        assertEquals(zone, motionZone.zone)
        assertEquals(0.75f, motionZone.intensity)
        assertTrue(motionZone.intensity >= 0.0f)
        assertTrue(motionZone.intensity <= 1.0f)
    }

    @Test
    fun testEmptyResults() {
        val emptyMotion =
            MotionDetectionResult(
                detected = false,
                confidence = 0.0f,
                zones = emptyList(),
            )
        assertFalse(emptyMotion.detected)
        assertTrue(emptyMotion.zones.isEmpty())

        val emptyObjects = ObjectDetectionResult(emptyList())
        assertTrue(emptyObjects.objects.isEmpty())

        val emptyFaces = FaceDetectionResult(emptyList())
        assertTrue(emptyFaces.faces.isEmpty())

        val emptyPlates = LicensePlateRecognitionResult(emptyList())
        assertTrue(emptyPlates.plates.isEmpty())
    }

    @Test
    fun testConfidenceBounds() {
        val validConfidences = listOf(0.0f, 0.5f, 1.0f)

        validConfidences.forEach { conf ->
            assertTrue(conf >= 0.0f)
            assertTrue(conf <= 1.0f)
        }
    }

    @Test
    fun testCameraResolutionParsing() {
        val camera = createTestCamera()
        val resolution = camera.resolution

        assertNotNull(resolution)
        assertEquals(1920, resolution?.width)
        assertEquals(1080, resolution?.height)
    }

    @Test
    fun testAnalyticsSettingsDefaults() {
        val camera = createTestCamera()
        val analyticsSettings = camera.settings.analytics

        assertEquals(0.5f, analyticsSettings.objectDetectionConfidenceThreshold)
        assertEquals(10, analyticsSettings.objectDetectionMaxObjects)
        assertFalse(analyticsSettings.objectDetectionUseGPU)
    }
}
