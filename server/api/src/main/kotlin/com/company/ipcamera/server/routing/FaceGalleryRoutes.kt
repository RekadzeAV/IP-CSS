package com.company.ipcamera.server.routing

import com.company.ipcamera.server.dto.ApiResponse
import com.company.ipcamera.server.dto.CreateFaceRequestDto
import com.company.ipcamera.server.dto.FaceSearchMatchDto
import com.company.ipcamera.server.dto.FaceSearchRequestDto
import com.company.ipcamera.server.dto.StoredFaceDto
import com.company.ipcamera.server.middleware.AuthorizationMiddleware.requireRole
import com.company.ipcamera.shared.domain.model.StoredFace
import com.company.ipcamera.shared.domain.model.UserRole
import com.company.ipcamera.shared.domain.repository.FaceRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID
import org.koin.ktor.ext.inject

fun Route.faceGalleryRoutes() {
    val faceRepository: FaceRepository by inject()
    val maxEmbeddingSize = 4096
    val minEmbeddingSize = 8
    val maxMetadataEntries = 64
    val maxMetadataValueLength = 512

    authenticate("jwt-auth") {
                route("/analytics/faces") {
            get { handleListFaces(faceRepository, call) }
            post {
                requireRole(UserRole.OPERATOR)
                handleCreateFace(faceRepository, call, minEmbeddingSize, maxEmbeddingSize, maxMetadataEntries, maxMetadataValueLength)
            }
            get("/labels") { handleListLabels(faceRepository, call) }
            get("/{id}") { handleGetFace(faceRepository, call) }
            delete("/{id}") {
                requireRole(UserRole.OPERATOR)
                handleDeleteFace(faceRepository, call)
            }
            delete("/by-label/{label}") {
                requireRole(UserRole.OPERATOR)
                handleDeleteFacesByLabel(faceRepository, call)
            }
            post("/search") { handleSearchFaces(faceRepository, call, minEmbeddingSize, maxEmbeddingSize) }
        }
    }
}

private suspend fun handleListFaces(faceRepository: FaceRepository, call: ApplicationCall) {
    val label = call.request.queryParameters["label"]?.trim().orEmpty()
    if (label.isBlank()) {
        call.respond(HttpStatusCode.BadRequest, ApiResponse<String>(success = false, data = null, message = "Query parameter 'label' is required"))
        return
    }
    val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 1000) ?: 100
    val faces = faceRepository.getByLabel(label, limit)
    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = faces.map { it.toDto() }, message = "Faces retrieved successfully"))
}

private suspend fun handleCreateFace(
    faceRepository: FaceRepository,
    call: ApplicationCall,
    minEmbeddingSize: Int,
    maxEmbeddingSize: Int,
    maxMetadataEntries: Int,
    maxMetadataValueLength: Int
) {
    val request = runCatching { call.receive<CreateFaceRequestDto>() }.getOrElse {
        call.respond(HttpStatusCode.BadRequest, ApiResponse<String>(success = false, data = null, message = "Invalid request body"))
        return
    }

    val validation = validateFaceRequest(request, minEmbeddingSize, maxEmbeddingSize, maxMetadataEntries, maxMetadataValueLength)
    if (validation != null) {
        call.respond(HttpStatusCode.BadRequest, ApiResponse<String>(success = false, data = null, message = validation))
        return
    }

    val face = StoredFace(
        id = request.id?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
        label = request.label.trim(),
        embedding = request.embedding.toFloatArray(),
        cameraId = request.cameraId?.takeIf { it.isNotBlank() },
        createdAt = System.currentTimeMillis(),
        metadata = request.metadata
    )

    faceRepository.insert(face).fold(
        onSuccess = { call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = face.toDto(), message = "Face created successfully")) },
        onFailure = { call.respond(HttpStatusCode.InternalServerError, ApiResponse<String>(success = false, data = null, message = "Failed to save face: ${it.message}")) }
    )
}

private suspend fun handleListLabels(faceRepository: FaceRepository, call: ApplicationCall) {
    val labels = faceRepository.listLabels()
    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = labels, message = "Face labels retrieved successfully"))
}

private suspend fun handleGetFace(faceRepository: FaceRepository, call: ApplicationCall) {
    val id = call.parameters["id"]?.takeIf { it.isNotBlank() }
    if (id == null) {
        call.respond(HttpStatusCode.BadRequest, ApiResponse<String>(success = false, data = null, message = "Face ID is required"))
        return
    }
    val face = faceRepository.getById(id)
    if (face == null) {
        call.respond(HttpStatusCode.NotFound, ApiResponse<String>(success = false, data = null, message = "Face not found"))
        return
    }
    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = face.toDto(), message = "Face retrieved successfully"))
}

