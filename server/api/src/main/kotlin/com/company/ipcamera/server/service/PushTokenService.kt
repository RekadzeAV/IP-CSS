package com.company.ipcamera.server.service

import com.company.ipcamera.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Контракт для автоматической очистки push-токенов, которые каналы доставки
 * признали невалидными (push-сервис вернул 404/410 — подписка отозвана).
 *
 * Реализуется [PushTokenService]; delivery-каналы могут опционально принимать
 * реализацию, чтобы не пытаться доставлять уведомления по мёртвым токенам.
 */
interface PushTokenPurger {
    /**
     * Удалить указанные токены у всех пользователей (сопоставление по точному значению).
     * @return количество фактически удалённых токенов.
     */
    fun purgeTokens(tokens: Collection<String>): Int
}

@Serializable
data class PushTokenEntry(
    val token: String,
    val platform: String,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Минимальный lifecycle сервис device tokens для push-канала (2.2.3).
 */
class PushTokenService(
    private val settingsRepository: SettingsRepository? = null
) : PushTokenPurger {
    private val tokensByUser = ConcurrentHashMap<String, ConcurrentHashMap<String, PushTokenEntry>>()
    private val loaded = AtomicBoolean(false)
    private val storageKey = "push_tokens_store_v1"
    private val json = Json { ignoreUnknownKeys = true }

    fun register(userId: String, token: String, platform: String): PushTokenEntry {
        ensureLoaded()
        val normalizedToken = token.trim()
        val normalizedPlatform = platform.trim().lowercase().ifBlank { "unknown" }
        val now = System.currentTimeMillis()
        val byToken = tokensByUser.getOrPut(userId) { ConcurrentHashMap() }
        val existing = byToken[normalizedToken]
        val entry = if (existing == null) {
            PushTokenEntry(
                token = normalizedToken,
                platform = normalizedPlatform,
                createdAt = now,
                updatedAt = now
            )
        } else {
            existing.copy(
                platform = normalizedPlatform,
                updatedAt = now
            )
        }
        byToken[normalizedToken] = entry
        persist()
        return entry
    }

    fun list(userId: String): List<PushTokenEntry> {
        ensureLoaded()
        return tokensByUser[userId]
            ?.values
            ?.sortedByDescending { it.updatedAt }
            .orEmpty()
    }

    fun listTokens(userId: String): List<String> =
        list(userId).map { it.token }

    fun revoke(userId: String, token: String): Boolean {
        ensureLoaded()
        val byToken = tokensByUser[userId] ?: return false
        val removed = byToken.remove(token.trim()) != null
        if (byToken.isEmpty()) tokensByUser.remove(userId)
        if (removed) persist()
        return removed
    }

    fun revokeMany(userId: String, tokens: List<String>): Int {
        ensureLoaded()
        if (tokens.isEmpty()) return 0
        val byToken = tokensByUser[userId] ?: return 0
        val normalized = tokens.map { it.trim() }.filter { it.isNotBlank() }.toSet()
        var removed = 0
        normalized.forEach { token ->
            if (byToken.remove(token) != null) removed++
        }
        if (byToken.isEmpty()) tokensByUser.remove(userId)
        if (removed > 0) persist()
        return removed
    }

    /**
     * Автоочистка невалидных токенов по всем пользователям ([PushTokenPurger]).
     * Вызывается delivery-каналами, когда push-сервис вернул 404/410 для токена.
     */
    override fun purgeTokens(tokens: Collection<String>): Int {
        if (tokens.isEmpty()) return 0
        ensureLoaded()
        val normalized = tokens.map { it.trim() }.filter { it.isNotBlank() }.toSet()
        if (normalized.isEmpty()) return 0
        var removed = 0
        tokensByUser.values.forEach { byToken ->
            normalized.forEach { token ->
                if (byToken.remove(token) != null) removed++
            }
        }
        tokensByUser.entries.removeIf { it.value.isEmpty() }
        if (removed > 0) persist()
        return removed
    }

    private fun ensureLoaded() {
        if (!loaded.compareAndSet(false, true)) return
        val repo = settingsRepository ?: return
        runBlocking {
            runCatching {
                repo.getSetting(storageKey)?.value
            }.getOrNull()?.takeIf { it.isNotBlank() }?.let { raw ->
                runCatching {
                    json.decodeFromString<Map<String, List<PushTokenEntry>>>(raw)
                }.onSuccess { store ->
                    store.forEach { (userId, entries) ->
                        val byToken = tokensByUser.getOrPut(userId) { ConcurrentHashMap() }
                        entries.forEach { entry -> byToken[entry.token] = entry }
                    }
                }
            }
        }
    }

    private fun persist() {
        val repo = settingsRepository ?: return
        val snapshot: Map<String, List<PushTokenEntry>> = tokensByUser.mapValues { (_, byToken) ->
            byToken.values.sortedByDescending { it.updatedAt }
        }
        val raw = json.encodeToString(snapshot)
        runBlocking {
            runCatching {
                repo.updateSetting(storageKey, raw)
            }
        }
    }
}
