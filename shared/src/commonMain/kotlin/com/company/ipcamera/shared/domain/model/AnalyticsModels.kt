package com.company.ipcamera.shared.domain.model

/**
 * Ограничивающий прямоугольник (bounding box)
 */
@kotlinx.serialization.Serializable
data class BoundingBox(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

/**
 * Обнаруженный объект
 */
@kotlinx.serialization.Serializable
data class DetectedObject(
    val type: String, // person, car, truck, etc.
    val confidence: Float, // 0.0 - 1.0
    val boundingBox: BoundingBox,
    val attributes: Map<String, String> = emptyMap()
)

/**
 * Обнаруженное лицо
 */
@kotlinx.serialization.Serializable
data class DetectedFace(
    val boundingBox: BoundingBox,
    val confidence: Float, // 0.0 - 1.0
    val landmarks: List<FaceLandmark>? = null, // Точки лица (глаза, нос, рот и т.д.)
    val embedding: FloatArray? = null, // Векторное представление лица для распознавания
    val attributes: Map<String, String> = emptyMap()
)

/**
 * Точка на лице (landmark)
 */
@kotlinx.serialization.Serializable
data class FaceLandmark(
    val x: Int,
    val y: Int,
    val type: LandmarkType
)

/**
 * Тип точки на лице
 */
@kotlinx.serialization.Serializable
enum class LandmarkType {
    LEFT_EYE,
    RIGHT_EYE,
    NOSE,
    MOUTH_LEFT,
    MOUTH_RIGHT,
    LEFT_EAR,
    RIGHT_EAR,
    CHIN
}

/**
 * Распознанный номерной знак
 */
@kotlinx.serialization.Serializable
data class RecognizedLicensePlate(
    val plateNumber: String,
    val confidence: Float, // 0.0 - 1.0
    val country: String? = null, // Код страны
    val boundingBox: BoundingBox,
    val attributes: Map<String, String> = emptyMap()
)