private suspend fun handleDeleteFace(faceRepository: FaceRepository, call: ApplicationCall) {
    val id = call.parameters["id"]?.takeIf { it.isNotBlank() }
    if (id == null) {
        call.respond(HttpStatusCode.BadRequest, ApiResponse<String>(success = false, data = null, message = "Face ID is required"))
        return
    }
    faceRepository.delete(id).fold(
        onSuccess = { call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = Unit, message = "Face deleted successfully")) },
        onFailure = { call.respond(HttpStatusCode.InternalServerError, ApiResponse<String>(success = false, data = null, message = "Failed to delete face: ${it.message}")) }
    )
}

private suspend fun handleDeleteFacesByLabel(faceRepository: FaceRepository, call: ApplicationCall) {
    val label = call.parameters["label"]?.takeIf { it.isNotBlank() }
    if (label == null) {
        call.respond(HttpStatusCode.BadRequest, ApiResponse<String>(success = false, data = null, message = "Label is required"))
        return
    }
    faceRepository.deleteByLabel(label).fold(
        onSuccess = { call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = it, message = "Faces deleted successfully")) },
        onFailure = { call.respond(HttpStatusCode.InternalServerError, ApiResponse<String>(success = false, data = null, message = "Failed to delete faces by label: ${it.message}")) }
    )
}

private suspend fun handleSearchFaces(
    faceRepository: FaceRepository,
    call: ApplicationCall,
    minEmbeddingSize: Int,
    maxEmbeddingSize: Int
) {
    val request = runCatching { call.receive<FaceSearchRequestDto>() }.getOrElse {
        call.respond(HttpStatusCode.BadRequest, ApiResponse<String>(success = false, data = null, message = "Invalid request body"))
        return
    }

    val validation = validateSearchRequest(request, minEmbeddingSize, maxEmbeddingSize)
    if (validation != null) {
        call.respond(HttpStatusCode.BadRequest, ApiResponse<String>(success = false, data = null, message = validation))
        return
    }

    val matches = faceRepository.findNearest(
        embedding = request.embedding.toFloatArray(),
        topK = request.topK.coerceIn(1, 1000),
        minSimilarity = request.minSimilarity
    ).map { (face, similarity) -> FaceSearchMatchDto(face = face.toDto(), similarity = similarity) }

    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = matches, message = "Face search completed successfully"))
}

private fun validateFaceRequest(
    request: CreateFaceRequestDto,
    minEmbeddingSize: Int,
    maxEmbeddingSize: Int,
    maxMetadataEntries: Int,
    maxMetadataValueLength: Int
): String? {
    if (request.label.isBlank()) return "Field 'label' must not be blank"
    if (request.embedding.isEmpty()) return "Field 'embedding' must not be empty"
    if (request.embedding.size !in minEmbeddingSize..maxEmbeddingSize) return "Field 'embedding' size must be in range [$minEmbeddingSize, $maxEmbeddingSize]"
    if (!request.embedding.all { it.isFinite() }) return "Field 'embedding' must contain only finite numbers"
    if (request.metadata.size > maxMetadataEntries) return "Field 'metadata' supports up to $maxMetadataEntries entries"
    if (request.metadata.any { (k, v) -> k.isBlank() || v.length > maxMetadataValueLength }) return "Field 'metadata' contains invalid key/value (blank key or too long value)"
    return null
}

private fun validateSearchRequest(request: FaceSearchRequestDto, minEmbeddingSize: Int, maxEmbeddingSize: Int): String? {
    if (request.embedding.isEmpty()) return "Field 'embedding' must not be empty"
    if (request.embedding.size !in minEmbeddingSize..maxEmbeddingSize) return "Field 'embedding' size must be in range [$minEmbeddingSize, $maxEmbeddingSize]"
    if (!request.embedding.all { it.isFinite() }) return "Field 'embedding' must contain only finite numbers"
    if (request.minSimilarity !in 0f..1f) return "Field 'minSimilarity' must be between 0.0 and 1.0"
    if (request.topK !in 1..1000) return "Field 'topK' must be in range [1, 1000]"
    return null
}

internal fun StoredFace.toDto(): StoredFaceDto {
    return StoredFaceDto(
        id = id,
        label = label,
        embedding = embedding.toList(),
        cameraId = cameraId,
        createdAt = createdAt,
        metadata = metadata
    )
}
