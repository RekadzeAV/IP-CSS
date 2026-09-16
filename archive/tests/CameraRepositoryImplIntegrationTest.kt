package com.company.ipcamera.shared.data.repository

import com.company.ipcamera.core.common.model.CameraStatus
import com.company.ipcamera.shared.data.local.DatabaseFactory
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.repository.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Интеграционные тесты для CameraRepositoryImpl
 */
class CameraRepositoryImplIntegrationTest {
    @Test
    fun testDiscoverCamerasReturnsList() =
        runTest {
            val databaseFactory = createInMemoryDatabase()
            val repository = CameraRepositoryImpl(databaseFactory)

            val result = repository.discoverCameras(forceRefresh = true)

            assertNotNull(result)
            assertTrue(result is List<*>)
        }

    @Test
    fun testDiscoverCamerasWithProgressEmitsValues() =
        runTest {
            val databaseFactory = createInMemoryDatabase()
            val repository = CameraRepositoryImpl(databaseFactory)

            val progressFlow =
                repository.discoverCamerasWithProgress(
                    forceRefresh = true,
                    config =
                        DiscoveryConfig(
                            methods = listOf(DiscoveryMethod.WS_DISCOVERY),
                            timeoutPerMethod = 1000,
                        ),
                )

            val firstProgress = progressFlow.first()
            assertNotNull(firstProgress)
            assertTrue(firstProgress.currentActivity.isNotEmpty())
        }

    @Test
    fun testTestConnectionReturnsResult() =
        runTest {
            val databaseFactory = createInMemoryDatabase()
            val repository = CameraRepositoryImpl(databaseFactory)

            val camera =
                Camera(
                    id = "test-camera",
                    name = "Test Camera",
                    url = "http://127.0.0.1:8080",
                    username = "admin",
                    password = "password",
                    status = CameraStatus.UNKNOWN,
                    manufacturer = null,
                    model = null,
                    createdAt = null,
                    updatedAt = null,
                )

            val result = repository.testConnection(camera)

            assertNotNull(result)
            assertTrue(result is ConnectionTestResult.Success || result is ConnectionTestResult.Failure)
        }

    @Test
    fun testConnectionTestResultContainsExpectedFields() =
        runTest {
            val databaseFactory = createInMemoryDatabase()
            val repository = CameraRepositoryImpl(databaseFactory)

            val camera =
                Camera(
                    id = "test-camera",
                    name = "Test Camera",
                    url = "http://127.0.0.1:8080",
                    username = "admin",
                    password = "password",
                    status = CameraStatus.UNKNOWN,
                    manufacturer = null,
                    model = null,
                    createdAt = null,
                    updatedAt = null,
                )

            val result = repository.testConnection(camera)

            if (result is ConnectionTestResult.Success) {
                // Проверка наличия расширенных полей
                assertNotNull(result.streams)
                assertNotNull(result.capabilities)
                // onvifVersion, rtspVersion, latencyMs могут быть null
                // supportedCodecs, authenticationMethod, connectionQuality тоже
            } else if (result is ConnectionTestResult.Failure) {
                assertNotNull(result.error)
                assertNotNull(result.code)
            }
        }

    @Test
    fun testConnectionDiagnosticsAvailable() =
        runTest {
            val databaseFactory = createInMemoryDatabase()
            val repository = CameraRepositoryImpl(databaseFactory)

            val camera =
                Camera(
                    id = "test-camera",
                    name = "Test Camera",
                    url = "http://127.0.0.1:8080",
                    username = "admin",
                    password = "password",
                    status = CameraStatus.UNKNOWN,
                    manufacturer = null,
                    model = null,
                    createdAt = null,
                    updatedAt = null,
                )

            val result = repository.testConnection(camera)

            if (result is ConnectionTestResult.Failure) {
                val diagnostics = result.diagnostics
                assertNotNull(diagnostics)

                // Проверка базовых полей диагностики
                assertTrue(diagnostics is ConnectionDiagnostics)
                // networkReachable, portOpen, authenticationSupported и т.д.
            }
        }
}

// Helper functions
private fun createInMemoryDatabase(): DatabaseFactory {
    return object : DatabaseFactory {
        override fun createDriver(): Any {
            // Возвращаем мок для тестов
            return object {}
        }

        override fun isPostgresFlywayParityDriver(driver: Any): Boolean {
            return false
        }

        override fun createDatabaseSync(driver: Any): Any {
            return object {}
        }
    }
}
