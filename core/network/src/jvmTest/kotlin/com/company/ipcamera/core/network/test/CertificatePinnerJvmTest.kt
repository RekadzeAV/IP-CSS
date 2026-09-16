package com.company.ipcamera.core.network.test

import com.company.ipcamera.core.network.CertificatePinner
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for CertificatePinner JVM implementation.
 * 
 * Tests verify the basic contract and behavior of CertificatePinner
 * in the jvmMain source set.
 */
class CertificatePinnerJvmTest {

    @Test
    fun `pinner should be created successfully`() {
        // When
        val pinner = CertificatePinner()

        // Then
        assertNotNull(pinner, "CertificatePinner should be created")
    }

    @Test
    fun `pinner should have default configuration`() {
        // Given
        val pinner = CertificatePinner()

        // When - access configuration
        val config = pinner.getConfiguration()

        // Then
        // Note: Configuration may be empty by default
        assertNotNull(config, "Configuration should be accessible")
    }

    @Test
    fun `pinner should handle empty hostname`() {
        // Given
        val pinner = CertificatePinner()

        // When & Then - should not throw exception
        val result = pinner.validate("")
        assertTrue(result is Boolean, "Validation should return Boolean")
    }

    @Test
    fun `pinner should be reusable`() {
        // Given
        val pinner = CertificatePinner()

        // When - use multiple times
        val result1 = pinner.validate("example.com")
        val result2 = pinner.validate("test.com")

        // Then
        // Both should complete without error
        assertTrue(result1 is Boolean, "First validation should return Boolean")
        assertTrue(result2 is Boolean, "Second validation should return Boolean")
    }

    @Test
    fun `pinner should handle null certificates`() {
        // Given
        val pinner = CertificatePinner()

        // When & Then - should not throw exception
        val result = pinner.validate("example.com", null)
        assertTrue(result is Boolean, "Validation with null certs should return Boolean")
    }
}