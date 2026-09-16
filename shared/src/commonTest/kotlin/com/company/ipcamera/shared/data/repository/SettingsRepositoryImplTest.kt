package com.company.ipcamera.shared.data.repository

import com.company.ipcamera.shared.domain.model.Settings
import com.company.ipcamera.shared.domain.model.SettingsCategory
import com.company.ipcamera.shared.domain.model.SettingsType
import com.company.ipcamera.shared.test.MockSettingsLocalDataSource
import com.company.ipcamera.shared.test.MockSettingsRemoteDataSource
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Тесты для SettingsRepositoryImpl
 */
class SettingsRepositoryImplTest {
    private lateinit var localDataSource: MockSettingsLocalDataSource
    private lateinit var remoteDataSource: MockSettingsRemoteDataSource
    private lateinit var repository: SettingsRepositoryImpl

    @BeforeTest
    fun setup() {
        localDataSource = MockSettingsLocalDataSource()
        remoteDataSource = MockSettingsRemoteDataSource()
        repository = SettingsRepositoryImpl(localDataSource, remoteDataSource)
    }

    @Test
    fun `test getSettings uses local-first strategy`() =
        runTest {
            val setting =
                Settings(
                    id = "setting-1",
                    category = SettingsCategory.RECORDING,
                    key = "quality",
                    value = "high",
                    type = SettingsType.STRING,
                    updatedAt = System.currentTimeMillis(),
                )
            localDataSource.addSettingDirectly(setting)

            val result = repository.getSettings(category = null)

            assertEquals(1, result.size)
            assertEquals("high", result.find { it.key == "quality" }?.value)
        }

    @Ignore
    @Test
    fun `test getSystemSettings`() =
        runTest {
            val systemSettings = com.company.ipcamera.shared.domain.model.SystemSettings()
            remoteDataSource.systemSettings = systemSettings

            val result = repository.getSystemSettings()

            assertNotNull(result)
        }

    @Test
    fun `test getSettings with remote fallback when local is empty`() =
        runTest {
            remoteDataSource.addSettingDirectly("remoteOnly", "enabled")

            val result = repository.getSettings(category = null)

            assertEquals(1, result.size)
            assertEquals("enabled", result.firstOrNull { it.key == "remoteOnly" }?.value)
            val localResult = localDataSource.getSettings()
            assertEquals(1, localResult.size)
        }

    @Test
    fun `test getSettings keeps local data when remote fails`() =
        runTest {
            val localSetting =
                Settings(
                    id = "setting-local",
                    category = SettingsCategory.NETWORK,
                    key = "network.sslEnabled",
                    value = "true",
                    type = SettingsType.BOOLEAN,
                    updatedAt = System.currentTimeMillis(),
                )
            localDataSource.addSettingDirectly(localSetting)
            remoteDataSource.shouldFailOnGet = true
            remoteDataSource.networkError = Exception("Network error")

            val result = repository.getSettings(category = null)

            assertEquals(1, result.size)
            assertEquals("network.sslEnabled", result[0].key)
        }

    @Test
    fun `test updateSystemSettings syncs with remote`() =
        runTest {
            val systemSettings = com.company.ipcamera.shared.domain.model.SystemSettings()

            val result = repository.updateSystemSettings(systemSettings)

            assertTrue(result.isSuccess)
        }

    @Test
    fun `test updateSystemSettings returns failure when remote update fails`() =
        runTest {
            val systemSettings = com.company.ipcamera.shared.domain.model.SystemSettings()
            remoteDataSource.shouldFailOnUpdate = true
            remoteDataSource.networkError = Exception("Network error")

            val result = repository.updateSystemSettings(systemSettings)

            assertTrue(result.isFailure)
        }

    @Test
    fun `test exportSettings`() =
        runTest {
            remoteDataSource.addSettingDirectly("key1", "value1")
            remoteDataSource.addSettingDirectly("key2", "value2")

            val result = repository.exportSettings()

            assertTrue(result.isSuccess)
            val map = result.getOrNull()
            assertNotNull(map)
            assertEquals("value1", map!!["key1"])
        }

    @Test
    fun `test importSettings`() =
        runTest {
            val settings = mapOf("key1" to "value1", "key2" to "value2")

            val result = repository.importSettings(settings)

            assertTrue(result.isSuccess)
        }

    @Test
    fun `test repository works with local-only strategy when remote is null`() =
        runTest {
            val repositoryLocalOnly = SettingsRepositoryImpl(localDataSource, null)
            val setting =
                Settings(
                    id = "setting-1",
                    category = SettingsCategory.RECORDING,
                    key = "quality",
                    value = "high",
                    type = SettingsType.STRING,
                    updatedAt = System.currentTimeMillis(),
                )
            localDataSource.addSettingDirectly(setting)

            val result = repositoryLocalOnly.getSettings(category = null)

            assertEquals(1, result.size)
            assertEquals("high", result.find { it.key == "quality" }?.value)
        }
}
