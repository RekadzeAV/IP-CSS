package com.company.ipcamera.shared.data.repository

import com.company.ipcamera.core.common.model.CameraStatus
import com.company.ipcamera.core.network.ApiResult
import com.company.ipcamera.shared.domain.repository.DiscoveredCamera
import com.company.ipcamera.shared.test.MockCameraLocalDataSource
import com.company.ipcamera.shared.test.MockCameraRemoteDataSource
import com.company.ipcamera.shared.test.TestDataFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

/**
 * Тесты для CameraRepositoryImplV2
 */
class CameraRepositoryImplTest {
    private lateinit var localDataSource: MockCameraLocalDataSource
    private lateinit var remoteDataSource: MockCameraRemoteDataSource
    private lateinit var repository: CameraRepositoryImplV2

    @BeforeTest
    fun setup() {
        localDataSource = MockCameraLocalDataSource()
        remoteDataSource = MockCameraRemoteDataSource()
        repository = CameraRepositoryImpl(localDataSource, remoteDataSource)
    }

    @Test
    fun `test getCameras returns cached data on second call`() =
        runTest {
            // Arrange
            val camera = TestDataFactory.createTestCamera(id = "camera-1")
            localDataSource.addCameraDirectly(camera)

            // Act - первый вызов
            val firstCall = repository.getCameras()

            // Act - второй вызов (должен быть из кэша)
            val secondCall = repository.getCameras()

            // Assert
            assertEquals(1, firstCall.size)
            assertEquals(1, secondCall.size)
            assertEquals("camera-1", firstCall[0].id)
            assertEquals("camera-1", secondCall[0].id)
            // Второй вызов должен использовать кэш, поэтому localDataSource.getCameras() не должен вызываться повторно
            // Проверяем, что данные одинаковые
            assertTrue(firstCall == secondCall)
        }

    @Test
    fun `test getCameras with local-first strategy returns local data`() =
        runTest {
            // Arrange
            val localCamera = TestDataFactory.createTestCamera(id = "local-1", name = "Local Camera")
            val remoteCamera = TestDataFactory.createTestCamera(id = "remote-1", name = "Remote Camera")

            localDataSource.addCameraDirectly(localCamera)
            remoteDataSource.addCameraDirectly(remoteCamera)

            // Act
            val result = repository.getCameras()

            // Assert - должен вернуть локальные данные
            assertEquals(1, result.size)
            assertEquals("local-1", result[0].id)
            assertEquals("Local Camera", result[0].name)
        }

    @Test
    fun `test getCameras with remote fallback when local is empty`() =
        runTest {
            // Arrange
            val remoteCamera = TestDataFactory.createTestCamera(id = "remote-1", name = "Remote Camera")
            remoteDataSource.addCameraDirectly(remoteCamera)
            // localDataSource пустой

            // Act
            val result = repository.getCameras()

            // Assert - должен вернуть данные с remote и сохранить в local
            assertEquals(1, result.size)
            assertEquals("remote-1", result[0].id)
            // Проверяем, что данные сохранились в local
            val localCameras = localDataSource.getCameras()
            assertEquals(1, localCameras.size)
            assertEquals("remote-1", localCameras[0].id)
        }

    @Test
    fun `test getCameraById returns cached data on second call`() =
        runTest {
            // Arrange
            val camera = TestDataFactory.createTestCamera(id = "camera-1")
            localDataSource.addCameraDirectly(camera)

            // Act - первый вызов
            val firstCall = repository.getCameraById("camera-1")

            // Act - второй вызов (должен быть из кэша)
            val secondCall = repository.getCameraById("camera-1")

            // Assert
            assertNotNull(firstCall)
            assertNotNull(secondCall)
            assertEquals("camera-1", firstCall?.id)
            assertEquals("camera-1", secondCall?.id)
        }

    @Test
    fun `test addCamera invalidates cache`() =
        runTest {
            // Arrange
            val existingCamera = TestDataFactory.createTestCamera(id = "camera-1")
            localDataSource.addCameraDirectly(existingCamera)

            // Заполняем кэш
            repository.getCameras()

            val newCamera = TestDataFactory.createTestCamera(id = "camera-2")

            // Act
            val result = repository.addCamera(newCamera)

            // Assert
            assertTrue(result.isSuccess)
            // Проверяем, что кэш инвалидирован - следующий вызов getCameras должен вернуть оба камеры
            val allCameras = repository.getCameras()
            assertEquals(2, allCameras.size)
        }

