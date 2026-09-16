package com.company.ipcamera.server.repository

import com.company.ipcamera.shared.domain.model.SettingsCategory
import com.company.ipcamera.shared.domain.model.SettingsType
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Тесты ServerSettingsRepositorySqlDelight на in-memory SQLite.
 * Полный контракт SettingsRepository: seed дефолтов, CRUD,
 * SystemSettings-агрегация, reset/export/import.
 */
class ServerSettingsRepositorySqlDelightTest {

    private fun createRepository(): ServerSettingsRepositorySqlDelight {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        val database = com.company.ipcamera.shared.data.local.createDatabaseSync(driver)
        return ServerSettingsRepositorySqlDelight(database)
    }

    @Test
    fun `seeds default settings on empty database`() = runBlocking {
        val repo = createRepository()

        val all = repo.getSettings()
        assertTrue(all.size >= 17, "Expected at least 17 defaults, got ${'$'}{all.size}")

        val categories = all.map { it.category }.toSet()
        assertTrue(
            categories.containsAll(
                setOf(
                    SettingsCategory.RECORDING,
                    SettingsCategory.STORAGE,
                    SettingsCategory.SECURITY,
                    SettingsCategory.NOTIFICATIONS,
                    SettingsCategory.NETWORK
                )
            )
        )

        // Дефолты не дублируются при повторном обращении
        val again = repo.getSettings()
        assertEquals(all.size, again.size)
    }

    @Test
    fun `getSetting returns null for unknown key`() = runBlocking {
        val repo = createRepository()
        assertNull(repo.getSetting("nonexistent_key"))
    }

    @Test
    fun `getSetting returns seeded default`() = runBlocking {
        val repo = createRepository()
        val setting = repo.getSetting("default_quality")
        assertNotNull(setting)
        assertEquals("HIGH", setting.value)
        assertEquals(SettingsCategory.RECORDING, setting.category)
    }

    @Test
    fun `updateSetting creates new setting with OTHER category`() = runBlocking {
        val repo = createRepository()
        val result = repo.updateSetting("custom_key", "custom_value")
        assertTrue(result.isSuccess)
        val setting = result.getOrThrow()
        assertEquals("custom_key", setting.key)
        assertEquals("custom_value", setting.value)

        val loaded = repo.getSetting("custom_key")
        assertNotNull(loaded)
        assertEquals("custom_value", loaded.value)
    }

    @Test
    fun `updateSetting preserves original category on existing key`() = runBlocking {
        val repo = createRepository()
        repo.updateSetting("default_quality", "LOW")
        val updated = repo.getSetting("default_quality")
        assertNotNull(updated)
        assertEquals("LOW", updated.value)
        assertEquals(SettingsCategory.RECORDING, updated.category)
    }

    @Test
    fun `updateSettings updates multiple and returns count`() = runBlocking {
        val repo = createRepository()
        val result = repo.updateSettings(mapOf("default_quality" to "MEDIUM", "brand_new" to "42"))
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow())
        assertEquals("MEDIUM", repo.getSetting("default_quality")?.value)
        assertEquals("42", repo.getSetting("brand_new")?.value)
    }

    @Test
    fun `deleteSetting removes setting and fails on unknown`() = runBlocking {
        val repo = createRepository()
        repo.updateSetting("temp_key", "v")

        val deleted = repo.deleteSetting("temp_key")
        assertTrue(deleted.isSuccess)
        assertNull(repo.getSetting("temp_key"))

        val missing = repo.deleteSetting("temp_key")
        assertTrue(missing.isFailure)
    }

    @Test
    fun `getSettings filters by category`() = runBlocking {
        val repo = createRepository()
        val network = repo.getSettings(SettingsCategory.NETWORK)
        assertTrue(network.isNotEmpty())
        assertTrue(network.all { it.category == SettingsCategory.NETWORK })
        assertTrue(network.any { it.key == "api_port" })
    }

    @Test
    fun `getSystemSettings aggregates from settings table with defaults`() = runBlocking {
        val repo = createRepository()
        val system = repo.getSystemSettings()
        assertNotNull(system)

        val recording = assertNotNull(system.recording)
        assertEquals("HIGH", recording.defaultQuality.name)
        assertEquals(3600L, recording.maxDuration)

        val security = assertNotNull(system.security)
        assertTrue(security.requireAuth)
        assertEquals(3600L, security.sessionTimeout)

        val network = assertNotNull(system.network)
        assertEquals(8080, network.apiPort)
    }

    @Test
    fun `updateSystemSettings persists recording and network values`() = runBlocking {
        val repo = createRepository()
        val original = assertNotNull(repo.getSystemSettings())

        val updatedRecording = original.recording?.copy(maxDuration = 7200, retentionDays = 7)
            ?: error("recording settings missing")
        val updatedNetwork = original.network?.copy(apiPort = 9090) ?: error("network settings missing")

        val result = repo.updateSystemSettings(
            original.copy(recording = updatedRecording, network = updatedNetwork)
        )
        assertTrue(result.isSuccess)

        val reloaded = assertNotNull(repo.getSystemSettings())
        assertEquals(7200L, reloaded.recording?.maxDuration)
        assertEquals(7, reloaded.recording?.retentionDays)
        assertEquals(9090, reloaded.network?.apiPort)
    }

    @Test
    fun `resetSettings restores defaults for category`() = runBlocking {
        val repo = createRepository()
        repo.updateSetting("default_quality", "LOW")
        assertEquals("LOW", repo.getSetting("default_quality")?.value)

        val result = repo.resetSettings(SettingsCategory.RECORDING)
        assertTrue(result.isSuccess)
        assertEquals("HIGH", repo.getSetting("default_quality")?.value)

        // Чужие категории не тронуты (custom key в OTHER остаётся)
        repo.updateSetting("other_cat_key", "keep")
        repo.resetSettings(SettingsCategory.RECORDING)
        assertEquals("keep", repo.getSetting("other_cat_key")?.value)
    }

    @Test
    fun `exportSettings returns key-value map including custom`() = runBlocking {
        val repo = createRepository()
        repo.updateSetting("exported_key", "exported_value")

        val exported = repo.exportSettings()
        assertTrue(exported.isSuccess)
        val map = exported.getOrThrow()
        assertTrue(map.containsKey("default_quality"))
        assertEquals("exported_value", map["exported_key"])
    }

    @Test
    fun `importSettings upserts keys`() = runBlocking {
        val repo = createRepository()
        val result = repo.importSettings(mapOf("imported_1" to "a", "imported_2" to "b"))
        assertTrue(result.isSuccess)
        assertEquals("a", repo.getSetting("imported_1")?.value)
        assertEquals("b", repo.getSetting("imported_2")?.value)

        // Повторный импорт обновляет существующие
        repo.importSettings(mapOf("imported_1" to "c"))
        assertEquals("c", repo.getSetting("imported_1")?.value)
    }

    @Test
    fun `settings types are preserved in mapping`() = runBlocking {
        val repo = createRepository()
        val setting = repo.getSetting("max_duration")
        assertNotNull(setting)
        assertIs<SettingsType>(setting.type)
        assertEquals(SettingsType.INTEGER, setting.type)
    }
}
