package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.common.newRandomId
import com.company.ipcamera.shared.common.nowMillis
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.repository.CameraRepository

/**
 * Use case для добавления новой камеры.
 * Проверка «только HTTPS» выполняется в репозитории при addCamera().
 */
class AddCameraUseCase(
    private val cameraRepository: CameraRepository,
) {
    suspend operator fun invoke(
        name: String,
        url: String,
        username: String? = null,
        password: String? = null,
        model: String? = null,
    ): Result<Camera> {
        val camera =
            Camera(
                id = newRandomId(),
                name = name,
                url = url,
                username = username,
                password = password,
                model = model,
                createdAt = nowMillis(),
                updatedAt = nowMillis(),
            )

        return cameraRepository.addCamera(camera)
    }
}