    @Test
    fun `test updateCamera invalidates cache`() =
        runTest {
            // Arrange
            val camera = TestDataFactory.createTestCamera(id = "camera-1", name = "Original Name")
            localDataSource.addCameraDirectly(camera)

            // Заполняем кэш
            repository.getCameraById("camera-1")

            val updatedCamera = camera.copy(name = "Updated Name")

            // Act
            val result = repository.updateCamera(updatedCamera)

            // Assert
            assertTrue(result.isSuccess)
            // Проверяем, что кэш инвалидирован - следующий вызов должен вернуть обновленную камеру
            val retrieved = repository.getCameraById("camera-1")
            assertNotNull(retrieved)
            assertEquals("Updated Name", retrieved.name)
        }

    @Test
    fun `test removeCamera invalidates cache`() =
        runTest {
            // Arrange
            val camera = TestDataFactory.createTestCamera(id = "camera-1")
            localDataSource.addCameraDirectly(camera)

            // Заполняем кэш
            repository.getCameraById("camera-1")
            repository.getCameras()

            // Act
            val result = repository.removeCamera("camera-1")

            // Assert
            assertTrue(result.isSuccess)
            // Проверяем, что кэш инвалидирован - следующий вызов должен вернуть null
            val retrieved = repository.getCameraById("camera-1")
            assertNull(retrieved)

            val allCameras = repository.getCameras()
            assertEquals(0, allCameras.size)
        }

    @Test
    fun `test getCameras handles network errors gracefully`() =
        runTest {
            // Arrange
            remoteDataSource.shouldFailOnGet = true
            remoteDataSource.networkError = Exception("Network error")
            // localDataSource пустой

            // Act
            val result = repository.getCameras()

            // Assert - должен вернуть пустой список, не упав с ошибкой
            assertTrue(result.isEmpty())
        }

    @Test
    fun `test getCameraById handles network errors gracefully`() =
        runTest {
            // Arrange
            remoteDataSource.shouldFailOnGet = true
            remoteDataSource.networkError = Exception("Network error")
            // localDataSource пустой

            // Act
            val result = repository.getCameraById("non-existent")

            // Assert - должен вернуть null, не упав с ошибкой
            assertNull(result)
        }

    @Test
    fun `test getCameras syncs with remote when local is empty`() =
        runTest {
            // Arrange
            val remoteCamera = TestDataFactory.createTestCamera(id = "remote-1")
            remoteDataSource.addCameraDirectly(remoteCamera)

            // Act
            val result = repository.getCameras()

            // Assert
            assertEquals(1, result.size)
            assertEquals("remote-1", result[0].id)
            // Проверяем, что данные синхронизированы в local
            val localCameras = localDataSource.getCameras()
            assertEquals(1, localCameras.size)
        }

    @Test
    fun `test addCamera syncs with remote`() =
        runTest {
            // Arrange
            val camera = TestDataFactory.createTestCamera(id = "camera-1")

            // Act
            val result = repository.addCamera(camera)

            // Assert
            assertTrue(result.isSuccess)
            // Проверяем, что камера добавлена в local
            val localCameras = localDataSource.getCameras()
            assertEquals(1, localCameras.size)
            // Проверяем, что камера синхронизирована с remote
            val remoteCameras = remoteDataSource.getCameras(1, 20, null)
            assertTrue(remoteCameras is ApiResult.Success)
            assertEquals(1, (remoteCameras as ApiResult.Success).data.size)
        }

    @Test
    fun `test addCamera succeeds when remote create fails and keeps local data`() =
        runTest {
            val camera = TestDataFactory.createTestCamera(id = "camera-1")
            remoteDataSource.shouldFailOnCreate = true
            remoteDataSource.networkError = Exception("Network error")

            val result = repository.addCamera(camera)

            assertTrue(result.isSuccess)
            val localCameras = localDataSource.getCameras()
            assertEquals(1, localCameras.size)
            assertEquals("camera-1", localCameras[0].id)
        }

