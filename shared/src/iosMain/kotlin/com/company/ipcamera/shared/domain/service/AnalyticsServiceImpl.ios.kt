package com.company.ipcamera.shared.domain.service

import com.company.ipcamera.core.network.analytics.NativeAnalytics
import com.company.ipcamera.shared.domain.model.BoundingBox
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.model.DetectedFace
import com.company.ipcamera.shared.domain.model.DetectedObject
import com.company.ipcamera.shared.domain.model.DetectionZone
import com.company.ipcamera.shared.domain.model.RecognizedLicensePlate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * iOS реализация AnalyticsService
 *
 * Использует NativeAnalytics через FFI (cinterop) для вызова нативных C++ функций.
 * Нативная библиотека должна быть скомпилирована для iOS и доступна через nativeMain source set.
 */
actual class AnalyticsServiceImpl actual constructor() : AnalyticsService {
    private val nativeAnalytics = NativeAnalytics()

    private val motionDetectorHandles = mutableMapOf<String, Long>()
    private val objectDetectorHandles = mutableMapOf<String, Long>()
    private val objectDetectorModelPathLoaded = mutableMapOf<String, String>()
    private val faceDetectorHandles = mutableMapOf<String, Long>()
    private val anprEngineHandles = mutableMapOf<String, Long>()
    private val objectTrackerHandles = mutableMapOf<String, Long>()

    override suspend fun detectMotion(
        camera: Camera,
        frameData: ByteArray,
        previousFrameData: ByteArray?,
        zones: List<DetectionZone>,
        threshold: Float,
        minArea: Int?,
    ): MotionDetectionResult =
        withContext(Dispatchers.Default) {
            try {
                val width = camera.resolution?.width ?: 1920
                val height = camera.resolution?.height ?: 1080
                val effectiveMinArea = minArea ?: (width * height * 0.001).toInt().coerceAtLeast(1)

                val handle =
                    motionDetectorHandles.getOrPut(camera.id) {
                        nativeAnalytics.createMotionDetector(width, height, threshold, effectiveMinArea)
                            ?: return@withContext MotionDetectionResult(
                                detected = false,
                                confidence = 0.0f,
                                zones = emptyList(),
                            )
                    }

                val result =
                    nativeAnalytics.detectMotion(handle, frameData, width, height)
                        ?: return@withContext MotionDetectionResult(
                            detected = false,
                            confidence = 0.0f,
                            zones = emptyList(),
                        )

                if (result.motionDetected) {
                    val motionBox =
                        BoundingBox(
                            x = result.x,
                            y = result.y,
                            width = result.width,
                            height = result.height,
                        )

                    val motionZones =
                        if (zones.isNotEmpty()) {
                            zones.mapNotNull { zone ->
                                val zoneIntensity =
                                    if (isBoundingBoxInZone(motionBox, zone)) {
                                        val sensitivityFactor = zone.sensitivity / 100.0f
                                        result.confidence * sensitivityFactor
                                    } else {
                                        0.0f
                                    }
                                if (zoneIntensity > 0) {
                                    MotionZone(zone, zoneIntensity)
                                } else {
                                    null
                                }
                            }
                        } else {
                            listOf(
                                MotionZone(
                                    zone = DetectionZone("full", emptyList(), 100),
                                    intensity = result.confidence,
                                ),
                            )
                        }

                    MotionDetectionResult(
                        detected = motionZones.isNotEmpty() && motionZones.any { it.intensity > 0 },
                        confidence = result.confidence,
                        zones = motionZones,
                    )
                } else {
                    MotionDetectionResult(
                        detected = false,
                        confidence = result.confidence,
                        zones = emptyList(),
                    )
                }
            } catch (e: Exception) {
                MotionDetectionResult(
                    detected = false,
                    confidence = 0.0f,
                    zones = emptyList(),
                )
            }
        }

    override suspend fun detectObjects(
        camera: Camera,
        frameData: ByteArray,
        objectTypes: List<String>,
        minConfidence: Float,
    ): ObjectDetectionResult =
        withContext(Dispatchers.Default) {
            try {
                val settings = camera.settings.analytics
                val handle =
                    objectDetectorHandles.getOrPut(camera.id) {
                        nativeAnalytics.createObjectDetector(
                            confidenceThreshold = settings.objectDetectionConfidenceThreshold,
                            maxObjects = settings.objectDetectionMaxObjects,
                            useGPU = settings.objectDetectionUseGPU,
                            inputSize = settings.objectDetectionInputSize.coerceIn(224, 1280),
                        ) ?: return@withContext ObjectDetectionResult(objects = emptyList())
                    }

                val modelPath = camera.settings.analytics.objectDetectionModelPath
                if (modelPath != null && objectDetectorModelPathLoaded[camera.id] != modelPath) {
                    val modelLoaded = nativeAnalytics.loadObjectDetectorModel(handle, modelPath)
                    if (!modelLoaded) {
                        return@withContext ObjectDetectionResult(objects = emptyList())
                    }
                    objectDetectorModelPathLoaded[camera.id] = modelPath
                }

                val width = camera.resolution?.width ?: 1920
                val height = camera.resolution?.height ?: 1080

                val nativeResult =
                    nativeAnalytics.detectObjects(handle, frameData, width, height)
                        ?: return@withContext ObjectDetectionResult(objects = emptyList())

                val objects =
                    nativeResult.objects
                        .filter { obj ->
                            val objectType = mapObjectType(obj.type)
                            (objectTypes.isEmpty() || objectTypes.contains(objectType)) &&
                                obj.confidence >= minConfidence
                        }
                        .map { obj ->
                            DetectedObject(
                                type = mapObjectType(obj.type),
                                confidence = obj.confidence,
                                boundingBox =
                                    BoundingBox(
                                        x = obj.x,
                                        y = obj.y,
                                        width = obj.width,
                                        height = obj.height,
                                    ),
                            )
                        }

                ObjectDetectionResult(objects = objects)
            } catch (e: Exception) {
                ObjectDetectionResult(objects = emptyList())
            }
        }

    override suspend fun detectFaces(
        camera: Camera,
        frameData: ByteArray,
        minConfidence: Float,
        includeLandmarks: Boolean,
        includeEmbeddings: Boolean,
    ): FaceDetectionResult =
        withContext(Dispatchers.Default) {
            try {
                val handle =
                    faceDetectorHandles.getOrPut(camera.id) {
                        nativeAnalytics.createFaceDetector(
                            scaleFactor = 1.1f,
                            minNeighbors = 3,
                            minSize = 30,
                            maxSize = 300,
                        ) ?: return@withContext FaceDetectionResult(faces = emptyList())
                    }

                val cascadePath = camera.settings.analytics.faceDetectionCascadePath
                if (cascadePath != null) {
                    nativeAnalytics.loadFaceDetectorCascade(handle, cascadePath)
                }

                val width = camera.resolution?.width ?: 1920
                val height = camera.resolution?.height ?: 1080

                val nativeResult =
                    nativeAnalytics.detectFaces(handle, frameData, width, height)
                        ?: return@withContext FaceDetectionResult(faces = emptyList())

                val faces =
                    nativeResult.faces
                        .filter { it.confidence >= minConfidence }
                        .map { face ->
                            val landmarks =
                                if (includeLandmarks && face.landmarks != null) {
                                    face.landmarks.chunked(2).mapIndexed { index, coords ->
                                        val lx = coords.getOrNull(0) ?: 0
                                        val ly = coords.getOrNull(1) ?: 0
                                        val type =
                                            when (index) {
                                                0 -> LandmarkType.LEFT_EYE
                                                1 -> LandmarkType.RIGHT_EYE
                                                2 -> LandmarkType.NOSE
                                                3 -> LandmarkType.MOUTH_LEFT
                                                4 -> LandmarkType.MOUTH_RIGHT
                                                else -> LandmarkType.CHIN
                                            }
                                        com.company.ipcamera.shared.domain.model.FaceLandmark(
                                            x = lx,
                                            y = ly,
                                            type = type,
                                        )
                                    }
                                } else {
                                    null
                                }

                            DetectedFace(
                                boundingBox =
                                    BoundingBox(
                                        x = face.x,
                                        y = face.y,
                                        width = face.width,
                                        height = face.height,
                                    ),
                                confidence = face.confidence,
                                landmarks = landmarks,
                                embedding = null,
                            )
                        }

                FaceDetectionResult(faces = faces)
            } catch (e: Exception) {
                FaceDetectionResult(faces = emptyList())
            }
        }

    override suspend fun trackObjects(
        camera: Camera,
        frameData: ByteArray,
    ): List<TrackInfo> = emptyList()

    override suspend fun recognizeLicensePlates(
        camera: Camera,
        frameData: ByteArray,
        minConfidence: Float,
        country: String?,
    ): LicensePlateRecognitionResult =
        withContext(Dispatchers.Default) {
            try {
                val handle =
                    anprEngineHandles.getOrPut(camera.id) {
                        nativeAnalytics.createANPREngine(
                            confidenceThreshold = minConfidence,
                            language = country ?: "eng",
                        ) ?: return@withContext LicensePlateRecognitionResult(plates = emptyList())
                    }

                val width = camera.resolution?.width ?: 1920
                val height = camera.resolution?.height ?: 1080

                val nativeResult =
                    nativeAnalytics.recognizeLicensePlates(handle, frameData, width, height)
                        ?: return@withContext LicensePlateRecognitionResult(plates = emptyList())

                val plates =
                    nativeResult.plates
                        .filter { it.confidence >= minConfidence }
                        .map { plate ->
                            RecognizedLicensePlate(
                                plateNumber = plate.text,
                                confidence = plate.confidence,
                                country = country,
                                boundingBox =
                                    BoundingBox(
                                        x = plate.x,
                                        y = plate.y,
                                        width = plate.width,
                                        height = plate.height,
                                    ),
                            )
                        }

                LicensePlateRecognitionResult(plates = plates)
            } catch (e: Exception) {
                LicensePlateRecognitionResult(plates = emptyList())
            }
        }

    private fun mapObjectType(type: com.company.ipcamera.core.network.analytics.ObjectType): String {
        return when (type) {
            com.company.ipcamera.core.network.analytics.ObjectType.PERSON -> "person"
            com.company.ipcamera.core.network.analytics.ObjectType.VEHICLE -> "vehicle"
            com.company.ipcamera.core.network.analytics.ObjectType.BICYCLE -> "bicycle"
            com.company.ipcamera.core.network.analytics.ObjectType.MOTORCYCLE -> "motorcycle"
            com.company.ipcamera.core.network.analytics.ObjectType.UNKNOWN -> "unknown"
        }
    }

    private fun isPointInPolygon(
        x: Int,
        y: Int,
        polygon: List<List<Int>>,
    ): Boolean {
        if (polygon.size < 3) return false
        var inside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val xi = polygon[i].getOrNull(0) ?: 0
            val yi = polygon[i].getOrNull(1) ?: 0
            val xj = polygon[j].getOrNull(0) ?: 0
            val yj = polygon[j].getOrNull(1) ?: 0
            val intersect =
                if (yj != yi) {
                    ((yi > y) != (yj > y)) &&
                        (x < (xj - xi) * (y - yi).toDouble() / (yj - yi) + xi)
                } else {
                    false
                }
            if (intersect) inside = !inside
            j = i
        }
        return inside
    }

    private fun isBoundingBoxInZone(
        box: BoundingBox,
        zone: DetectionZone,
    ): Boolean {
        if (zone.polygon.isEmpty()) return true
        val corners =
            listOf(
                box.x to box.y,
                box.x + box.width to box.y,
                box.x + box.width to box.y + box.height,
                box.x to box.y + box.height,
            )
        if (corners.any { (x, y) -> isPointInPolygon(x, y, zone.polygon) }) {
            return true
        }
        val centerX = box.x + box.width / 2
        val centerY = box.y + box.height / 2
        return isPointInPolygon(centerX, centerY, zone.polygon)
    }

    fun cleanup(cameraId: String) {
        motionDetectorHandles[cameraId]?.let { nativeAnalytics.destroyMotionDetector(it) }
        motionDetectorHandles.remove(cameraId)
        objectDetectorHandles[cameraId]?.let { nativeAnalytics.destroyObjectDetector(it) }
        objectDetectorHandles.remove(cameraId)
        objectDetectorModelPathLoaded.remove(cameraId)
        faceDetectorHandles[cameraId]?.let { nativeAnalytics.destroyFaceDetector(it) }
        faceDetectorHandles.remove(cameraId)
        anprEngineHandles[cameraId]?.let { nativeAnalytics.destroyANPREngine(it) }
        anprEngineHandles.remove(cameraId)
        objectTrackerHandles[cameraId]?.let { nativeAnalytics.destroyObjectTracker(it) }
        objectTrackerHandles.remove(cameraId)
    }

    override suspend fun compareFaces(
        embedding1: FloatArray,
        embedding2: FloatArray,
    ): Float =
        withContext(Dispatchers.Default) {
            if (embedding1.size != embedding2.size || embedding1.isEmpty()) return@withContext 0f
            var dot = 0.0
            var norm1 = 0.0
            var norm2 = 0.0
            for (i in embedding1.indices) {
                dot += embedding1[i] * embedding2[i]
                norm1 += embedding1[i] * embedding1[i]
                norm2 += embedding2[i] * embedding2[i]
            }
            val n = (norm1 * norm2).takeIf { it > 0 } ?: return@withContext 0f
            (dot / kotlin.math.sqrt(n)).toFloat().coerceIn(-1f, 1f).let { (it + 1f) / 2f }
        }

    fun cleanupAll() {
        motionDetectorHandles.values.forEach { nativeAnalytics.destroyMotionDetector(it) }
        motionDetectorHandles.clear()
        objectDetectorHandles.values.forEach { nativeAnalytics.destroyObjectDetector(it) }
        objectDetectorHandles.clear()
        objectDetectorModelPathLoaded.clear()
        faceDetectorHandles.values.forEach { nativeAnalytics.destroyFaceDetector(it) }
        faceDetectorHandles.clear()
        anprEngineHandles.values.forEach { nativeAnalytics.destroyANPREngine(it) }
        anprEngineHandles.clear()
        objectTrackerHandles.values.forEach { nativeAnalytics.destroyObjectTracker(it) }
        objectTrackerHandles.clear()
    }
}
