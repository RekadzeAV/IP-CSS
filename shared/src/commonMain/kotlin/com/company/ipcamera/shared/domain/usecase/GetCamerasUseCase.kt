package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.repository.CameraRepository

/**
 * Use case для получения списка всех камер
 */
class GetCamerasUseCase(
    private val cameraRepository: CameraRepository
) {
    suspend operator fun invoke(): List<Camera> {
        return cameraRepository.getCameras()
    }
}




