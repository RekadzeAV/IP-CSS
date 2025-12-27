package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.repository.CameraRepository
import com.company.ipcamera.shared.domain.repository.ConnectionTestResult
import com.company.ipcamera.shared.domain.repository.DiscoveredCamera
import java.util.UUID

/**
 * Use case для проверки подключения к обнаруженной камере
 *
 * Создает временный объект Camera из DiscoveredCamera
 * и проверяет возможность подключения к камере.
 */
class TestDiscoveredCameraUseCase(
    private val cameraRepository: CameraRepository
) {
    /**
     * Проверить подключение к обнаруженной камере
     *
     * @param discoveredCamera обнаруженная камера
     * @param username имя пользователя для подключения (опционально)
     * @param password пароль для подключения (опционально)
     * @return результат проверки подключения
     */
    suspend operator fun invoke(
        discoveredCamera: DiscoveredCamera,
        username: String? = null,
        password: String? = null
    ): ConnectionTestResult {
        // Создаем временный объект Camera для проверки подключения
        val testCamera = Camera(
            id = UUID.randomUUID().toString(),
            name = discoveredCamera.name,
            url = discoveredCamera.url,
            username = username,
            password = password,
            model = discoveredCamera.model,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        return cameraRepository.testConnection(testCamera)
    }
}


