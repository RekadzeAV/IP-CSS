package com.company.ipcamera.desktop.data

import com.company.ipcamera.shared.domain.model.Camera
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * Менеджер кэширования данных для Desktop приложения
 *
 * Поддерживает:
 * - Кэширование списка камер
 * - TTL (Time To Live) для кэшированных данных
 * - Инвалидацию кэша
 */
class CacheManager {
    private val cache = ConcurrentHashMap<String, CacheEntry<*>>()
    private val timeSource = TimeSource.Monotonic

    /**
     * Получить данные из кэша
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? {
        val entry = cache[key] ?: return null

        // Проверяем TTL
        if (entry.isExpired()) {
            cache.remove(key)
            return null
        }

        return entry.data as? T
    }

    /**
     * Сохранить данные в кэш
     */
    fun <T> put(key: String, data: T, ttl: Duration = Duration.INFINITE) {
        cache[key] = CacheEntry(data, ttl, timeSource.markNow())
    }

    /**
     * Проверить наличие данных в кэше
     */
    fun contains(key: String): Boolean {
        val entry = cache[key] ?: return false
        if (entry.isExpired()) {
            cache.remove(key)
            return false
        }
        return true
    }

    /**
     * Инвалидировать кэш по ключу
     */
    fun invalidate(key: String) {
        cache.remove(key)
    }

    /**
     * Инвалидировать весь кэш
     */
    fun invalidateAll() {
        cache.clear()
    }

    /**
     * Очистить истекшие записи
     */
    fun cleanExpired() {
        val expiredKeys = cache.entries
            .filter { it.value.isExpired() }
            .map { it.key }
        expiredKeys.forEach { cache.remove(it) }
    }

    /**
     * Запись в кэше
     */
    private data class CacheEntry<T>(
        val data: T,
        val ttl: Duration,
        val timestamp: TimeSource.Monotonic.ValueTimeMark
    ) {
        fun isExpired(): Boolean {
            if (ttl == Duration.INFINITE) return false
            return timestamp.elapsedNow() >= ttl
        }
    }
}

/**
 * Ключи для кэша
 */
object CacheKeys {
    const val CAMERAS = "cameras"
    const val CAMERA_PREFIX = "camera_"
    const val SETTINGS = "settings"
    const val EVENTS = "events"
    const val RECORDINGS = "recordings"

    fun camera(id: String) = "$CAMERA_PREFIX$id"
}
