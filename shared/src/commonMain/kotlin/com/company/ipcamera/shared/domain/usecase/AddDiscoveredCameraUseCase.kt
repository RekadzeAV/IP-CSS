package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.repository.CameraRepository
import com.company.ipcamera.shared.domain.repository.DiscoveredCamera
import java.util.UUID

/**
 * Use case для добавления обнаруженной камеры в систему
 *
 * Преобразует обнаруженную камеру в полную модель Camera
 * и добавляет её в репозиторий с базовыми настройками.
 */
class AddDiscoveredCameraUseCase(
    private val cameraRepository: CameraRepository,
    private val addCameraUseCase: AddCameraUseCase
) {
    /**
     * Добавить обнаруженную камеру в систему
     *
     * @param discoveredCamera обнаруженная камера
     * @param username имя пользователя для подключения (опционально)
     * @param password пароль для подключения (опционально)
     * @return результат операции с добавленной камерой
     */
    suspend operator fun invoke(
        discoveredCamera: DiscoveredCamera,
        username: String? = null,
        password: String? = null
    ): Result<Camera> {
        return addCameraUseCase(
            name = discoveredCamera.name,
            url = discoveredCamera.url,
            username = username,
            password = password,
            model = discoveredCamera.model
        )
    }
}

