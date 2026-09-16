package com.company.ipcamera.server.service.face

import com.company.ipcamera.shared.domain.model.StoredFace

class FaceRecognitionService {
    suspend fun detectFaces(image: ByteArray): Result<List<FaceInfo>> {
        return Result.success(emptyList())
    }
    
    suspend fun recognizeFace(image: ByteArray, gallery: List<StoredFace>): Result<FaceMatch?> {
        return Result.success(null)
    }
    
    suspend fun enrollFace(image: ByteArray, userId: String): Result<StoredFace> {
        return Result.failure(Exception("Not implemented"))
    }
    
    suspend fun getAllFaces(page: Int, limit: Int): List<StoredFace> = emptyList()
    suspend fun getFaceById(id: String): StoredFace? = null
    suspend fun deleteFace(id: String): Result<Unit> = Result.success(Unit)
}

data class FaceInfo(
    val id: String,
    val confidence: Double,
    val bbox: FaceBbox,
    val landmarks: List<FaceLandmark>? = null
)

data class FaceMatch(
    val faceId: String,
    val userId: String,
    val confidence: Double
)

data class FaceBbox(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double
)

data class FaceLandmark(
    val type: String,
    val x: Double,
    val y: Double
)