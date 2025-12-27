package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.repository.CameraRepository

/**
 * Use case для обновления камеры
 */
class UpdateCameraUseCase(
    private val cameraRepository: CameraRepository
) {
    suspend operator fun invoke(camera: Camera): Result<Camera> {
        val updatedCamera = camera.copy(
            updatedAt = System.currentTimeMillis()
        )
        return cameraRepository.updateCamera(updatedCamera)
    }
}



