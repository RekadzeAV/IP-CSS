package com.company.ipcamera.core.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Тесты логики блокировки BruteForceProtectionManager (JVM-actual).
 * Время детерминировано — часы подменяются через internal-конструктор.
 */
class BruteForceProtectionManagerTest {

    private var nowMs: Long = 1_000_000L

    private fun manager(
        maxAttempts: Int = 5,
        lockoutMinutes: Long = 30,
        windowMinutes: Long = 15
    ): BruteForceProtectionManager = BruteForceProtectionManager(
        BruteForceConfig(
            maxFailedAttempts = maxAttempts,
            lockoutDurationMinutes = lockoutMinutes,
            timeWindowMinutes = windowMinutes
        )
    ) { nowMs }

    @Test
    fun `не блокирует пока порог не достигнут`() {
        val m = manager(maxAttempts = 5)
        repeat(4) { m.recordLoginAttempt("user", "10.0.0.1", success = false) }
        assertFalse(m.isUserBlocked("user"))
        assertFalse(m.isIpBlocked("10.0.0.1"))
        assertEquals(4, m.getFailedAttempts("user"))
    }

    @Test
    fun `блокирует пользователя и ip при достижении порога`() {
        val m = manager(maxAttempts = 5, lockoutMinutes = 30)
        repeat(5) { m.recordLoginAttempt("user", "10.0.0.1", success = false) }
        assertTrue(m.isUserBlocked("user"))
        assertTrue(m.isIpBlocked("10.0.0.1"))
        assertEquals(nowMs + 30L * 60_000L, m.getUserUnlockTime("user"))
        assertEquals(nowMs + 30L * 60_000L, m.getIpUnlockTime("10.0.0.1"))
    }

    @Test
    fun `блокировка снимается по истечении срока`() {
        val m = manager(maxAttempts = 3, lockoutMinutes = 10)
        repeat(3) { m.recordLoginAttempt("user", "10.0.0.1", success = false) }
        assertTrue(m.isUserBlocked("user"))
        nowMs += 10L * 60_000L + 1
        assertFalse(m.isUserBlocked("user"))
        assertNull(m.getUserUnlockTime("user"))
    }

    @Test
    fun `успешный вход сбрасывает счётчики но не активную блокировку`() {
        val m = manager(maxAttempts = 3)
        repeat(2) { m.recordLoginAttempt("user", "10.0.0.1", success = false) }
        m.recordLoginAttempt("user", "10.0.0.1", success = true)
        assertEquals(0, m.getFailedAttempts("user"))
        assertFalse(m.isUserBlocked("user"))

        repeat(3) { m.recordLoginAttempt("user", "10.0.0.2", success = false) }
        assertTrue(m.isUserBlocked("user"))
        m.recordLoginAttempt("user", "10.0.0.2", success = true)
        assertTrue(m.isUserBlocked("user"), "активная блокировка не должна сниматься успешным входом")
    }

    @Test
    fun `окно времени устаревшие попытки не учитываются`() {
        val m = manager(maxAttempts = 3, windowMinutes = 15)
        m.recordLoginAttempt("user", null, success = false)
        m.recordLoginAttempt("user", null, success = false)
        nowMs += 16L * 60_000L // обе попытки вышли из окна
        m.recordLoginAttempt("user", null, success = false)
        assertFalse(m.isUserBlocked("user"))
        assertEquals(1, m.getFailedAttempts("user"))
    }

    @Test
    fun `resetFailedAttempts снимает блокировку`() {
        val m = manager(maxAttempts = 2)
        repeat(2) { m.recordLoginAttempt("user", null, success = false) }
        assertTrue(m.isUserBlocked("user"))
        m.resetFailedAttempts("user")
        assertFalse(m.isUserBlocked("user"))
    }

    @Test
    fun `resetIpFailedAttempts снимает ip-блокировку`() {
        val m = manager(maxAttempts = 2)
        repeat(2) { m.recordLoginAttempt(null, "10.0.0.9", success = false) }
        assertTrue(m.isIpBlocked("10.0.0.9"))
        m.resetIpFailedAttempts("10.0.0.9")
        assertFalse(m.isIpBlocked("10.0.0.9"))
    }

    @Test
    fun `отключенная блокировка по ip не блокирует ip`() {
        val m = BruteForceProtectionManager(
            BruteForceConfig(maxFailedAttempts = 2, enableIpBlocking = false)
        ) { nowMs }
        repeat(4) { m.recordLoginAttempt(null, "10.0.0.9", success = false) }
        assertFalse(m.isIpBlocked("10.0.0.9"))
    }

    @Test
    fun `null-username и null-ip не приводят к блокировкам`() {
        val m = manager(maxAttempts = 1)
        repeat(5) { m.recordLoginAttempt(null, null, success = false) }
        assertFalse(m.isUserBlocked(""))
        assertFalse(m.isIpBlocked(""))
    }

    @Test
    fun `фабрика применяет конфиг`() {
        val m = BruteForceProtectionFactory.create(BruteForceConfig(maxFailedAttempts = 2))
        repeat(2) { m.recordLoginAttempt("user", null, success = false) }
        assertTrue(m.isUserBlocked("user"))
    }
}
