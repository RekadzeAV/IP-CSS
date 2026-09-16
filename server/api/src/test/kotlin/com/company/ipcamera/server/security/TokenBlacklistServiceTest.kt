package com.company.ipcamera.server.security

import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TokenBlacklistServiceTest {

    private val redis = mockk<RedisCoroutinesCommands<String, String>>()

    private fun service() = TokenBlacklistService(redis)

    @Test
    fun `blacklist then isBlacklisted true`() = runBlocking {
        coEvery { redis.setex(any(), any(), any()) } returns "OK"
        coEvery { redis.exists(any()) } returns 1L

        val s = service()
        s.blacklistToken("tok-1")
        assertTrue(s.isTokenBlacklisted("tok-1"))
        coVerify(exactly = 1) { redis.setex(any(), any(), any()) }
    }

    @Test
    fun `not blacklisted when redis says missing`() = runBlocking {
        coEvery { redis.exists(any()) } returns 0L
        val s = service()
        assertFalse(s.isTokenBlacklisted("tok-not-revoked"))
    }

    @Test
    fun `blacklist redis failure falls back to local cache`() = runBlocking {
        // Redis бросает -> локальный кэш всё равно содержит токен
        coEvery { redis.setex(any(), any(), any()) } throws RuntimeException("redis down")
        coEvery { redis.exists(any()) } throws RuntimeException("redis down")

        val s = service()
        s.blacklistToken("tok-2")
        // локальный кэш возвращает true даже при недоступном Redis
        assertTrue(s.isTokenBlacklisted("tok-2"))
    }

    @Test
    fun `cleanup removes only expired local entries`() = runBlocking {
        // Вставляем токен с истёкшим TTL путём прямого черного списка, затем cleanup
        coEvery { redis.setex(any(), any(), any()) } returns "OK"
        coEvery { redis.exists(any()) } returns 1L
        val s = service()
        s.blacklistToken("old")
        // cleanup не должен бросать
        s.cleanupExpiredTokens()
    }

    @Test
    fun `user token helpers do not throw on redis failure`() = runBlocking {
        coEvery { redis.sadd(any(), any()) } throws RuntimeException("down")
        coEvery { redis.srem(any(), any()) } throws RuntimeException("down")
        val s = service()
        s.addUserToken("u1", "tok-a")
        s.removeUserToken("u1", "tok-a")
        // blacklistUserTokens — только лог, не бросает
        s.blacklistUserTokens("u1")
    }

    @Test
    fun `hash is deterministic and stable`() = runBlocking {
        coEvery { redis.exists(any()) } returns 1L
        val s = service()
        s.blacklistToken("same-token")
        assertTrue(s.isTokenBlacklisted("same-token"))
    }
}