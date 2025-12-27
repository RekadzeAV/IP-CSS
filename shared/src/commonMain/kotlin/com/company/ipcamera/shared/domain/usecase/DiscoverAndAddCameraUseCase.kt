package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.repository.CameraRepository
import com.company.ipcamera.shared.domain.repository.DiscoveredCamera

/**
 * Use case для обнаружения камер и автоматического добавления их в систему
 *
 * Объединяет процесс обнаружения камер через ONVIF и их добавление
 * в систему с указанными учетными данными.
 */
class DiscoverAndAddCameraUseCase(
    private val discoverCamerasUseCase: DiscoverCamerasUseCase,
    private val addDiscoveredCameraUseCase: AddDiscoveredCameraUseCase
) {
    /**
     * Обнаружить камеры и добавить их в систему
     *
     * @param username имя пользователя для подключения (опционально)
     * @param password пароль для подключения (опционально)
     * @param autoAdd если true, автоматически добавляет все обнаруженные камеры
     * @return список результатов добавления камер (успешных и неуспешных)
     */
    suspend operator fun invoke(
        username: String? = null,
        password: String? = null,
        autoAdd: Boolean = false
    ): DiscoveryAndAddResult {
        val discoveredCameras = discoverCamerasUseCase()

        if (!autoAdd) {
            return DiscoveryAndAddResult(
                discoveredCameras = discoveredCameras,
                addedCameras = emptyList(),
                failedCameras = emptyList()
            )
        }

        val addedCameras = mutableListOf<Camera>()
        val failedCameras = mutableListOf<Pair<DiscoveredCamera, String>>()

        for (discovered in discoveredCameras) {
            val result = addDiscoveredCameraUseCase(
                discoveredCamera = discovered,
                username = username,
                password = password
            )

            result.fold(
                onSuccess = { camera -> addedCameras.add(camera) },
                onFailure = { error ->
                    failedCameras.add(discovered to (error.message ?: "Unknown error"))
                }
            )
        }

        return DiscoveryAndAddResult(
            discoveredCameras = discoveredCameras,
            addedCameras = addedCameras,
            failedCameras = failedCameras
        )
    }
}

/**
 * Результат обнаружения и добавления камер
 */
data class DiscoveryAndAddResult(
    val discoveredCameras: List<DiscoveredCamera>,
    val addedCameras: List<Camera>,
    val failedCameras: List<Pair<DiscoveredCamera, String>>
) {
    val totalDiscovered: Int get() = discoveredCameras.size
    val totalAdded: Int get() = addedCameras.size
    val totalFailed: Int get() = failedCameras.size
    val isSuccess: Boolean get() = failedCameras.isEmpty() && addedCameras.isNotEmpty()
}

