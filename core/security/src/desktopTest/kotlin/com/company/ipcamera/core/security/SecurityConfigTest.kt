package com.company.ipcamera.core.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SecurityConfigTest {

    @Test
    fun testDefaultSecurityConfig() {
        val config = SecurityConfig()

        assertTrue(config.enablePasswordEncryption)
        assertTrue(config.enableLocalDataEncryption)
        assertTrue(config.enableCertificatePinning)
        assertFalse(config.strictMode)
        assertTrue(config.logAttackAttempts)
        assertEquals("security/certificate-pinning.json", config.certificatePinningConfigPath)
    }

    @Test
    fun testProductionConfig() {
        val config = SecurityConfigFactory.createProduction()

        assertTrue(config.enablePasswordEncryption)
        assertTrue(config.enableLocalDataEncryption)
        assertTrue(config.enableCertificatePinning)
        assertTrue(config.strictMode)
        assertTrue(config.logAttackAttempts)
    }

    @Test
    fun testDevelopmentConfig() {
        val config = SecurityConfigFactory.createDevelopment()

        assertTrue(config.enablePasswordEncryption)
        assertFalse(config.enableLocalDataEncryption)
        assertFalse(config.enableCertificatePinning)
        assertFalse(config.strictMode)
        assertTrue(config.logAttackAttempts)
    }

    @Test
    fun testConfigBuilder() {
        val config = SecurityConfigFactory.create {
            enablePasswordEncryption(false)
            enableLocalDataEncryption(true)
            enableCertificatePinning(false)
            strictMode(true)
            logAttackAttempts(false)
            certificatePinningConfigPath("custom/path.json")
            keyStorePath("/custom/keystore")
        }

        assertFalse(config.enablePasswordEncryption)
        assertTrue(config.enableLocalDataEncryption)
        assertFalse(config.enableCertificatePinning)
        assertTrue(config.strictMode)
        assertFalse(config.logAttackAttempts)
        assertEquals("custom/path.json", config.certificatePinningConfigPath)
        assertEquals("/custom/keystore", config.keyStorePath)
    }

    @Test
    fun testConfigBuilderChain() {
        val config = SecurityConfigBuilder()
            .enablePasswordEncryption(true)
            .enableLocalDataEncryption(true)
            .enableCertificatePinning(true)
            .strictMode(true)
            .build()

        assertTrue(config.enablePasswordEncryption)
        assertTrue(config.enableLocalDataEncryption)
        assertTrue(config.enableCertificatePinning)
        assertTrue(config.strictMode)
    }

    @Test
    fun testConfigImmutability() {
        val original = SecurityConfig(strictMode = true)
        val modified = original.copy(strictMode = false)

        assertTrue(original.strictMode)
        assertFalse(modified.strictMode)
    }
}
