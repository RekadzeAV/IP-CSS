package com.company.ipcamera.shared.domain.model.face

import kotlinx.serialization.Serializable

/**
 * Detected face in image/video frame
 */
@Serializable
data class DetectedFace(
    val id: String,
    val boundingBox: BoundingBox,
    val confidence: Double, // 0.0 - 1.0
    val embedding: FloatArray, // FaceNet: 512, ArcFace: 512
    val landmarks: List<FaceLandmark> = emptyList(),
)

/**
 * Bounding box for face detection
 */
@Serializable
data class BoundingBox(
    val x: Int, // Top-left x
    val y: Int, // Top-left y
    val width: Int,
    val height: Int,
)

/**
 * Face landmark (eye, nose, mouth, etc.)
 */
@Serializable
data class FaceLandmark(
    val type: LandmarkType,
    val x: Int,
    val y: Int,
) {
    @Serializable
    enum class LandmarkType {
        LEFT_EYE,
        RIGHT_EYE,
        NOSE_TIP,
        LEFT_MOUTH_CORNER,
        RIGHT_MOUTH_CORNER,
        LEFT_EYEBROW,
        RIGHT_EYEBROW,
        CHIN,
    }
}

/**
 * Face match result from gallery
 */
@Serializable
data class FaceMatch(
    val faceId: String,
    val name: String,
    val similarity: Double, // 0.0 - 1.0
    val threshold: Double = 0.6,
) {
    val isMatch: Boolean
        get() = similarity >= threshold
}

/**
 * Face gallery for storing embeddings
 */
@Serializable
data class FaceGallery(
    val id: String,
    val name: String,
    val faceCount: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * Face embedding stored in gallery
 */
@Serializable
data class FaceEmbedding(
    val id: String,
    val galleryId: String,
    val name: String,
    val embedding: List<Float>, // Serialized FloatArray
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: Long,
)

/**
 * Face recognition event
 */
@Serializable
data class FaceRecognitionEvent(
    val id: String,
    val cameraId: String,
    val timestamp: Long,
    val matchedFaceId: String?,
    val matchedName: String?,
    val similarity: Double?,
    val snapshotPath: String?,
    val processed: Boolean = false,
)
