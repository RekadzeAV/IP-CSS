package com.company.ipcamera.core.network.analytics

/**
 * Expect класс для нативной аналитики
 * Реализации для разных платформ будут в actual классах
 */
expect class NativeAnalytics {
    /**
     * Версия нативной библиотеки аналитики (для тестов инфраструктуры и отладки).
     */
    fun getVersion(): String

    /**
     * Создать детектор движения
     */
    fun createMotionDetector(
        width: Int,
        height: Int,
        threshold: Float = 0.5f,
        minArea: Int = 100
    ): Long?

    /**
     * Детектировать движение в кадре
     */
    suspend fun detectMotion(
        handle: Long,
        frameData: ByteArray,
        width: Int,
        height: Int
    ): MotionDetectionResult?

    /**
     * Уничтожить детектор движения
     */
    fun destroyMotionDetector(handle: Long)

    /**
     * Создать детектор объектов.
     * @param inputSize Размер входа модели (ширина и высота, например 640 для YOLO).
     */
    fun createObjectDetector(
        confidenceThreshold: Float = 0.5f,
        maxObjects: Int = 10,
        useGPU: Boolean = false,
        inputSize: Int = 640
    ): Long?

    /**
     * Загрузить модель для детектора объектов
     */
    suspend fun loadObjectDetectorModel(
        handle: Long,
        modelPath: String
    ): Boolean

    /**
     * Детектировать объекты в кадре
     */
    suspend fun detectObjects(
        handle: Long,
        frameData: ByteArray,
        width: Int,
        height: Int
    ): ObjectDetectionResult?

    /**
     * Уничтожить детектор объектов
     */
    fun destroyObjectDetector(handle: Long)

    /**
     * Создать детектор лиц
     */
    fun createFaceDetector(
        scaleFactor: Float = 1.1f,
        minNeighbors: Int = 3,
        minSize: Int = 30,
        maxSize: Int = 300
    ): Long?

    /**
     * Загрузить каскад для детектора лиц
     */
    suspend fun loadFaceDetectorCascade(
        handle: Long,
        cascadePath: String
    ): Boolean

    /**
     * Детектировать лица в кадре
     */
    suspend fun detectFaces(
        handle: Long,
        frameData: ByteArray,
        width: Int,
        height: Int
    ): FaceDetectionResult?

    /**
     * Уничтожить детектор лиц
     */
    fun destroyFaceDetector(handle: Long)

    /**
     * Создать движок ANPR
     */
    fun createANPREngine(
        confidenceThreshold: Float = 0.7f,
        language: String = "eng"
    ): Long?

    /**
     * Распознать номерные знаки в кадре
     */
    suspend fun recognizeLicensePlates(
        handle: Long,
        frameData: ByteArray,
        width: Int,
        height: Int
    ): ANPRResult?

    /**
     * Уничтожить движок ANPR
     */
    fun destroyANPREngine(handle: Long)

    /**
     * Создать трекер объектов
     */
    fun createObjectTracker(
        iouThreshold: Float = 0.5f,
        maxAge: Int = 30,
        minConfidence: Float = 0.5f
    ): Long?

    /**
     * Обновить треки на основе новых детекций.
     * @param timestampMs временная метка кадра (мс); используется для lastSeen (0 = не обновлять)
     */
    suspend fun updateTracking(
        handle: Long,
        detections: ObjectDetectionResult,
        timestampMs: Long = 0L
    ): TrackingResult?

    /**
     * Уничтожить трекер объектов
     */
    fun destroyObjectTracker(handle: Long)
}

/**
 * Результат детекции движения
 */
data class MotionDetectionResult(
    val motionDetected: Boolean,
    val confidence: Float,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

/**
 * Результат детекции объектов
 */
data class ObjectDetectionResult(
    val objects: List<DetectedObject>
)

/**
 * Обнаруженный объект
 */
data class DetectedObject(
    val type: ObjectType,
    val confidence: Float,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

/**
 * Тип объекта
 */
enum class ObjectType {
    PERSON,
    VEHICLE,
    BICYCLE,
    MOTORCYCLE,
    UNKNOWN
}

/**
 * Результат детекции лиц
 */
data class FaceDetectionResult(
    val faces: List<DetectedFace>
)

/**
 * Обнаруженное лицо
 */
data class DetectedFace(
    val confidence: Float,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val landmarks: IntArray? = null // 10 элементов: x1, y1, x2, y2, ... (5 точек)
)

/**
 * Результат распознавания номерных знаков
 */
data class ANPRResult(
    val plates: List<RecognizedPlate>
)

/**
 * Распознанный номерной знак
 */
data class RecognizedPlate(
    val text: String,
    val confidence: Float,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

/**
 * Результат трекинга объектов
 */
data class TrackingResult(
    val objects: List<TrackedObject>
)

/**
 * Отслеживаемый объект
 */
data class TrackedObject(
    val id: Int,
    val type: ObjectType,
    val confidence: Float,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val lastSeen: Long
)
