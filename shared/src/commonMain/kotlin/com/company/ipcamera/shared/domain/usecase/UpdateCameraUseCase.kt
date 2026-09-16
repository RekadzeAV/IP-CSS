package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.common.nowMillis
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.repository.CameraRepository

/**
 * Use case для обновления камеры.
 * Проверка «только HTTPS» выполняется в репозитории при updateCamera().
 */
class UpdateCameraUseCase(
    private val cameraRepository: CameraRepository,
) {
    suspend operator fun invoke(camera: Camera): Result<Camera> {
        val updatedCamera =
            camera.copy(
                updatedAt = nowMillis(),
            )
        return cameraRepository.updateCamera(updatedCamera)
    }
}
