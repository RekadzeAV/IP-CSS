package com.company.ipcamera.core.security

import kotlin.test.Test
import kotlin.test.assertTrue

class PerformanceTest {

    private val hasher = PasswordHasherFactory.create()
    private val encryption = TokenEncryptionFactory.create()

    @Test
    fun testPasswordHashingPerformance() {
        val password = "TestPassword123!".toCharArray()
        val iterations = 100

        val startTime = System.currentTimeMillis()
        repeat(iterations) {
            hasher.hash(password)
        }
        val endTime = System.currentTimeMillis()

        val avgTime = (endTime - startTime) / iterations
        println("Average password hash time: ${avgTime}ms")

        // Should complete in under 200ms per hash (bcrypt)
        assertTrue(avgTime < 200, "Password hashing should complete in under 200ms, got ${avgTime}ms")
    }

    @Test
    fun testPasswordVerificationPerformance() {
        val password = "TestPassword123!".toCharArray()
        val hashed = hasher.hash(password)
        val iterations = 1000

        val startTime = System.currentTimeMillis()
        repeat(iterations) {
            hasher.verify(password, hashed)
        }
        val endTime = System.currentTimeMillis()

        val avgTime = (endTime - startTime) / iterations
        println("Average password verification time: ${avgTime}ms")

        // Should complete in under 200ms per verification
        assertTrue(avgTime < 200, "Password verification should complete in under 200ms, got ${avgTime}ms")
    }

    @Test
    fun testEncryptionPerformance() {
        val password = "TestPassword123!"
        val iterations = 1000

        val startTime = System.currentTimeMillis()
        repeat(iterations) {
            encryption.encrypt(password)
        }
        val endTime = System.currentTimeMillis()

        val avgTime = (endTime - startTime) / iterations
        println("Average encryption time: ${avgTime}ms")

        // Should complete in under 10ms per encryption
        assertTrue(avgTime < 10, "Encryption should complete in under 10ms, got ${avgTime}ms")
    }

    @Test
    fun testDecryptionPerformance() {
        val password = "TestPassword123!"
        val encrypted = encryption.encrypt(password)
        val iterations = 1000

        val startTime = System.currentTimeMillis()
        repeat(iterations) {
            encryption.decrypt(encrypted)
        }
        val endTime = System.currentTimeMillis()

        val avgTime = (endTime - startTime) / iterations
        println("Average decryption time: ${avgTime}ms")

        // Should complete in under 10ms per decryption
        assertTrue(avgTime < 10, "Decryption should complete in under 10ms, got ${avgTime}ms")
    }

    @Test
    fun testConcurrentPasswordHashing() {
        val password = "TestPassword123!".toCharArray()
        val threads = 10

        val startTime = System.currentTimeMillis()
        val jobs = (1..threads).map {
            Thread {
                hasher.hash(password)
            }.apply { start() }
        }

        jobs.forEach { it.join() }
        val endTime = System.currentTimeMillis()

        val totalTime = endTime - startTime
        println("Concurrent hashing (${threads} threads): ${totalTime}ms total")

        // Should complete all in reasonable time
        assertTrue(totalTime < 2000, "Concurrent hashing should complete in under 2s, got ${totalTime}ms")
    }

    @Test
    fun testMemoryUsageForLargePasswords() {
        val longPassword = "a".repeat(10000)
        val iterations = 100

        val startTime = System.currentTimeMillis()
        repeat(iterations) {
            hasher.hash(longPassword.toCharArray())
        }
        val endTime = System.currentTimeMillis()

        val avgTime = (endTime - startTime) / iterations
        println("Large password hash time: ${avgTime}ms")

        // Should handle large passwords reasonably
        assertTrue(avgTime < 500, "Large password hashing should complete in under 500ms, got ${avgTime}ms")
    }
}
