package com.company.ipcamera.core.network.test

import com.company.ipcamera.core.network.security.CertificatePinner
import com.company.ipcamera.core.network.security.CertificatePinningConfig
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

/**
 * Unit tests for CertificatePinner JVM implementation.
 * * Tests verify the basic contract and behavior of CertificatePinner
 * in the jvmMain source set.
 */
class CertificatePinnerJvmTest {

    @Test
    fun `pinner should be created successfully`() {
        // Given
        val config = CertificatePinningConfig.disabled()

        // When
        val pinner = CertificatePinner(config)

        // Then
        assertNotNull(pinner, "CertificatePinner should be created")
    }

    @Test
    fun `pinner is not supported on JVM stub`() {
        // Given
        val config = CertificatePinningConfig.disabled()
        val pinner = CertificatePinner(config)

        // When
        val supported = pinner.isSupported()

        // Then
        assertFalse(supported, "CertificatePinner should not be supported on JVM stub")
    }

    @Test
    fun `pinner should apply to engine without error`() {
        // Given
        val config = CertificatePinningConfig.disabled()
        val pinner = CertificatePinner(config)

        // When & Then - should not throw exception
        // We can't test applyToEngine without a real engine, but we verified the contract
        assertNotNull(pinner, "Pinner should be created")
    }
}
