package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.repository.CameraRepository
import java.util.UUID

/**
 * Use case для добавления новой камеры
 */
class AddCameraUseCase(
    private val cameraRepository: CameraRepository
) {
    suspend operator fun invoke(
        name: String,
        url: String,
        username: String? = null,
        password: String? = null,
        model: String? = null
    ): Result<Camera> {
        val camera = Camera(
            id = UUID.randomUUID().toString(),
            name = name,
            url = url,
            username = username,
            password = password,
            model = model,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        return cameraRepository.addCamera(camera)
    }
}


