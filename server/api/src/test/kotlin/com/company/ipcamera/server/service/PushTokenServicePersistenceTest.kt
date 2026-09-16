package com.company.ipcamera.server.service

import com.company.ipcamera.shared.domain.model.Settings
import com.company.ipcamera.shared.domain.model.SettingsCategory
import com.company.ipcamera.shared.domain.model.SystemSettings
import com.company.ipcamera.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PushTokenServicePersistenceTest {

    @Test
    fun `register persists tokens into settings repository`() = runBlocking {
        val settingsRepository = InMemorySettingsRepository()
        val service = PushTokenService(settingsRepository = settingsRepository)

        service.register(userId = "u1", token = "t1", platform = "android")

        val raw = settingsRepository.getSetting("push_tokens_store_v1")?.value.orEmpty()
        assertTrue(raw.contains("\"u1\""))
        assertTrue(raw.contains("\"t1\""))
    }

    @Test
    fun `service restores tokens from settings repository on startup`() = runBlocking {
        val settingsRepository = InMemorySettingsRepository()
        val first = PushTokenService(settingsRepository = settingsRepository)
        first.register(userId = "u1", token = "t1", platform = "android")
        first.register(userId = "u1", token = "t2", platform = "ios")

        val restored = PushTokenService(settingsRepository = settingsRepository)
        val tokens = restored.listTokens("u1")

        assertEquals(2, tokens.size)
        assertTrue(tokens.contains("t1"))
        assertTrue(tokens.contains("t2"))
    }

    @Test
    fun `purgeTokens removes token across all users and persists`() = runBlocking {
        val settingsRepository = InMemorySettingsRepository()
        val service = PushTokenService(settingsRepository = settingsRepository)
        service.register(userId = "u1", token = "bad", platform = "web")
        service.register(userId = "u2", token = "bad", platform = "web")
        service.register(userId = "u1", token = "good", platform = "android")

        val removed = service.purgeTokens(listOf("bad"))

        assertEquals(2, removed, "токен должен быть удалён у обоих пользователей")
        assertTrue(service.listTokens("u1").contains("good"))
        assertTrue(!service.listTokens("u1").contains("bad"))
        assertTrue(!service.listTokens("u2").contains("bad"))

        // Персистентность: после "перезапуска" токен не восстанавливается.
        val restored = PushTokenService(settingsRepository = settingsRepository)
        assertTrue(!restored.listTokens("u1").contains("bad"))
        assertTrue(!restored.listTokens("u2").contains("bad"))
    }

    @Test
    fun `purgeTokens returns zero for unknown tokens`() = runBlocking {
        val settingsRepository = InMemorySettingsRepository()
        val service = PushTokenService(settingsRepository = settingsRepository)
        service.register(userId = "u1", token = "t1", platform = "android")

        val removed = service.purgeTokens(listOf("unknown", "   "))

        assertEquals(0, removed)
        assertTrue(service.listTokens("u1").contains("t1"))
    }

    @Test
    fun `purgeTokens handles empty input`() = runBlocking {
        val service = PushTokenService(settingsRepository = InMemorySettingsRepository())
        assertEquals(0, service.purgeTokens(emptyList()))
    }
}

private class InMemorySettingsRepository : SettingsRepository {
    private val settings = ConcurrentHashMap<String, Settings>()

    override suspend fun getSettings(category: SettingsCategory?): List<Settings> =
        settings.values.filter { category == null || it.category == category }

    override suspend fun getSetting(key: String): Settings? = settings[key]

    override suspend fun updateSettings(settings: Map<String, String>): Result<Int> {
        settings.forEach { (key, value) -> updateSetting(key, value) }
        return Result.success(settings.size)
    }

    override suspend fun updateSetting(key: String, value: String): Result<Settings> {
        val now = System.currentTimeMillis()
        val updated = this.settings[key]?.copy(value = value, updatedAt = now) ?: Settings(
            id = UUID.randomUUID().toString(),
            category = SettingsCategory.OTHER,
            key = key,
            value = value,
            updatedAt = now
        )
        this.settings[key] = updated
        return Result.success(updated)
    }

    override suspend fun deleteSetting(key: String): Result<Unit> {
        this.settings.remove(key)
        return Result.success(Unit)
    }

    override suspend fun getSystemSettings(): SystemSettings? = null

    override suspend fun updateSystemSettings(settings: SystemSettings): Result<Unit> = Result.success(Unit)

    override suspend fun resetSettings(category: SettingsCategory?): Result<Unit> = Result.success(Unit)

    override suspend fun exportSettings(): Result<Map<String, String>> =
        Result.success(settings.values.associate { it.key to it.value })

    override suspend fun importSettings(settings: Map<String, String>): Result<Unit> {
        updateSettings(settings)
        return Result.success(Unit)
    }
}
