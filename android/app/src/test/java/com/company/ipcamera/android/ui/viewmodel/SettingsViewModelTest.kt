package com.company.ipcamera.android.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.company.ipcamera.shared.domain.model.Settings
import com.company.ipcamera.shared.domain.model.SettingsCategory
import com.company.ipcamera.shared.domain.model.SystemSettings
import com.company.ipcamera.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadSettings fills state with repository data`() = runTest(dispatcher) {
        val expected = listOf(
            Settings(
                id = "1",
                category = SettingsCategory.NETWORK,
                key = "api.port",
                value = "8080",
                updatedAt = 123L
            )
        )
        val repo = FakeSettingsRepository(settings = expected)
        val vm = SettingsViewModel(repo)

        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(expected, state.settings)
        assertEquals(null, state.error)
    }

    @Test
    fun `updateSetting updates item and resets saving flag`() = runTest(dispatcher) {
        val existing = Settings(
            id = "1",
            category = SettingsCategory.NETWORK,
            key = "api.port",
            value = "8080",
            updatedAt = 123L
        )
        val repo = FakeSettingsRepository(settings = listOf(existing))
        val vm = SettingsViewModel(repo)
        advanceUntilIdle()

        vm.updateSetting("api.port", "9090")
        advanceUntilIdle()

        val updated = vm.uiState.value.settings.first { it.key == "api.port" }
        assertEquals("9090", updated.value)
        assertFalse(vm.uiState.value.isSaving)
    }

    @Test
    fun `loadSettings sets error on repository failure`() = runTest(dispatcher) {
        val repo = FakeSettingsRepository(settings = emptyList(), failLoad = true)
        val vm = SettingsViewModel(repo)

        advanceUntilIdle()

        assertTrue(vm.uiState.value.error?.contains("load failed") == true)
        assertFalse(vm.uiState.value.isLoading)
    }
}

private class FakeSettingsRepository(
    settings: List<Settings>,
    private val failLoad: Boolean = false
) : SettingsRepository {
    private val store = settings.associateBy { it.key }.toMutableMap()

    override suspend fun getSettings(category: SettingsCategory?): List<Settings> {
        if (failLoad) error("load failed")
        return store.values.filter { category == null || it.category == category }
    }

    override suspend fun getSetting(key: String): Settings? = store[key]

    override suspend fun updateSettings(settings: Map<String, String>): Result<Int> {
        settings.forEach { (key, value) ->
            val current = store[key] ?: return@forEach
            store[key] = current.copy(value = value)
        }
        return Result.success(settings.size)
    }

    override suspend fun updateSetting(key: String, value: String): Result<Settings> {
        val current = store[key] ?: return Result.failure(IllegalArgumentException("not found"))
        val updated = current.copy(value = value)
        store[key] = updated
        return Result.success(updated)
    }

    override suspend fun deleteSetting(key: String): Result<Unit> {
        store.remove(key)
        return Result.success(Unit)
    }

    override suspend fun getSystemSettings(): SystemSettings? = null

    override suspend fun updateSystemSettings(settings: SystemSettings): Result<Unit> = Result.success(Unit)

    override suspend fun resetSettings(category: SettingsCategory?): Result<Unit> = Result.success(Unit)

    override suspend fun exportSettings(): Result<Map<String, String>> =
        Result.success(store.mapValues { it.value.value })

    override suspend fun importSettings(settings: Map<String, String>): Result<Unit> = Result.success(Unit)
}
