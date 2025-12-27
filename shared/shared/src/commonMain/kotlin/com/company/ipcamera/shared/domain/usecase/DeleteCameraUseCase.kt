package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.repository.CameraRepository

/**
 * Use case для удаления камеры
 */
class DeleteCameraUseCase(
    private val cameraRepository: CameraRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        return cameraRepository.removeCamera(id)
    }
}

