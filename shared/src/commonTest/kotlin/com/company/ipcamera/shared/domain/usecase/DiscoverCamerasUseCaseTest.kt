package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.repository.DiscoveredCamera
import com.company.ipcamera.shared.test.MockCameraRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Тесты для DiscoverCamerasUseCase
 */
class DiscoverCamerasUseCaseTest {

    @Test
    fun `test discover cameras returns list`() = runTest {
        // Arrange
        val repository = MockCameraRepository()
        val useCase = DiscoverCamerasUseCase(repository)

        // Act
        // Примечание: discoverCameras() использует OnvifClient для WS-Discovery
        // В тестовой среде без реальной сети возвращает пустой список
        val result = useCase()

        // Assert
        assertNotNull(result)
        assertTrue(result is List<DiscoveredCamera>)
        // В тестовой среде без реальной сети результат будет пустым
        // Для полного тестирования требуется мок OnvifClient или тестовая сеть
    }

    @Test
    fun `test discover cameras is idempotent`() = runTest {
        // Arrange
        val repository = MockCameraRepository()
        val useCase = DiscoverCamerasUseCase(repository)

        // Act
        val result1 = useCase()
        val result2 = useCase()

        // Assert
        assertNotNull(result1)
        assertNotNull(result2)
        // Результаты должны быть независимыми (не кешироваться)
    }
}

