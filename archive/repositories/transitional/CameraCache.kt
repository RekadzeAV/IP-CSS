package com.company.ipcamera.shared.data.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

internal data class CameraCacheEntry<T>(
    val data: T,
    val timestamp: Long,
    val expirationTime: Duration,
)

/**
 * In-memory кэш с TTL для использования в CameraRepositoryImpl и CameraRepositoryImplV2.
 */
internal class CameraCache(
    private val maxSize: Int = 1000,
    private val expirationTime: Duration = 5.minutes,
) {
    private val cache = mutableMapOf<String, CameraCacheEntry<*>>()
    private val mutex = Mutex()

    suspend fun <T> get(key: String): T? =
        mutex.withLock {
            val entry = cache[key] as? CameraCacheEntry<T> ?: return@withLock null
            val now = Clock.System.now().toEpochMilliseconds()
            if (now - entry.timestamp > entry.expirationTime.inWholeMilliseconds) {
                cache.remove(key)
                return@withLock null
            }
            return@withLock entry.data
        }

    suspend fun <T> put(
        key: String,
        data: T,
    ) = mutex.withLock {
        if (cache.size >= maxSize) {
            var oldestKey: String? = null
            var oldestTs = Long.MAX_VALUE
            cache.forEach { (k, v) ->
                val ts = (v as? CameraCacheEntry<*>)?.timestamp ?: 0L
                if (ts < oldestTs) {
                    oldestTs = ts
                    oldestKey = k
                }
            }
            oldestKey?.let { cache.remove(it) }
        }
        cache[key] =
            CameraCacheEntry(
                data = data,
                timestamp = Clock.System.now().toEpochMilliseconds(),
                expirationTime = expirationTime,
            )
    }

    suspend fun clear() =
        mutex.withLock {
            cache.clear()
        }

    suspend fun remove(key: String) =
        mutex.withLock {
            cache.remove(key)
        }

    suspend fun invalidateAll() =
        mutex.withLock {
            cache.clear()
        }
}