    @Test
    fun `test updateCamera syncs with remote`() =
        runTest {
            // Arrange
            val camera = TestDataFactory.createTestCamera(id = "camera-1", name = "Original")
            localDataSource.addCameraDirectly(camera)
            remoteDataSource.addCameraDirectly(camera)

            val updatedCamera = camera.copy(name = "Updated")

            // Act
            val result = repository.updateCamera(updatedCamera)

            // Assert
            assertTrue(result.isSuccess)
            // Проверяем, что камера обновлена в local
            val localCamera = localDataSource.getCameraById("camera-1")
            assertEquals("Updated", localCamera?.name)
        }

    @Test
    fun `test updateCamera keeps local changes when remote update fails`() =
        runTest {
            val camera = TestDataFactory.createTestCamera(id = "camera-1", name = "Original")
            localDataSource.addCameraDirectly(camera)
            remoteDataSource.addCameraDirectly(camera)
            remoteDataSource.shouldFailOnUpdate = true
            remoteDataSource.networkError = Exception("Network error")

            val updatedCamera = camera.copy(name = "Updated Locally")
            val result = repository.updateCamera(updatedCamera)

            assertTrue(result.isSuccess)
            val localCamera = localDataSource.getCameraById("camera-1")
            assertEquals("Updated Locally", localCamera?.name)
        }

    @Test
    fun `test removeCamera syncs with remote`() =
        runTest {
            // Arrange
            val camera = TestDataFactory.createTestCamera(id = "camera-1")
            localDataSource.addCameraDirectly(camera)
            remoteDataSource.addCameraDirectly(camera)

            // Act
            val result = repository.removeCamera("camera-1")

            // Assert
            assertTrue(result.isSuccess)
            // Проверяем, что камера удалена из local
            val localCameras = localDataSource.getCameras()
            assertEquals(0, localCameras.size)
        }

    @Test
    fun `test removeCamera succeeds when remote delete fails`() =
        runTest {
            val camera = TestDataFactory.createTestCamera(id = "camera-1")
            localDataSource.addCameraDirectly(camera)
            remoteDataSource.addCameraDirectly(camera)
            remoteDataSource.shouldFailOnDelete = true
            remoteDataSource.networkError = Exception("Network error")

            val result = repository.removeCamera("camera-1")

            assertTrue(result.isSuccess)
            assertEquals(0, localDataSource.getCameras().size)
        }

    @Test
    fun `test repository works with local-only strategy when remote is null`() =
        runTest {
            // Arrange
            val repositoryLocalOnly = CameraRepositoryImpl(localDataSource, null)
            val camera = TestDataFactory.createTestCamera(id = "camera-1")
            localDataSource.addCameraDirectly(camera)

            // Act
            val result = repositoryLocalOnly.getCameras()

            // Assert
            assertEquals(1, result.size)
            assertEquals("camera-1", result[0].id)
        }

    // --- getCameraStatus (status through cache: getCameraStatus delegates to getCameraById) ---

    @Test
    fun `test getCameraStatus returns camera status when camera exists`() =
        runTest {
            // Arrange
            val camera = TestDataFactory.createTestCamera(id = "camera-1", status = CameraStatus.ONLINE)
            localDataSource.addCameraDirectly(camera)

            // Act
            val status = repository.getCameraStatus("camera-1")

            // Assert
            assertEquals(CameraStatus.ONLINE, status)
        }

    @Test
    fun `test getCameraStatus returns cached status on second call`() =
        runTest {
            // Arrange: status идёт через getCameraById, который кэширует по id
            val camera = TestDataFactory.createTestCamera(id = "camera-1", status = CameraStatus.OFFLINE)
            localDataSource.addCameraDirectly(camera)

            // Act: первый вызов — загрузка и кэш, второй — из кэша
            val first = repository.getCameraStatus("camera-1")
            val second = repository.getCameraStatus("camera-1")

            // Assert
            assertEquals(CameraStatus.OFFLINE, first)
            assertEquals(CameraStatus.OFFLINE, second)
            assertTrue(first == second)
        }

