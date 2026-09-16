package com.company.ipcamera.core.security

import java.util.concurrent.ConcurrentHashMap

/**
 * JVM реализация защиты от брутфорс атак (in-memory, потокобезопасная).
 *
 * Поведение согласно [BruteForceConfig]: скользящее окно неудачных попыток,
 * блокировка при достижении порога, разблокировка по истечении срока.
 *
 * Персистентность (БД) — пост-релиз (Фаза 6), см. docs/stub-audit-active-2026-08-09.md.
 */
actual class BruteForceProtectionManager : BruteForceProtection {

    private val config: BruteForceConfig
    private val clock: () -> Long

    actual constructor() : this(BruteForceConfig())

    actual constructor(config: BruteForceConfig) : this(config, System::currentTimeMillis)

    constructor(config: BruteForceConfig, clock: () -> Long) {
        this.config = config
        this.clock = clock
    }

    private val userAttempts = ConcurrentHashMap<String, MutableList<Long>>()
    private val ipAttempts = ConcurrentHashMap<String, MutableList<Long>>()
    private val userBlockedUntil = ConcurrentHashMap<String, Long>()
    private val ipBlockedUntil = ConcurrentHashMap<String, Long>()

    private val lock = Any()

    /** Точка времени — подменяется в тестах через внутренний конструктор. */
    private fun currentTimeMillis(): Long = clock()

    override fun recordLoginAttempt(username: String?, ipAddress: String?, success: Boolean) {
        val now = currentTimeMillis()
        synchronized(lock) {
            if (success) {
                // Счётчики сбрасываются всегда; активная блокировка снимается только по истечении срока
                if (!isUserBlockedAt(username, now)) username?.let { userBlockedUntil.remove(it) }
                username?.let { userAttempts.remove(it) }
                if (!isIpBlockedAt(ipAddress, now)) ipAddress?.let { ipBlockedUntil.remove(it) }
                ipAddress?.let { ipAttempts.remove(it) }
                return
            }
            if (config.enableUsernameBlocking && username != null) {
                val attempts = userAttempts.getOrPut(username) { mutableListOf() }
                synchronized(attempts) { prune(attempts, now) }
                attempts.add(now)
                if (attempts.size >= config.maxFailedAttempts) {
                    userBlockedUntil[username] = now + config.lockoutDurationMinutes * 60_000L
                    userAttempts.remove(username)
                }
            }
            if (config.enableIpBlocking && ipAddress != null) {
                val attempts = ipAttempts.getOrPut(ipAddress) { mutableListOf() }
                prune(attempts, now)
                attempts.add(now)
                if (attempts.size >= config.maxFailedAttempts) {
                    ipBlockedUntil[ipAddress] = now + config.lockoutDurationMinutes * 60_000L
                    ipAttempts.remove(ipAddress)
                }
            }
        }
    }

    override fun isUserBlocked(username: String): Boolean =
        synchronized(lock) { isUserBlockedAt(username, currentTimeMillis()) }

    override fun isIpBlocked(ipAddress: String): Boolean =
        synchronized(lock) { isIpBlockedAt(ipAddress, currentTimeMillis()) }

    override fun getUserUnlockTime(username: String): Long? =
        synchronized(lock) { userBlockedUntil[username]?.takeIf { currentTimeMillis() < it } }

    override fun getIpUnlockTime(ipAddress: String): Long? =
        synchronized(lock) { ipBlockedUntil[ipAddress]?.takeIf { currentTimeMillis() < it } }

    override fun resetFailedAttempts(username: String) {
        synchronized(lock) {
            userAttempts.remove(username)
            userBlockedUntil.remove(username)
        }
    }

    override fun resetIpFailedAttempts(ipAddress: String) {
        synchronized(lock) {
            ipAttempts.remove(ipAddress)
            ipBlockedUntil.remove(ipAddress)
        }
    }

    override fun getFailedAttempts(username: String): Int =
        synchronized(lock) { userAttempts[username]?.size ?: 0 }

    private fun isUserBlockedAt(username: String?, now: Long): Boolean {
        val until = username?.let { userBlockedUntil[it] } ?: return false
        return now < until
    }

    private fun isIpBlockedAt(ipAddress: String?, now: Long): Boolean {
        val until = ipAddress?.let { ipBlockedUntil[it] } ?: return false
        return now < until
    }

    /** Оставить в окне только свежие попытки. */
    private fun prune(attempts: MutableList<Long>, now: Long) {
        val windowMs = config.timeWindowMinutes * 60_000L
        attempts.removeAll { it <= now - windowMs }
    }
}