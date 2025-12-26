package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.repository.CameraRepository

/**
 * Use case для получения камеры по ID
 */
class GetCameraByIdUseCase(
    private val cameraRepository: CameraRepository
) {
    suspend operator fun invoke(id: String): Camera? {
        return cameraRepository.getCameraById(id)
    }
}


