package com.company.ipcamera.desktop.ui.viewmodel

import com.company.ipcamera.shared.domain.model.NetworkSettings
import com.company.ipcamera.shared.domain.model.NotificationSystemSettings
import com.company.ipcamera.shared.domain.model.PasswordPolicy
import com.company.ipcamera.shared.domain.model.RecordingSystemSettings
import com.company.ipcamera.shared.domain.model.SecuritySettings
import com.company.ipcamera.shared.domain.model.Settings
import com.company.ipcamera.shared.domain.model.SettingsCategory
import com.company.ipcamera.shared.domain.model.StorageSettings
import com.company.ipcamera.shared.domain.model.SystemSettings
import com.company.ipcamera.shared.domain.usecase.GetSettingsUseCase
import com.company.ipcamera.shared.domain.usecase.UpdateSettingUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var getSettingsUseCase: GetSettingsUseCase
    private lateinit var updateSettingUseCase: UpdateSettingUseCase
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        getSettingsUseCase = mock()
        updateSettingUseCase = mock()
    }

    @Test
    fun `loadSettings should emit Loading then Success when settings are loaded`() = runTest(testDispatcher) {
        // Arrange
        val settings = listOf(
            Settings(
                id = "s1",
                category = SettingsCategory.SYSTEM,
                key = "test_key",
                value = "test_value",
                updatedAt = System.currentTimeMillis()
            )
        )
        val systemSettings = createSystemSettings()

        whenever(getSettingsUseCase.invoke(anyOrNull())).thenReturn(settings)
        whenever(getSettingsUseCase.getSystemSettings()).thenReturn(systemSettings)

        val viewModel = SettingsViewModel(
            getSettingsUseCase = getSettingsUseCase,
            updateSettingUseCase = updateSettingUseCase,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )

        // Act
        viewModel.loadSettings()
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertTrue(state is SettingsUiState.Success)
        assertEquals(settings, (state as SettingsUiState.Success).settings)
        assertEquals(systemSettings, (state as SettingsUiState.Success).systemSettings)
    }

    @Test
    fun `loadSettings should emit Error when exception occurs`() = runTest(testDispatcher) {
        // Arrange
        val errorMessage = "Network error"
        whenever(getSettingsUseCase.invoke(anyOrNull())).thenThrow(RuntimeException(errorMessage))

        val viewModel = SettingsViewModel(
            getSettingsUseCase = getSettingsUseCase,
            updateSettingUseCase = updateSettingUseCase,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )

        // Act
        viewModel.loadSettings()
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertTrue(state is SettingsUiState.Error)
    }

    @Test
    fun `updateSetting should call use case and reload settings`() = runTest(testDispatcher) {
        // Arrange
        val key = "test_key"
        val value = "new_value"
        val settings = listOf(setting(key = key, value = value))
        val systemSettings = createSystemSettings()

        whenever(updateSettingUseCase.invoke(key, value))
            .thenReturn(Result.success(setting(key = key, value = value)))
        whenever(getSettingsUseCase.invoke(anyOrNull())).thenReturn(settings)
        whenever(getSettingsUseCase.getSystemSettings()).thenReturn(systemSettings)

        val viewModel = SettingsViewModel(
            getSettingsUseCase = getSettingsUseCase,
            updateSettingUseCase = updateSettingUseCase,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )

        // Act
        var onSuccessCalled = false
        viewModel.updateSetting(key, value) {
            onSuccessCalled = true
        }
        advanceUntilIdle()

        // Assert
        verify(updateSettingUseCase).invoke(key, value)
        assertTrue(viewModel.uiState.value is SettingsUiState.Success)
    }

    @Test
    fun `updateSettings should call use case and reload settings`() = runTest(testDispatcher) {
        // Arrange
        val settingsMap = mapOf(
            "key1" to "value1",
            "key2" to "value2"
        )
        val settings = listOf(
            setting(key = "key1", value = "value1"),
            setting(key = "key2", value = "value2")
        )
        val systemSettings = createSystemSettings()

        whenever(updateSettingUseCase.updateSettings(settingsMap))
            .thenReturn(Result.success(2))
        whenever(getSettingsUseCase.invoke(anyOrNull())).thenReturn(settings)
        whenever(getSettingsUseCase.getSystemSettings()).thenReturn(systemSettings)

        val viewModel = SettingsViewModel(
            getSettingsUseCase = getSettingsUseCase,
            updateSettingUseCase = updateSettingUseCase,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )

        // Act
        var onSuccessCalled = false
        viewModel.updateSettings(settingsMap) {
            onSuccessCalled = true
        }
        advanceUntilIdle()

        // Assert
        verify(updateSettingUseCase).updateSettings(settingsMap)
        assertTrue(viewModel.uiState.value is SettingsUiState.Success)
    }

    @Test
    fun `setCategory should reload settings with category filter`() = runTest(testDispatcher) {
        // Arrange
        val category = SettingsCategory.SYSTEM
        val settings = listOf(
            Settings(
                id = "s1",
                category = category,
                key = "test_key",
                value = "test_value",
                updatedAt = System.currentTimeMillis()
            )
        )
        val systemSettings = createSystemSettings()

        whenever(getSettingsUseCase.invoke(category)).thenReturn(settings)
        whenever(getSettingsUseCase.getSystemSettings()).thenReturn(systemSettings)

        val viewModel = SettingsViewModel(
            getSettingsUseCase = getSettingsUseCase,
            updateSettingUseCase = updateSettingUseCase,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )

        // Act
        viewModel.setCategory(category)
        advanceUntilIdle()

        // Assert
        assertEquals(category, viewModel.selectedCategory)
        verify(getSettingsUseCase).invoke(category)
    }

    @Test
    fun `dismissSaveSuccess should hide success message`() = runTest(testDispatcher) {
        // Arrange
        val settings = emptyList<Settings>()
        val systemSettings = createSystemSettings()

        whenever(getSettingsUseCase.invoke(anyOrNull())).thenReturn(settings)
        whenever(getSettingsUseCase.getSystemSettings()).thenReturn(systemSettings)
        whenever(updateSettingUseCase.invoke(any(), any()))
            .thenReturn(Result.success(setting()))

        val viewModel = SettingsViewModel(
            getSettingsUseCase = getSettingsUseCase,
            updateSettingUseCase = updateSettingUseCase,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )

        viewModel.updateSetting("key", "value")
        advanceUntilIdle()
        assertTrue(viewModel.showSaveSuccess)

        // Act
        viewModel.dismissSaveSuccess()

        // Assert
        assertTrue(!viewModel.showSaveSuccess)
    }

    private fun setting(
        key: String = "k",
        value: String = "v",
        category: SettingsCategory = SettingsCategory.SYSTEM
    ): Settings {
        return Settings(
            id = "id-$key",
            category = category,
            key = key,
            value = value,
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun createSystemSettings(): SystemSettings {
        return SystemSettings(
            recording = RecordingSystemSettings(maxDuration = 3600, autoDelete = true, retentionDays = 14),
            storage = StorageSettings(maxStorageSize = 100_000_000L, currentStorageUsed = 10_000_000L, storagePath = "/tmp"),
            notifications = NotificationSystemSettings(pushEnabled = true),
            security = SecuritySettings(
                requireAuth = true,
                sessionTimeout = 3600,
                passwordPolicy = PasswordPolicy(minLength = 8)
            ),
            network = NetworkSettings(apiPort = 8080, websocketPort = 8081, allowRemoteAccess = false, sslEnabled = false)
        )
    }
}
