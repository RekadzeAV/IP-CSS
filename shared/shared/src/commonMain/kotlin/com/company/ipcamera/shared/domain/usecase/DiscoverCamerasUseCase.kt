package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.repository.CameraRepository
import com.company.ipcamera.shared.domain.repository.DiscoveredCamera

/**
 * Use case для обнаружения камер в сети через ONVIF WS-Discovery
 *
 * Использует протокол WS-Discovery для автоматического обнаружения
 * ONVIF-совместимых камер в локальной сети.
 *
 * @return список обнаруженных камер
 */
class DiscoverCamerasUseCase(
    private val cameraRepository: CameraRepository
) {
    /**
     * Обнаружить камеры в сети
     *
     * @return список обнаруженных камер
     */
    suspend operator fun invoke(): List<DiscoveredCamera> {
        return cameraRepository.discoverCameras()
    }
}