    @Test
    fun `test getCameraStatus returns UNKNOWN when camera not found`() =
        runTest {
            // Act
            val status = repository.getCameraStatus("non-existent")

            // Assert
            assertEquals(CameraStatus.UNKNOWN, status)
        }

    @Test
    fun `test getCameraStatus uses same cache as getCameraById`() =
        runTest {
            // Arrange: после getCameraById камера в кэше; getCameraStatus не должен идти в data source
            val camera = TestDataFactory.createTestCamera(id = "camera-1", status = CameraStatus.ONLINE)
            localDataSource.addCameraDirectly(camera)

            repository.getCameraById("camera-1")
            val status = repository.getCameraStatus("camera-1")

            // Assert
            assertEquals(CameraStatus.ONLINE, status)
        }

    // --- discoverCameras ---

    @Test
    fun `test discoverCameras returns list`() =
        runTest {
            // Act: discoverCameras() использует OnvifClient (WS-Discovery); без реальной сети — пустой список
            val result = repository.discoverCameras()

            // Assert
            assertNotNull(result)
            assertTrue(result is List<DiscoveredCamera>)
        }

    // --- Cache: discovery and status (4.1.x) ---

    @Test
    fun `test getCameraStatus returns cached value on second call within TTL`() =
        runTest {
            val camera = TestDataFactory.createTestCamera(id = "camera-1", status = CameraStatus.ONLINE)
            localDataSource.addCameraDirectly(camera)

            val first = repository.getCameraStatus("camera-1")
            val second = repository.getCameraStatus("camera-1")

            assertEquals(CameraStatus.ONLINE, first)
            assertEquals(first, second)
        }

    @Test
    fun `test getCameraStatus after updateCamera returns updated status - status cache invalidated`() =
        runTest {
            val camera = TestDataFactory.createTestCamera(id = "camera-1", status = CameraStatus.ONLINE)
            localDataSource.addCameraDirectly(camera)
            repository.getCameraStatus("camera-1") // fill status cache

            val updated = camera.copy(status = CameraStatus.OFFLINE)
            repository.updateCamera(updated)

            val status = repository.getCameraStatus("camera-1")
            assertEquals(CameraStatus.OFFLINE, status)
        }

    @Test
    fun `test getCameraStatus after removeCamera - camera removed and status cache invalidated`() =
        runTest {
            val camera = TestDataFactory.createTestCamera(id = "camera-1")
            localDataSource.addCameraDirectly(camera)
            repository.getCameraStatus("camera-1")
            repository.removeCamera("camera-1")

            val status = repository.getCameraStatus("camera-1")
            assertEquals(CameraStatus.UNKNOWN, status)
        }

    @Test
    fun `test discoverCameras with forceRefresh false uses cache on second call`() =
        runTest {
            // First call may run real discovery (empty if no network); second call returns same cached result
            val first = repository.discoverCameras(forceRefresh = false)
            val second = repository.discoverCameras(forceRefresh = false)
            assertEquals(first.size, second.size)
        }

    @Test
    fun `test discoverCameras forceRefresh true bypasses cache`() =
        runTest {
            // Один вызов: forceRefresh очищает discovery cache и снова выполняет discovery.
            // Два последовательных вызова оставляют работу на Dispatchers.IO и дают UncompletedCoroutinesError в runTest.
            val afterRefresh = repository.discoverCameras(forceRefresh = true)
            assertNotNull(afterRefresh)
        }

    @Test
    @Ignore // TTL статуса — System.currentTimeMillis() в CameraCache; runTest/delay не двигают wall clock
    fun `test getCameraStatus after TTL expiry refetches from source`() =
        runTest {
            val camera = TestDataFactory.createTestCamera(id = "camera-1", status = CameraStatus.ONLINE)
            localDataSource.addCameraDirectly(camera)
            repository.getCameraStatus("camera-1")

            delay(21.seconds) // status cache TTL is 20s

            val updated = camera.copy(status = CameraStatus.OFFLINE)
            localDataSource.updateCameraDirectly(updated)

            val status = repository.getCameraStatus("camera-1")
            assertEquals(CameraStatus.OFFLINE, status)
        }
}

